package com.flansmodultimate.common.raytracing;

import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.entity.Bullet;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.guns.ShootingHelper;
import com.flansmodultimate.common.raytracing.hits.BlockHit;
import com.flansmodultimate.common.raytracing.hits.BulletHit;
import com.flansmodultimate.common.raytracing.hits.EntityHit;
import com.flansmodultimate.common.raytracing.hits.PlayerBulletHit;
import com.flansmodultimate.common.teams.Team;
import com.flansmodultimate.common.types.BulletType;
import com.flansmodultimate.util.ModUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Raytracer
{
    public static List<BulletHit> raytraceShot(Level level, @Nullable Bullet bullet, @Nullable LivingEntity owner, List<Entity> entitiesToIgnore, Vec3 origin, Vec3 motion, int pingOfShooter, float gunPenetration, float bulletHitBoxSize, BulletType type)
    {
        //Create a list for all bullet hits
        List<BulletHit> hits = new ArrayList<>();

        final Vec3 destination = origin.add(motion);

        // Query only entities along the ray segment
        AABB search = new AABB(origin, destination).inflate(motion.length());

        for (Entity entity : ModUtils.queryEntities(level, bullet, search))
        {
            if (!entity.isAlive() || (entity instanceof LivingEntity living && living.isDeadOrDying()) || entitiesToIgnore.contains(entity) || !ModUtils.canEntityBeHitByBullets(entity))
                continue;

            if (entity instanceof Driveable driveable)
                checkDriveableHit(driveable, origin, motion, owner, hits);
            else if (entity instanceof Player player)
                checkPlayerHit(player, origin, motion, pingOfShooter, bulletHitBoxSize, hits);
            else
                checkEntityHit(entity, origin, motion, bulletHitBoxSize, hits);
        }

        hits = raytraceBlock(level, origin, Vec3.ZERO, motion, motion.normalize().scale(0.5), hits, gunPenetration, null, type);

        //We hit something
        if (!hits.isEmpty())
        {
            //Sort the hits according to the intercept position
            Collections.sort(hits);
        }

        return hits;
    }

    private static void checkDriveableHit(Driveable driveable, Vec3 origin, Vec3 motion, @Nullable LivingEntity owner, List<BulletHit> hits)
    {
        if (owner != null && driveable.isPartOfThis(owner))
            return;

        double speed = motion.length();

        // If this bullet is within the driveable's detection range
        if (driveable.distanceToSqr(origin) <= (driveable.getConfigType().getBulletDetectionRadius() + speed) * (driveable.getConfigType().getBulletDetectionRadius() + speed))
        {
            // Raytrace the bullet
            hits.addAll(driveable.attackFromBullet(origin, motion));
        }
    }

    private static void checkPlayerHit(Player player, Vec3 origin, Vec3 motion, int pingOfShooter, float hitBoxSize, List<BulletHit> hits)
    {
        PlayerData data = PlayerData.getInstance(player);
        boolean shouldDoNormalHitDetect = false;

        if (data.getTeam() == Team.SPECTATORS)
            return;

        SnapshotSelection selection = SnapshotSelection.selectSnapshotForPing(data, pingOfShooter);

        // If we couldn't get any snapshot at all, fall back to normal hit detection
        if (selection.snapshot() == null)
            shouldDoNormalHitDetect = true;
        else
            performSnapshotRaytrace(data, selection, origin, motion, hits);

        if (shouldDoNormalHitDetect)
            performNormalPlayerHitboxRaytrace(player, origin, motion, hitBoxSize, hits);
    }

    private static void performSnapshotRaytrace(PlayerData data, SnapshotSelection selection, Vec3 origin, Vec3 motion, List<BulletHit> hits)
    {
        PlayerSnapshot snapshot = selection.snapshot();
        int snapshotToTry = selection.snapshotIndex();
        float snapshotPortion = selection.snapshotPortion();

        boolean snapshotBeforeExists = snapshotToTry != 0 && data.getSnapshots()[snapshotToTry - 1] != null;
        boolean snapshotAfterExists = snapshotToTry + 1 < data.getSnapshots().length && data.getSnapshots()[snapshotToTry + 1] != null;

        float bias = 0.25F;
        float offset = snapshotPortion + bias;

        float lb = offset - 0.5F;
        float ub = offset + 0.5F;

        List<BulletHit> onStepHits;
        List<BulletHit> altStepHits = new ArrayList<>();

        if (offset > 0.5F && snapshotAfterExists)
        {
            // Timestep t and t+1
            onStepHits = snapshot.raytrace(origin, motion, lb, 1F);
            if (onStepHits.isEmpty())
            {
                altStepHits = data.getSnapshots()[snapshotToTry + 1].raytrace(origin, motion, 0F, lb);
            }
        }
        else if (offset < 0.5F && snapshotBeforeExists)
        {
            // Timestep t and t-1
            onStepHits = snapshot.raytrace(origin, motion, 0F, ub);
            if (onStepHits.isEmpty())
            {
                altStepHits = data.getSnapshots()[snapshotToTry - 1].raytrace(origin, motion, ub, 1F);
            }
        }
        else
        {
            // Timestep t ONLY
            onStepHits = snapshot.raytrace(origin, motion, 0F, 1F);
        }

        hits.addAll(onStepHits);
        hits.addAll(altStepHits);
    }

    private static void performNormalPlayerHitboxRaytrace(Player player, Vec3 origin, Vec3 motion, double hitBoxSize, List<BulletHit> hits) {

        // Expand player's AABB
        AABB expanded = player.getBoundingBox().inflate(hitBoxSize, hitBoxSize, hitBoxSize);

        Vec3 destination = origin.add(motion);

        // AABB#clip(start, end) returns intersection point or null
        Vec3 hitVec = expanded.clip(origin, destination).orElse(null);
        if (hitVec == null)
            return;

        Vec3 hitPoint = hitVec.subtract(origin);
        hits.add(new PlayerBulletHit(new PlayerHitbox(player, new RotatedAxes(), new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f(), EnumHitboxType.BODY), (float) computeHitLambda(hitPoint, motion)));
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

    private static void checkEntityHit(Entity entity, Vec3 origin, Vec3 motion, float hitBoxSize, List<BulletHit> hits)
    {
        EntityHit hit = findHitAgainstEntityAndParts(entity, origin, motion, hitBoxSize);
        if (hit != null)
            hits.add(new EntityHit(hit.getEntity(), hit.getIntersectTime(), hit.getImpact()));
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

    public static List<BulletHit> raytraceBlock(Level level, Vec3 posVec, Vec3 previousHit, Vec3 motion, Vec3 direction, List<BulletHit> hits, float penetration, @Nullable BlockPos oldPos, BulletType type)
    {
        //Ray trace the bullet by comparing its next position to its current position
        Vec3 nextPosVec = posVec.add(motion);

        BlockHitResult hit = level.clip(new ClipContext(posVec, nextPosVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null));

        if (hit.getType() == HitResult.Type.BLOCK)
        {
            Vec3 hitVec = hit.getLocation().subtract(posVec).add(previousHit);
            BlockPos pos = hit.getBlockPos();
            BlockState blockState = level.getBlockState(pos);

            if (!pos.equals(oldPos))
            {
                //Calculate the lambda value of the intercept
                float lambda = 1;
                //Try each co-ordinate one at a time.
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
     * Performs a block ray trace from the given entity's eyes in the direction it is looking.
     * <p>
     * The start position is the entity's interpolated eye position at the given {@code partialTicks},
     * and the end position is that eye position extended by {@code dist} blocks along the current
     * view vector. The result is a {@link BlockHitResult} describing the first block hit (or a miss).
     *
     * @param entity       the living entity from whose perspective the ray trace is performed
     * @param partialTicks interpolation factor between the previous and current tick (typically render partial ticks);
     *                     used to smoothly interpolate the eye position and view direction
     * @param dist         maximum distance, in blocks, that the ray trace will extend from the entity's eyes
     * @param interact     whether the ray trace should interact with fluids; if {@code true}, the ray
     *                     can hit fluids ({@link ClipContext.Fluid#ANY}), otherwise fluids are ignored
     *                     ({@link ClipContext.Fluid#NONE})
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

    public static Vector3f getPlayerMuzzlePosition(Player player, InteractionHand hand)
    {
        PlayerSnapshot snapshot = new PlayerSnapshot(player);

        ItemStack itemstack = (hand == InteractionHand.OFF_HAND) ? player.getOffhandItem() : player.getMainHandItem();

        //TODO: Fix this code
        /*if (itemstack.getItem() instanceof GunItem gunItem)
        {
            GunType gunType = gunItem.getConfigType();
            AttachmentType barrelType = gunType.getBarrel(itemstack);
            return Vector3f.add(new Vector3f(player.getX(), player.getY(), player.getZ()), snapshot.getMuzzleLocation(gunType, barrelType, hand), null);
        }*/

        return new Vector3f(player.getEyePosition(0.0F));
    }
}
