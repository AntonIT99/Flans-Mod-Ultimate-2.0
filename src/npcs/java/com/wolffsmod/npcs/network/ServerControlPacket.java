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
public record ServerControlPacket(Action action, int nearInterval, int farInterval, boolean profiling, Ranges ranges, int seconds)
{
    private static final Ranges NO_RANGES = new Ranges(0, 0, 0, 0);

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

    /**
     * The ranges of the server, in blocks.
     *
     * @param aggro
     *            aggro range a new NPC starts with
     * @param firing
     *            firing range a new NPC starts with
     * @param modelAggro
     *            aggro range an NPC inherits from a Flan's model
     * @param modelFiring
     *            firing range an NPC inherits from a Flan's model
     */
    public record Ranges(int aggro, int firing, int modelAggro, int modelFiring)
    {
        private void write(FriendlyByteBuf buffer)
        {
            buffer.writeVarInt(aggro);
            buffer.writeVarInt(firing);
            buffer.writeVarInt(modelAggro);
            buffer.writeVarInt(modelFiring);
        }

        private static Ranges read(FriendlyByteBuf buffer)
        {
            return new Ranges(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
        }
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
        return new ServerControlPacket(action, 0, 0, false, NO_RANGES, seconds);
    }

    public static ServerControlPacket set(int nearInterval, int farInterval, boolean profiling, Ranges ranges)
    {
        return new ServerControlPacket(Action.SET, nearInterval, farInterval, profiling, ranges, 0);
    }

    public static ServerControlPacket values(NpcServerSettings settings)
    {
        Ranges ranges = new Ranges(settings.getAggroRange(), settings.getFiringRange(), settings.getModelAggroRange(), settings.getModelFiringRange());
        return new ServerControlPacket(Action.VALUES, settings.getSearchNearInterval(), settings.getSearchFarInterval(), settings.isProfiling(), ranges, 0);
    }

    public void write(FriendlyByteBuf buffer)
    {
        buffer.writeEnum(action);
        buffer.writeVarInt(nearInterval);
        buffer.writeVarInt(farInterval);
        buffer.writeBoolean(profiling);
        ranges.write(buffer);
        buffer.writeVarInt(seconds);
    }

    public static ServerControlPacket read(FriendlyByteBuf buffer)
    {
        return new ServerControlPacket(buffer.readEnum(Action.class), buffer.readVarInt(), buffer.readVarInt(), buffer.readBoolean(), Ranges.read(buffer), buffer.readVarInt());
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
            case SET -> settings.set(nearInterval, farInterval, profiling, ranges.aggro(), ranges.firing(), ranges.modelAggro(), ranges.modelFiring());
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
