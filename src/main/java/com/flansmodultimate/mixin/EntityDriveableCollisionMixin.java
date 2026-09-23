package com.flansmodultimate.mixin;

import com.flansmodultimate.common.driveables.DriveableCollisionWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Lets ordinary entity movement collide with driveable hulls.
 *
 * <p>Vanilla collides moving entities only with blocks and with other entities'
 * bounding boxes, which cannot describe a rotated, shaped hull. Movement that
 * could reach a hull is resolved by {@link DriveableCollisionWorld} using the
 * same axis order and step-up rules; all other movement runs vanilla unchanged.</p>
 */
@Mixin(Entity.class)
public abstract class EntityDriveableCollisionMixin
{
    @Inject(method = "collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;", at = @At("HEAD"),
        cancellable = true)
    private void flansmodultimate$collideWithDriveableHulls(Vec3 movement, CallbackInfoReturnable<Vec3> callback)
    {
        Vec3 result = DriveableCollisionWorld.collide((Entity) (Object) this, movement);
        if (result != null)
            callback.setReturnValue(result);
    }
}
