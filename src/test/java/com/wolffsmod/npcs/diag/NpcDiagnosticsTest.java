package com.wolffsmod.npcs.diag;

import com.wolffsmod.npcs.network.ServerControlPacket;
import com.wolffsmod.npcs.network.ServerReportPacket;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NpcDiagnosticsTest
{
    @Test
    void statisticsOfNothingAreZero()
    {
        BenchmarkStats stats = new BenchmarkStats();
        assertEquals(0, stats.count());
        assertEquals(0D, stats.mean());
        assertEquals(0D, stats.max());
        assertEquals(0D, stats.percentile(0.99D));
        assertEquals(0D, stats.worstPercentMean());
    }

    @Test
    void percentilesAndTheWorstPercentComeFromTheSortedMeasurements()
    {
        BenchmarkStats stats = new BenchmarkStats();
        // 1 to 1000 in an order that is not sorted, and more than the first allocation holds.
        for (int value = 1000; value >= 1; value--)
            stats.add(value);
        assertEquals(1000, stats.count());
        assertEquals(500.5D, stats.mean(), 1.0E-9D);
        assertEquals(500D, stats.percentile(0.5D));
        assertEquals(950D, stats.percentile(0.95D));
        assertEquals(990D, stats.percentile(0.99D));
        assertEquals(1000D, stats.percentile(1D));
        assertEquals(1D, stats.percentile(0D));
        assertEquals(1000D, stats.max());
        // The slowest hundredth is 991 to 1000.
        assertEquals(995.5D, stats.worstPercentMean(), 1.0E-9D);
        // Adding after reading still gives the right answers.
        stats.add(5000D);
        assertEquals(5000D, stats.max());
        assertTrue(stats.summary("ms").startsWith("mean 504.995 ms, median 501.000"));
    }

    @Test
    void theProfilerCountsPerTickAndKeepsSlowPathsApart()
    {
        NpcProfiler.setServer(false);
        NpcProfiler.setServer(true);
        assertTrue(NpcProfiler.server());
        NpcProfiler.npcTick(2_000_000L);
        NpcProfiler.npcTick(1_000_000L);
        NpcProfiler.pathSearch(40L, "Tank", 1, 64, 2, 30D, 500_000L, true);
        NpcProfiler.pathSearch(41L, "Ship", 5, 62, 9, 200D, 6_000_000L, false);
        NpcProfiler.targetSearch(0, 12, 3, true, 250_000L);
        NpcProfiler.targetSearch(4, 80, 80, false, 750_000L);
        NpcProfiler.ServerSample sample = NpcProfiler.drainServer();
        assertEquals(3D, sample.npcMillis(), 1.0E-9D);
        assertEquals(2, sample.npcTicks());
        assertEquals(6.5D, sample.pathMillis(), 1.0E-9D);
        assertEquals(2, sample.pathSearches());
        assertEquals(1, sample.pathFailures());
        assertEquals(1D, sample.targetMillis(), 1.0E-9D);
        // Draining starts the next tick; the bands and the slow paths last until a reset.
        assertEquals(0, NpcProfiler.drainServer().npcTicks());
        assertEquals("1 searches, 12 candidates, 3 tested, 1 acquired, 0.250 ms", NpcProfiler.band(0).summary());
        assertEquals(1, NpcProfiler.band(4).searches());
        assertEquals(1, NpcProfiler.slowPaths().size());
        assertEquals("tick 41: Ship at 5 62 9, goal 200 blocks away, 6.00 ms, no path", NpcProfiler.slowPaths().get(0).line());
        NpcProfiler.resetServer();
        assertEquals(0, NpcProfiler.band(0).searches());
        assertTrue(NpcProfiler.slowPaths().isEmpty());
        NpcProfiler.setServer(false);
        assertFalse(NpcProfiler.server());
    }

    @Test
    void theRenderCountersAreDrainedPerFrame()
    {
        NpcProfiler.setClient(true);
        NpcProfiler.npcRender(400_000L);
        NpcProfiler.npcRender(600_000L);
        NpcProfiler.ClientSample sample = NpcProfiler.drainClient();
        assertEquals(1D, sample.npcMillis(), 1.0E-9D);
        assertEquals(2, sample.npcs());
        assertEquals(0, NpcProfiler.drainClient().npcs());
        NpcProfiler.setClient(false);
    }

    @Test
    void theServerPageRequestsSurviveTheWire()
    {
        ServerControlPacket sent = ServerControlPacket.set(List.of(15, 120, 1, 96, 72, 160, 140, 40, 8, 64, 1, 150, 2));
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        sent.write(buffer);
        assertEquals(sent, ServerControlPacket.read(buffer));
        assertEquals(0, buffer.readableBytes());
        ServerControlPacket benchmark = ServerControlPacket.benchmark(45);
        benchmark.write(buffer);
        ServerControlPacket received = ServerControlPacket.read(buffer);
        assertEquals(ServerControlPacket.Action.BENCHMARK, received.action());
        assertEquals(45, received.seconds());
        assertEquals(ServerControlPacket.Action.GET, ServerControlPacket.request().action());
        assertEquals(ServerControlPacket.Action.APPLY_RANGES, ServerControlPacket.applyRanges().action());
        assertEquals(ServerControlPacket.Action.APPLY_SEARCH, ServerControlPacket.applySearch().action());
        assertEquals(ServerControlPacket.Action.REPORT, ServerControlPacket.report().action());
    }

    @Test
    void theCensusAndTheBenchmarksSurviveTheWire()
    {
        NpcCensus.Row row = new NpcCensus.Row("minecraft:overworld", List.of(30, 2, 6, 3, 1), 5, 4, 3, 1, 10);
        BenchmarkResult result = new BenchmarkResult("tanks", 600, true, List.of(12.5D, 30D, 4D, 32D, 42D, 1.5D, 0.75D, 0.25D));
        ServerReportPacket sent = new ServerReportPacket(List.of(row), new NpcBudgets.Deferred(7, 2), List.of(result));
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        sent.write(buffer);
        assertEquals(sent, ServerReportPacket.read(buffer));
        assertEquals(0, buffer.readableBytes());
        assertEquals(42, row.total());
        assertEquals(6, row.count(NpcCensus.Kind.VEHICLE));
        assertEquals(42D, result.figure(BenchmarkResult.Figure.NPCS_TICKING));
        // A run from a version with fewer figures reads 0 for the ones it lacks.
        assertEquals(0D, new BenchmarkResult("", 1, false, List.of(1D)).figure(BenchmarkResult.Figure.TARGET_MILLIS));
    }

    @Test
    void theSettingsOfTheServerAreClampedAndAMissingValueKeepsItsOwn()
    {
        NpcServerSettings settings = new NpcServerSettings();
        assertEquals(100, settings.get(NpcServerSettings.Setting.HEALTH_PERCENT));
        assertEquals(NpcServerSettings.AmmoPolicy.PER_NPC, settings.ammoPolicy());
        assertFalse(settings.isFrozen());
        List<Integer> sent = new ArrayList<>(settings.all());
        sent.set(NpcServerSettings.Setting.SEARCH_BUDGET.ordinal(), -5);
        sent.set(NpcServerSettings.Setting.HEALTH_PERCENT.ordinal(), 5000);
        sent.set(NpcServerSettings.Setting.AMMO_POLICY.ordinal(), 2);
        assertTrue(settings.set(sent));
        assertEquals(0, settings.get(NpcServerSettings.Setting.SEARCH_BUDGET));
        assertEquals(1000, settings.get(NpcServerSettings.Setting.HEALTH_PERCENT));
        assertEquals(NpcServerSettings.AmmoPolicy.ALWAYS, settings.ammoPolicy());
        // An older client sends fewer values; the rest stay as they were.
        assertFalse(settings.set(List.of(20, 90)));
        assertEquals(20, settings.getSearchNearInterval());
        assertEquals(1000, settings.get(NpcServerSettings.Setting.HEALTH_PERCENT));
        assertEquals(0, settings.getSearchGeneration());
        settings.applySearchToAll();
        assertEquals(1, settings.getSearchGeneration());
    }

    @Test
    void theRangesOfNewNpcsAreClampedAndApplyingThemCountsUp()
    {
        NpcServerSettings settings = new NpcServerSettings();
        assertEquals(NpcServerSettings.DEFAULT_AGGRO_RANGE, settings.getAggroRange());
        assertEquals(NpcServerSettings.DEFAULT_FIRING_RANGE, settings.getFiringRange());
        assertEquals(0, settings.getRangeGeneration());
        assertEquals(NpcServerSettings.DEFAULT_MODEL_RANGE, settings.getModelAggroRange());
        settings.set(List.of(NpcServerSettings.DEFAULT_NEAR_INTERVAL, NpcServerSettings.DEFAULT_FAR_INTERVAL, 0, 0, 10_000, 0, 200));
        assertEquals(1, settings.getAggroRange());
        assertEquals(NpcServerSettings.MAX_RANGE, settings.getFiringRange());
        assertEquals(1, settings.getModelAggroRange());
        assertEquals(200, settings.getModelFiringRange());
        assertEquals(0, settings.getRangeGeneration());
        settings.applyRangesToAll();
        settings.applyRangesToAll();
        assertEquals(2, settings.getRangeGeneration());
    }
}
