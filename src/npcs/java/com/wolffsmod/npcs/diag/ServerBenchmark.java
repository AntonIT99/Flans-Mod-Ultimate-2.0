package com.wolffsmod.npcs.diag;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import org.jetbrains.annotations.Nullable;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import java.nio.file.Path;
import java.util.*;

/**
 * Measures what NPCs cost the server: for a number of seconds it records how long every server tick takes and how much
 * of it goes to NPC ticks, path searches and target searches, then writes a summary and the per-tick figures to the
 * logs folder. One run at a time; server thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ServerBenchmark
{
    public static final int MIN_SECONDS = 5;
    public static final int MAX_SECONDS = 600;
    public static final int DEFAULT_SECONDS = 30;
    /** Ticks left out at the start, so the run does not measure the server recovering from whatever started it. */
    private static final int WARM_UP_TICKS = 100;
    private static final int MAX_SLOW_PATHS_LISTED = 15;
    private static final double NANOS_PER_MILLI = 1.0E6D;
    private static final String PER_TICK = "ms per tick";

    @Nullable
    private static Run run;

    /** One benchmark in progress. */
    private static final class Run
    {
        private final CommandSourceStack requester;
        private final int ticks;
        private final BenchmarkStats tick = new BenchmarkStats();
        private final BenchmarkStats npc = new BenchmarkStats();
        private final BenchmarkStats path = new BenchmarkStats();
        private final BenchmarkStats target = new BenchmarkStats();
        private final BenchmarkStats ticking = new BenchmarkStats();
        private final BenchmarkReport report = new BenchmarkReport("server", "tick,tick_ms,npc_ms,npc_ticks,path_ms,path_searches,target_ms");
        private int warmUp = WARM_UP_TICKS;
        private long started;
        private int pathSearches;

        private Run(CommandSourceStack requester, int ticks)
        {
            this.requester = requester;
            this.ticks = ticks;
        }
    }

    public static boolean running()
    {
        return run != null;
    }

    /** @return false when a run is already in progress */
    public static boolean start(CommandSourceStack requester, int seconds)
    {
        if (run != null)
            return false;
        int clamped = Math.max(MIN_SECONDS, Math.min(MAX_SECONDS, seconds));
        run = new Run(requester, clamped * 20);
        NpcProfiler.setServer(true);
        requester.sendSuccess(() -> Component.translatable("commands.wolffsmodnpcs.benchmark.started", clamped, WARM_UP_TICKS / 20), true);
        return true;
    }

    /** @return false when nothing was running */
    public static boolean stop(MinecraftServer server, String reason)
    {
        if (run == null)
            return false;
        finish(server, reason);
        return true;
    }

    public static void serverTick(TickEvent.ServerTickEvent event)
    {
        Run current = run;
        if (current == null)
            return;
        if (event.phase == TickEvent.Phase.START)
        {
            current.started = System.nanoTime();
            return;
        }
        NpcProfiler.ServerSample sample = NpcProfiler.drainServer();
        if (current.warmUp > 0)
        {
            if (--current.warmUp == 0)
                NpcProfiler.resetServer();
            return;
        }
        double millis = (System.nanoTime() - current.started) / NANOS_PER_MILLI;
        current.tick.add(millis);
        current.npc.add(sample.npcMillis());
        current.path.add(sample.pathMillis());
        current.target.add(sample.targetMillis());
        current.ticking.add(sample.npcTicks());
        current.pathSearches += sample.pathSearches();
        current.report.row(String.format(Locale.ROOT, "%d,%.4f,%.4f,%d,%.4f,%d,%.4f", current.tick.count(), millis, sample.npcMillis(), sample.npcTicks(), sample.pathMillis(), sample.pathSearches(),
            sample.targetMillis()));
        if (current.tick.count() >= current.ticks)
            finish(event.getServer(), null);
    }

    public static void serverStopping(ServerStoppingEvent event)
    {
        stop(event.getServer(), "the server stopped");
    }

    private static void finish(MinecraftServer server, @Nullable String abortReason)
    {
        Run finished = run;
        run = null;
        if (finished == null)
            return;
        summarise(finished, abortReason);
        NpcProfiler.setServer(NpcServerSettings.get(server).isProfiling());
        NpcProfiler.resetServer();
        Optional<Path> file = finished.report.write();
        List<String> lines = finished.report.summary();
        // The first lines are the result; the rest is in the file.
        for (int line = 0; line < Math.min(6, lines.size()); line++)
        {
            String text = lines.get(line);
            finished.requester.sendSuccess(() -> Component.literal(text), false);
        }
        file.ifPresent(path -> finished.requester.sendSuccess(() -> Component.translatable("commands.wolffsmodnpcs.benchmark.written", path.toString()), false));
    }

    private static void summarise(Run finished, @Nullable String abortReason)
    {
        BenchmarkReport report = finished.report;
        int ticks = finished.tick.count();
        double mean = finished.tick.mean();
        report.line(
            abortReason == null ? "NPC server benchmark: " + ticks + " ticks measured" : "NPC server benchmark aborted (" + abortReason + ") after " + ticks + " of " + finished.ticks + " ticks");
        report.line("Server tick: " + finished.tick.summary("ms") + String.format(Locale.ROOT, "; about %.1f ticks per second", mean <= 50D ? 20D : 1000D / mean));
        report.line(
            String.format(Locale.ROOT, "NPC ticks: %s; %.1f%% of the tick; %.1f NPCs ticking on average", finished.npc.summary(PER_TICK), share(finished.npc.mean(), mean), finished.ticking.mean()));
        report.line(String.format(Locale.ROOT, "Path searches: %d in all, %.2f per tick; %s", finished.pathSearches, ticks == 0 ? 0D : (double) finished.pathSearches / ticks,
            finished.path.summary(PER_TICK)));
        report.line("Target searches: " + finished.target.summary(PER_TICK));
        if (finished.ticking.mean() < 0.5D)
            report.line("Note: hardly any NPC was ticking. Stand near the NPCs to be measured, or let them stay active at a distance.");
        report.line("");
        report.line("Target search bands (0 is nearest):");
        for (int band = 0; band < NpcProfiler.BANDS; band++)
            report.line("  " + band + ": " + NpcProfiler.band(band).summary());
        List<NpcProfiler.SlowPath> slow = NpcProfiler.slowPaths().stream().sorted((first, second) -> Double.compare(second.millis(), first.millis())).toList();
        report.line("");
        report.line(String.format(Locale.ROOT, "Path searches of %.0f ms or more: %d", NpcProfiler.SLOW_PATH_MILLIS, slow.size() + NpcProfiler.slowPathsDropped()));
        for (NpcProfiler.SlowPath path : slow.subList(0, Math.min(MAX_SLOW_PATHS_LISTED, slow.size())))
            report.line("  " + path.line());
    }

    private static double share(double part, double whole)
    {
        return whole <= 0D ? 0D : 100D * part / whole;
    }
}
