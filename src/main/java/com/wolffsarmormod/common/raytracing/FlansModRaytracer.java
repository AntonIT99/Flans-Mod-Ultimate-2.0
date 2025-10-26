package com.wolffsarmormod.common.raytracing;

import com.wolffsarmormod.common.PlayerData;
import com.wolffsarmormod.common.entity.AAGun;
import com.wolffsarmormod.common.entity.Driveable;
import com.wolffsarmormod.common.entity.EntityHit;
import com.wolffsarmormod.common.entity.Grenade;
import com.wolffsarmormod.common.guns.ShotHandler;
import com.wolffsarmormod.common.item.GunItem;
import com.wolffsarmormod.common.types.AttachmentType;
import com.wolffsarmormod.common.types.GunType;
import com.wolffsarmormod.common.vector.Vector3f;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.core.BlockPos;
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
import java.util.Objects;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FlansModRaytracer
{
    public static List<BulletHit> raytrace(Level level, Entity playerToIgnore, boolean canHitSelf, Entity entityToIgnore, Vector3f origin, Vector3f motion, int pingOfShooter, float gunPenetration)
    {
        //Create a list for all bullet hits
        List<BulletHit> hits = new ArrayList<>();

        final float speed = motion.length();
        final Vec3 start = origin.toVec3();
        final Vec3 end = start.add(motion.x, motion.y, motion.z);

        // Query only entities along the ray segment (plus some padding)
        AABB search = new AABB(start, end).inflate(speed);
        List<Entity> candidates = level.getEntities((Entity) null, search, Objects::nonNull);

        for (Entity obj : candidates)
        {
            boolean shouldDoNormalHitDetect = true;


            //TODO: driveables
            if (obj instanceof Driveable driveable)
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
            else if (obj instanceof Player player)
            {
                PlayerData data = PlayerData.getInstance(player);
                shouldDoNormalHitDetect = false;

                if(data != null)
                {
                    //TODO: Teams
                    if (player.isDeadOrDying() /*|| data.team == Team.spectators*/)
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

            if (shouldDoNormalHitDetect)
            {
                Entity entity = obj;
                boolean isDead = entity.isRemoved() || (entity instanceof LivingEntity le && le.isDeadOrDying());

                if (entity != entityToIgnore
                    && entity != playerToIgnore
                    && !isDead
                    && (entity instanceof LivingEntity || entity instanceof AAGun || entity instanceof Grenade))
                {

                    Optional<Vec3> wholeHit = entity.getBoundingBox().clip(start, end);

                    if (wholeHit.isPresent())
                    {
                        boolean hit = true;
                        Entity hitEntity = entity;
                        Entity[] parts = entity.getParts();
                        Vec3 impact = wholeHit.get();

                        // If parts exist, the intercepted part is calculated and used instead of the whole entity.
                        // If no part is intercepted, the entity itself is not hit
                        if (parts != null)
                        {
                            hit = false;
                            for (Entity part : parts)
                            {
                                Optional<Vec3> partHit = part.getBoundingBox().clip(start, end);
                                if (partHit.isPresent())
                                {
                                    hitEntity = part;
                                    impact = partHit.get();
                                    hit = true;
                                    break;
                                }
                            }
                        }

                        if (hit)
                        {
                            Vec3 delta = impact.subtract(start);

                            float hitLambda = 1.0F;
                            if (motion.x != 0F)
                                hitLambda = (float) (delta.x / motion.x);
                            else if (motion.y != 0F)
                                hitLambda = (float) (delta.y / motion.y);
                            else if (motion.z != 0F)
                                hitLambda = (float) (delta.z / motion.z);
                            if (hitLambda < 0F)
                                hitLambda = -hitLambda;

                            hits.add(new EntityHit(hitEntity, hitLambda));
                        }
                    }
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

    private static List<BulletHit> raytraceBlock(Level level, Vec3 posVec, Vec3 previousHit, Vector3f motion, Vec3 normalized_motion, List<BulletHit> hits, Float penetration, BlockPos oldPos)
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
                penetration -= ShotHandler.getBlockPenetrationDecrease(blockState, pos, level);
            }

            if (penetration > 0)
            {
                hits = raytraceBlock(level, hit.getLocation().add(normalized_motion), hitVec.add(normalized_motion), motion, normalized_motion, hits, penetration, pos);
            }
        }
        return hits;
    }

    public static Vector3f getPlayerMuzzlePosition(Player player, InteractionHand hand)
    {
        PlayerSnapshot snapshot = new PlayerSnapshot(player);

        ItemStack itemstack = hand == InteractionHand.OFF_HAND ? player.getOffhandItem() : player.getMainHandItem();

        if (itemstack.getItem() instanceof GunItem gunItem)
        {
            GunType gunType = gunItem.getConfigType();
            AttachmentType barrelType = gunType.getBarrel(itemstack);

            return Vector3f.add(new Vector3f(player.getX(), player.getY(), player.getZ()), snapshot.getMuzzleLocation(gunType, barrelType, hand), null);
        }

        return new Vector3f(player.getEyePosition(0.0F));
    }
}
