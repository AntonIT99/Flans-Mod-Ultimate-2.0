package com.flansmodultimate.mixin;

import com.flansmodultimate.client.search.LazySearchTrees;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.minecraft.client.searchtree.SearchRegistry;

/**
 * Defers every registered search tree to its first search; see {@link LazySearchTrees}.
 *
 * <p>Wrapping the factory at registration covers the creative search tab, every creative tab with
 * a search bar and the recipe book, whichever mod registers them, without copying how the game
 * fills the trees.</p>
 */
@Mixin(SearchRegistry.class)
public abstract class SearchRegistryLazyTreesMixin
{
    @ModifyVariable(method = "register", at = @At("HEAD"), argsOnly = true)
    private SearchRegistry.TreeBuilderSupplier<?> flansmodultimate$buildOnFirstSearch(SearchRegistry.TreeBuilderSupplier<?> factory)
    {
        return LazySearchTrees.wrap(factory);
    }
}
