package com.flansmodultimate.client.model;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.function.Supplier;

/**
 * A cache whose values are built by whichever thread asks first, while other threads asking for
 * the same key wait for that build instead of repeating it.
 *
 * <p>
 * Unlike {@link ConcurrentHashMap#computeIfAbsent}, no map lock is held while a value is built, so
 * a slow build blocks only the threads that need that very key. A {@code null} value is cached like
 * any other; a build that throws caches nothing and the next request tries again.
 * </p>
 */
final class SharedLoadCache<K, V>
{
    private final Map<K, Optional<V>> loaded = new ConcurrentHashMap<>();
    private final Object lock = new Object();
    /** Builds not finished yet. Guarded by {@link #lock}. */
    private final Map<K, FutureTask<Optional<V>>> loading = new HashMap<>();
    /** Identifies the builds {@link #clear()} has not discarded. Guarded by {@link #lock}. */
    private long generation;

    /** The value of this key if it has been built, without building it. */
    @Nullable
    V getIfLoaded(K key)
    {
        return loaded.getOrDefault(key, Optional.empty()).orElse(null);
    }

    @Nullable
    @SuppressWarnings("java:S2789") // A null Optional is the map's "no entry"; a present entry may hold an empty Optional.
    V getOrLoad(K key, Supplier<V> loader)
    {
        Optional<V> value = loaded.get(key);
        if (value != null)
            return value.orElse(null);

        FutureTask<Optional<V>> load;
        synchronized (lock)
        {
            value = loaded.get(key);
            if (value != null)
                return value.orElse(null);

            load = loading.get(key);
            if (load == null)
            {
                long started = generation;
                load = new FutureTask<>(() -> build(key, loader, started));
                loading.put(key, load);
            }
        }

        // Builds here unless another thread got there first, in which case this does nothing.
        load.run();
        return await(load).orElse(null);
    }

    /** Forgets every value. Builds still running finish for their callers but are not kept. */
    void clear()
    {
        synchronized (lock)
        {
            generation++;
            loading.clear();
            loaded.clear();
        }
    }

    private Optional<V> build(K key, Supplier<V> loader, long started)
    {
        V value = null;
        boolean built = false;
        try
        {
            value = loader.get();
            built = true;
            return Optional.ofNullable(value);
        }
        finally
        {
            synchronized (lock)
            {
                if (started == generation)
                {
                    loading.remove(key);
                    if (built)
                        loaded.put(key, Optional.ofNullable(value));
                }
            }
        }
    }

    private Optional<V> await(FutureTask<Optional<V>> load)
    {
        try
        {
            return load.get();
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
        catch (ExecutionException e)
        {
            if (e.getCause() instanceof RuntimeException runtimeException)
                throw runtimeException;
            if (e.getCause() instanceof Error error)
                throw error;
            throw new IllegalStateException(e.getCause());
        }
    }
}
