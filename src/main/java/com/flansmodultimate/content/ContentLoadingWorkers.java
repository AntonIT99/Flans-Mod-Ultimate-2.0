package com.flansmodultimate.content;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Worker threads for the file reading and indexing that content startup can do away from the
 * loading thread. Every result is handed back in input order, so whatever consumes it, such as
 * registration, still sees the order a single thread would have produced.
 * <p>
 * With one thread there is no pool at all and every task runs on the calling thread.
 */
final class ContentLoadingWorkers implements AutoCloseable
{
    /** Leaves headroom for the loader, which constructs other mods at the same time. */
    private static final int MAX_AUTOMATIC_THREADS = 8;

    private static final ContentLoadingWorkers SEQUENTIAL = new ContentLoadingWorkers(null);

    private final ForkJoinPool pool;

    private ContentLoadingWorkers(ForkJoinPool pool)
    {
        this.pool = pool;
    }

    static ContentLoadingWorkers sequential()
    {
        return SEQUENTIAL;
    }

    /**
     * @param configuredThreads threads to use; 0 or less chooses from the processor count
     */
    static ContentLoadingWorkers create(int configuredThreads)
    {
        int threads = threadCount(configuredThreads, Runtime.getRuntime().availableProcessors());
        if (threads <= 1)
            return SEQUENTIAL;

        AtomicInteger counter = new AtomicInteger();
        ClassLoader classLoader = ContentLoadingWorkers.class.getClassLoader();
        ForkJoinPool.ForkJoinWorkerThreadFactory factory = forkJoinPool -> {
            ForkJoinWorkerThread thread = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(forkJoinPool);
            thread.setName("Flan content loading " + counter.incrementAndGet());
            thread.setDaemon(true);
            // Pack code is loaded by the mod class loader, not by the loader's own.
            thread.setContextClassLoader(classLoader);
            return thread;
        };
        return new ContentLoadingWorkers(new ForkJoinPool(threads, factory, null, false));
    }

    static int threadCount(int configuredThreads, int processors)
    {
        if (configuredThreads > 0)
            return configuredThreads;
        return Math.max(1, Math.min(processors - 1, MAX_AUTOMATIC_THREADS));
    }

    boolean isParallel()
    {
        return pool != null;
    }

    int threads()
    {
        return pool == null ? 1 : pool.getParallelism();
    }

    /** Starts the task on a worker, or returns a future that runs it on the first {@code join}. */
    <T> Task<T> submit(Supplier<T> task)
    {
        if (pool == null)
            return new Task<>(null, task);
        return new Task<>(CompletableFuture.supplyAsync(task, pool), null);
    }

    /** Applies the function to every input, in parallel when there are workers, and keeps the input order. */
    <I, O> List<O> map(List<I> inputs, Function<? super I, ? extends O> function)
    {
        if (pool == null || inputs.size() < 2)
            return inputs.stream().<O>map(function).toList();
        // A parallel stream started on one of the workers runs in their pool; started anywhere
        // else it would run in the common pool, so it is handed to a worker first.
        if (ForkJoinTask.getPool() == pool)
            return inputs.parallelStream().<O>map(function).toList();
        // Waited on as a CompletableFuture: the mod constructor runs on a thread of the loader's own
        // pool, which waiting on a ForkJoinTask could set to run other mods' tasks in the meantime.
        return submit(() -> inputs.parallelStream().<O>map(function).toList()).join();
    }

    @Override
    public void close()
    {
        if (pool != null)
            pool.shutdownNow();
    }

    private static RuntimeException rethrow(Throwable cause)
    {
        if (cause instanceof CompletionException && cause.getCause() != null)
            cause = cause.getCause();
        if (cause instanceof RuntimeException runtimeException)
            throw runtimeException;
        if (cause instanceof Error error)
            throw error;
        throw new IllegalStateException(cause);
    }

    /** A result that may still be computing. */
    static final class Task<T>
    {
        private final CompletableFuture<T> future;
        private Supplier<T> deferred;

        private Task(CompletableFuture<T> future, Supplier<T> deferred)
        {
            this.future = future;
            this.deferred = deferred;
        }

        /** Waits for the result, rethrowing what the task threw. */
        T join()
        {
            if (future == null)
            {
                Supplier<T> task = deferred;
                deferred = null;
                return task.get();
            }
            try
            {
                return future.join();
            }
            catch (CompletionException e)
            {
                throw rethrow(e);
            }
        }
    }
}
