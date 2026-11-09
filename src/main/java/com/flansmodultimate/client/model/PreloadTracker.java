package com.flansmodultimate.client.model;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Submits each item for preloading once and knows which are still waiting for it.
 *
 * <p>
 * An item stops being pending when its preload ends, however it ends, so a caller holding off
 * for a pending item can never be left waiting after a failure.
 * </p>
 */
final class PreloadTracker<T>
{
    private final Executor executor;
    private final Consumer<T> preload;
    private final Set<T> requested = ConcurrentHashMap.newKeySet();
    private final Set<T> pending = ConcurrentHashMap.newKeySet();

    PreloadTracker(Executor executor, Consumer<T> preload)
    {
        this.executor = executor;
        this.preload = preload;
    }

    void request(T item)
    {
        if (requested.contains(item) || !requested.add(item))
            return;

        pending.add(item);
        executor.execute(() ->
        {
            try
            {
                preload.accept(item);
            }
            finally
            {
                pending.remove(item);
            }
        });
    }

    boolean isPending(T item)
    {
        return pending.contains(item);
    }

    /** Lets every item be requested again. Preloads already submitted still end their pending state. */
    void clear()
    {
        requested.clear();
    }
}
