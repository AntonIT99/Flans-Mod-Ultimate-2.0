package com.flansmodultimate.client.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.*;

class PreloadTrackerTest
{
    private final Queue<Runnable> queue = new ArrayDeque<>();
    private final List<String> preloaded = new ArrayList<>();
    private final PreloadTracker<String> tracker = new PreloadTracker<>(queue::add, preloaded::add);

    @Test
    void itemIsPendingUntilItsPreloadRan()
    {
        assertFalse(tracker.isPending("a"));

        tracker.request("a");
        assertTrue(tracker.isPending("a"));

        queue.remove().run();
        assertFalse(tracker.isPending("a"));
        assertEquals(List.of("a"), preloaded);
    }

    @Test
    void requestsAnItemOnlyOnce()
    {
        tracker.request("a");
        tracker.request("a");
        queue.remove().run();
        tracker.request("a");

        assertTrue(queue.isEmpty());
        assertFalse(tracker.isPending("a"));
    }

    @Test
    void failedPreloadIsNoLongerPending()
    {
        PreloadTracker<String> failing = new PreloadTracker<>(queue::add, item ->
        {
            throw new IllegalStateException(item);
        });

        failing.request("a");
        Runnable task = queue.remove();

        assertThrows(IllegalStateException.class, task::run);
        assertFalse(failing.isPending("a"));
    }

    @Test
    void clearAllowsRequestingAgain()
    {
        tracker.request("a");
        queue.remove().run();

        tracker.clear();
        tracker.request("a");

        assertTrue(tracker.isPending("a"));
        queue.remove().run();
        assertEquals(List.of("a", "a"), preloaded);
    }

    @Test
    void preloadSubmittedBeforeClearStillEnds()
    {
        tracker.request("a");
        tracker.clear();

        queue.remove().run();

        assertFalse(tracker.isPending("a"));
    }
}
