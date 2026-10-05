package com.flansmodultimate.client.distant;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.phys.Vec3;

import java.util.OptionalDouble;
import java.util.concurrent.CompletableFuture;

/**
 * What the mod uses from a renderer that draws simplified terrain beyond the vanilla chunks, such as
 * Distant Horizons. Keeps the rest of the client free of that renderer's classes, so it runs unchanged
 * without it. Client-only; render thread unless stated otherwise.
 */
public interface IDistantTerrain
{
    /** No far-terrain renderer: nothing is drawn and nothing can be measured. */
    IDistantTerrain NONE = new IDistantTerrain()
    {
        @Override
        public boolean drawsBoxes()
        {
            return false;
        }

        @Override
        public boolean canRaycast()
        {
            return false;
        }

        @Override
        public CompletableFuture<OptionalDouble> raycast(Vec3 origin, Vec3 direction, double maxDistance)
        {
            return CompletableFuture.completedFuture(OptionalDouble.empty());
        }

        @Override
        @Nullable
        public IDistantBoxGroup createGroup(String name, DistantBoxStyle style)
        {
            return null;
        }

        @Override
        public void reset()
        {
            // no-op
        }
    };

    /** Whether far terrain is drawn for the current level, so boxes beyond the vanilla chunks are seen. */
    boolean drawsBoxes();

    /** Whether the far terrain of the current level can be measured. */
    boolean canRaycast();

    /**
     * Distance in blocks along a ray to the first far-terrain surface, measured off the render thread from
     * the renderer's simplified terrain, so it is approximate. Completes empty when there is none within
     * {@code maxDistance} or the terrain cannot be read; never completes exceptionally.
     */
    CompletableFuture<OptionalDouble> raycast(Vec3 origin, Vec3 direction, double maxDistance);

    /** A new group of boxes drawn with the far terrain, or null when none can be drawn now. */
    @Nullable
    IDistantBoxGroup createGroup(String name, DistantBoxStyle style);

    /** Called at the end of every client tick. */
    default void tick()
    {
    }

    /** Forgets everything tied to the current world, on disconnecting. */
    void reset();
}
