package com.wolffsarmormod.network;

import com.wolffsarmormod.ArmorMod;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.server.ServerLifecycleHooks;

import net.minecraft.client.Minecraft;
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
    public static final ResourceLocation CHANNEL_ID = ResourceLocation.fromNamespaceAndPath(ArmorMod.MOD_ID, "main");
    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(CHANNEL_ID)
            .networkProtocolVersion(() -> PROTOCOL)
            .clientAcceptedVersions(PROTOCOL::equals)
            .serverAcceptedVersions(PROTOCOL::equals)
            .simpleChannel();

    private static final List<Entry> entries = new ArrayList<>();
    private static boolean frozen = false;
    private static int nextId = 0;

    private record Entry(Class<? extends PacketBase> clazz, NetworkDirection dir) {}

    /**
     * Initialisation method called from FMLCommonSetupEvent
     */
    public static void registerPackets()
    {
        registerS2C(PacketGunAnimation.class);
        registerS2C(PacketPlaySound.class);
        registerC2S(PacketGunFire.class);
        registerC2S(PacketReload.class);
        initAndRegister();
    }

    /** Register a packet type for C2S (client -> server). */
    public static boolean registerC2S(Class<? extends PacketBase> clz)
    {
        return add(clz, NetworkDirection.PLAY_TO_SERVER);
    }

    /** Register a packet type for S2C (server -> client). */
    public static boolean registerS2C(Class<? extends PacketBase> clz)
    {
        return add(clz, NetworkDirection.PLAY_TO_CLIENT);
    }

    private static boolean add(Class<? extends PacketBase> clz, NetworkDirection dir)
    {
        if (frozen)
        {
            ArmorMod.log.warn("Tried to register {} after init", clz.getCanonicalName());
            return false;
        }
        if (entries.size() >= 256)
        {
            ArmorMod.log.error("Packet limit exceeded by {}", clz.getCanonicalName());
            return false;
        }
        if (entries.stream().anyMatch(e -> e.clazz == clz && e.dir == dir))
        {
            ArmorMod.log.warn("Duplicate packet registration for {} {}", clz.getCanonicalName(), dir);
            return false;
        }
        entries.add(new Entry(clz, dir));
        return true;
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

    private static <T extends PacketBase> void registerOne(Class<T> clz, NetworkDirection dir) {
        CHANNEL.messageBuilder(clz, nextId++, dir)
                .encoder(PacketBase::encodeInto)
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
                        if (ctx.getDirection().getReceptionSide().isServer())
                        {
                            ServerPlayer sender = ctx.getSender(); // non-null on server
                            if (sender != null)
                                msg.handleServerSide(sender);
                        }
                        else
                        {
                            msg.handleClientSide(Minecraft.getInstance());
                        }
                    });
                    ctx.setPacketHandled(true);
                })
                .add();
    }

    // ---------------------------
    // Send helpers (Forge style)
    // ---------------------------

    /** server -> specific player */
    public static void sendTo(PacketBase msg, ServerPlayer player)
    {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }

    /** server -> everyone */
    public static void sendToAll(PacketBase msg)
    {
        CHANNEL.send(PacketDistributor.ALL.noArg(), msg);
    }

    /** server -> all in a dimension */
    public static void sendToDimension(ResourceKey<Level> dimension, PacketBase msg)
    {
        CHANNEL.send(PacketDistributor.DIMENSION.with(() -> dimension), msg);
    }

    /** server -> players near a point (like your TargetPoint) */
    public static void sendToAllAround(PacketBase msg, double x, double y, double z, double range, ResourceKey<Level> dim)
    {
        PacketDistributor.TargetPoint tp = new PacketDistributor.TargetPoint(x, y, z, range, dim);
        CHANNEL.send(PacketDistributor.NEAR.with(() -> tp), msg);
    }

    /** client -> server */
    public static void sendToServer(PacketBase msg)
    {
        CHANNEL.sendToServer(msg);
    }

    /** server -> all in a donut (min..max radius) */
    public static void sendToDonut(ResourceKey<Level> dim, Vec3 center, double minRange, double maxRange, PacketBase msg)
    {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null)
            return;
        ServerLevel level = server.getLevel(dim);
        if (level == null)
            return;

        double min2 = minRange * minRange, max2 = maxRange * maxRange;
        for (ServerPlayer p : level.players())
        {
            double d2 = p.position().distanceToSqr(center);
            if (d2 > min2 && d2 < max2)
                sendTo(msg, p);
        }
    }

    /** server -> all within range except one player */
    public static void sendToAllExcept(ResourceKey<Level> dim, Vec3 center, double range, ServerPlayer except, PacketBase msg)
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
