package com.flansmodultimate.mixin;

import com.flansmodultimate.common.driveables.DriveableCollisionWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;

/**
 * Counts a driveable deck as something to stand on for the server's floating
 * check, which otherwise disconnects a player for flying after four seconds on
 * any deck more than half a block above terrain.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerDriveableMixin
{
    @Inject(method = "noBlocksAround", at = @At("HEAD"), cancellable = true)
    private void flansmodultimate$standingOnDriveableHull(Entity entity, CallbackInfoReturnable<Boolean> callback)
    {
        if (DriveableCollisionWorld.isStandingOnHull(entity))
            callback.setReturnValue(false);
    }
}
