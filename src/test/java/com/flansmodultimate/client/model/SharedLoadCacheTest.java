package com.flansmodultimate.client.model;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class SharedLoadCacheTest
{
    private static final long TIMEOUT_SECONDS = 10;

    @Test
    void buildsOnTheCallingThreadAndOnlyOnce()
    {
        SharedLoadCache<String, Thread> cache = new SharedLoadCache<>();
        AtomicInteger builds = new AtomicInteger();

        Thread builtBy = cache.getOrLoad("a", () ->
        {
            builds.incrementAndGet();
            return Thread.currentThread();
        });

        assertSame(Thread.currentThread(), builtBy);
        assertSame(builtBy, cache.getOrLoad("a", Thread::currentThread));
        assertEquals(1, builds.get());
    }

    @Test
    void cachesAMissingValue()
    {
        SharedLoadCache<String, String> cache = new SharedLoadCache<>();
        AtomicInteger builds = new AtomicInteger();

        assertNull(cache.getOrLoad("a", () ->
        {
            builds.incrementAndGet();
            return null;
        }));
        assertNull(cache.getOrLoad("a", () -> "other"));
        assertEquals(1, builds.get());
    }

    @Test
    void getIfLoadedNeverBuilds()
    {
        SharedLoadCache<String, String> cache = new SharedLoadCache<>();

        assertNull(cache.getIfLoaded("a"));
        cache.getOrLoad("a", () -> "value");
        assertEquals("value", cache.getIfLoaded("a"));
    }

    @Test
    void failedBuildPropagatesAndIsRetried()
    {
        SharedLoadCache<String, String> cache = new SharedLoadCache<>();

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> cache.getOrLoad("a", () ->
        {
            throw new IllegalArgumentException("broken");
        }));

        assertEquals("broken", thrown.getMessage());
        assertEquals("value", cache.getOrLoad("a", () -> "value"));
    }

    @Test
    void secondCallerWaitsForTheBuildInProgress() throws Exception
    {
        SharedLoadCache<String, String> cache = new SharedLoadCache<>();
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger builds = new AtomicInteger();

        CompletableFuture<String> first = CompletableFuture.supplyAsync(() -> cache.getOrLoad("a", () ->
        {
            builds.incrementAndGet();
            started.countDown();
            await(release);
            return "first";
        }));
        assertTrue(started.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));

        CompletableFuture<String> second = CompletableFuture.supplyAsync(() -> cache.getOrLoad("a", () ->
        {
            builds.incrementAndGet();
            return "second";
        }));
        assertFalse(second.isDone());

        release.countDown();
        assertEquals("first", first.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertEquals("first", second.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertEquals(1, builds.get());
    }

    @Test
    void slowBuildDoesNotBlockOtherKeys() throws Exception
    {
        SharedLoadCache<String, String> cache = new SharedLoadCache<>();
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        CompletableFuture<String> slow = CompletableFuture.supplyAsync(() -> cache.getOrLoad("slow", () ->
        {
            started.countDown();
            await(release);
            return "slow";
        }));
        assertTrue(started.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));

        assertEquals("fast", cache.getOrLoad("fast", () -> "fast"));

        release.countDown();
        assertEquals("slow", slow.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
    }

    @Test
    void clearDiscardsABuildInProgress() throws Exception
    {
        SharedLoadCache<String, String> cache = new SharedLoadCache<>();
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        CompletableFuture<String> stale = CompletableFuture.supplyAsync(() -> cache.getOrLoad("a", () ->
        {
            started.countDown();
            await(release);
            return "stale";
        }));
        assertTrue(started.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));

        cache.clear();
        assertEquals("fresh", cache.getOrLoad("a", () -> "fresh"));

        release.countDown();
        assertEquals("stale", stale.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertEquals("fresh", cache.getIfLoaded("a"));
    }

    private static void await(CountDownLatch latch)
    {
        try
        {
            assertTrue(latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
