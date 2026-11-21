package com.flansmodultimate.mixin;

import com.flansmodultimate.common.driveables.collision.DriveableCollisionWorld;
import com.flansmodultimate.common.teams.TeamsRegeneration;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/**
 * Treats a driveable hull as ground for sneaking edge protection and leaves peaceful healing to Teams during rounds.
 *
 * <p>
 * Vanilla only probes blocks for the ground under a sneaking player, so on a
 * deck every step looks like a ledge and the player cannot move at all.
 * </p>
 */
@Mixin(Player.class)
public abstract class PlayerDriveableEdgeMixin
{
    @Redirect(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;heal(F)V"))
    private void flansmodultimatePeacefulRoundRegeneration(Player player, float pHealAmount)
    {
        if (!TeamsRegeneration.applies(player))
            player.heal(pHealAmount);
    }

    // Sable replaces the noCollision call. Compose with the final verdict from the block collision check.
    @ModifyReturnValue(method = "canFallAtLeast", at = @At("RETURN"))
    private boolean flansmodultimateNoDriveableHullBelow(boolean canFall, double x, double z, float distance)
    {
        if (!canFall)
            return false;
        Entity player = (Player) (Object) this;
        AABB bounds = player.getBoundingBox();
        AABB below = new AABB(bounds.minX + x, bounds.minY - distance - 1.0E-5F, bounds.minZ + z, bounds.maxX + x, bounds.minY, bounds.maxZ + z);
        return !DriveableCollisionWorld.intersectsHull(player, below);
    }
}
