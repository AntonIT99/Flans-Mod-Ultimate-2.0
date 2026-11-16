package com.flansmodultimate.common.entity;

import com.flansmodultimate.api.entity.IFlanNpcDistance;
import com.flansmodultimate.config.ModClientConfig;
import com.flansmodultimate.config.ModCommonConfig;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.Entity;

import java.util.concurrent.atomic.AtomicLong;

/** Common distance rules. Reading client configuration never initializes a client-only class. */
public final class EntityDistancePolicy
{
    private static final AtomicLong TRACKING_REVISION = new AtomicLong();
    /** The largest player ticket radius the vanilla ticket tracker can hold, in chunks. */
    private static final int MAX_PLAYER_TICKET_RADIUS = 32;

    private EntityDistancePolicy()
    {}

    public static boolean isFlanNpc(Entity entity)
    {
        return entity instanceof IFlanNpcDistance npc && npc.usesFlanNpcDistanceSettings();
    }

    /**
     * The driveable an entity rides, or null. A crew member, a player or an NPC, rides a seat, which follows its
     * driveable without being one of its passengers.
     */
    @Nullable
    public static Driveable riddenDriveable(Entity entity)
    {
        Entity root = entity.getRootVehicle();
        if (root == entity)
            return null;
        if (root instanceof Seat seat)
            return seat.getDriveable();
        return root instanceof Driveable driveable ? driveable : null;
    }

    /** Negative means leave the vanilla tracker alone. All returned ranges are blocks. Crews share their driveable's range. */
    public static int trackingRange(Entity entity)
    {
        if (entity instanceof Driveable || entity instanceof Seat || entity instanceof Wheel || riddenDriveable(entity) != null)
            return ModCommonConfig.driveableTrackingRange();
        return isFlanNpc(entity) ? ModCommonConfig.flanNpcTrackingRange() : -1;
    }

    /**
     * Driveables, their proxies and their crews stay paired past the server view distance, up to their tracking range,
     * so that a client can draw them beyond its loaded chunks.
     */
    public static boolean tracksBeyondViewDistance(Entity entity)
    {
        return entity instanceof Driveable || entity instanceof Seat || entity instanceof Wheel || riddenDriveable(entity) != null;
    }

    /** Whether the client draws this entity, a driveable or one of its crew, in chunks it has not loaded or built. */
    public static boolean rendersBeyondChunks(Entity entity)
    {
        ModClientConfig config = ModClientConfig.get();
        return config != null && extendsBeyondChunks(config.driveableRenderDistanceMultiplier) && (entity instanceof Driveable || riddenDriveable(entity) != null);
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

    /**
     * The radius in chunks of the server's player chunk tickets. Vanilla loads chunks only within the view distance,
     * so a larger simulation distance ticks nothing beyond it; extending the tickets loads those chunks, which keeps
     * the driveables in them alive and tracked without sending their terrain to clients.
     */
    public static int playerTicketRadius(int viewDistance, int simulationDistance)
    {
        return playerTicketRadius(viewDistance, simulationDistance, ModCommonConfig.loadChunksToSimulationDistance());
    }

    static int playerTicketRadius(int viewDistance, int simulationDistance, boolean loadToSimulationDistance)
    {
        if (!loadToSimulationDistance || simulationDistance <= viewDistance)
            return viewDistance;
        return Math.max(viewDistance, Math.min(simulationDistance, MAX_PLAYER_TICKET_RADIUS));
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
