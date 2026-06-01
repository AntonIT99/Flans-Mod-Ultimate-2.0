package com.flansmodultimate.network;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.network.client.PacketAllowDebug;
import com.flansmodultimate.network.client.PacketBlockHitEffect;
import com.flansmodultimate.network.client.PacketBulletTrail;
import com.flansmodultimate.network.client.PacketCancelGunReloadClient;
import com.flansmodultimate.network.client.PacketCancelSound;
import com.flansmodultimate.network.client.PacketExplodeParticles;
import com.flansmodultimate.network.client.PacketFlak;
import com.flansmodultimate.network.client.PacketFlanExplosionBlockParticles;
import com.flansmodultimate.network.client.PacketFlanExplosionParticles;
import com.flansmodultimate.network.client.PacketFlashBang;
import com.flansmodultimate.network.client.PacketGunFireModeClient;
import com.flansmodultimate.network.client.PacketGunMeleeClient;
import com.flansmodultimate.network.client.PacketGunReloadClient;
import com.flansmodultimate.network.client.PacketGunShootClient;
import com.flansmodultimate.network.client.PacketHitMarker;
import com.flansmodultimate.network.client.PacketParticle;
import com.flansmodultimate.network.client.PacketPlaySound;
import com.flansmodultimate.network.client.PacketSyncCommonConfig;
import com.flansmodultimate.network.client.PacketSyncDigitalAmmo;
import com.flansmodultimate.network.server.PacketAAGunInput;
import com.flansmodultimate.network.server.PacketBuyWeapon;
import com.flansmodultimate.network.server.PacketDeployedGunInput;
import com.flansmodultimate.network.server.PacketGunFireMode;
import com.flansmodultimate.network.server.PacketGunInput;
import com.flansmodultimate.network.server.PacketGunReload;
import com.flansmodultimate.network.server.PacketGunScopedState;
import com.flansmodultimate.network.server.PacketGunSpread;
import com.flansmodultimate.network.server.PacketManualGuidance;
import com.flansmodultimate.network.server.PacketRequestDebug;
import com.flansmodultimate.network.server.PacketRequestDismount;
import com.flansmodultimate.network.server.PacketRequestPlaySound;
import com.flansmodultimate.network.server.PacketSelectPaintjob;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

@net.neoforged.fml.common.EventBusSubscriber(modid = FlansMod.MOD_ID, bus = net.neoforged.fml.common.EventBusSubscriber.Bus.MOD)
public final class PacketHandler
{
    private PacketHandler()
    {
    }

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event)
    {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToClient(PacketAllowDebug.TYPE, PacketAllowDebug.STREAM_CODEC, PacketHandler::handleClientPayload);
        registrar.playToClient(PacketBlockHitEffect.TYPE, PacketBlockHitEffect.STREAM_CODEC, PacketHandler::handleClientPayload);
        registrar.playToClient(PacketBulletTrail.TYPE, PacketBulletTrail.STREAM_CODEC, PacketHandler::handleClientPayload);
        registrar.playToClient(PacketCancelGunReloadClient.TYPE, PacketCancelGunReloadClient.STREAM_CODEC, PacketHandler::handleClientPayload);
        registrar.playToClient(PacketCancelSound.TYPE, PacketCancelSound.STREAM_CODEC, PacketHandler::handleClientPayload);
        registrar.playToClient(PacketExplodeParticles.TYPE, PacketExplodeParticles.STREAM_CODEC, PacketHandler::handleClientPayload);
        registrar.playToClient(PacketFlak.TYPE, PacketFlak.STREAM_CODEC, PacketHandler::handleClientPayload);
        registrar.playToClient(PacketFlanExplosionBlockParticles.TYPE, PacketFlanExplosionBlockParticles.STREAM_CODEC, PacketHandler::handleClientPayload);
        registrar.playToClient(PacketFlanExplosionParticles.TYPE, PacketFlanExplosionParticles.STREAM_CODEC, PacketHandler::handleClientPayload);
        registrar.playToClient(PacketFlashBang.TYPE, PacketFlashBang.STREAM_CODEC, PacketHandler::handleClientPayload);
        registrar.playToClient(PacketGunFireModeClient.TYPE, PacketGunFireModeClient.STREAM_CODEC, PacketHandler::handleClientPayload);
        registrar.playToClient(PacketGunMeleeClient.TYPE, PacketGunMeleeClient.STREAM_CODEC, PacketHandler::handleClientPayload);
        registrar.playToClient(PacketGunReloadClient.TYPE, PacketGunReloadClient.STREAM_CODEC, PacketHandler::handleClientPayload);
        registrar.playToClient(PacketGunShootClient.TYPE, PacketGunShootClient.STREAM_CODEC, PacketHandler::handleClientPayload);
        registrar.playToClient(PacketHitMarker.TYPE, PacketHitMarker.STREAM_CODEC, PacketHandler::handleClientPayload);
        registrar.playToClient(PacketParticle.TYPE, PacketParticle.STREAM_CODEC, PacketHandler::handleClientPayload);
        registrar.playToClient(PacketPlaySound.TYPE, PacketPlaySound.STREAM_CODEC, PacketHandler::handleClientPayload);
        registrar.playToClient(PacketSyncCommonConfig.TYPE, PacketSyncCommonConfig.STREAM_CODEC, PacketHandler::handleClientPayload);
        registrar.playToClient(PacketSyncDigitalAmmo.TYPE, PacketSyncDigitalAmmo.STREAM_CODEC, PacketHandler::handleClientPayload);

        registrar.playToServer(PacketAAGunInput.TYPE, PacketAAGunInput.STREAM_CODEC, PacketHandler::handleServerPayload);
        registrar.playToServer(PacketBuyWeapon.TYPE, PacketBuyWeapon.STREAM_CODEC, PacketHandler::handleServerPayload);
        registrar.playToServer(PacketDeployedGunInput.TYPE, PacketDeployedGunInput.STREAM_CODEC, PacketHandler::handleServerPayload);
        registrar.playToServer(PacketGunFireMode.TYPE, PacketGunFireMode.STREAM_CODEC, PacketHandler::handleServerPayload);
        registrar.playToServer(PacketGunInput.TYPE, PacketGunInput.STREAM_CODEC, PacketHandler::handleServerPayload);
        registrar.playToServer(PacketGunReload.TYPE, PacketGunReload.STREAM_CODEC, PacketHandler::handleServerPayload);
        registrar.playToServer(PacketGunScopedState.TYPE, PacketGunScopedState.STREAM_CODEC, PacketHandler::handleServerPayload);
        registrar.playToServer(PacketGunSpread.TYPE, PacketGunSpread.STREAM_CODEC, PacketHandler::handleServerPayload);
        registrar.playToServer(PacketManualGuidance.TYPE, PacketManualGuidance.STREAM_CODEC, PacketHandler::handleServerPayload);
        registrar.playToServer(PacketRequestDebug.TYPE, PacketRequestDebug.STREAM_CODEC, PacketHandler::handleServerPayload);
        registrar.playToServer(PacketRequestDismount.TYPE, PacketRequestDismount.STREAM_CODEC, PacketHandler::handleServerPayload);
        registrar.playToServer(PacketRequestPlaySound.TYPE, PacketRequestPlaySound.STREAM_CODEC, PacketHandler::handleServerPayload);
        registrar.playToServer(PacketSelectPaintjob.TYPE, PacketSelectPaintjob.STREAM_CODEC, PacketHandler::handleServerPayload);
    }

    private static void handleClientPayload(IClientPacket packet, IPayloadContext context)
    {
        context.enqueueWork(() -> ClientPacketDispatcher.dispatch(packet));
    }

    private static void handleServerPayload(IServerPacket packet, IPayloadContext context)
    {
        context.enqueueWork(() -> {
            ServerPlayer sender = (ServerPlayer) context.player();
            packet.handleServerSide(sender, sender.serverLevel());
        });
    }

    public static void sendToServer(IServerPacket msg)
    {
        PacketDistributor.sendToServer(msg);
    }

    public static void sendTo(IClientPacket msg, ServerPlayer player)
    {
        PacketDistributor.sendToPlayer(player, msg);
    }

    public static void sendToAll(IClientPacket msg)
    {
        PacketDistributor.sendToAllPlayers(msg);
    }

    public static void sendToDimension(ResourceKey<Level> dimension, IClientPacket msg)
    {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null)
            return;
        ServerLevel level = server.getLevel(dimension);
        if (level != null)
        {
            PacketDistributor.sendToPlayersInDimension(level, msg);
        }
    }

    public static void sendToAllAround(IClientPacket msg, double x, double y, double z, double range, ResourceKey<Level> dim)
    {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null)
            return;
        ServerLevel level = server.getLevel(dim);
        if (level != null)
        {
            double r2 = range * range;
            for (ServerPlayer p : level.players())
            {
                if (p.position().distanceToSqr(x, y, z) < r2)
                    sendTo(msg, p);
            }
        }
    }

    public static void sendToAllAround(IClientPacket msg, Vec3 position, double range, ResourceKey<Level> dim)
    {
        sendToAllAround(msg, position.x, position.y, position.z, range, dim);
    }

    public static void sendToDonut(ResourceKey<Level> dimension, Vec3 center, double minRange, double maxRange, IClientPacket msg)
    {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null)
            return;
        ServerLevel level = server.getLevel(dimension);
        if (level == null)
            return;

        double min2 = minRange * minRange;
        double max2 = maxRange * maxRange;
        for (ServerPlayer p : level.players())
        {
            double d2 = p.position().distanceToSqr(center);
            if (d2 > min2 && d2 < max2)
                sendTo(msg, p);
        }
    }

    public static void sendToAllExcept(ResourceKey<Level> dim, Vec3 center, double range, ServerPlayer except, IClientPacket msg)
    {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null)
            return;
        ServerLevel level = server.getLevel(dim);
        if (level == null)
            return;

        double r2 = range * range;
        UUID ex = except.getUUID();
        for (ServerPlayer p : level.players())
        {
            if (p.getUUID().equals(ex))
                continue;
            if (p.position().distanceToSqr(center) < r2)
                sendTo(msg, p);
        }
    }
}