package com.flansmodultimate.client.distant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.phys.Vec3;

import java.util.OptionalDouble;
import java.util.concurrent.CompletableFuture;

/**
 * Carries a rangefinder measurement on beyond the loaded chunks with the far-terrain renderer's simplified
 * terrain. One measurement runs at a time, off the render thread; callers watch {@link #results()} to learn
 * when a newer one has finished. Render thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DistantRangefinder
{
    @Nullable
    private static CompletableFuture<OptionalDouble> pending;
    private static long results;
    private static double latest = -1D;

    /** Whether measurements beyond the loaded chunks can be made now. */
    public static boolean available()
    {
        return DistantHorizonsClient.rangefinderEnabled() && DistantHorizonsClient.terrain().canRaycast();
    }

    /**
     * Starts measuring along a ray that has already crossed {@code travelled} blocks of loaded terrain without
     * hitting anything, unless a measurement is still running.
     *
     * @param from        where the loaded terrain ends along the ray
     * @param direction   unit vector
     * @param travelled   distance from the observer to {@code from}
     * @param maxDistance the rangefinder's reach from the observer
     * @return whether a measurement is now running
     */
    public static boolean request(Vec3 from, Vec3 direction, double travelled, double maxDistance)
    {
        if (pending != null)
            return true;
        if (!available() || travelled >= maxDistance)
            return false;

        pending = DistantHorizonsClient.terrain().raycast(from, direction, maxDistance - travelled)
            .thenApply(range -> range.isPresent() ? OptionalDouble.of(range.getAsDouble() + travelled) : range);
        return true;
    }

    /** Collects a finished measurement; called every tick and before reading the results. */
    public static void poll()
    {
        if (pending == null || !pending.isDone())
            return;

        OptionalDouble range = pending.isCompletedExceptionally() ? OptionalDouble.empty() : pending.join();
        pending = null;
        latest = range.isPresent() ? range.getAsDouble() : -1D;
        results++;
    }

    /** Number of measurements finished so far; it grows when a new one is ready. */
    public static long results()
    {
        return results;
    }

    /** The newest finished measurement in blocks from the observer, or -1 when it found nothing. */
    public static double latest()
    {
        return latest;
    }

    public static void reset()
    {
        if (pending != null)
            pending.cancel(false);
        pending = null;
        latest = -1D;
    }
}
