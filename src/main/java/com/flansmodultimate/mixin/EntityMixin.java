package com.flansmodultimate.mixin;

import com.flansmodultimate.common.driveables.DriveableCollisionWorld;
import com.flansmodultimate.common.entity.EntityDistancePolicy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Hooks into the vanilla entity base class.
 *
 * <p>
 * Lets ordinary entity movement collide with driveable hulls. Vanilla collides moving entities only with blocks
 * and with other entities' bounding boxes, which cannot describe a rotated, shaped hull. Movement that could reach a
 * hull is resolved by {@link DriveableCollisionWorld} using the same axis order and step-up rules; all other movement
 * runs vanilla unchanged.
 * </p>
 *
 * <p>
 * The owning Flan NPC is culled before its detached Flan model renderer is called.
 * </p>
 */
@Mixin(Entity.class)
@SuppressWarnings("DataFlowIssue") // Mixin merges this class into the target, so the self-cast is valid.
public abstract class EntityMixin
{
    @Inject(method = "collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;", at = @At("HEAD"), cancellable = true)
    private void flansmodultimateCollideWithDriveableHulls(Vec3 movement, CallbackInfoReturnable<Vec3> callback)
    {
        Vec3 result = DriveableCollisionWorld.collide((Entity) (Object) this, movement);
        if (result != null)
            callback.setReturnValue(result);
    }

    // shouldRenderAtSqrDistance is client-only and stripped from the dedicated server, hence require = 0.
    @Inject(method = "shouldRenderAtSqrDistance", at = @At("HEAD"), cancellable = true, require = 0)
    private void flansmodultimateNpcDistance(double distanceSquared, CallbackInfoReturnable<Boolean> result)
    {
        Entity entity = (Entity) (Object) this;
        if (EntityDistancePolicy.isFlanNpc(entity))
        {
            double distance = EntityDistancePolicy.renderDistance(entity, true);
            result.setReturnValue(distanceSquared < distance * distance);
        }
    }
}
