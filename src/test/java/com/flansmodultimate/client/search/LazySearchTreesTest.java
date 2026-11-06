package com.flansmodultimate.client.search;

import com.flansmodultimate.hooks.client.ClientTooltipHooksImpl;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class LazySearchTreesTest
{
    /** Never runs anything: a build handed to it would never finish. */
    private static final Executor NO_EXECUTOR = task -> fail("a deferred tree must not be scheduled");

    /** Counts what the game's own reload actions start: one tree built from the contents. */
    private static final class CountingBuild implements Supplier<List<String>>
    {
        final List<String> contents;
        int built;
        final List<Boolean> shiftSeen = new ArrayList<>();

        CountingBuild(List<String> contents)
        {
            this.contents = contents;
        }

        @Override
        public List<String> get()
        {
            built++;
            shiftSeen.add(new ClientTooltipHooksImpl().isShiftDown());
            return contents;
        }
    }

    @Test
    void treesAreBuiltOnTheirFirstSearchOnly()
    {
        CountingBuild build = new CountingBuild(List.of("m4 carbine", "panzer iv", "m1 garand"));
        CompletableFuture<List<String>> tree = LazySearchTrees.supply(build, NO_EXECUTOR);
        assertEquals(0, build.built, "starting a tree builds nothing");
        assertFalse(((LazySearchTrees.Deferred<List<String>>) tree).isBuilt());

        assertEquals(build.contents, tree.join());
        assertEquals(build.contents, tree.join());
        assertEquals(1, build.built);
        assertTrue(((LazySearchTrees.Deferred<List<String>>) tree).isBuilt());
        assertEquals(List.of(false), build.shiftSeen, "tooltips are indexed as shown at rest");
    }

    @Test
    void replacedTreesAreNeverBuilt()
    {
        CountingBuild stale = new CountingBuild(List.of("m4 carbine"));
        CompletableFuture<List<String>> tree = LazySearchTrees.supply(stale, NO_EXECUTOR);
        // What a reload action does to the tree it replaces.
        tree.cancel(true);

        assertThrows(CancellationException.class, tree::join);
        assertEquals(0, stale.built);
    }

    @Test
    void aFailedBuildFailsTheSearch()
    {
        CompletableFuture<List<String>> tree = LazySearchTrees.supply(() -> {
            throw new IllegalStateException("broken tooltip");
        }, NO_EXECUTOR);

        CompletionException failure = assertThrows(CompletionException.class, tree::join);
        assertInstanceOf(IllegalStateException.class, failure.getCause());
        assertThrows(CompletionException.class, tree::join, "a failed build is not retried");
    }
}
