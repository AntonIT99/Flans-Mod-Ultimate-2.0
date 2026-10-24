package com.wolffsmod.npcs.network;

import com.wolffsmod.npcs.diag.NpcServerSettings;
import com.wolffsmod.npcs.diag.ServerBenchmark;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * The Server pages of the Wolff's Mod tab talking to the server: a client asks for the world-wide NPC settings, changes
 * them, hands the ranges to every NPC or starts a server benchmark, and the server answers with the settings as they now
 * are.
 *
 * @param seconds
 *            length of the benchmark to start; unused otherwise
 */
public record ServerControlPacket(Action action, int nearInterval, int farInterval, boolean profiling, int aggroRange, int firingRange, int seconds)
{

    /** What the packet asks for or carries. */
    public enum Action
    {
        /** Client to server: send the settings. */
        GET,
        /** Client to server: change the settings. */
        SET,
        /** Client to server: give every NPC the saved ranges. Carries no values, so it cannot undo a change sent just before. */
        APPLY_RANGES,
        /** Client to server: run a server benchmark. */
        BENCHMARK,
        /** Server to client: these are the settings. */
        VALUES
    }

    public static ServerControlPacket request()
    {
        return command(Action.GET, 0);
    }

    public static ServerControlPacket applyRanges()
    {
        return command(Action.APPLY_RANGES, 0);
    }

    public static ServerControlPacket benchmark(int seconds)
    {
        return command(Action.BENCHMARK, seconds);
    }

    private static ServerControlPacket command(Action action, int seconds)
    {
        return new ServerControlPacket(action, 0, 0, false, 0, 0, seconds);
    }

    public static ServerControlPacket set(int nearInterval, int farInterval, boolean profiling, int aggroRange, int firingRange)
    {
        return new ServerControlPacket(Action.SET, nearInterval, farInterval, profiling, aggroRange, firingRange, 0);
    }

    public static ServerControlPacket values(NpcServerSettings settings)
    {
        return new ServerControlPacket(Action.VALUES, settings.getSearchNearInterval(), settings.getSearchFarInterval(), settings.isProfiling(), settings.getAggroRange(), settings.getFiringRange(),
            0);
    }

    public void write(FriendlyByteBuf buffer)
    {
        buffer.writeEnum(action);
        buffer.writeVarInt(nearInterval);
        buffer.writeVarInt(farInterval);
        buffer.writeBoolean(profiling);
        buffer.writeVarInt(aggroRange);
        buffer.writeVarInt(firingRange);
        buffer.writeVarInt(seconds);
    }

    public static ServerControlPacket read(FriendlyByteBuf buffer)
    {
        return new ServerControlPacket(buffer.readEnum(Action.class), buffer.readVarInt(), buffer.readVarInt(), buffer.readBoolean(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
    }

    /** Carries out a client's request. Anyone may read the settings; changing them, applying them and benchmarking take an operator. */
    public void handle(ServerPlayer player)
    {
        MinecraftServer server = player.server;
        NpcServerSettings settings = NpcServerSettings.get(server);
        if (action == Action.GET)
        {
            NpcNetwork.sendTo(player, values(settings));
            return;
        }
        if (action == Action.VALUES)
            return;
        if (!player.hasPermissions(2) && !server.isSingleplayerOwner(player.getGameProfile()))
        {
            player.sendSystemMessage(Component.translatable("commands.wolffsmodnpcs.server.denied"));
            NpcNetwork.sendTo(player, values(settings));
            return;
        }
        switch (action)
        {
            case SET -> settings.set(nearInterval, farInterval, profiling, aggroRange, firingRange);
            case APPLY_RANGES -> {
                settings.applyRangesToAll();
                player.sendSystemMessage(Component.translatable("commands.wolffsmodnpcs.server.ranges_applied", settings.getAggroRange(), settings.getFiringRange()));
            }
            default -> {
                if (!ServerBenchmark.start(player.createCommandSourceStack(), seconds))
                    player.sendSystemMessage(Component.translatable("commands.wolffsmodnpcs.benchmark.busy"));
            }
        }
        NpcNetwork.sendTo(player, values(settings));
    }
}
