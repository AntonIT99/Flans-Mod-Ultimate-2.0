package com.flansmodultimate.content;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class ContentLoadingWorkersTest
{
    @Test
    void automaticThreadCountLeavesOneProcessorAndIsCapped()
    {
        assertEquals(1, ContentLoadingWorkers.threadCount(0, 1));
        assertEquals(1, ContentLoadingWorkers.threadCount(0, 2));
        assertEquals(3, ContentLoadingWorkers.threadCount(0, 4));
        assertEquals(8, ContentLoadingWorkers.threadCount(0, 32));
        assertEquals(12, ContentLoadingWorkers.threadCount(12, 4));
        assertFalse(ContentLoadingWorkers.create(1).isParallel());
    }

    @Test
    void mapKeepsTheInputOrderAndUsesTheWorkers()
    {
        List<Integer> inputs = IntStream.range(0, 2_000).boxed().toList();
        Set<String> threads = ConcurrentHashMap.newKeySet();
        try (ContentLoadingWorkers workers = ContentLoadingWorkers.create(4))
        {
            List<Integer> squares = workers.map(inputs, value -> {
                threads.add(Thread.currentThread().getName());
                return value * value;
            });
            assertEquals(inputs.stream().map(value -> value * value).toList(), squares);
            // Nested work started on a worker stays in the same pool.
            List<List<Integer>> nested = workers.map(List.of(1, 2, 3), outer -> workers.map(List.of(outer, outer * 10), inner -> inner + 1));
            assertEquals(List.of(List.of(2, 11), List.of(3, 21), List.of(4, 31)), nested);
        }
        assertTrue(threads.stream().allMatch(name -> name.startsWith("Flan content loading")), threads::toString);
    }

    @Test
    void failuresReachTheCallerUnwrapped()
    {
        try (ContentLoadingWorkers workers = ContentLoadingWorkers.create(3))
        {
            IllegalArgumentException mapped = assertThrows(IllegalArgumentException.class,
                () -> workers.map(List.of(1, 2, 3), value -> {
                    if (value == 2)
                        throw new IllegalArgumentException("broken");
                    return value;
                }));
            // Thrown on another worker, it may arrive as a copy of the same type that wraps the original.
            assertTrue(mapped.getMessage().contains("broken"), mapped::getMessage);
            ContentLoadingWorkers.Task<Object> task = workers.submit(() -> { throw new IllegalStateException("task"); });
            assertEquals("task", assertThrows(IllegalStateException.class, task::join).getMessage());
        }
    }

    @Test
    void sequentialTasksRunOnTheJoiningThread()
    {
        Thread caller = Thread.currentThread();
        ContentLoadingWorkers.Task<Thread> task = ContentLoadingWorkers.sequential().submit(Thread::currentThread);
        assertSame(caller, task.join());
    }
}
