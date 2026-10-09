package com.flansmodultimate.mixin;

import com.flansmodultimate.common.driveables.collision.DriveableCollisionWorld;
import com.flansmodultimate.common.entity.AAGun;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.Predicate;

/** Uses the rotated AA gun hull for vanilla targeting while retaining other entities' normal boxes. */
@Mixin(ProjectileUtil.class)
public abstract class ProjectileUtilAAGunHullMixin
{
    @Inject(method = "getEntityHitResult(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;", at = @At("HEAD"), cancellable = true)
    private static void flansmodultimatePickAAGunHull(Entity shooter, Vec3 start, Vec3 end, AABB search, Predicate<Entity> filter, double distance, CallbackInfoReturnable<EntityHitResult> callback)
    {
        if (!DriveableCollisionWorld.hasAAGunHull(shooter.level(), search))
            return;
        List<Entity> candidates = shooter.level().getEntities(shooter, search, filter);
        Entity chosen = null;
        Vec3 chosenHit = null;
        double nearest = distance;
        for (Entity candidate : candidates)
        {
            AABB box = candidate.getBoundingBox().inflate(candidate.getPickRadius());
            Vec3 hit = candidate instanceof AAGun gun ? gun.clipCollisionBox(start, end, candidate.getPickRadius()) : box.clip(start, end).orElse(null);
            boolean inside = candidate instanceof AAGun ? hit != null && hit.distanceToSqr(start) <= 1.0E-12D : box.contains(start);
            if (inside)
            {
                if (nearest >= 0D)
                {
                    chosen = candidate;
                    chosenHit = hit == null ? start : hit;
                    nearest = 0D;
                }
            }
            else if (hit != null)
            {
                double hitDistance = start.distanceToSqr(hit);
                if (hitDistance < nearest || nearest == 0D)
                {
                    if (candidate.getRootVehicle() == shooter.getRootVehicle() && !candidate.canRiderInteract())
                    {
                        if (nearest == 0D)
                        {
                            chosen = candidate;
                            chosenHit = hit;
                        }
                    }
                    else
                    {
                        chosen = candidate;
                        chosenHit = hit;
                        nearest = hitDistance;
                    }
                }
            }
        }
        callback.setReturnValue(chosen == null ? null : new EntityHitResult(chosen, chosenHit));
    }

    @Inject(method = "getEntityHitResult(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;F)Lnet/minecraft/world/phys/EntityHitResult;", at = @At("HEAD"), cancellable = true)
    private static void flansmodultimateProjectileAAGunHull(Level level, Entity projectile, Vec3 start, Vec3 end, AABB search, Predicate<Entity> filter, float inflation,
        CallbackInfoReturnable<EntityHitResult> callback)
    {
        if (!DriveableCollisionWorld.hasAAGunHull(level, search))
            return;
        List<Entity> candidates = level.getEntities(projectile, search, filter);
        Entity chosen = null;
        Vec3 chosenHit = null;
        double nearest = Double.MAX_VALUE;
        for (Entity candidate : candidates)
        {
            Vec3 hit = candidate instanceof AAGun gun ? gun.clipCollisionBox(start, end, inflation) : candidate.getBoundingBox().inflate(inflation).clip(start, end).orElse(null);
            if (hit == null)
                continue;
            double distance = start.distanceToSqr(hit);
            if (distance < nearest)
            {
                chosen = candidate;
                chosenHit = hit;
                nearest = distance;
            }
        }
        callback.setReturnValue(chosen == null ? null : new EntityHitResult(chosen, chosenHit));
    }
}
