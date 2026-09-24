package com.flansmodultimate.platform.network;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.network.ClientPacketDispatcher;
import com.flansmodultimate.network.IClientPacket;
import com.flansmodultimate.network.IPacket;
import com.flansmodultimate.network.IServerPacket;
import com.flansmodultimate.network.PacketBuffer;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.platform.PlatformEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * NeoForge transport for the loader-neutral packets listed by {@link PacketHandler}. Packets are
 * carried in one payload envelope per direction, prefixed with their id in that direction's list.
 */
public final class NetworkPlatform
{
    public static final String PROTOCOL = "8";

    private static final Map<Class<? extends IClientPacket>, Integer> CLIENT_PACKET_IDS = new HashMap<>();
    private static final Map<Class<? extends IServerPacket>, Integer> SERVER_PACKET_IDS = new HashMap<>();
    private static boolean prepared;

    private NetworkPlatform() {}

    public static void register(RegisterPayloadHandlersEvent event)
    {
        preparePacketIds();
        PayloadRegistrar registrar = event.registrar(PROTOCOL);
        registrar.playToClient(ClientboundPayload.TYPE, ClientboundPayload.STREAM_CODEC, NetworkPlatform::handleClientPayload);
        registrar.playToServer(ServerboundPayload.TYPE, ServerboundPayload.STREAM_CODEC, NetworkPlatform::handleServerPayload);
    }

    private static synchronized void preparePacketIds()
    {
        if (prepared)
            return;

        List<Class<? extends IClientPacket>> clientTypes = PacketHandler.clientPacketTypes();
        for (int i = 0; i < clientTypes.size(); i++)
            CLIENT_PACKET_IDS.put(clientTypes.get(i), i);
        List<Class<? extends IServerPacket>> serverTypes = PacketHandler.serverPacketTypes();
        for (int i = 0; i < serverTypes.size(); i++)
            SERVER_PACKET_IDS.put(serverTypes.get(i), i);
        prepared = true;
    }

    private static void handleClientPayload(ClientboundPayload payload, IPayloadContext context)
    {
        context.enqueueWork(() -> ClientPacketDispatcher.dispatch(payload.packet()));
    }

    private static void handleServerPayload(ServerboundPayload payload, IPayloadContext context)
    {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer sender)
                payload.packet().handleServerSide(sender, sender.serverLevel());
        });
    }

    private static <T extends IPacket> T decodePacket(RegistryFriendlyByteBuf buffer, List<Class<? extends T>> types)
    {
        int id = buffer.readVarInt();
        if (id < 0 || id >= types.size())
            throw new IllegalArgumentException("Unknown Flan's Mod packet id " + id);
        return PacketHandler.decode(types.get(id), new PacketBuffer(buffer));
    }

    private static <T extends IPacket> void encodePacket(RegistryFriendlyByteBuf buffer, T packet, Map<Class<? extends T>, Integer> ids)
    {
        @SuppressWarnings("unchecked")
        Integer id = ids.get((Class<? extends T>) packet.getClass());
        if (id == null)
            throw new IllegalArgumentException("Unregistered Flan's Mod packet " + packet.getClass().getName());
        buffer.writeVarInt(id);
        packet.encodeInto(new PacketBuffer(buffer));
    }

    private record ClientboundPayload(IClientPacket packet) implements CustomPacketPayload
    {
        private static final Type<ClientboundPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "clientbound"));
        private static final StreamCodec<RegistryFriendlyByteBuf, ClientboundPayload> STREAM_CODEC = StreamCodec.ofMember(
            (payload, buffer) -> encodePacket(buffer, payload.packet, CLIENT_PACKET_IDS),
            buffer -> new ClientboundPayload(decodePacket(buffer, PacketHandler.clientPacketTypes()))
        );

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }
    }

    private record ServerboundPayload(IServerPacket packet) implements CustomPacketPayload
    {
        private static final Type<ServerboundPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "serverbound"));
        private static final StreamCodec<RegistryFriendlyByteBuf, ServerboundPayload> STREAM_CODEC = StreamCodec.ofMember(
            (payload, buffer) -> encodePacket(buffer, payload.packet, SERVER_PACKET_IDS),
            buffer -> new ServerboundPayload(decodePacket(buffer, PacketHandler.serverPacketTypes()))
        );

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }
    }

    public static void sendToServer(IServerPacket message)
    {
        preparePacketIds();
        PacketDistributor.sendToServer(new ServerboundPayload(message));
    }

    public static void sendToPlayer(ServerPlayer player, IClientPacket message)
    {
        preparePacketIds();
        PacketDistributor.sendToPlayer(player, new ClientboundPayload(message));
    }

    public static void sendToAll(IClientPacket message)
    {
        preparePacketIds();
        PacketDistributor.sendToAllPlayers(new ClientboundPayload(message));
    }

    public static void sendToTrackingEntityAndSelf(Entity entity, IClientPacket message)
    {
        preparePacketIds();
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, new ClientboundPayload(message));
    }

    public static void sendToDimension(ResourceKey<Level> dimension, IClientPacket message)
    {
        ServerLevel level = serverLevel(dimension);
        if (level != null)
        {
            preparePacketIds();
            PacketDistributor.sendToPlayersInDimension(level, new ClientboundPayload(message));
        }
    }

    public static void sendToNear(ResourceKey<Level> dimension, double x, double y, double z, double range, IClientPacket message)
    {
        ServerLevel level = serverLevel(dimension);
        if (level != null)
        {
            preparePacketIds();
            PacketDistributor.sendToPlayersNear(level, null, x, y, z, range, new ClientboundPayload(message));
        }
    }

    private static ServerLevel serverLevel(ResourceKey<Level> dimension)
    {
        MinecraftServer server = PlatformEnvironment.currentServer();
        return server == null ? null : server.getLevel(dimension);
    }
}
