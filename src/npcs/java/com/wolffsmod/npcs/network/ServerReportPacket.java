package com.wolffsmod.npcs.network;

import com.wolffsmod.npcs.diag.BenchmarkResult;
import com.wolffsmod.npcs.diag.NpcBudgets;
import com.wolffsmod.npcs.diag.NpcCensus;
import com.wolffsmod.npcs.diag.ServerBenchmark;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * What the Server pages show besides the settings, server to client: the loaded NPCs of every level, the searches the
 * limits put off in the last tick, and the latest server benchmarks.
 *
 * @param census
 *            one row per level with loaded NPCs
 * @param deferred
 *            searches the server's limits put off in the last tick that searched
 * @param benchmarks
 *            the latest finished server benchmarks, oldest first
 */
public record ServerReportPacket(List<NpcCensus.Row> census, NpcBudgets.Deferred deferred, List<BenchmarkResult> benchmarks)
{
    /** More entries than a list holds in practice, so a corrupt packet cannot ask for a huge one. */
    private static final int MAX_ENTRIES = 256;

    public static ServerReportPacket of(MinecraftServer server)
    {
        return new ServerReportPacket(NpcCensus.take(server), NpcBudgets.deferred(), ServerBenchmark.results());
    }

    public void write(FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(census.size());
        census.forEach(row -> row.write(buffer));
        buffer.writeVarInt(deferred.targets());
        buffer.writeVarInt(deferred.paths());
        buffer.writeVarInt(benchmarks.size());
        benchmarks.forEach(result -> result.write(buffer));
    }

    public static ServerReportPacket read(FriendlyByteBuf buffer)
    {
        List<NpcCensus.Row> census = list(buffer, NpcCensus.Row::read);
        NpcBudgets.Deferred deferred = new NpcBudgets.Deferred(buffer.readVarInt(), buffer.readVarInt());
        return new ServerReportPacket(census, deferred, list(buffer, BenchmarkResult::read));
    }

    private static <T> List<T> list(FriendlyByteBuf buffer, Function<FriendlyByteBuf, T> reader)
    {
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_ENTRIES)
            throw new IllegalArgumentException("Too many report entries: " + count);
        List<T> entries = new ArrayList<>(count);
        for (int index = 0; index < count; index++)
            entries.add(reader.apply(buffer));
        return List.copyOf(entries);
    }
}
