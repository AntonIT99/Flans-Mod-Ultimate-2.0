package com.flansmodultimate.network;

import com.flansmodultimate.FlansMod;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.server.ServerLifecycleHooks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PacketHandler {

    public static final String PROTOCOL = "1";
    public static final ResourceLocation CHANNEL_ID = ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "main");
    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(CHANNEL_ID)
            .networkProtocolVersion(() -> PROTOCOL)
            .clientAcceptedVersions(PROTOCOL::equals)
            .serverAcceptedVersions(PROTOCOL::equals)
            .simpleChannel();

    private static final List<Entry> entries = new ArrayList<>();
    private static boolean frozen = false;
    private static int nextId = 0;

    private record Entry(Class<? extends IPacket> clazz, NetworkDirection dir) {}

    /**
     * Initialisation method called from FMLCommonSetupEvent
     */
    public static void registerPackets()
    {
        // Server to Client Packets
        registerS2C(PacketAllowDebug.class);
        registerS2C(PacketBlockHitEffect.class);
        registerS2C(PacketBulletTrail.class);
        registerS2C(PacketFlak.class);
        registerS2C(PacketGunAnimation.class);
        registerS2C(PacketHitMarker.class);
        registerS2C(PacketParticle.class);
        registerS2C(PacketPlaySound.class);

        // Client to Server Packets
        registerC2S(PacketGunFire.class);
        registerC2S(PacketRequestDebug.class);
        registerC2S(PacketReload.class);

        initAndRegister();
    }

    /** Register a packet type for C2S (client -> server). */
    public static void registerC2S(Class<? extends IServerPacket> clz)
    {
        add(clz, NetworkDirection.PLAY_TO_SERVER);
    }

    /** Register a packet type for S2C (server -> client). */
    public static void registerS2C(Class<? extends IClientPacket> clz)
    {
        add(clz, NetworkDirection.PLAY_TO_CLIENT);
    }

    private static void add(Class<? extends IPacket> clz, NetworkDirection dir)
    {
        if (frozen)
        {
            FlansMod.log.warn("Tried to register {} after init", clz.getCanonicalName());
        }
        if (entries.size() >= 256)
        {
            FlansMod.log.error("Packet limit exceeded by {}", clz.getCanonicalName());
        }
        if (entries.stream().anyMatch(e -> e.clazz == clz && e.dir == dir))
        {
            FlansMod.log.warn("Duplicate packet registration for {} {}", clz.getCanonicalName(), dir);
        }
        entries.add(new Entry(clz, dir));
    }

    /** Call during common setup (inside enqueueWork). Sort deterministically and register with IDs. */
    public static void initAndRegister()
    {
        if (frozen)
            return;
        frozen = true;

        entries.sort(Comparator.comparing(e -> e.getClass().getCanonicalName(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(e -> e.getClass().getCanonicalName()));

        for (Entry e : entries)
            registerOne(e.clazz, e.dir);
    }

    private static <T extends IPacket> void registerOne(Class<T> clz, NetworkDirection dir) {
        CHANNEL.messageBuilder(clz, nextId++, dir)
                .encoder(IPacket::encodeInto)
                .decoder(buf -> {
                    try
                    {
                        T p = clz.getDeclaredConstructor().newInstance();
                        p.decodeInto(buf);
                        return p;
                    }
                    catch (Exception ex)
                    {
                        throw new RuntimeException("Failed to construct/decode " + clz.getCanonicalName(), ex);
                    }
                })
                .consumerMainThread((msg, ctxSup) -> {
                    NetworkEvent.Context ctx = ctxSup.get();
                    ctx.enqueueWork(() -> {
                        if (ctx.getDirection().getReceptionSide().isServer() && msg instanceof IServerPacket serverPacket)
                        {
                            // Server
                            ServerPlayer sender = ctx.getSender();
                            if (sender != null)
                                serverPacket.handleServerSide(sender);
                        }
                        else if (msg instanceof IClientPacket clientPacket)
                        {
                            // Client
                            Minecraft mc = Minecraft.getInstance();
                            ClientLevel level = mc.level;
                            LocalPlayer player = mc.player;
                            if (level != null && player != null)
                                clientPacket.handleClientSide(player, level);
                        }
                    });
                    ctx.setPacketHandled(true);
                })
                .add();
    }

    /** server -> specific player */
    public static void sendTo(IClientPacket msg, ServerPlayer player)
    {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }

    /** server -> everyone */
    public static void sendToAll(IClientPacket msg)
    {
        CHANNEL.send(PacketDistributor.ALL.noArg(), msg);
    }

    /** server -> all in a dimension */
    public static void sendToDimension(ResourceKey<Level> dimension, IClientPacket msg)
    {
        CHANNEL.send(PacketDistributor.DIMENSION.with(() -> dimension), msg);
    }

    /** server -> players near a point (like your TargetPoint) */
    public static void sendToAllAround(IClientPacket msg, double x, double y, double z, double range, ResourceKey<Level> dim)
    {
        PacketDistributor.TargetPoint tp = new PacketDistributor.TargetPoint(x, y, z, range, dim);
        CHANNEL.send(PacketDistributor.NEAR.with(() -> tp), msg);
    }

    /** client -> server */
    public static void sendToServer(IServerPacket msg)
    {
        CHANNEL.sendToServer(msg);
    }

    /** server -> all in a donut (min..max radius) */
    public static void sendToDonut(ResourceKey<Level> dim, Vec3 center, double minRange, double maxRange, IClientPacket msg)
    {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null)
            return;
        ServerLevel level = server.getLevel(dim);
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

    /** server -> all within range except one player */
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
