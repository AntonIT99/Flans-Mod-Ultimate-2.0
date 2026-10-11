package com.wolffsmod.npcs.sim;

import net.minecraft.world.phys.Vec3;

/**
 * Follows where a target has been, so a shot can be led by how it really moves rather than by its velocity in a single
 * tick. The velocity is smoothed over recent positions, an acceleration is estimated cautiously from how the velocity
 * changes, and a teleport or a gap in the observations starts over instead of producing a wild lead. No world access.
 */
public final class TargetMotion
{
    /** Share of each new measurement taken into the smoothed velocity. */
    static final double VELOCITY_BLEND = 0.35D;
    static final double ACCELERATION_BLEND = 0.2D;
    /** Blocks per tick above which a jump counts as a teleport rather than as movement. */
    static final double TELEPORT_SPEED = 8D;
    /** Ticks without an observation after which the history is stale. */
    static final long MAX_GAP = 5L;
    /** Blocks per tick squared; a target is not assumed to accelerate harder than this. */
    static final double MAX_ACCELERATION = 0.06D;
    /** Ticks over which an acceleration is assumed to last. */
    static final double ACCELERATION_HORIZON = 20D;
    public static final double MAX_FLIGHT_TICKS = 100D;
    public static final double MAX_HORIZONTAL_LEAD = 96D;
    public static final double MAX_VERTICAL_LEAD = 48D;

    private Vec3 lastPosition;
    private long lastTime = Long.MIN_VALUE;
    private Vec3 velocity = Vec3.ZERO;
    private Vec3 acceleration = Vec3.ZERO;
    private int samples;

    /** Forgets the target. */
    public void reset()
    {
        lastPosition = null;
        lastTime = Long.MIN_VALUE;
        velocity = Vec3.ZERO;
        acceleration = Vec3.ZERO;
        samples = 0;
    }

    /**
     * Records where the target is now. Call once per tick while the target is held.
     *
     * @param time
     *            game time of the observation, in ticks
     */
    public void observe(Vec3 position, long time)
    {
        if (lastPosition == null || time <= lastTime || time - lastTime > MAX_GAP)
        {
            restart(position, time);
            return;
        }
        double elapsed = (double) time - lastTime;
        Vec3 measured = position.subtract(lastPosition).scale(1D / elapsed);
        if (measured.length() > TELEPORT_SPEED)
        {
            restart(position, time);
            return;
        }
        if (samples == 0)
        {
            // A newly acquired target is led by its first tick of movement at once.
            velocity = measured;
            acceleration = Vec3.ZERO;
        }
        else
        {
            Vec3 previous = velocity;
            velocity = previous.add(measured.subtract(previous).scale(VELOCITY_BLEND));
            Vec3 change = velocity.subtract(previous).scale(1D / elapsed);
            acceleration = limit(acceleration.add(change.subtract(acceleration).scale(ACCELERATION_BLEND)), MAX_ACCELERATION);
        }
        lastPosition = position;
        lastTime = time;
        samples++;
    }

    private void restart(Vec3 position, long time)
    {
        reset();
        lastPosition = position;
        lastTime = time;
    }

    /** @return the smoothed velocity in blocks per tick; zero until the target has been seen to move */
    public Vec3 velocity()
    {
        return velocity;
    }

    /** @return the cautious acceleration estimate in blocks per tick squared */
    public Vec3 acceleration()
    {
        return acceleration;
    }

    /**
     * @param flightTicks
     *            how long the projectile flies
     * @return how far the target is expected to have moved by then, within the lead limits
     */
    public Vec3 lead(double flightTicks)
    {
        if (!Double.isFinite(flightTicks) || flightTicks <= 0D)
            return Vec3.ZERO;
        double time = Math.min(MAX_FLIGHT_TICKS, flightTicks);
        double accelerating = Math.min(time, ACCELERATION_HORIZON);
        // Once the acceleration is assumed to have ended, the target coasts at the speed it reached.
        Vec3 offset = velocity.scale(time).add(acceleration.scale(accelerating * (time - accelerating * 0.5D)));
        double horizontal = Math.sqrt(offset.x * offset.x + offset.z * offset.z);
        double scale = horizontal > MAX_HORIZONTAL_LEAD ? MAX_HORIZONTAL_LEAD / horizontal : 1D;
        return new Vec3(offset.x * scale, Math.max(-MAX_VERTICAL_LEAD, Math.min(MAX_VERTICAL_LEAD, offset.y)), offset.z * scale);
    }

    private static Vec3 limit(Vec3 vector, double length)
    {
        double current = vector.length();
        return current > length ? vector.scale(length / current) : vector;
    }
}
