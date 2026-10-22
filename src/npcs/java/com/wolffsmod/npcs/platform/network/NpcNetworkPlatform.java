package com.wolffsmod.npcs.platform.network;

import com.wolffsmod.npcs.NpcsMod;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Version boundary for the module's packets. Forge 1.20.1 numbers them on one {@code SimpleChannel}; NeoForge 1.21.1
 * carries each in a payload type of its own ({@code wolffsmodnpcs:<name>}), registered once the payload event fires. The
 * packet records and their byte layouts are shared.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcNetworkPlatform
{
    private static final String VERSION = "3";
    private static final List<Consumer<PayloadRegistrar>> registrations = new ArrayList<>();
    private static final Map<Class<?>, CustomPacketPayload.Type<NpcPayload>> types = new HashMap<>();

    /** One module packet in the payload type registered for its class. */
    private record NpcPayload(CustomPacketPayload.Type<NpcPayload> type, Object packet) implements CustomPacketPayload
    {}

    /** Hooks the declared packets into the loader's networking. */
    public static void init(IEventBus modEventBus)
    {
        modEventBus.addListener(NpcNetworkPlatform::registerPayloads);
    }

    /** A packet the server sends; the handler is resolved and run on the client's main thread only. */
    public static <P> void toClient(String name, Class<P> type, BiConsumer<P, FriendlyByteBuf> write, Function<FriendlyByteBuf, P> read, Supplier<Consumer<P>> clientHandler)
    {
        CustomPacketPayload.Type<NpcPayload> payloadType = declare(name, type);
        StreamCodec<FriendlyByteBuf, NpcPayload> codec = codec(payloadType, type, write, read);
        registrations.add(registrar -> registrar.playToClient(payloadType, codec, (payload, context) -> runOnClient(context, clientHandler, type.cast(payload.packet()))));
    }

    /** A packet a client sends; the handler runs on the server thread with its sender. */
    public static <P> void toServer(String name, Class<P> type, BiConsumer<P, FriendlyByteBuf> write, Function<FriendlyByteBuf, P> read, BiConsumer<P, ServerPlayer> serverHandler)
    {
        CustomPacketPayload.Type<NpcPayload> payloadType = declare(name, type);
        StreamCodec<FriendlyByteBuf, NpcPayload> codec = codec(payloadType, type, write, read);
        registrations.add(registrar -> registrar.playToServer(payloadType, codec, (payload, context) -> runOnServer(context, serverHandler, type.cast(payload.packet()))));
    }

    /** A packet that travels both ways, with one handler for each side. */
    public static <P> void bidirectional(String name, Class<P> type, BiConsumer<P, FriendlyByteBuf> write, Function<FriendlyByteBuf, P> read, Supplier<Consumer<P>> clientHandler,
        BiConsumer<P, ServerPlayer> serverHandler)
    {
        CustomPacketPayload.Type<NpcPayload> payloadType = declare(name, type);
        StreamCodec<FriendlyByteBuf, NpcPayload> codec = codec(payloadType, type, write, read);
        registrations
            .add(registrar -> registrar.playBidirectional(payloadType, codec, new DirectionalPayloadHandler<>((payload, context) -> runOnClient(context, clientHandler, type.cast(payload.packet())),
                (payload, context) -> runOnServer(context, serverHandler, type.cast(payload.packet())))));
    }

    public static void sendToPlayer(ServerPlayer player, Object packet)
    {
        PacketDistributor.sendToPlayer(player, wrap(packet));
    }

    public static void sendToServer(Object packet)
    {
        PacketDistributor.sendToServer(wrap(packet));
    }

    public static void sendToTracking(Entity entity, Object packet)
    {
        PacketDistributor.sendToPlayersTrackingEntity(entity, wrap(packet));
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event)
    {
        PayloadRegistrar registrar = event.registrar(VERSION);
        registrations.forEach(registration -> registration.accept(registrar));
    }

    private static CustomPacketPayload.Type<NpcPayload> declare(String name, Class<?> type)
    {
        CustomPacketPayload.Type<NpcPayload> payloadType = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(NpcsMod.MOD_ID, name));
        types.put(type, payloadType);
        return payloadType;
    }

    private static <P> StreamCodec<FriendlyByteBuf, NpcPayload> codec(CustomPacketPayload.Type<NpcPayload> payloadType, Class<P> type, BiConsumer<P, FriendlyByteBuf> write,
        Function<FriendlyByteBuf, P> read)
    {
        return StreamCodec.of((buffer, payload) -> write.accept(type.cast(payload.packet()), buffer), buffer -> new NpcPayload(payloadType, read.apply(buffer)));
    }

    private static NpcPayload wrap(Object packet)
    {
        CustomPacketPayload.Type<NpcPayload> payloadType = types.get(packet.getClass());
        if (payloadType == null)
            throw new IllegalArgumentException("Unregistered NPC packet " + packet.getClass().getName());
        return new NpcPayload(payloadType, packet);
    }

    private static <P> void runOnClient(IPayloadContext context, Supplier<Consumer<P>> handler, P packet)
    {
        context.enqueueWork(() -> handler.get().accept(packet));
    }

    private static <P> void runOnServer(IPayloadContext context, BiConsumer<P, ServerPlayer> handler, P packet)
    {
        context.enqueueWork(() ->
        {
            if (context.player() instanceof ServerPlayer sender)
                handler.accept(packet, sender);
        });
    }
}
