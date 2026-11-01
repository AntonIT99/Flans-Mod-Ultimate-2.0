package com.flansmodultimate.common.raytracing;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.api.entity.HumanoidBody;
import com.flansmodultimate.api.entity.IFlanHumanoidBody;
import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.driveables.parts.PartBodies;
import com.flansmodultimate.common.driveables.parts.PartBody;
import com.flansmodultimate.common.entity.*;
import com.flansmodultimate.common.guns.ShootingHelper;
import com.flansmodultimate.common.raytracing.hits.*;
import com.flansmodultimate.common.types.*;
import com.flansmodultimate.hooks.ClientHooks;
import com.flansmodultimate.util.ModUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Raytracer
{
    /**
     * Upper bound on how far past the segment a non-driveable target is searched. It covers
     * hitbox inflation, the swept box of a fast mover and lag-compensated player snapshots.
     */
    private static final double ENTITY_SEARCH_MARGIN = 10D;
    /** Fastest a target is expected to move per tick, which bounds how far a rewound target can be from where it is now */
    private static final double REWIND_TARGET_SPEED = 0.5D;

    /**
     * @param rewindTicks
     *            how far behind the server the shooter saw its targets, from {@link LagCompensation#rewindTicks}; 0 for a shot aimed on the server
     */
    public static List<BulletHit> raytraceShot(Level level, @Nullable Bullet bullet, @Nullable LivingEntity owner, List<Entity> entitiesToIgnore, Vec3 origin, Vec3 motion, int rewindTicks,
        float gunPenetration, float bulletHitBoxSize, BulletType type)
    {
        // Create a list for all bullet hits
        List<BulletHit> hits = new ArrayList<>();

        final Vec3 destination = origin.add(motion);
        final long gameTime = level.getGameTime();

        // Query only entities along the ray segment. Driveable hulls reach well past
        // their compact entity box, so they get a separate class-filtered query sized
        // by the largest hull radius instead of widening the search for every entity.
        AABB segment = new AABB(origin, destination);
        double margin = Math.min(Math.max(motion.length(), (rewindTicks + LagCompensation.WINDOW_TICKS) * REWIND_TARGET_SPEED), ENTITY_SEARCH_MARGIN);
        AABB search = segment.inflate(margin);
        List<Entity> candidates = new ArrayList<>(ModUtils.queryEntities(level, bullet, search, entity -> !(entity instanceof Driveable)));
        candidates.addAll(ModUtils.queryEntities(level, bullet, segment.inflate(DriveableType.getMaxBulletDetectionRadius()), Driveable.class, null));

        checkPartBodyHits(level, entitiesToIgnore, origin, motion, owner, hits);

        for (Entity entity : candidates)
        {
            if (!entity.isAlive() || (entity instanceof LivingEntity living && living.isDeadOrDying()) || entitiesToIgnore.contains(entity) || !ModUtils.canEntityBeHitByBullets(entity)
                || PartBodies.of(entity) != null)
                continue;

            if (entity instanceof Driveable driveable)
                checkDriveableHit(driveable, origin, motion, owner, hits);
            else if (entity instanceof Player player)
                checkPlayerHit(player, origin, motion, gameTime, rewindTicks, bulletHitBoxSize, hits);
            else
                checkEntityHit(entity, origin, motion, gameTime, rewindTicks, bulletHitBoxSize, hits);
        }

        hits = raytraceBlock(level, origin, Vec3.ZERO, motion, motion.normalize().scale(0.5), hits, gunPenetration, null, type);

        // We hit something
        if (!hits.isEmpty())
        {
            // Sort the hits according to the intercept position
            Collections.sort(hits);
        }

        return hits;
    }

    private static void checkDriveableHit(Driveable driveable, Vec3 origin, Vec3 motion, @Nullable LivingEntity owner, List<BulletHit> hits)
    {
        if (driveable.getConfigType() == null || owner != null && driveable.isPartOfThis(owner))
            return;

        double speed = motion.length();

        // If this bullet is within the driveable's detection range
        if (driveable.distanceToSqr(origin) <= (driveable.getConfigType().getBulletDetectionRadius() + speed) * (driveable.getConfigType().getBulletDetectionRadius() + speed))
        {
            // Raytrace the bullet
            hits.addAll(driveable.attackFromBullet(origin, motion));
        }
    }

    /** Entities hit by the part are found by their part boxes, wherever their own box is. */
    private static void checkPartBodyHits(Level level, List<Entity> entitiesToIgnore, Vec3 origin, Vec3 motion, @Nullable LivingEntity owner, List<BulletHit> hits)
    {
        for (Entity wearer : PartBodies.wearers(level))
        {
            PartBody body = PartBodies.of(wearer);
            // A stand-in does not shoot itself, nor is it shot by whoever rides it.
            boolean own = owner != null && (owner == wearer || wearer.hasIndirectPassenger(owner));
            if (body == null || own || entitiesToIgnore.contains(wearer) || wearer instanceof LivingEntity living && living.isDeadOrDying())
                continue;
            double range = body.reach() + motion.length();
            if (body.getPose().origin().distanceToSqr(origin) <= range * range)
                hits.addAll(body.trace(wearer, origin, motion));
        }
    }

    private static void checkPlayerHit(Player player, Vec3 origin, Vec3 motion, long gameTime, int rewindTicks, float hitBoxSize, List<BulletHit> hits)
    {
        PlayerData data = PlayerData.getInstance(player);

        // Login uses the Teams spectator placeholder even outside a round; it must not grant survival immunity.
        if (player.isSpectator() || FlansMod.teamsManager.isRoundRunning() && Team.SPECTATORS.equals(data.getTeam()))
            return;

        List<PlayerSnapshot> snapshots = LagCompensation.playerSnapshots(data, gameTime, rewindTicks);

        // If we couldn't get any snapshot at all, fall back to normal hit detection
        if (snapshots.isEmpty())
        {
            performNormalPlayerHitboxRaytrace(player, origin, motion, hitBoxSize, hits);
            return;
        }

        // The first snapshot the shot meets counts, so one pass cannot hit the same player in several poses.
        for (PlayerSnapshot snapshot : snapshots)
        {
            List<BulletHit> snapshotHits = snapshot.raytrace(origin, motion, 0F, 1F);
            if (!snapshotHits.isEmpty())
            {
                hits.addAll(snapshotHits);
                return;
            }
        }
    }

    private static void performNormalPlayerHitboxRaytrace(Player player, Vec3 origin, Vec3 motion, double hitBoxSize, List<BulletHit> hits)
    {

        // Expand player's AABB
        AABB expanded = player.getBoundingBox().inflate(hitBoxSize, hitBoxSize, hitBoxSize);

        Vec3 destination = origin.add(motion);

        // AABB#clip(start, end) returns intersection point or null
        Vec3 hitVec = expanded.clip(origin, destination).orElse(null);
        if (hitVec == null)
            return;

        Vec3 hitPoint = hitVec.subtract(origin);
        hits.add(new PlayerBulletHit(new PlayerHitbox(player, new Matrix4f(), new Vector3f(), new Vector3f(), new Vector3f(), EnumHitboxType.BODY), (float) computeHitLambda(hitPoint, motion)));
    }

    private static double computeHitLambda(Vec3 hitPoint, Vec3 motion)
    {
        double hitLambda = 1.0;
        if (motion.x != 0.0)
            hitLambda = hitPoint.x / motion.x;
        else if (motion.y != 0.0)
            hitLambda = hitPoint.y / motion.y;
        else if (motion.z != 0.0)
            hitLambda = hitPoint.z / motion.z;
        if (hitLambda < 0.0)
            hitLambda = -hitLambda;
        return hitLambda;
    }

    private static void checkEntityHit(Entity entity, Vec3 origin, Vec3 motion, long gameTime, int rewindTicks, float hitBoxSize, List<BulletHit> hits)
    {
        if (entity instanceof IFlanHumanoidBody humanoid && entity instanceof LivingEntity living)
        {
            checkHumanoidHit(living, humanoid, origin, motion, gameTime, rewindTicks, hitBoxSize, hits);
            return;
        }
        EntityHit hit = findRewoundHit(entity, origin, motion, gameTime, rewindTicks, hitBoxSize);
        if (hit == null)
            hit = findHitAgainstEntityAndParts(entity, origin, motion, hitBoxSize);
        if (hit != null)
            hits.add(new EntityHit(hit.getEntity(), hit.getIntersectTime(), hit.getImpact()));
    }

    /**
     * A humanoid is first tested by its boxes, where the shooter saw it and where it is now, as any entity. Only when the
     * round crosses one are its limbs built, once, in their current pose, and tested at each box crossed, the box the
     * shooter saw first; a round that passes between them misses, as it would miss a player.
     */
    private static void checkHumanoidHit(LivingEntity entity, IFlanHumanoidBody humanoid, Vec3 origin, Vec3 motion, long gameTime, int rewindTicks, float hitBoxSize, List<BulletHit> hits)
    {
        final double len2 = motion.lengthSqr();
        final boolean noMotion = len2 <= 1e-9;
        AABB current = entity.getBoundingBox();
        EntityHit boxHit = null;
        // How far each box crossed lies from where the entity is now, which moves the round onto the limbs instead
        List<Vec3> offsets = new ArrayList<>(2 * LagCompensation.WINDOW_TICKS + 2);
        for (AABB box : LagCompensation.entityBoxes(entity, gameTime, rewindTicks))
        {
            EntityHit hit = tryHitBox(entity, box.inflate(hitBoxSize), origin, motion, len2, noMotion);
            if (hit == null)
                continue;
            boxHit = boxHit == null ? hit : boxHit;
            offsets.add(new Vec3((current.minX + current.maxX - box.minX - box.maxX) / 2D, current.minY - box.minY, (current.minZ + current.maxZ - box.minZ - box.maxZ) / 2D));
        }
        EntityHit currentHit = findHitAgainstEntityAndParts(entity, origin, motion, hitBoxSize);
        if (currentHit != null)
        {
            boxHit = boxHit == null ? currentHit : boxHit;
            offsets.add(Vec3.ZERO);
        }
        if (boxHit == null)
            return;

        Optional<HumanoidBody> body = humanoid.flansmodultimateHumanoidBody();
        if (body.isEmpty())
        {
            hits.add(boxHit);
            return;
        }
        PlayerSnapshot snapshot = new PlayerSnapshot(entity, 1F, body.get());
        for (Vec3 offset : offsets)
        {
            List<BulletHit> limbHits = snapshot.raytrace(origin.add(offset), motion, 0F, 1F);
            if (!limbHits.isEmpty())
            {
                hits.addAll(limbHits);
                return;
            }
        }
        if (!entity.level().isClientSide && ClientHooks.RENDER.isDebugMode())
            showMissedLimbs(snapshot, offsets.get(0), boxHit.getImpact());
    }

    /**
     * Debug mode: a round crossed a humanoid's box but none of its limbs. Marks where it entered the box in magenta and
     * outlines the limbs where the server tested them, at the box the shooter saw, so they can be compared with the model.
     */
    private static void showMissedLimbs(PlayerSnapshot snapshot, Vec3 offset, Vec3 impact)
    {
        ClientHooks.RENDER.spawnDebugDot(impact, 1000, 1F, 0F, 1F);
        Vec3 base = new Vec3(snapshot.pos.x, snapshot.pos.y, snapshot.pos.z).subtract(offset);
        for (PlayerHitbox hitbox : snapshot.hitboxes)
        {
            Vec3[] corners = new Vec3[8];
            for (int i = 0; i < 8; i++)
            {
                Vector3f corner = new Vector3f(hitbox.o.x + ((i & 1) != 0 ? hitbox.d.x : 0F), hitbox.o.y + ((i & 2) != 0 ? hitbox.d.y : 0F), hitbox.o.z + ((i & 4) != 0 ? hitbox.d.z : 0F));
                hitbox.transform.transformPosition(corner);
                corners[i] = base.add(corner.x, corner.y, corner.z);
            }
            float green = hitbox.type == EnumHitboxType.HEAD ? 1F : 0F;
            for (int i = 0; i < 8; i++)
            {
                // Each edge joins corners that differ in one axis
                for (int axis = 1; axis < 8; axis <<= 1)
                {
                    if ((i & axis) == 0)
                        ClientHooks.RENDER.spawnDebugVector(corners[i], corners[i | axis].subtract(corners[i]), 1000, 1F, green, 1F);
                }
            }
        }
    }

    /**
     * Tests the entity where the shooter saw it, from its recorded boxes. Without a record for that time the entity
     * is tested where it is now instead.
     */
    @Nullable
    private static EntityHit findRewoundHit(Entity entity, Vec3 origin, Vec3 motion, long gameTime, int rewindTicks, float bulletHitBoxSize)
    {
        if (entity.getParts() != null)
            return null;
        final double len2 = motion.lengthSqr();
        final boolean noMotion = len2 <= 1e-9;
        for (AABB box : LagCompensation.entityBoxes(entity, gameTime, rewindTicks))
        {
            EntityHit hit = tryHitBox(entity, box.inflate(bulletHitBoxSize), origin, motion, len2, noMotion);
            if (hit != null)
                return hit;
        }
        return null;
    }

    /**
     * Finds the earliest hit against an entity and its parts.
     * - Expands AABBs by projectile radius
     * - Sweeps AABBs by entity velocity
     * - Handles start-inside
     * - Picks earliest t; parts win ties
     */
    @Nullable
    public static EntityHit findHitAgainstEntityAndParts(Entity entity, Vec3 start, Vec3 motion, float bulletHitBoxSize)
    {
        if (entity instanceof AAGun gun)
        {
            Vec3 impact = gun.clipCollisionBox(start, start.add(motion), bulletHitBoxSize);
            return impact == null ? null : new EntityHit(gun, (float) computeHitLambda(impact.subtract(start), motion), impact);
        }
        final double len2 = motion.lengthSqr();
        final boolean noMotion = len2 <= 1e-9;

        // Whole entity
        AABB wholeBox = buildSweptInflatedBox(entity, bulletHitBoxSize);
        EntityHit best = tryHitBox(entity, wholeBox, start, motion, len2, noMotion);

        // Parts (take earliest; prefer parts on tie)
        Entity[] parts = entity.getParts();
        if (parts != null)
        {
            for (Entity part : parts)
            {
                AABB partBox = buildSweptInflatedBox(part, bulletHitBoxSize);
                EntityHit cand = tryHitBox(part, partBox, start, motion, len2, noMotion);
                if (isBetterHit(cand, best))
                    best = cand;
            }
        }

        return best;
    }

    private static AABB buildSweptInflatedBox(Entity entity, float bulletHtiBoxSize)
    {
        double dx = entity.getX() - entity.xo;
        double dy = entity.getY() - entity.yo;
        double dz = entity.getZ() - entity.zo;

        Vec3 deltaBack = new Vec3(-dx * 2.0, -dy * 2.0, -dz * 2.0);
        AABB swept = entity.getBoundingBox().expandTowards(deltaBack);

        return swept.inflate(bulletHtiBoxSize);
    }

    /** Attempt a hit against a single AABB with start-inside handling. */
    @Nullable
    private static EntityHit tryHitBox(Entity owner, AABB box, Vec3 start, Vec3 motion, double len2, boolean noMotion)
    {
        // Start-inside → immediate hit at t=0
        if (box.contains(start))
        {
            return new EntityHit(owner, 0F, start);
        }

        // Segment clip
        Vec3 end = start.add(motion);
        Optional<Vec3> hit = box.clip(start, end);
        if (hit.isEmpty())
            return null;

        Vec3 impact = hit.get();
        float t = computeParametricT(start, impact, motion, len2, noMotion);
        return new EntityHit(owner, t, impact);
    }

    /** Compute parametric t along start + t * motion (clamped to [0,1]). */
    private static float computeParametricT(Vec3 start, Vec3 impact, Vec3 motion, double len2, boolean noMotion)
    {
        if (noMotion)
            return 0f; // define as immediate or skip upstream

        Vec3 delta = impact.subtract(start);
        double dot = delta.x * motion.x + delta.y * motion.y + delta.z * motion.z;
        float t = (float) (dot / len2);

        // Clamp for numerical safety
        return Mth.clamp(t, 0F, 1F);
    }

    /** Compare two hits; prefer candidate if it is earlier, or equal when preferCandOnTie=true. */
    private static boolean isBetterHit(@Nullable EntityHit cand, @Nullable EntityHit best)
    {
        if (cand == null)
            return false;
        if (best == null)
            return true;
        return cand.compareTo(best) <= 0;
    }

    public static List<BulletHit> raytraceBlock(Level level, Vec3 posVec, Vec3 previousHit, Vec3 motion, Vec3 direction, List<BulletHit> hits, float penetration, @Nullable BlockPos oldPos,
        BulletType type)
    {
        // Ray trace the bullet by comparing its next position to its current position
        Vec3 nextPosVec = posVec.add(motion);

        BlockHitResult hit = clipBlocks(level, posVec, nextPosVec);

        if (hit.getType() == HitResult.Type.BLOCK)
        {
            Vec3 hitVec = hit.getLocation().subtract(posVec).add(previousHit);
            BlockPos pos = hit.getBlockPos();
            BlockState blockState = level.getBlockState(pos);

            if (!pos.equals(oldPos))
            {
                // Calculate the lambda value of the intercept
                float lambda = 1;
                // Try each co-ordinate one at a time.
                if (motion.x != 0)
                    lambda = (float) (hitVec.x / motion.x);
                else if (motion.y != 0)
                    lambda = (float) (hitVec.y / motion.y);
                else if (motion.z != 0)
                    lambda = (float) (hitVec.z / motion.z);

                if (lambda < 0)
                    lambda = -lambda;

                hits.add(new BlockHit(hit, lambda, blockState));
                penetration -= ShootingHelper.getBlockPenetrationDecrease(level, blockState, pos, type);
            }

            if (penetration > 0)
                hits = raytraceBlock(level, hit.getLocation().add(direction), hitVec.add(direction), motion, direction, hits, penetration, pos, type);
        }
        return hits;
    }

    /**
     * The first block a shot from {@code from} to {@code to} strikes, using {@link #shotShape}.
     */
    public static BlockHitResult clipBlocks(BlockGetter level, Vec3 from, Vec3 to)
    {
        return BlockGetter.traverseBlocks(from, to, level, (getter, pos) ->
        {
            BlockState state = getter.getBlockState(pos);
            return getter.clipWithInteractionOverride(from, to, pos, shotShape(getter, pos, state), state);
        }, getter ->
        {
            Vec3 back = from.subtract(to);
            return BlockHitResult.miss(to, Direction.getNearest(back.x, back.y, back.z), BlockPos.containing(to));
        });
    }

    /**
     * The shape of a block that stops shots. Only blocks with a collision shape stop them, so grass, flowers and
     * cobwebs let them through, but they are struck where they are drawn: fences and walls collide half a block
     * above their top, which would stop shots fired over them.
     */
    public static VoxelShape shotShape(BlockGetter level, BlockPos pos, BlockState state)
    {
        VoxelShape collision = state.getCollisionShape(level, pos, CollisionContext.empty());
        if (collision.isEmpty())
            return collision;
        VoxelShape outline = state.getShape(level, pos, CollisionContext.empty());
        return outline.isEmpty() ? collision : outline;
    }

    /**
     * Performs a block ray trace from the given entity's eyes in the direction it is looking.
     * <p>
     * The start position is the entity's interpolated eye position at the given {@code partialTicks},
     * and the end position is that eye position extended by {@code dist} blocks along the current
     * view vector. The result is a {@link BlockHitResult} describing the first block hit (or a miss).
     *
     * @param entity
     *            the living entity from whose perspective the ray trace is performed
     * @param partialTicks
     *            interpolation factor between the previous and current tick (typically render partial ticks);
     *            used to smoothly interpolate the eye position and view direction
     * @param dist
     *            maximum distance, in blocks, that the ray trace will extend from the entity's eyes
     * @param interact
     *            whether the ray trace should interact with fluids; if {@code true}, the ray
     *            can hit fluids ({@link ClipContext.Fluid#ANY}), otherwise fluids are ignored
     *            ({@link ClipContext.Fluid#NONE})
     * @return the {@link BlockHitResult} for the first block intersected by the ray; if no block is hit,
     *         the result will have type {@link net.minecraft.world.phys.HitResult.Type#MISS}
     */
    public static BlockHitResult getSpottedPoint(Entity entity, float partialTicks, double dist, boolean interact)
    {
        // Eye position with interpolation
        Vec3 eyePos = entity.getEyePosition(partialTicks);

        // Look direction with interpolation
        Vec3 lookVec = entity.getViewVector(partialTicks);

        // Target point in front of the eyes
        Vec3 targetPos = eyePos.add(lookVec.scale(dist));

        ClipContext.Fluid fluidMode = interact ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE;
        ClipContext ctx = new ClipContext(eyePos, targetPos, ClipContext.Block.OUTLINE, fluidMode, entity);
        return entity.level().clip(ctx);
    }
}
