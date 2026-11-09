package com.flansmodultimate.content;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class ContentPackJobsTest
{
    @Test
    void concurrencyFollowsTheConfiguredThreadsUpToTheLimit()
    {
        assertEquals(1, ContentPackJobs.concurrency(1));
        assertEquals(1, ContentPackJobs.concurrency(2));
        assertEquals(2, ContentPackJobs.concurrency(4));
        assertEquals(4, ContentPackJobs.concurrency(8));
        assertEquals(4, ContentPackJobs.concurrency(32));
    }

    @Test
    void sequentialJobsStayOnTheCallingThreadAndStopAtTheFirstFailure()
    {
        Thread caller = Thread.currentThread();
        List<Integer> ran = new ArrayList<>();

        List<Integer> inputs = List.of(1, 2, 3);
        Function<Integer, Integer> job = input ->
        {
            assertSame(caller, Thread.currentThread());
            ran.add(input);
            if (input == 2)
                throw new IllegalArgumentException("failed");
            return input;
        };

        assertThrows(IllegalArgumentException.class, () -> ContentPackJobs.runAll(inputs, job, 1, finished -> {}));

        assertEquals(List.of(1, 2), ran);
    }

    @Test
    void resultsKeepInputOrderAndConcurrencyStaysBounded()
    {
        List<Integer> inputs = IntStream.range(0, 24).boxed().toList();
        AtomicInteger running = new AtomicInteger();
        AtomicInteger peak = new AtomicInteger();
        List<Integer> finished = new CopyOnWriteArrayList<>();
        Thread caller = Thread.currentThread();

        List<Integer> results = ContentPackJobs.runAll(inputs, input ->
        {
            peak.accumulateAndGet(running.incrementAndGet(), Math::max);
            sleep(5);
            running.decrementAndGet();
            return input * 2;
        }, 3, input ->
        {
            assertSame(caller, Thread.currentThread());
            finished.add(input);
        });

        assertEquals(inputs.stream().map(input -> input * 2).toList(), results);
        assertEquals(inputs.size(), finished.size());
        assertTrue(peak.get() > 1, "Jobs should overlap");
        assertTrue(peak.get() <= 3, "Peak of " + peak.get() + " exceeds the limit");
    }

    @Test
    void everyJobEndsBeforeTheFirstFailureInInputOrderIsRethrown()
    {
        CountDownLatch firstMayFail = new CountDownLatch(1);
        AtomicInteger completed = new AtomicInteger();

        List<Integer> inputs = List.of(0, 1, 2);
        Function<Integer, Integer> job = input ->
        {
            if (input == 0)
            {
                await(firstMayFail);
                throw new IllegalStateException("first");
            }
            if (input == 1)
            {
                firstMayFail.countDown();
                throw new IllegalStateException("second");
            }
            sleep(20);
            completed.incrementAndGet();
            return input;
        };

        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> ContentPackJobs.runAll(inputs, job, 3, finished -> {}));

        assertEquals("first", failure.getMessage());
        assertEquals(1, completed.get());
    }

    @Test
    void jobsRunWithTheModClassLoader()
    {
        ClassLoader expected = ContentPackJobs.class.getClassLoader();

        assertEquals(List.of(expected, expected), ContentPackJobs.runAll(List.of(1, 2), input -> Thread.currentThread().getContextClassLoader(), 2, finished -> {}));
    }

    private static void await(CountDownLatch latch)
    {
        try
        {
            assertTrue(latch.await(10, TimeUnit.SECONDS));
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    // Deliberately holds jobs open so that they overlap, which the concurrency bound assertions depend on.
    @SuppressWarnings("java:S2925")
    private static void sleep(long millis)
    {
        try
        {
            Thread.sleep(millis);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
