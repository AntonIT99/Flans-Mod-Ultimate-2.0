package com.flansmodultimate.mixin;

import com.flansmodultimate.common.driveables.DriveableCollisionWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * Treats a driveable hull as ground for sneaking edge protection.
 *
 * <p>Vanilla only probes blocks for the ground under a sneaking player, so on a
 * deck every step looks like a ledge and the player cannot move at all.</p>
 */
@Mixin(Player.class)
public abstract class PlayerDriveableEdgeMixin
{
    @Redirect(method = {"maybeBackOffFromEdge", "isAboveGround"}, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/Level;noCollision(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Z"))
    private boolean flansmodultimate$noDriveableHullBelow(Level level, Entity entity, AABB box)
    {
        return level.noCollision(entity, box) && !DriveableCollisionWorld.intersectsHull(entity, box);
    }
}
