package com.flansmodultimate.client.search;

import com.flansmodultimate.hooks.client.ClientTooltipHooksImpl;
import com.flansmodultimate.util.FlansLog;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * Builds each search tree the first time something is searched in it, rather than every time its
 * contents change.
 * <p>
 * The game builds the tree of every creative tab with a search bar, of the creative search tab and
 * of the recipe book eagerly, as background tasks: on opening the creative inventory, on joining a
 * world, on every tag and recipe sync and on every language change. Each build reads the tooltip of
 * every item it holds, and with thousands of guns, vehicles and their paintjobs that cost is paid
 * over and over for trees nobody searches. A tree is only ever read by joining its future for a
 * search, so a deferred future is built from the same contents on that first join and searching
 * finds the same items; the build just moves to the first query.
 * <p>
 * {@code -Dflansmodultimate.eagerSearchTrees=true} restores eager building.
 */
public final class LazySearchTrees
{
    private static final boolean EAGER = Boolean.getBoolean("flansmodultimate.eagerSearchTrees");

    private LazySearchTrees() {}

    /** Stands in for {@link CompletableFuture#supplyAsync(Supplier, Executor)} for a search tree build. */
    public static <T> CompletableFuture<T> supply(Supplier<T> build, Executor executor)
    {
        if (EAGER)
            return CompletableFuture.supplyAsync(build, executor);
        return new Deferred<>(build);
    }

    /**
     * A future that runs its build on the thread that first waits for it. Replaced trees are
     * cancelled, which completes the future without building it.
     */
    static final class Deferred<T> extends CompletableFuture<T>
    {
        private Supplier<T> build;

        Deferred(Supplier<T> build)
        {
            this.build = build;
        }

        @Override
        public T join()
        {
            buildIfPending();
            return super.join();
        }

        @Override
        public T get() throws InterruptedException, ExecutionException
        {
            buildIfPending();
            return super.get();
        }

        @Override
        public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
        {
            buildIfPending();
            return super.get(timeout, unit);
        }

        boolean isBuilt()
        {
            return build == null && isDone() && !isCompletedExceptionally();
        }

        private synchronized void buildIfPending()
        {
            Supplier<T> pending = build;
            if (pending == null)
                return;
            build = null;
            if (isDone())
                return;

            long start = System.nanoTime();
            try
            {
                // Built on a keystroke, which may be a capital letter: Shift must not switch the indexed
                // tooltips of Flan's items to their detailed statistics.
                complete(ClientTooltipHooksImpl.withoutHeldKeys(pending));
            }
            catch (RuntimeException e)
            {
                completeExceptionally(e);
                return;
            }
            FlansLog.log.debug("Built a search tree in {} ms on its first search", (System.nanoTime() - start) / 1_000_000);
        }
    }
}
