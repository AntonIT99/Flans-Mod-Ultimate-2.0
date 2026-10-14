package com.wolffsmod.npcs.network;

import com.wolffsmod.npcs.NpcsMod;
import com.wolffsmod.npcs.sim.NpcVehicleState;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * The module's own channel. Everything else it sends rides on Custom NPCs' packets. NeoForge carries each packet record
 * in a payload of its own; the records and their byte layouts are the same as on Forge.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcNetwork
{
    private static final String VERSION = "2";

    private record VehicleState(VehicleStatePacket packet) implements CustomPacketPayload
    {
        private static final Type<VehicleState> TYPE = payloadType("vehicle_state");
        private static final StreamCodec<FriendlyByteBuf, VehicleState> CODEC = codec(VehicleStatePacket::write, VehicleStatePacket::read, VehicleState::new, VehicleState::packet);

        @Override
        @NotNull
        public Type<VehicleState> type()
        {
            return TYPE;
        }
    }

    private record ServerControl(ServerControlPacket packet) implements CustomPacketPayload
    {
        private static final Type<ServerControl> TYPE = payloadType("server_control");
        private static final StreamCodec<FriendlyByteBuf, ServerControl> CODEC = codec(ServerControlPacket::write, ServerControlPacket::read, ServerControl::new, ServerControl::packet);

        @Override
        @NotNull
        public Type<ServerControl> type()
        {
            return TYPE;
        }
    }

    private record ApplyLoadout(ApplyLoadoutPacket packet) implements CustomPacketPayload
    {
        private static final Type<ApplyLoadout> TYPE = payloadType("apply_loadout");
        private static final StreamCodec<FriendlyByteBuf, ApplyLoadout> CODEC = codec(ApplyLoadoutPacket::write, ApplyLoadoutPacket::read, ApplyLoadout::new, ApplyLoadout::packet);

        @Override
        @NotNull
        public Type<ApplyLoadout> type()
        {
            return TYPE;
        }
    }

    private record PartState(PartStatePacket packet) implements CustomPacketPayload
    {
        private static final Type<PartState> TYPE = payloadType("part_state");
        private static final StreamCodec<FriendlyByteBuf, PartState> CODEC = codec(PartStatePacket::write, PartStatePacket::read, PartState::new, PartState::packet);

        @Override
        @NotNull
        public Type<PartState> type()
        {
            return TYPE;
        }
    }

    public static void register(RegisterPayloadHandlersEvent event)
    {
        PayloadRegistrar registrar = event.registrar(VERSION);
        // The client handler class is only loaded when a client runs one of these handlers.
        registrar.playToClient(VehicleState.TYPE, VehicleState.CODEC, (payload, context) -> context.enqueueWork(() -> com.wolffsmod.npcs.client.NpcClientPackets.vehicleState(payload.packet())));
        registrar.playBidirectional(ServerControl.TYPE, ServerControl.CODEC, new DirectionalPayloadHandler<>(
            (payload, context) -> context.enqueueWork(() -> com.wolffsmod.npcs.client.NpcClientPackets.serverSettings(payload.packet())), (payload, context) -> context.enqueueWork(() ->
            {
                if (context.player() instanceof ServerPlayer sender)
                    payload.packet().handle(sender);
            })));
        registrar.playToServer(ApplyLoadout.TYPE, ApplyLoadout.CODEC, (payload, context) -> context.enqueueWork(() ->
        {
            if (context.player() instanceof ServerPlayer sender)
                payload.packet().handle(sender);
        }));
        registrar.playToClient(PartState.TYPE, PartState.CODEC, (payload, context) -> context.enqueueWork(() -> com.wolffsmod.npcs.client.NpcClientPackets.partState(payload.packet())));
    }

    public static void sendTo(ServerPlayer player, ServerControlPacket packet)
    {
        PacketDistributor.sendToPlayer(player, new ServerControl(packet));
    }

    public static void sendToServer(ServerControlPacket packet)
    {
        PacketDistributor.sendToServer(new ServerControl(packet));
    }

    public static void sendToServer(ApplyLoadoutPacket packet)
    {
        PacketDistributor.sendToServer(new ApplyLoadout(packet));
    }

    public static void sendToTracking(EntityNPCInterface npc, VehicleStatePacket packet)
    {
        PacketDistributor.sendToPlayersTrackingEntity(npc, new VehicleState(packet));
    }

    public static void sendToTracking(EntityNPCInterface npc, PartStatePacket packet)
    {
        PacketDistributor.sendToPlayersTrackingEntity(npc, new PartState(packet));
    }

    /** A client that starts tracking an NPC needs its current state, not just the next change. */
    public static void startTracking(PlayerEvent.StartTracking event)
    {
        if (event.getTarget() instanceof EntityNPCInterface npc && event.getEntity() instanceof ServerPlayer player && NpcVehicleState.of(npc).isActive())
        {
            PacketDistributor.sendToPlayer(player, new VehicleState(NpcVehicleState.of(npc).packet(npc)));
            NpcVehicleState.of(npc).getParts().packet(npc).ifPresent(packet -> PacketDistributor.sendToPlayer(player, new PartState(packet)));
        }
    }

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> payloadType(String path)
    {
        return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(NpcsMod.MOD_ID, path));
    }

    private static <P, T extends CustomPacketPayload> StreamCodec<FriendlyByteBuf, T> codec(BiConsumer<P, FriendlyByteBuf> write, Function<FriendlyByteBuf, P> read, Function<P, T> wrap,
        Function<T, P> unwrap)
    {
        return StreamCodec.of((buffer, payload) -> write.accept(unwrap.apply(payload), buffer), buffer -> wrap.apply(read.apply(buffer)));
    }
}
