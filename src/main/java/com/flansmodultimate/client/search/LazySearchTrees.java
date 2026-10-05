package com.flansmodultimate.client.search;

import com.flansmodultimate.hooks.client.ClientTooltipHooksImpl;
import com.flansmodultimate.util.FlansLog;
import org.jetbrains.annotations.NotNull;

import net.minecraft.client.searchtree.RefreshableSearchTree;
import net.minecraft.client.searchtree.SearchRegistry;

import java.util.List;

/**
 * Builds each search tree the first time something is searched in it, rather than every time its
 * contents change.
 * <p>
 * The game builds the tree of every creative tab with a search bar, of the creative search tab and
 * of the recipe book eagerly: on opening the creative inventory, on joining a world, on every tag
 * sync and on every resource reload. Each build reads the tooltip of every item it holds, and with
 * thousands of guns, vehicles and their paintjobs that cost is paid over and over for trees nobody
 * searches. A tree is only ever queried for a non-empty search and is then built from the same
 * contents, so searching finds the same items; the build just moves to the first query.
 * <p>
 * {@code -Dflansmodultimate.eagerSearchTrees=true} restores eager building.
 */
public final class LazySearchTrees
{
    private static final boolean EAGER = Boolean.getBoolean("flansmodultimate.eagerSearchTrees");

    private LazySearchTrees() {}

    public static <T> SearchRegistry.TreeBuilderSupplier<T> wrap(SearchRegistry.TreeBuilderSupplier<T> factory)
    {
        if (EAGER || factory instanceof Deferred<?>)
            return factory;
        return new Deferred<>(factory);
    }

    private record Deferred<T>(SearchRegistry.TreeBuilderSupplier<T> factory) implements SearchRegistry.TreeBuilderSupplier<T>
    {
        @Override
        public RefreshableSearchTree<T> apply(List<T> contents)
        {
            return new LazyTree<>(factory, contents);
        }
    }

    /** Used on the render thread only, like every search tree. */
    static final class LazyTree<T> implements RefreshableSearchTree<T>
    {
        private final SearchRegistry.TreeBuilderSupplier<T> factory;
        private final List<T> contents;
        private RefreshableSearchTree<T> tree;

        LazyTree(SearchRegistry.TreeBuilderSupplier<T> factory, List<T> contents)
        {
            this.factory = factory;
            this.contents = contents;
        }

        /** Called once the tree is created and on every resource reload: whatever was built is stale. */
        @Override
        public void refresh()
        {
            tree = null;
        }

        @Override
        @NotNull
        public List<T> search(@NotNull String query)
        {
            RefreshableSearchTree<T> built = tree;
            if (built == null)
            {
                built = build();
                tree = built;
            }
            return built.search(query);
        }

        boolean isBuilt()
        {
            return tree != null;
        }

        private RefreshableSearchTree<T> build()
        {
            long start = System.nanoTime();
            // Built on a keystroke, which may be a capital letter: Shift must not switch the indexed
            // tooltips of Flan's items to their detailed statistics.
            RefreshableSearchTree<T> built = ClientTooltipHooksImpl.withoutHeldKeys(() -> {
                RefreshableSearchTree<T> created = factory.apply(contents);
                created.refresh();
                return created;
            });
            FlansLog.log.debug("Built a search tree of {} entries in {} ms on its first search",
                contents.size(), (System.nanoTime() - start) / 1_000_000);
            return built;
        }
    }
}
