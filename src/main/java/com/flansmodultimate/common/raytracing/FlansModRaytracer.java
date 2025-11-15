package com.flansmodultimate.common.raytracing;

import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.entity.AAGun;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.entity.Grenade;
import com.flansmodultimate.common.guns.ShootingHelper;
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
public class FlansModRaytracer
{
    //TODO: investigate why high speed bullets can not eliminate near targets
    public static List<BulletHit> raytrace(Level level, Entity playerToIgnore, boolean canHitSelf, Entity entityToIgnore, Vector3f origin, Vector3f motion, int pingOfShooter, float gunPenetration, float projectileHitBoxSize)
    {
        //Create a list for all bullet hits
        List<BulletHit> hits = new ArrayList<>();

        final float speed = motion.length();
        final Vec3 start = origin.toVec3();
        final Vec3 end = start.add(motion.x, motion.y, motion.z);

        // Query only entities along the ray segment (plus some padding)
        AABB search = new AABB(start, end).inflate(speed);

        for (Entity entity : ModUtils.queryEntities(level, entityToIgnore, search))
        {
            boolean shouldDoNormalHitDetect = true;


            //TODO: driveables
            if (entity instanceof Driveable driveable)
            {
                /*shouldDoNormalHitDetect = false;

                if(driveable.isDead() || driveable.isPartOfThis(playerToIgnore))
                    continue;

                //If this bullet is within the driveable's detection range
                if(driveable.getDistanceSq(origin.x, origin.y, origin.z) <= (driveable.getDriveableType().bulletDetectionRadius + speed) * (driveable.getDriveableType().bulletDetectionRadius + speed))
                {
                    //Raytrace the bullet
                    ArrayList<BulletHit> driveableHits = driveable.attackFromBullet(origin, motion);
                    hits.addAll(driveableHits);
                }*/
            }
            //TODO: Check logic for Players
            else if (entity instanceof Player player)
            {
                PlayerData data = PlayerData.getInstance(player);
                shouldDoNormalHitDetect = false;

                if(data != null)
                {
                    //TODO: Teams
                    if (!player.isAlive() /*|| data.team == Team.spectators*/)
                        continue;

                    if (player == playerToIgnore && !canHitSelf)
                        continue;

                    int snapshotToTry = Math.max(0, Math.min(pingOfShooter / 50, data.getSnapshots().length - 1));
                    PlayerSnapshot snapshot = data.getSnapshots()[snapshotToTry];

                    if (snapshot == null)
                        snapshot = data.getSnapshots()[0];

                    // Check one last time for a null snapshot. If this is the case, fall back to normal hit detection
                    if (snapshot == null)
                        shouldDoNormalHitDetect = true;
                    else
                        hits.addAll(snapshot.raytrace(origin, motion));
                }
            }

            if (shouldDoNormalHitDetect
                && entity != entityToIgnore
                && entity != playerToIgnore
                && entity.isAlive()
                && (entity instanceof LivingEntity || entity instanceof AAGun || entity instanceof Grenade))
            {
                EntityHit hit = findHitAgainstEntityAndParts(entity, start, motion.toVec3(), projectileHitBoxSize);
                if (hit != null) {
                    hits.add(new EntityHit(hit.getEntity(), hit.intersectTime, hit.impact));
                }
            }
        }

        Vec3 mot = motion.toVec3().normalize().scale(0.5d);
        hits = raytraceBlock(level, origin.toVec3(), new Vec3(0, 0, 0), motion, mot, hits, gunPenetration, null);

        //We hit something
        if(!hits.isEmpty())
        {
            //Sort the hits according to the intercept position
            Collections.sort(hits);
        }

        return hits;
    }

    /**
     * Finds the earliest hit against an entity and its parts.
     * - Expands AABBs by projectile radius
     * - Sweeps AABBs by entity velocity
     * - Handles start-inside
     * - Picks earliest t; parts win ties
     */
    @Nullable
    public static EntityHit findHitAgainstEntityAndParts(Entity entity, Vec3 start, Vec3 motion, float projectileHitBoxSize)
    {
        final double len2 = motion.lengthSqr();
        final boolean noMotion = len2 <= 1e-9;
        final Vec3 entVel = entity.getDeltaMovement();

        // Whole entity
        AABB wholeBox = buildSweptInflatedBox(entity.getBoundingBox(), projectileHitBoxSize, entVel);
        EntityHit best = tryHitBox(entity, wholeBox, start, motion, len2, noMotion);

        // Parts (take earliest; prefer parts on tie)
        Entity[] parts = entity.getParts();
        if (parts != null)
        {
            for (Entity part : parts)
            {
                AABB partBox = buildSweptInflatedBox(part.getBoundingBox(), projectileHitBoxSize, entVel);
                EntityHit cand = tryHitBox(part, partBox, start, motion, len2, noMotion);
                if (isBetterHit(cand, best))
                    best = cand;
            }
        }

        return best;
    }

    /** Inflate by bullet radius and sweep by entity velocity for ~1 tick. */
    private static AABB buildSweptInflatedBox(AABB box, float projectileHtiBoxSize, Vec3 entityVelocity)
    {
        return box.inflate(projectileHtiBoxSize / 2F).expandTowards(entityVelocity);
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

    private static List<BulletHit> raytraceBlock(Level level, Vec3 posVec, Vec3 previousHit, Vector3f motion, Vec3 direction, List<BulletHit> hits, Float penetration, BlockPos oldPos)
    {
        //Ray trace the bullet by comparing its next position to its current position
        Vec3 nextPosVec = new Vec3(posVec.x + motion.x, posVec.y + motion.y, posVec.z + motion.z);

        BlockHitResult hit = level.clip(new ClipContext(posVec, nextPosVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null));

        if (hit.getType() == HitResult.Type.BLOCK)
        {
            Vec3 hitVec = hit.getLocation().subtract(posVec);
            hitVec = hitVec.add(previousHit);

            BlockPos pos = hit.getBlockPos();
            BlockState blockState = level.getBlockState(pos);

            if (!pos.equals(oldPos))
            {
                //Calculate the lambda value of the intercept
                float lambda = 1;
                //Try each co-ordinate one at a time.
                if (motion.x != 0)
                    lambda = (float)(hitVec.x / motion.x);
                else if (motion.y != 0)
                    lambda = (float)(hitVec.y / motion.y);
                else if (motion.z != 0)
                    lambda = (float)(hitVec.z / motion.z);

                if (lambda < 0)
                    lambda = -lambda;

                hits.add(new BlockHit(hit, lambda, blockState));
                penetration -= ShootingHelper.getBlockPenetrationDecrease(blockState, pos, level);
            }

            if (penetration > 0)
            {
                hits = raytraceBlock(level, hit.getLocation().add(direction), hitVec.add(direction), motion, direction, hits, penetration, pos);
            }
        }
        return hits;
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
