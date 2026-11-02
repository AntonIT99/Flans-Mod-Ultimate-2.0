package com.wolffsmod.npcs.sim;

import com.wolffsmod.npcs.NpcsCommonConfig;
import com.wolffsmod.npcs.NpcsMod;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;

import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Runs NPC path searches on worker threads, so that a crowd asking for paths does not lengthen the server tick.
 *
 * <p>
 * The server thread builds the snapshot of the blocks around the search, as it always did, and hands it to a worker with
 * the navigator's own path finder. It then goes on without a path: the NPC keeps the one it has, and takes the new one a
 * tick or two later, which is what a pursuit already does between its own searches. Only one search of a navigator runs
 * at a time, so a path finder and its node evaluator are never used by two threads at once.
 * </p>
 *
 * <p>
 * A search reads the blocks of its snapshot while the server may be changing them. That is the one thing this gives up,
 * and it can at worst cost a path that is a tick out of date with the world; a worker that fails is reported, its search
 * is run on the server thread instead, and after a handful of failures searching goes back on the server thread
 * altogether for the rest of the session.
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcPathWorkers
{
    /** Workers at most, leaving the server thread a core of its own. */
    private static final int MAX_WORKERS = 4;
    /** Failures after which every search goes back on the server thread. */
    private static final int MAX_FAILURES = 8;
    /** Searches waiting at once; over this a search runs on the server thread rather than queue up behind a backlog. */
    private static final int MAX_QUEUED = 256;

    private static final AtomicInteger failures = new AtomicInteger();
    private static final AtomicInteger threadNumber = new AtomicInteger();
    private static final AtomicReference<ThreadPoolExecutor> workers = new AtomicReference<>();

    /** One search handed to a worker. Created and read on the server thread, completed by a worker. */
    public static final class Search
    {
        private final Set<BlockPos> targets;

        /**
         * What a finished search came to.
         *
         * @param path
         *            the path it found, or null when it found none
         */
        private record Outcome(@Nullable Path path)
        {}

        /** Set once the search has run; null while it is still running or has failed. */
        private final AtomicReference<Outcome> outcome = new AtomicReference<>();
        private final AtomicReference<Throwable> failure = new AtomicReference<>();

        private Search(Set<BlockPos> targets)
        {
            this.targets = Set.copyOf(targets);
        }

        /** Whether a result belongs to the requested destination, including a small allowance for ordinary pursuit. */
        public boolean matches(Set<BlockPos> requested, boolean precise)
        {
            return sameDestination(targets, requested, precise);
        }

        /** @return whether the search has run, with or without a path, or given up */
        public boolean done()
        {
            return outcome.get() != null || failure.get() != null;
        }

        /** @return whether the search failed and has to be run on the server thread instead */
        public boolean failed()
        {
            return failure.get() != null;
        }

        /** @return the path the search found, or null when it found none; only meaningful once {@link #done()} */
        @Nullable
        public Path path()
        {
            Outcome finished = outcome.get();
            return finished == null ? null : finished.path();
        }
    }

    /** Moving quarry may drift a few blocks while searched; safety destinations must match exactly. */
    static boolean sameDestination(Set<BlockPos> previous, Set<BlockPos> requested, boolean precise)
    {
        return previous.equals(requested) || !precise && requested.stream().anyMatch(point -> previous.stream().anyMatch(old -> old.distSqr(point) <= 16D));
    }

    /**
     * Hands a search to a worker.
     *
     * @return the search to read on a later tick, or null when it has to be run on the server thread
     */
    // Mirrors the path finder's own call, whose arguments the worker has to pass on unchanged.
    @SuppressWarnings("java:S107")
    @Nullable
    public static Search submit(PathFinder finder, PathNavigationRegion region, Mob mob, Set<BlockPos> targets, float followRange, int accuracy, float maxVisitedNodes)
    {
        if (!NpcsCommonConfig.asyncPathfinding() || failures.get() >= MAX_FAILURES)
            return null;
        ThreadPoolExecutor executor = pool();
        if (executor == null || executor.getQueue().size() >= MAX_QUEUED)
            return null;
        Search search = new Search(targets);
        // The set the navigator built is copied, so nothing it does with it afterwards can reach the worker.
        Set<BlockPos> goals = Set.copyOf(targets);
        try
        {
            executor.execute(() -> run(search, finder, region, mob, goals, followRange, accuracy, maxVisitedNodes));
        }
        catch (RejectedExecutionException rejected)
        {
            return null;
        }
        return search;
    }

    /** Stops the workers, as a stopping server does. */
    public static void clear()
    {
        ThreadPoolExecutor executor = workers.getAndSet(null);
        if (executor != null)
            executor.shutdownNow();
        failures.set(0);
    }

    // Mirrors the path finder's own call, whose arguments the worker has to pass on unchanged.
    @SuppressWarnings("java:S107")
    private static void run(Search search, PathFinder finder, PathNavigationRegion region, Mob mob, Set<BlockPos> targets, float followRange, int accuracy, float maxVisitedNodes)
    {
        try
        {
            search.outcome.set(new Search.Outcome(finder.findPath(region, mob, targets, followRange, accuracy, maxVisitedNodes)));
        }
        catch (RuntimeException | LinkageError exception)
        {
            search.failure.set(exception);
            if (failures.incrementAndGet() == MAX_FAILURES)
                NpcsMod.log.error("NPC path worker failed {} times; path searches return to the server thread for this session", MAX_FAILURES, exception);
            else
                NpcsMod.log.warn("NPC path worker failed; this search is run on the server thread instead", exception);
        }
    }

    @Nullable
    private static ThreadPoolExecutor pool()
    {
        ThreadPoolExecutor executor = workers.get();
        if (executor != null)
            return executor;
        int threads = Math.max(1, Math.min(MAX_WORKERS, Runtime.getRuntime().availableProcessors() - 1));
        ThreadPoolExecutor created = new ThreadPoolExecutor(threads, threads, 30L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), NpcPathWorkers::worker);
        created.allowCoreThreadTimeOut(true);
        return workers.compareAndSet(null, created) ? created : workers.get();
    }

    private static Thread worker(Runnable task)
    {
        Thread thread = new Thread(task, "wolffsmodnpcs-path-" + threadNumber.incrementAndGet());
        thread.setDaemon(true);
        // Below the server thread: a path that arrives a tick later costs nothing, a tick that arrives later does.
        thread.setPriority(Thread.NORM_PRIORITY - 1);
        return thread;
    }
}
