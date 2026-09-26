package com.flansmodultimate.network;

import com.flansmodultimate.network.client.PacketAimPoseState;
import com.flansmodultimate.network.client.PacketAllowDebug;
import com.flansmodultimate.network.client.PacketApocalypseCountdown;
import com.flansmodultimate.network.client.PacketBaseEditState;
import com.flansmodultimate.network.client.PacketBlockHitEffect;
import com.flansmodultimate.network.client.PacketBulletTrail;
import com.flansmodultimate.network.client.PacketCancelGunReloadClient;
import com.flansmodultimate.network.client.PacketCancelSound;
import com.flansmodultimate.network.client.PacketCommonConfigValues;
import com.flansmodultimate.network.client.PacketContentFingerprint;
import com.flansmodultimate.network.client.PacketDebugShootPoint;
import com.flansmodultimate.network.client.PacketDriveableBankFired;
import com.flansmodultimate.network.client.PacketDriveableCrashFireball;
import com.flansmodultimate.network.client.PacketDriveableDamage;
import com.flansmodultimate.network.client.PacketDriveablePrediction;
import com.flansmodultimate.network.client.PacketDriveableRenderState;
import com.flansmodultimate.network.client.PacketExplodeParticles;
import com.flansmodultimate.network.client.PacketFlak;
import com.flansmodultimate.network.client.PacketFlanExplosionBlockParticles;
import com.flansmodultimate.network.client.PacketFlanExplosionParticles;
import com.flansmodultimate.network.client.PacketFlashBang;
import com.flansmodultimate.network.client.PacketGunFireModeClient;
import com.flansmodultimate.network.client.PacketGunMeleeClient;
import com.flansmodultimate.network.client.PacketGunMuzzleFlash;
import com.flansmodultimate.network.client.PacketGunPreferredAmmoClient;
import com.flansmodultimate.network.client.PacketGunReloadClient;
import com.flansmodultimate.network.client.PacketGunSecondaryModeClient;
import com.flansmodultimate.network.client.PacketGunShootClient;
import com.flansmodultimate.network.client.PacketGunShotPose;
import com.flansmodultimate.network.client.PacketGunVariableZoomClient;
import com.flansmodultimate.network.client.PacketHitMarker;
import com.flansmodultimate.network.client.PacketKillMessage;
import com.flansmodultimate.network.client.PacketLoadoutState;
import com.flansmodultimate.network.client.PacketParticle;
import com.flansmodultimate.network.client.PacketParticles;
import com.flansmodultimate.network.client.PacketPlaySound;
import com.flansmodultimate.network.client.PacketPlayerClassSkins;
import com.flansmodultimate.network.client.PacketSmokeShell;
import com.flansmodultimate.network.client.PacketSyncCommonConfig;
import com.flansmodultimate.network.client.PacketSyncDigitalAmmo;
import com.flansmodultimate.network.client.PacketTeamsState;
import com.flansmodultimate.network.server.ArmorBoxBuyPacket;
import com.flansmodultimate.network.server.PacketAAGunModelBarrelOrigins;
import com.flansmodultimate.network.server.PacketAimPosePreference;
import com.flansmodultimate.network.server.PacketBaseEditAction;
import com.flansmodultimate.network.server.PacketBuyWeapon;
import com.flansmodultimate.network.server.PacketDeployedGunInput;
import com.flansmodultimate.network.server.PacketDriveableInput;
import com.flansmodultimate.network.server.PacketGunFireMode;
import com.flansmodultimate.network.server.PacketGunInput;
import com.flansmodultimate.network.server.PacketGunPreferredAmmo;
import com.flansmodultimate.network.server.PacketGunReload;
import com.flansmodultimate.network.server.PacketGunScopedState;
import com.flansmodultimate.network.server.PacketGunSecondaryMode;
import com.flansmodultimate.network.server.PacketGunSwitchDelay;
import com.flansmodultimate.network.server.PacketGunVariableZoom;
import com.flansmodultimate.network.server.PacketLoadoutAction;
import com.flansmodultimate.network.server.PacketManualGuidance;
import com.flansmodultimate.network.server.PacketReloadPreferences;
import com.flansmodultimate.network.server.PacketRequestCommonConfig;
import com.flansmodultimate.network.server.PacketRequestDebug;
import com.flansmodultimate.network.server.PacketRequestDismount;
import com.flansmodultimate.network.server.PacketSelectPaintjob;
import com.flansmodultimate.network.server.PacketSetCommonConfigValue;
import com.flansmodultimate.network.server.PacketTeamsAction;
import com.flansmodultimate.platform.PlatformEnvironment;
import com.flansmodultimate.platform.network.NetworkPlatform;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Loader-neutral packet registry and send API. Gameplay packets implement {@link IPacket};
 * the loader-specific transport lives in {@link NetworkPlatform}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PacketHandler
{
    private static final List<Class<? extends IClientPacket>> CLIENT_PACKET_TYPES = new ArrayList<>();
    private static final List<Class<? extends IServerPacket>> SERVER_PACKET_TYPES = new ArrayList<>();
    private static boolean prepared;

    /** Server-to-client packet types, sorted by class name so every side derives the same network order. */
    public static synchronized List<Class<? extends IClientPacket>> clientPacketTypes()
    {
        preparePacketTypes();
        return Collections.unmodifiableList(CLIENT_PACKET_TYPES);
    }

    /** Client-to-server packet types, sorted by class name so every side derives the same network order. */
    public static synchronized List<Class<? extends IServerPacket>> serverPacketTypes()
    {
        preparePacketTypes();
        return Collections.unmodifiableList(SERVER_PACKET_TYPES);
    }

    private static void preparePacketTypes()
    {
        if (prepared)
            return;

        addClientPackets(
            PacketAimPoseState.class, PacketAllowDebug.class, PacketApocalypseCountdown.class, PacketBaseEditState.class,
            PacketBlockHitEffect.class, PacketBulletTrail.class, PacketCancelGunReloadClient.class,
            PacketCancelSound.class, PacketCommonConfigValues.class, PacketContentFingerprint.class,
            PacketDebugShootPoint.class, PacketDriveableBankFired.class, PacketDriveableCrashFireball.class,
            PacketDriveableDamage.class, PacketDriveablePrediction.class, PacketDriveableRenderState.class,
            PacketExplodeParticles.class, PacketFlak.class,
            PacketFlanExplosionBlockParticles.class, PacketFlanExplosionParticles.class, PacketFlashBang.class,
            PacketGunFireModeClient.class, PacketGunMeleeClient.class, PacketGunMuzzleFlash.class,
            PacketGunPreferredAmmoClient.class, PacketGunReloadClient.class, PacketGunSecondaryModeClient.class,
            PacketGunShootClient.class, PacketGunShotPose.class, PacketGunVariableZoomClient.class, PacketHitMarker.class,
            PacketKillMessage.class, PacketLoadoutState.class, PacketParticle.class, PacketParticles.class,
            PacketPlayerClassSkins.class, PacketPlaySound.class, PacketSmokeShell.class,
            PacketSyncCommonConfig.class, PacketSyncDigitalAmmo.class, PacketTeamsState.class
        );
        addServerPackets(
            PacketAAGunModelBarrelOrigins.class, PacketAimPosePreference.class, PacketBaseEditAction.class, ArmorBoxBuyPacket.class,
            PacketDeployedGunInput.class, PacketDriveableInput.class, PacketBuyWeapon.class, PacketGunFireMode.class,
            PacketGunInput.class, PacketGunPreferredAmmo.class, PacketGunReload.class, PacketGunScopedState.class,
            PacketGunSecondaryMode.class, PacketGunSwitchDelay.class, PacketGunVariableZoom.class,
            PacketManualGuidance.class, PacketReloadPreferences.class, PacketRequestCommonConfig.class,
            PacketRequestDebug.class, PacketRequestDismount.class, PacketSelectPaintjob.class,
            PacketSetCommonConfigValue.class, PacketTeamsAction.class, PacketLoadoutAction.class
        );

        Comparator<Class<?>> byName = Comparator.comparing(Class::getName, String.CASE_INSENSITIVE_ORDER);
        CLIENT_PACKET_TYPES.sort(byName);
        SERVER_PACKET_TYPES.sort(byName);
        prepared = true;
    }

    @SafeVarargs
    private static void addClientPackets(Class<? extends IClientPacket>... types)
    {
        CLIENT_PACKET_TYPES.addAll(List.of(types));
    }

    @SafeVarargs
    private static void addServerPackets(Class<? extends IServerPacket>... types)
    {
        SERVER_PACKET_TYPES.addAll(List.of(types));
    }

    /** Creates an empty packet of the given type and reads its content from the buffer. */
    public static <T extends IPacket> T decode(Class<? extends T> type, PacketBuffer buffer)
    {
        try
        {
            T packet = type.getDeclaredConstructor().newInstance();
            packet.decodeInto(buffer);
            return packet;
        }
        catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException exception)
        {
            throw new IllegalStateException("Failed to decode " + type.getName(), exception);
        }
    }

    /** client -> server */
    public static void sendToServer(IServerPacket message)
    {
        NetworkPlatform.sendToServer(message);
    }

    /** server -> specific player */
    public static void sendTo(IClientPacket message, ServerPlayer player)
    {
        NetworkPlatform.sendToPlayer(player, message);
    }

    /** server -> everyone */
    public static void sendToAll(IClientPacket message)
    {
        NetworkPlatform.sendToAll(message);
    }

    /** server -> players currently tracking an entity (and the entity itself, if it is a player) */
    public static void sendToTracking(IClientPacket message, Entity entity)
    {
        NetworkPlatform.sendToTrackingEntityAndSelf(entity, message);
    }

    /** server -> all in a dimension */
    public static void sendToDimension(ResourceKey<Level> dimension, IClientPacket message)
    {
        NetworkPlatform.sendToDimension(dimension, message);
    }

    /** server -> players near a point */
    public static void sendToAllAround(IClientPacket message, double x, double y, double z, double range, ResourceKey<Level> dimension)
    {
        NetworkPlatform.sendToNear(dimension, x, y, z, range, message);
    }

    /** server -> players near a point */
    public static void sendToAllAround(IClientPacket message, Vec3 position, double range, ResourceKey<Level> dimension)
    {
        sendToAllAround(message, position.x, position.y, position.z, range, dimension);
    }

    /** server -> all in a donut (min..max radius) */
    public static void sendToDonut(ResourceKey<Level> dimension, Vec3 center, double minRange, double maxRange, IClientPacket message)
    {
        MinecraftServer server = PlatformEnvironment.currentServer();
        ServerLevel level = server == null ? null : server.getLevel(dimension);
        if (level == null)
            return;

        double minSquared = minRange * minRange;
        double maxSquared = maxRange * maxRange;
        for (ServerPlayer player : level.players())
        {
            double distanceSquared = player.position().distanceToSqr(center);
            if (distanceSquared > minSquared && distanceSquared < maxSquared)
                sendTo(message, player);
        }
    }

    /** server -> all within range except one player */
    public static void sendToAllExcept(ResourceKey<Level> dimension, Vec3 center, double range, ServerPlayer except, IClientPacket message)
    {
        MinecraftServer server = PlatformEnvironment.currentServer();
        ServerLevel level = server == null ? null : server.getLevel(dimension);
        if (level == null)
            return;

        double rangeSquared = range * range;
        UUID excludedId = except.getUUID();
        for (ServerPlayer player : level.players())
        {
            if (!player.getUUID().equals(excludedId) && player.position().distanceToSqr(center) < rangeSquared)
                sendTo(message, player);
        }
    }
}
