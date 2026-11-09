package com.flansmodultimate.content;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Runs one job per content pack, a bounded number at a time. Each job spends most of its time on the
 * single-threaded stages of generation and hands its file work to {@link ContentLoadingWorkers}, so
 * jobs run on threads of their own rather than occupying the workers they wait for.
 */
final class ContentPackJobs
{
    /** More packs at once mostly queue on the disk, while each adds its pending files to memory. */
    private static final int MAX_CONCURRENT_PACKS = 4;

    private ContentPackJobs()
    {}

    /** Half of the content threads, so that one configured thread keeps reprocessing sequential. */
    static int concurrency(int workerThreads)
    {
        return Math.max(1, Math.min(MAX_CONCURRENT_PACKS, workerThreads / 2));
    }

    /**
     * Applies the job to every input and returns the results in input order.
     * <p>
     * With a concurrency of one, or a single input, everything runs on the calling thread and the first
     * failure stops the rest. Otherwise every job started finishes before the first failure in input
     * order is rethrown, so no job is still writing when the caller sees it.
     *
     * @param onFinished
     *            called on the calling thread as each job ends, successfully or not
     */
    static <I, O> List<O> runAll(List<I> inputs, Function<? super I, ? extends O> job, int concurrency, Consumer<? super I> onFinished)
    {
        if (concurrency <= 1 || inputs.size() < 2)
            return runSequentially(inputs, job, onFinished);

        ExecutorService executor = newExecutor(Math.min(concurrency, inputs.size()));
        try
        {
            CompletionService<O> completion = new ExecutorCompletionService<>(executor);
            List<Future<O>> futures = new ArrayList<>(inputs.size());
            Map<Future<O>, I> futureInputs = new HashMap<>();
            for (I input : inputs)
            {
                Future<O> future = completion.submit(() -> job.apply(input));
                futures.add(future);
                futureInputs.put(future, input);
            }
            for (int i = 0; i < inputs.size(); i++)
                onFinished.accept(futureInputs.get(completion.take()));
            return collect(futures);
        }
        catch (InterruptedException e)
        {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while reprocessing content packs", e);
        }
        finally
        {
            executor.shutdown();
        }
    }

    private static <I, O> List<O> runSequentially(List<I> inputs, Function<? super I, ? extends O> job, Consumer<? super I> onFinished)
    {
        List<O> results = new ArrayList<>(inputs.size());
        for (I input : inputs)
        {
            results.add(job.apply(input));
            onFinished.accept(input);
        }
        return results;
    }

    private static <O> List<O> collect(List<Future<O>> futures) throws InterruptedException
    {
        List<O> results = new ArrayList<>(futures.size());
        for (Future<O> future : futures)
        {
            try
            {
                results.add(future.get());
            }
            catch (ExecutionException e)
            {
                if (e.getCause() instanceof RuntimeException runtimeException)
                    throw runtimeException;
                if (e.getCause() instanceof Error error)
                    throw error;
                throw new IllegalStateException(e.getCause());
            }
        }
        return results;
    }

    private static ExecutorService newExecutor(int threads)
    {
        AtomicInteger counter = new AtomicInteger();
        ClassLoader classLoader = ContentPackJobs.class.getClassLoader();
        return Executors.newFixedThreadPool(threads, task ->
        {
            Thread thread = new Thread(task, "Flan content reprocessing " + counter.incrementAndGet());
            thread.setDaemon(true);
            // Pack code is loaded by the mod class loader, not by the loader's own.
            thread.setContextClassLoader(classLoader);
            return thread;
        });
    }
}
