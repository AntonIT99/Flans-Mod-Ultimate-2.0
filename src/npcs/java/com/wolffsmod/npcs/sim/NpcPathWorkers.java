package com.wolffsmod.npcs.sim;

import com.wolffsmod.npcs.NpcsCommonConfig;
import com.wolffsmod.npcs.NpcsMod;
import com.wolffsmod.npcs.mixin.PathFinderAccessor;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.ai.NpcWalkNodeEvaluator;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.FlyNodeEvaluator;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;

import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Runs NPC path searches on worker threads, so that a crowd asking for paths does not lengthen the server tick.
 *
 * <p>
 * The server thread builds the snapshot of the blocks around the search, as it always did, and hands it to a worker with
 * an independent path finder and node evaluator. It then goes on without a path: the NPC keeps the one it has, and takes
 * the new one a tick or two later, which is what a pursuit already does between its own searches. The navigator keeps its
 * own evaluator on the server thread, where Custom NPCs also uses it to check the path it follows.
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
    /** Searches waiting at once. Ordinary requests leave room for urgent retreat and evasion paths. */
    private static final int MAX_QUEUED = 256;
    private static final int RESERVED_URGENT = 32;

    private static final AtomicInteger failures = new AtomicInteger();
    private static final AtomicInteger threadNumber = new AtomicInteger();
    private static final AtomicLong sequence = new AtomicLong();
    private static final AtomicReference<ThreadPoolExecutor> workers = new AtomicReference<>();

    /** An unsupported search runs synchronously; overload waits instead of moving work back onto the server thread. */
    public record Submission(@Nullable Search search, boolean deferred)
    {
        private static final Submission SYNCHRONOUS = new Submission(null, false);
        private static final Submission WAIT_FOR_CAPACITY = new Submission(null, true);
    }

    /** Urgent work may overtake recent requests; older ordinary work still advances under sustained urgent demand. */
    @SuppressWarnings("java:S1210") // Each task's unique sequence preserves identity equality, including queue removal.
    static final class Work extends FutureTask<Void> implements Comparable<Work>
    {
        private final boolean urgent;
        private final long order = sequence.getAndIncrement();

        Work(Runnable task, boolean urgent)
        {
            super(task, null);
            this.urgent = urgent;
        }

        @Override
        public int compareTo(Work other)
        {
            int priority = Long.compare(order - (urgent ? RESERVED_URGENT : 0), other.order - (other.urgent ? RESERVED_URGENT : 0));
            return priority != 0 ? priority : Long.compare(order, other.order);
        }
    }

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
        @Nullable
        private Work work;

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

        /** Discards a superseded request and removes it from the backlog if its worker has not started. */
        public void cancel()
        {
            Work task = work;
            if (task == null)
                return;
            // A running finder is allowed to finish; interrupting game code cannot safely cancel its reads.
            task.cancel(false);
            ThreadPoolExecutor executor = workers.get();
            if (executor != null)
                executor.remove(task);
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
     * @return the search to read later, a deferred request when overloaded, or a synchronous fallback
     */
    // Mirrors the path finder's own call, whose arguments the worker has to pass on unchanged.
    @SuppressWarnings("java:S107")
    public static Submission submit(PathFinder finder, PathNavigationRegion region, Mob mob, Set<BlockPos> targets, float followRange, int accuracy, float maxVisitedNodes, boolean urgent)
    {
        if (!NpcsCommonConfig.asyncPathfinding() || failures.get() >= MAX_FAILURES)
            return Submission.SYNCHRONOUS;
        NodeEvaluator evaluator = ((PathFinderAccessor) finder).wolffsmodnpcsNodeEvaluator();
        if (evaluator.getClass() != NpcWalkNodeEvaluator.class && evaluator.getClass() != FlyNodeEvaluator.class)
            return Submission.SYNCHRONOUS;
        ThreadPoolExecutor executor = pool();
        if (executor == null || !queueAllows(executor.getQueue().size(), urgent))
            return Submission.WAIT_FOR_CAPACITY;
        PathFinder isolated = workerFinder(finder, region, mob);
        if (isolated == null)
            return Submission.SYNCHRONOUS;
        Search search = new Search(targets);
        // The set the navigator built is copied, so nothing it does with it afterwards can reach the worker.
        Set<BlockPos> goals = Set.copyOf(targets);
        try
        {
            search.work = new Work(() -> run(search, isolated, region, mob, goals, followRange, accuracy, maxVisitedNodes), urgent);
            executor.execute(search.work);
        }
        catch (RejectedExecutionException rejected)
        {
            return Submission.WAIT_FOR_CAPACITY;
        }
        return new Submission(search, false);
    }

    static boolean queueAllows(int queued, boolean urgent)
    {
        return queued < MAX_QUEUED - (urgent ? 0 : RESERVED_URGENT);
    }

    /** Copies supported evaluator settings on the server thread; unknown evaluators keep their synchronous behavior. */
    @Nullable
    static PathFinder workerFinder(PathFinder finder, PathNavigationRegion region, Mob mob)
    {
        PathFinderAccessor access = (PathFinderAccessor) finder;
        NodeEvaluator original = access.wolffsmodnpcsNodeEvaluator();
        NodeEvaluator isolated;
        if (original.getClass() == NpcWalkNodeEvaluator.class)
        {
            isolated = new NpcWalkNodeEvaluator();
            // Custom NPCs follows paths with this evaluator between searches. Refresh its dimensions and region on
            // the server just as a synchronous search would; its done() retains them but clears the search caches.
            original.prepare(region, mob);
            original.done();
        }
        else if (original.getClass() == FlyNodeEvaluator.class)
            isolated = new FlyNodeEvaluator();
        else
            return null;
        isolated.setCanPassDoors(original.canPassDoors());
        isolated.setCanOpenDoors(original.canOpenDoors());
        isolated.setCanFloat(original.canFloat());
        isolated.setCanWalkOverFences(original.canWalkOverFences());
        return new PathFinder(isolated, access.wolffsmodnpcsMaxVisitedNodes());
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
        ThreadPoolExecutor created = new ThreadPoolExecutor(threads, threads, 30L, TimeUnit.SECONDS, new PriorityBlockingQueue<>(), NpcPathWorkers::worker);
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
