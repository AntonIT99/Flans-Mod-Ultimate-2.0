package com.flansmodultimate.mixin;

import com.flansmodultimate.common.driveables.collision.DriveableCollisionWorld;
import com.flansmodultimate.common.teams.RoundRule;
import com.flansmodultimate.common.teams.TeamsManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundTeleportToEntityPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;

/**
 * Counts a driveable deck as something to stand on for the server's floating
 * check, which otherwise disconnects a player for flying after four seconds on
 * any deck more than half a block above terrain.
 * Also rejects spectator movement, spectator teleports and flag-carrier flight disallowed by a Teams round.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerDriveableMixin
{
    @Shadow
    public ServerPlayer player;

    @Inject(method = "noBlocksAround", at = @At("HEAD"), cancellable = true)
    private void flansmodultimateStandingOnDriveableHull(Entity entity, CallbackInfoReturnable<Boolean> callback)
    {
        if (DriveableCollisionWorld.isStandingOnHull(entity))
            callback.setReturnValue(false);
    }

    // Run after vanilla has moved packet handling to the server thread.
    @Inject(method = "handleMovePlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V", shift = At.Shift.AFTER), cancellable = true)
    private void flansmodultimateRestrictSpectatorMovement(ServerboundMovePlayerPacket pPacket, CallbackInfo callback)
    {
        if (TeamsManager.getInstance().getRoundRuleRuntime().restrictedSpectator(player))
            callback.cancel();
    }

    @Inject(method = "handleTeleportToEntityPacket", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V", shift = At.Shift.AFTER), cancellable = true)
    private void flansmodultimateRestrictSpectatorTeleport(ServerboundTeleportToEntityPacket pPacket, CallbackInfo callback)
    {
        if (TeamsManager.getInstance().getRoundRuleRuntime().restrictedSpectator(player))
            callback.cancel();
    }

    @Inject(method = "handlePlayerCommand", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V", shift = At.Shift.AFTER), cancellable = true)
    private void flansmodultimateRestrictFlagGliding(ServerboundPlayerCommandPacket pPacket, CallbackInfo callback)
    {
        if (pPacket.getAction() == ServerboundPlayerCommandPacket.Action.START_FALL_FLYING && flansmodultimateFlagFlightRestricted())
            callback.cancel();
    }

    @Inject(method = "handlePlayerAbilities", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V", shift = At.Shift.AFTER), cancellable = true)
    private void flansmodultimateRestrictFlagFlight(ServerboundPlayerAbilitiesPacket pPacket, CallbackInfo callback)
    {
        if (pPacket.isFlying() && flansmodultimateFlagFlightRestricted())
        {
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
            callback.cancel();
        }
    }

    @org.spongepowered.asm.mixin.Unique
    private boolean flansmodultimateFlagFlightRestricted()
    {
        TeamsManager manager = TeamsManager.getInstance();
        return manager.isRoundRunning() && manager.getRoundRuleRuntime().rules().enabled(RoundRule.FLAG_NO_FLIGHT) && manager.getFlagCarriedBy(player) != null;
    }
}
