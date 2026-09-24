package com.flansmodultimate.platform.network;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.network.ClientPacketDispatcher;
import com.flansmodultimate.network.IClientPacket;
import com.flansmodultimate.network.IPacket;
import com.flansmodultimate.network.IServerPacket;
import com.flansmodultimate.network.PacketBuffer;
import com.flansmodultimate.network.PacketHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Forge transport for the loader-neutral packets listed by {@link PacketHandler}. */
public final class NetworkPlatform
{
    public static final String PROTOCOL = "15";
    private static final ResourceLocation CHANNEL_ID = ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "main");
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(CHANNEL_ID)
            .networkProtocolVersion(() -> PROTOCOL)
            .clientAcceptedVersions(PROTOCOL::equals)
            .serverAcceptedVersions(PROTOCOL::equals)
            .simpleChannel();

    private static boolean registered;

    private record Entry(Class<? extends IPacket> type, NetworkDirection direction) {}

    private NetworkPlatform() {}

    /** Registers every packet with a deterministic id. Called once from common setup. */
    public static synchronized void register()
    {
        if (registered)
            return;
        registered = true;

        List<Entry> entries = new ArrayList<>();
        PacketHandler.clientPacketTypes().forEach(type -> entries.add(new Entry(type, NetworkDirection.PLAY_TO_CLIENT)));
        PacketHandler.serverPacketTypes().forEach(type -> entries.add(new Entry(type, NetworkDirection.PLAY_TO_SERVER)));
        entries.sort(Comparator
            .comparing((Entry e) -> e.type().getName(), String.CASE_INSENSITIVE_ORDER)
            .thenComparing(e -> e.direction().name()));

        int nextId = 0;
        for (Entry entry : entries)
            registerOne(entry.type(), entry.direction(), nextId++);
    }

    private static <T extends IPacket> void registerOne(Class<T> type, NetworkDirection direction, int id)
    {
        CHANNEL.messageBuilder(type, id, direction)
            .encoder((packet, buf) -> packet.encodeInto(new PacketBuffer(buf)))
            .decoder(buf -> PacketHandler.<T>decode(type, new PacketBuffer(buf)))
            .consumerMainThread((msg, ctxSup) -> {
                NetworkEvent.Context ctx = ctxSup.get();
                ctx.enqueueWork(() -> {
                    if (ctx.getDirection().getReceptionSide().isServer() && msg instanceof IServerPacket serverPacket)
                    {
                        // Server
                        ServerPlayer sender = ctx.getSender();
                        if (sender != null)
                            serverPacket.handleServerSide(sender, sender.serverLevel());
                    }
                    else if (msg instanceof IClientPacket clientPacket)
                    {
                        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketDispatcher.dispatch(clientPacket));
                    }
                });
                ctx.setPacketHandled(true);
            })
            .add();
    }

    public static void sendToServer(IServerPacket message)
    {
        CHANNEL.sendToServer(message);
    }

    public static void sendToPlayer(ServerPlayer player, IClientPacket message)
    {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static void sendToAll(IClientPacket message)
    {
        CHANNEL.send(PacketDistributor.ALL.noArg(), message);
    }

    public static void sendToTrackingEntityAndSelf(Entity entity, IClientPacket message)
    {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), message);
    }

    public static void sendToDimension(ResourceKey<Level> dimension, IClientPacket message)
    {
        CHANNEL.send(PacketDistributor.DIMENSION.with(() -> dimension), message);
    }

    public static void sendToNear(ResourceKey<Level> dimension, double x, double y, double z, double range, IClientPacket message)
    {
        PacketDistributor.TargetPoint point = new PacketDistributor.TargetPoint(x, y, z, range, dimension);
        CHANNEL.send(PacketDistributor.NEAR.with(() -> point), message);
    }
}
