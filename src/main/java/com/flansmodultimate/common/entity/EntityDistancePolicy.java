package com.flansmodultimate.common.entity;

import com.flansmodultimate.api.entity.IFlanNpcDistance;
import com.flansmodultimate.config.ModClientConfig;
import com.flansmodultimate.config.ModCommonConfig;

import net.minecraft.world.entity.Entity;

import java.util.concurrent.atomic.AtomicLong;

/** Common distance rules. Reading client configuration never initializes a client-only class. */
public final class EntityDistancePolicy
{
    private static final AtomicLong TRACKING_REVISION = new AtomicLong();

    private EntityDistancePolicy()
    {}

    public static boolean isFlanNpc(Entity entity)
    {
        return entity instanceof IFlanNpcDistance npc && npc.usesFlanNpcDistanceSettings();
    }

    /** Negative means leave the vanilla tracker alone. All returned ranges are blocks. */
    public static int trackingRange(Entity entity)
    {
        if (entity instanceof Driveable || entity instanceof Seat || entity instanceof Wheel)
            return ModCommonConfig.driveableTrackingRange();
        return isFlanNpc(entity) ? ModCommonConfig.flanNpcTrackingRange() : -1;
    }

    /**
     * Driveables and their proxies stay paired past the server view distance, up to their tracking range,
     * so that a client can draw them beyond its loaded chunks.
     */
    public static boolean tracksBeyondViewDistance(Entity entity)
    {
        return entity instanceof Driveable || entity instanceof Seat || entity instanceof Wheel;
    }

    /** Whether the client draws this entity in chunks it has not loaded or built. */
    public static boolean rendersBeyondChunks(Entity entity)
    {
        ModClientConfig config = ModClientConfig.get();
        return entity instanceof Driveable && config != null && extendsBeyondChunks(config.driveableRenderDistanceMultiplier);
    }

    static boolean extendsBeyondChunks(double multiplier)
    {
        return multiplier > 1D && multiplier <= 4D;
    }

    public static double renderDistance(Entity entity, boolean npc)
    {
        ModClientConfig config = ModClientConfig.get();
        double multiplier = 1D;
        if (config != null)
            multiplier = npc ? config.flanNpcRenderDistanceMultiplier : config.driveableRenderDistanceMultiplier;
        return proportionalDistance(entity.getBoundingBox().getSize(), Entity.getViewScale(), multiplier);
    }

    static double proportionalDistance(double size, double viewScale, double multiplier)
    {
        if (!Double.isFinite(size) || size <= 0D)
            size = 1D;
        if (!Double.isFinite(viewScale) || viewScale <= 0D)
            viewScale = 1D;
        if (!Double.isFinite(multiplier) || multiplier < 0.25D || multiplier > 4D)
            multiplier = 1D;
        double distance = size * 64D * viewScale * multiplier;
        return Double.isFinite(distance) && distance > 0D ? distance : 64D;
    }

    public static long trackingRevision()
    {
        return TRACKING_REVISION.get();
    }

    /** Server thread: each dimension will reevaluate its player pairings on the next tick. */
    public static void requestTrackingRefresh()
    {
        TRACKING_REVISION.incrementAndGet();
    }
}
