package com.flansmodultimate.network;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.network.client.PacketAllowDebug;
import com.flansmodultimate.network.client.PacketBaseEditState;
import com.flansmodultimate.network.client.PacketBlockHitEffect;
import com.flansmodultimate.network.client.PacketBulletTrail;
import com.flansmodultimate.network.client.PacketCancelGunReloadClient;
import com.flansmodultimate.network.client.PacketCancelSound;
import com.flansmodultimate.network.client.PacketDriveableDamage;
import com.flansmodultimate.network.client.PacketDriveableRenderState;
import com.flansmodultimate.network.client.PacketExplodeParticles;
import com.flansmodultimate.network.client.PacketFlak;
import com.flansmodultimate.network.client.PacketFlanExplosionBlockParticles;
import com.flansmodultimate.network.client.PacketFlanExplosionParticles;
import com.flansmodultimate.network.client.PacketFlashBang;
import com.flansmodultimate.network.client.PacketGunFireModeClient;
import com.flansmodultimate.network.client.PacketGunMeleeClient;
import com.flansmodultimate.network.client.PacketGunMuzzleFlash;
import com.flansmodultimate.network.client.PacketGunReloadClient;
import com.flansmodultimate.network.client.PacketGunShootClient;
import com.flansmodultimate.network.client.PacketHitMarker;
import com.flansmodultimate.network.client.PacketLoadoutState;
import com.flansmodultimate.network.client.PacketParticle;
import com.flansmodultimate.network.client.PacketParticles;
import com.flansmodultimate.network.client.PacketPlaySound;
import com.flansmodultimate.network.client.PacketSyncCommonConfig;
import com.flansmodultimate.network.client.PacketSyncDigitalAmmo;
import com.flansmodultimate.network.client.PacketTeamsState;
import com.flansmodultimate.network.server.ArmorBoxBuyPacket;
import com.flansmodultimate.network.server.PacketAAGunModelBarrelOrigins;
import com.flansmodultimate.network.server.PacketBaseEditAction;
import com.flansmodultimate.network.server.PacketBuyWeapon;
import com.flansmodultimate.network.server.PacketDeployedGunInput;
import com.flansmodultimate.network.server.PacketDriveableInput;
import com.flansmodultimate.network.server.PacketGunFireMode;
import com.flansmodultimate.network.server.PacketGunInput;
import com.flansmodultimate.network.server.PacketGunReload;
import com.flansmodultimate.network.server.PacketGunScopedState;
import com.flansmodultimate.network.server.PacketGunSpread;
import com.flansmodultimate.network.server.PacketLoadoutAction;
import com.flansmodultimate.network.server.PacketManualGuidance;
import com.flansmodultimate.network.server.PacketRequestDebug;
import com.flansmodultimate.network.server.PacketRequestDismount;
import com.flansmodultimate.network.server.PacketSelectPaintjob;
import com.flansmodultimate.network.server.PacketTeamsAction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * NeoForge network bridge. Gameplay packets keep their loader-neutral {@link IPacket}
 * contract and are transported through one payload envelope per direction.
 */
public final class PacketHandler
{
    public static final String PROTOCOL = "7";

    private static final List<Class<? extends IClientPacket>> CLIENT_PACKET_TYPES = new ArrayList<>();
    private static final List<Class<? extends IServerPacket>> SERVER_PACKET_TYPES = new ArrayList<>();
    private static final Map<Class<? extends IClientPacket>, Integer> CLIENT_PACKET_IDS = new HashMap<>();
    private static final Map<Class<? extends IServerPacket>, Integer> SERVER_PACKET_IDS = new HashMap<>();
    private static boolean prepared;

    private PacketHandler()
    {
    }

    public static void register(RegisterPayloadHandlersEvent event)
    {
        preparePacketTypes();
        PayloadRegistrar registrar = event.registrar(PROTOCOL);
        registrar.playToClient(ClientboundPayload.TYPE, ClientboundPayload.STREAM_CODEC, PacketHandler::handleClientPayload);
        registrar.playToServer(ServerboundPayload.TYPE, ServerboundPayload.STREAM_CODEC, PacketHandler::handleServerPayload);
    }

    private static synchronized void preparePacketTypes()
    {
        if (prepared)
            return;

        addClientPackets(
            PacketAllowDebug.class, PacketBaseEditState.class, PacketBlockHitEffect.class, PacketBulletTrail.class,
            PacketCancelGunReloadClient.class, PacketCancelSound.class, PacketDriveableDamage.class,
            PacketDriveableRenderState.class, PacketExplodeParticles.class, PacketFlak.class,
            PacketFlanExplosionBlockParticles.class, PacketFlanExplosionParticles.class, PacketFlashBang.class,
            PacketGunFireModeClient.class, PacketGunMeleeClient.class, PacketGunMuzzleFlash.class,
            PacketGunReloadClient.class, PacketGunShootClient.class, PacketHitMarker.class, PacketLoadoutState.class,
            PacketParticle.class, PacketParticles.class, PacketPlaySound.class, PacketSyncCommonConfig.class,
            PacketSyncDigitalAmmo.class, PacketTeamsState.class
        );
        addServerPackets(
            PacketAAGunModelBarrelOrigins.class, PacketBaseEditAction.class, ArmorBoxBuyPacket.class,
            PacketDeployedGunInput.class, PacketDriveableInput.class, PacketBuyWeapon.class, PacketGunFireMode.class,
            PacketGunInput.class, PacketGunReload.class, PacketGunScopedState.class, PacketGunSpread.class,
            PacketManualGuidance.class, PacketRequestDebug.class, PacketRequestDismount.class,
            PacketSelectPaintjob.class, PacketTeamsAction.class, PacketLoadoutAction.class
        );

        Comparator<Class<?>> byName = Comparator.comparing(Class::getName, String.CASE_INSENSITIVE_ORDER);
        CLIENT_PACKET_TYPES.sort(byName);
        SERVER_PACKET_TYPES.sort(byName);
        for (int i = 0; i < CLIENT_PACKET_TYPES.size(); i++)
            CLIENT_PACKET_IDS.put(CLIENT_PACKET_TYPES.get(i), i);
        for (int i = 0; i < SERVER_PACKET_TYPES.size(); i++)
            SERVER_PACKET_IDS.put(SERVER_PACKET_TYPES.get(i), i);
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

    private static void handleClientPayload(ClientboundPayload payload, IPayloadContext context)
    {
        context.enqueueWork(() -> ClientPacketDispatcher.dispatch(payload.packet()));
    }

    private static void handleServerPayload(ServerboundPayload payload, IPayloadContext context)
    {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer sender)
                payload.packet().handleServerSide(sender, sender.level());
        });
    }

    private static <T extends IPacket> T decodePacket(RegistryFriendlyByteBuf buffer, List<Class<? extends T>> types)
    {
        int id = buffer.readVarInt();
        if (id < 0 || id >= types.size())
            throw new IllegalArgumentException("Unknown Flan's Mod packet id " + id);

        Class<? extends T> type = types.get(id);
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

    private static <T extends IPacket> void encodePacket(RegistryFriendlyByteBuf buffer, T packet, Map<Class<? extends T>, Integer> ids)
    {
        @SuppressWarnings("unchecked")
        Integer id = ids.get((Class<? extends T>) packet.getClass());
        if (id == null)
            throw new IllegalArgumentException("Unregistered Flan's Mod packet " + packet.getClass().getName());
        buffer.writeVarInt(id);
        packet.encodeInto(buffer);
    }

    private record ClientboundPayload(IClientPacket packet) implements CustomPacketPayload
    {
        private static final Type<ClientboundPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(FlansMod.MOD_ID, "clientbound"));
        private static final StreamCodec<RegistryFriendlyByteBuf, ClientboundPayload> STREAM_CODEC = StreamCodec.ofMember(
            (payload, buffer) -> encodePacket(buffer, payload.packet, CLIENT_PACKET_IDS),
            buffer -> new ClientboundPayload(decodePacket(buffer, CLIENT_PACKET_TYPES))
        );

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }
    }

    private record ServerboundPayload(IServerPacket packet) implements CustomPacketPayload
    {
        private static final Type<ServerboundPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(FlansMod.MOD_ID, "serverbound"));
        private static final StreamCodec<RegistryFriendlyByteBuf, ServerboundPayload> STREAM_CODEC = StreamCodec.ofMember(
            (payload, buffer) -> encodePacket(buffer, payload.packet, SERVER_PACKET_IDS),
            buffer -> new ServerboundPayload(decodePacket(buffer, SERVER_PACKET_TYPES))
        );

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }
    }

    public static void sendToServer(IServerPacket message)
    {
        preparePacketTypes();
        net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(new ServerboundPayload(message));
    }

    public static void sendTo(IClientPacket message, ServerPlayer player)
    {
        preparePacketTypes();
        PacketDistributor.sendToPlayer(player, new ClientboundPayload(message));
    }

    public static void sendToAll(IClientPacket message)
    {
        preparePacketTypes();
        PacketDistributor.sendToAllPlayers(new ClientboundPayload(message));
    }

    public static void sendToTracking(IClientPacket message, Entity entity)
    {
        preparePacketTypes();
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, new ClientboundPayload(message));
    }

    public static void sendToDimension(ResourceKey<Level> dimension, IClientPacket message)
    {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null)
        {
            ServerLevel level = server.getLevel(dimension);
            if (level != null)
                PacketDistributor.sendToPlayersInDimension(level, new ClientboundPayload(message));
        }
    }

    public static void sendToAllAround(IClientPacket message, double x, double y, double z, double range, ResourceKey<Level> dimension)
    {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null)
        {
            ServerLevel level = server.getLevel(dimension);
            if (level != null)
                PacketDistributor.sendToPlayersNear(level, null, x, y, z, range, new ClientboundPayload(message));
        }
    }

    public static void sendToAllAround(IClientPacket message, Vec3 position, double range, ResourceKey<Level> dimension)
    {
        sendToAllAround(message, position.x, position.y, position.z, range, dimension);
    }

    public static void sendToDonut(ResourceKey<Level> dimension, Vec3 center, double minRange, double maxRange, IClientPacket message)
    {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
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

    public static void sendToAllExcept(ResourceKey<Level> dimension, Vec3 center, double range, ServerPlayer except, IClientPacket message)
    {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
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
