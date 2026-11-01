package com.wolffsmod.npcs.diag;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
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
    /** Seconds left out by the quick-start command and editor action. */
    public static final int DEFAULT_WARM_UP_SECONDS = 5;
    private static final int MAX_SLOW_PATHS_LISTED = 15;
    private static final double NANOS_PER_MILLI = 1.0E6D;
    private static final String PER_TICK = "ms per tick";

    /** Finished runs kept for the Server page to compare. */
    private static final int KEPT_RESULTS = 2;

    @Nullable
    private static Run run;
    /** The latest finished runs since the server started, oldest first. Server thread. */
    private static final Deque<BenchmarkResult> results = new ArrayDeque<>();

    /** One benchmark in progress. */
    private static final class Run
    {
        private final CommandSourceStack requester;
        private final BenchmarkOptions options;
        private final int ticks;
        private final BenchmarkStats tick = new BenchmarkStats();
        private final BenchmarkStats npc = new BenchmarkStats();
        private final BenchmarkStats path = new BenchmarkStats();
        private final BenchmarkStats target = new BenchmarkStats();
        private final BenchmarkStats ticking = new BenchmarkStats();
        private final BenchmarkReport report;
        private int warmUp;
        private long started;
        private int pathSearches;
        private int pathFailures;

        private Run(CommandSourceStack requester, BenchmarkOptions options)
        {
            this.requester = requester;
            this.options = options;
            ticks = options.seconds() * 20;
            warmUp = options.warmUpSeconds() * 20;
            report = new BenchmarkReport("server", "tick,tick_ms,npc_ms,npc_ticks,path_ms,path_searches,path_failures,target_ms", options);
        }
    }

    public static boolean running()
    {
        return run != null;
    }

    /** @return false when a run is already in progress */
    public static boolean start(CommandSourceStack requester, int seconds)
    {
        return start(requester, new BenchmarkOptions(seconds, DEFAULT_WARM_UP_SECONDS, ""));
    }

    /** Starts an explicitly configured run on the server thread. */
    public static boolean start(CommandSourceStack requester, BenchmarkOptions options)
    {
        if (run != null)
            return false;
        run = new Run(requester, options);
        NpcProfiler.setServer(true);
        // Zero warm-up must also start with fresh counters when profiling was already enabled.
        NpcProfiler.resetServer();
        requester.sendSuccess(() -> Component.translatable("commands.wolffsmodnpcs.benchmark.started", options.seconds(), options.warmUpSeconds()), true);
        return true;
    }

    /** Reports progress to the operator who requested it, without changing the run or its counters. */
    public static boolean status(CommandSourceStack requester)
    {
        Run current = run;
        if (current == null)
            return false;
        Component label = current.options.label().isEmpty() ? Component.translatable("commands.wolffsmodnpcs.benchmark.unnamed") : Component.literal(current.options.label());
        requester.sendSuccess(() -> Component.translatable("commands.wolffsmodnpcs.benchmark.status", label, current.tick.count() / 20, current.options.seconds(), (current.warmUp + 19) / 20), false);
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

    /** At the start of every server tick. */
    public static void serverTickStart()
    {
        Run current = run;
        if (current != null)
            current.started = System.nanoTime();
    }

    /** At the end of every server tick. */
    public static void serverTickEnd(MinecraftServer server)
    {
        Run current = run;
        if (current == null)
            return;
        NpcProfiler.ServerSample sample = NpcProfiler.drainServer();
        // A run started by a command in the middle of a tick has no start for that tick to measure from.
        if (current.started == 0L)
            return;
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
        current.pathFailures += sample.pathFailures();
        current.report.row(String.format(Locale.ROOT, "%d,%.4f,%.4f,%d,%.4f,%d,%d,%.4f", current.tick.count(), millis, sample.npcMillis(), sample.npcTicks(), sample.pathMillis(),
            sample.pathSearches(), sample.pathFailures(), sample.targetMillis()));
        if (current.tick.count() >= current.ticks)
            finish(server, null);
    }

    public static void serverStopping(MinecraftServer server)
    {
        stop(server, "the server stopped");
    }

    private static void finish(MinecraftServer server, @Nullable String abortReason)
    {
        Run finished = run;
        run = null;
        if (finished == null)
            return;
        summarise(finished, abortReason);
        remember(finished, abortReason != null);
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
        report.line(String.format(Locale.ROOT, "Path searches: %d in all, %d without a path, %.2f per tick; %s", finished.pathSearches, finished.pathFailures,
            ticks == 0 ? 0D : (double) finished.pathSearches / ticks, finished.path.summary(PER_TICK)));
        report.line("Target searches: " + finished.target.summary(PER_TICK));
        if (finished.ticking.mean() < 0.5D)
            report.line("Note: hardly any NPC was ticking. Stand near the NPCs to be measured, or let them stay active at a distance.");
        report.describeSession();
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

    /** Keeps the headline figures of a run that measured anything, forgetting the oldest beyond the ones compared. */
    private static void remember(Run finished, boolean aborted)
    {
        int ticks = finished.tick.count();
        if (ticks == 0)
            return;
        double mean = finished.tick.mean();
        List<Double> figures = new ArrayList<>();
        for (BenchmarkResult.Figure figure : BenchmarkResult.Figure.values())
        {
            figures.add(switch (figure)
            {
                case TICK_MEAN -> mean;
                case TICK_P95 -> finished.tick.percentile(0.95D);
                case NPC_MILLIS -> finished.npc.mean();
                case NPC_SHARE -> share(finished.npc.mean(), mean);
                case NPCS_TICKING -> finished.ticking.mean();
                case PATHS_PER_TICK -> (double) finished.pathSearches / ticks;
                case PATH_MILLIS -> finished.path.mean();
                case TARGET_MILLIS -> finished.target.mean();
            });
        }
        results.addLast(new BenchmarkResult(finished.options.label(), ticks, aborted, List.copyOf(figures)));
        while (results.size() > KEPT_RESULTS)
            results.removeFirst();
    }

    /** @return the latest finished runs since the server started, oldest first */
    public static List<BenchmarkResult> results()
    {
        return List.copyOf(results);
    }

    private static double share(double part, double whole)
    {
        return whole <= 0D ? 0D : 100D * part / whole;
    }
}
