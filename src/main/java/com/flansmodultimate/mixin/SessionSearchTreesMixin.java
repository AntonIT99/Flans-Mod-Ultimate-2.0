package com.flansmodultimate.mixin;

import com.flansmodultimate.client.search.LazySearchTrees;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.client.multiplayer.SessionSearchTrees;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Defers every session search tree to its first search; see {@link LazySearchTrees}.
 *
 * <p>
 * The recipe book, creative name and creative tag trees, including NeoForge's per-tab trees, are
 * all started by the reload actions of this class with {@code CompletableFuture.supplyAsync}.
 * Those actions are compiler-generated lambdas, some of them static, hence the wildcard target and
 * the static handler. Replacing the started future covers every tree, whichever mod's tab it
 * belongs to, without copying how the game fills them.
 * </p>
 */
@Mixin(SessionSearchTrees.class)
@SuppressWarnings("java:S1118") // Mixin classes are never instantiated and must not declare constructors
public abstract class SessionSearchTreesMixin
{
    @Redirect(method = "*", expect = 3, at = @At(value = "INVOKE",
        target = "Ljava/util/concurrent/CompletableFuture;supplyAsync(Ljava/util/function/Supplier;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"))
    private static <T> CompletableFuture<T> flansmodultimateBuildOnFirstSearch(Supplier<T> build, Executor executor)
    {
        return LazySearchTrees.supply(build, executor);
    }
}
