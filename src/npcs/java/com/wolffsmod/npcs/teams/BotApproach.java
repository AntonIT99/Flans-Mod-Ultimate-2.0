package com.wolffsmod.npcs.teams;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/** A squad's transient approach to its objective. The final task stays available to capture and transport logic. */
final class BotApproach
{
    private static final double MIN_DISTANCE = 40D;
    private static final double REACHED = 8D;
    private static final long TIMEOUT = 600L;
    @Nullable
    private SquadTask task;
    @Nullable
    private Vec3 waypoint;
    private long expiresAt;
    private int retries;

    /** Keeps an approach until reached or timed out; repeated commander passes never send a squad back to it. */
    void update(SquadTask next, Vec3 position, int lane, List<Vec3> markers, long now, boolean enabled)
    {
        if (!matches(next))
        {
            task = next;
            waypoint = enabled ? waypoint(position, next, lane, markers) : null;
            expiresAt = now + TIMEOUT;
        }
        if (!enabled || waypoint != null && (now >= expiresAt || position.subtract(waypoint).horizontalDistance() <= REACHED))
            waypoint = null;
    }

    void clear()
    {
        task = null;
        waypoint = null;
        retries = 0;
    }

    /** A leader-reported failure tries the other side before asking the commander for another objective. */
    void retry(SquadTask next, Vec3 position, int lane, List<Vec3> markers, long now)
    {
        task = next;
        retries++;
        waypoint = waypoint(position, next, 1 + Math.floorMod(lane + retries, 4), markers);
        expiresAt = now + TIMEOUT;
    }

    @Nullable
    Vec3 waypoint(SquadTask current)
    {
        return matches(current) ? waypoint : null;
    }

    private boolean matches(SquadTask current)
    {
        return task != null && current.kind() == task.kind() && current.target().distanceToSqr(task.target()) <= 256D;
    }

    @Nullable
    private static Vec3 waypoint(Vec3 from, SquadTask task, int lane, List<Vec3> markers)
    {
        if (lane == 0 || task.kind() != SquadTask.Kind.ATTACK && task.kind() != SquadTask.Kind.HUNT)
            return null;
        Vec3 delta = task.target().subtract(from).multiply(1D, 0D, 1D);
        double distance = delta.length();
        if (distance < Math.max(MIN_DISTANCE, task.radius() * 2D))
            return null;
        Vec3 forward = delta.scale(1D / distance);
        double side = lane % 2 == 1 ? 1D : -1D;
        int widening = (lane - 1) / 2;
        double width = Math.min(32D, Math.min(distance * 0.25D, 16D + 8D * widening));
        Vec3 desired = from.add(delta.scale(0.6D)).add(-forward.z * side * width, 0D, forward.x * side * width);
        // Prefer authored terrain routes on the assigned side, without sending squads behind their start or goal.
        return markers.stream().filter(marker ->
        {
            Vec3 offset = marker.subtract(from);
            double along = offset.dot(forward);
            double lateral = (-forward.z * offset.x + forward.x * offset.z) * side;
            return along > distance * 0.2D && along < distance * 0.85D && lateral >= width * 0.5D && marker.distanceTo(desired) <= 32D;
        }).min(Comparator.comparingDouble(marker -> marker.distanceToSqr(desired))).orElse(desired);
    }
}
