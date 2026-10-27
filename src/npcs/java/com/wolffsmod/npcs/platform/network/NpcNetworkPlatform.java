package com.wolffsmod.npcs.platform.network;

import com.wolffsmod.npcs.NpcsMod;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Version boundary for the module's packets. Forge 1.20.1 numbers them on one {@link SimpleChannel} in the order they are
 * declared; NeoForge 1.21.1 carries each in a payload type of its own, named after the packet. The packet records and their
 * byte layouts are shared.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcNetworkPlatform
{
    private static final String VERSION = "10";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(ResourceLocation.fromNamespaceAndPath(NpcsMod.MOD_ID, "main"), () -> VERSION, VERSION::equals, VERSION::equals);
    private static int nextId;

    /** Hooks the declared packets into the loader's networking; the Forge channel registers them as they are declared. */
    // Part of the cross-version platform signature; NeoForge registers payloads from a mod-bus event.
    @SuppressWarnings("java:S1172")
    public static void init(IEventBus modEventBus)
    {
        // The channel already carries every declared packet.
    }

    /** A packet the server sends; the handler is resolved and run on the client's main thread only. */
    // The name identifies NeoForge's payload type; the Forge channel numbers packets instead.
    @SuppressWarnings("java:S1172")
    public static <P> void toClient(String name, Class<P> type, BiConsumer<P, FriendlyByteBuf> write, Function<FriendlyByteBuf, P> read, Supplier<Consumer<P>> clientHandler)
    {
        CHANNEL.registerMessage(nextId++, type, write, read, (packet, context) -> runOnClient(context.get(), clientHandler, packet), Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    /** A packet a client sends; the handler runs on the server thread with its sender. */
    // The name identifies NeoForge's payload type; the Forge channel numbers packets instead.
    @SuppressWarnings("java:S1172")
    public static <P> void toServer(String name, Class<P> type, BiConsumer<P, FriendlyByteBuf> write, Function<FriendlyByteBuf, P> read, BiConsumer<P, ServerPlayer> serverHandler)
    {
        CHANNEL.registerMessage(nextId++, type, write, read, (packet, context) -> runOnServer(context.get(), serverHandler, packet), Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    /** A packet that travels both ways, with one handler for each side. */
    // The name identifies NeoForge's payload type; the Forge channel numbers packets instead.
    @SuppressWarnings("java:S1172")
    public static <P> void bidirectional(String name, Class<P> type, BiConsumer<P, FriendlyByteBuf> write, Function<FriendlyByteBuf, P> read, Supplier<Consumer<P>> clientHandler,
        BiConsumer<P, ServerPlayer> serverHandler)
    {
        CHANNEL.registerMessage(nextId++, type, write, read, (packet, context) ->
        {
            if (context.get().getSender() != null)
                runOnServer(context.get(), serverHandler, packet);
            else
                runOnClient(context.get(), clientHandler, packet);
        });
    }

    public static void sendToPlayer(ServerPlayer player, Object packet)
    {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToServer(Object packet)
    {
        CHANNEL.sendToServer(packet);
    }

    public static void sendToTracking(Entity entity, Object packet)
    {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), packet);
    }

    private static <P> void runOnClient(NetworkEvent.Context context, Supplier<Consumer<P>> handler, P packet)
    {
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handler.get().accept(packet)));
        context.setPacketHandled(true);
    }

    private static <P> void runOnServer(NetworkEvent.Context context, BiConsumer<P, ServerPlayer> handler, P packet)
    {
        ServerPlayer sender = context.getSender();
        if (sender != null)
            context.enqueueWork(() -> handler.accept(packet, sender));
        context.setPacketHandled(true);
    }
}
