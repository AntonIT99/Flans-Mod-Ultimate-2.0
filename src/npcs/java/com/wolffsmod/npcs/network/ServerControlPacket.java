package com.wolffsmod.npcs.network;

import com.wolffsmod.npcs.diag.NpcServerSettings;
import com.wolffsmod.npcs.diag.ServerBenchmark;
import com.wolffsmod.npcs.sim.NpcServerRules;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * The Server pages of the Wolff's Mod tab talking to the server: a client asks for the world-wide NPC settings, changes
 * them, hands the ranges or the search intervals to every NPC, starts a server benchmark or asks for the NPC census and
 * the latest benchmarks, and the server answers with the settings as they now are.
 *
 * @param values
 *            the settings, in the order of {@link NpcServerSettings.Setting}; empty for a request that carries none
 * @param seconds
 *            length of the benchmark to start; unused otherwise
 */
public record ServerControlPacket(Action action, List<Integer> values, int seconds)
{
    /** More values than any version sends, so a corrupt packet cannot ask for a huge list. */
    private static final int MAX_VALUES = 64;

    /** What the packet asks for or carries. */
    public enum Action
    {
        /** Client to server: send the settings. */
        GET,
        /** Client to server: change the settings. */
        SET,
        /** Client to server: give every NPC the saved ranges. Carries no values, so it cannot undo a change sent just before. */
        APPLY_RANGES,
        /** Client to server: make every NPC follow the saved search intervals. */
        APPLY_SEARCH,
        /** Client to server: run a server benchmark. */
        BENCHMARK,
        /** Client to server: send the NPC census and the latest benchmarks. */
        REPORT,
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

    public static ServerControlPacket applySearch()
    {
        return command(Action.APPLY_SEARCH, 0);
    }

    public static ServerControlPacket benchmark(int seconds)
    {
        return command(Action.BENCHMARK, seconds);
    }

    public static ServerControlPacket report()
    {
        return command(Action.REPORT, 0);
    }

    private static ServerControlPacket command(Action action, int seconds)
    {
        return new ServerControlPacket(action, List.of(), seconds);
    }

    public static ServerControlPacket set(List<Integer> values)
    {
        return new ServerControlPacket(Action.SET, List.copyOf(values), 0);
    }

    public static ServerControlPacket values(NpcServerSettings settings)
    {
        return new ServerControlPacket(Action.VALUES, settings.all(), 0);
    }

    public void write(FriendlyByteBuf buffer)
    {
        buffer.writeEnum(action);
        buffer.writeVarInt(values.size());
        values.forEach(buffer::writeVarInt);
        buffer.writeVarInt(seconds);
    }

    public static ServerControlPacket read(FriendlyByteBuf buffer)
    {
        Action action = buffer.readEnum(Action.class);
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_VALUES)
            throw new IllegalArgumentException("Too many NPC server settings: " + count);
        List<Integer> values = new ArrayList<>(count);
        for (int index = 0; index < count; index++)
            values.add(buffer.readVarInt());
        return new ServerControlPacket(action, List.copyOf(values), buffer.readVarInt());
    }

    /**
     * Carries out a client's request. Anyone may read the settings, the census and the latest benchmarks; changing the
     * settings, applying them and benchmarking take an operator.
     */
    public void handle(ServerPlayer player)
    {
        MinecraftServer server = player.server;
        NpcServerSettings settings = NpcServerSettings.get(server);
        if (action == Action.GET)
        {
            NpcNetwork.sendTo(player, values(settings));
            return;
        }
        if (action == Action.REPORT)
        {
            NpcNetwork.sendTo(player, ServerReportPacket.of(server));
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
            case SET -> {
                if (settings.set(values))
                    NpcServerRules.healthChanged(server);
                // Every player draws NPC name tags at the server's range, so every player hears of the change.
                server.getPlayerList().getPlayers().forEach(other -> NpcNetwork.sendTo(other, values(settings)));
                return;
            }
            case APPLY_RANGES -> {
                settings.applyRangesToAll();
                player.sendSystemMessage(Component.translatable("commands.wolffsmodnpcs.server.ranges_applied", settings.getAggroRange(), settings.getFiringRange()));
            }
            case APPLY_SEARCH -> {
                settings.applySearchToAll();
                player.sendSystemMessage(Component.translatable("commands.wolffsmodnpcs.server.search_applied", settings.getSearchNearInterval(), settings.getSearchFarInterval()));
            }
            default -> {
                if (!ServerBenchmark.start(player.createCommandSourceStack(), seconds))
                    player.sendSystemMessage(Component.translatable("commands.wolffsmodnpcs.benchmark.busy"));
            }
        }
        NpcNetwork.sendTo(player, values(settings));
    }
}
