package com.flansmodultimate.client.search;

import com.flansmodultimate.hooks.client.ClientTooltipHooksImpl;
import org.junit.jupiter.api.Test;

import net.minecraft.client.searchtree.RefreshableSearchTree;
import net.minecraft.client.searchtree.SearchRegistry;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LazySearchTreesTest
{
    /** Counts what the game's own factories would do: create a tree, then fill it on refresh. */
    private static final class CountingFactory implements SearchRegistry.TreeBuilderSupplier<String>
    {
        int created;
        int filled;
        final List<Boolean> shiftSeen = new ArrayList<>();

        @Override
        public RefreshableSearchTree<String> apply(List<String> contents)
        {
            created++;
            return new RefreshableSearchTree<>()
            {
                @Override
                public void refresh()
                {
                    filled++;
                    shiftSeen.add(new ClientTooltipHooksImpl().isShiftDown());
                }

                @Override
                public List<String> search(String query)
                {
                    return contents.stream().filter(entry -> entry.contains(query)).toList();
                }
            };
        }
    }

    @Test
    void treesAreBuiltOnTheirFirstSearchAndAgainOnlyAfterAReload()
    {
        CountingFactory factory = new CountingFactory();
        SearchRegistry registry = new SearchRegistry();
        SearchRegistry.Key<String> key = new SearchRegistry.Key<>();
        registry.register(key, LazySearchTrees.wrap(factory));

        registry.populate(key, List.of("m4 carbine", "panzer iv", "m1 garand"));
        registry.onResourceManagerReload(null);
        assertEquals(0, factory.created, "populating and reloading build nothing");

        assertEquals(List.of("m4 carbine", "m1 garand"), registry.getTree(key).search("m"));
        assertEquals(List.of("panzer iv"), registry.getTree(key).search("panzer"));
        assertEquals(1, factory.created);
        assertEquals(1, factory.filled);

        registry.onResourceManagerReload(null);
        assertEquals(1, factory.created, "a reload only drops the built tree");
        registry.getTree(key).search("m");
        assertEquals(2, factory.created);

        registry.populate(key, List.of("tiger"));
        assertEquals(List.of("tiger"), registry.getTree(key).search("tig"));
        assertEquals(List.of(false, false, false), factory.shiftSeen, "tooltips are indexed as shown at rest");
    }

    @Test
    void wrappingIsIdempotent()
    {
        CountingFactory factory = new CountingFactory();
        SearchRegistry.TreeBuilderSupplier<String> once = LazySearchTrees.wrap(factory);
        assertSame(once, LazySearchTrees.wrap(once));
    }
}
