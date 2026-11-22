package com.flansmodultimate.common.driveables.physics;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * The Labjac fork's helicopter hover hold: once the pilot engages it, the helicopter climbs or sinks back to the height
 * it was at, at most {@link #MAX_VERTICAL_SPEED} blocks a tick, and its horizontal drift is damped.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HoverHold
{
    /** Fastest climb or descent the hold commands, in blocks per tick. */
    public static final double MAX_VERTICAL_SPEED = 0.22D;
    /** Commanded vertical speed per block off the held height. */
    public static final double ALTITUDE_GAIN = 0.08D;
    /** Share of the gap to the commanded vertical speed closed each tick. */
    public static final double VERTICAL_RESPONSE = 0.18D;
    /** Share of horizontal speed kept each tick. */
    public static final double DRIFT_RETENTION = 0.985D;

    /**
     * @param velocity this tick's velocity from the rotor
     * @param altitudeError held height minus current height, in blocks
     * @return the velocity steered back to the held height, with its drift damped
     */
    public static Vec3 hold(Vec3 velocity, double altitudeError)
    {
        if (!Double.isFinite(altitudeError))
            return velocity;
        double commanded = Mth.clamp(altitudeError * ALTITUDE_GAIN, -MAX_VERTICAL_SPEED, MAX_VERTICAL_SPEED);
        double vertical = velocity.y + (commanded - velocity.y) * VERTICAL_RESPONSE;
        return new Vec3(velocity.x * DRIFT_RETENTION, vertical, velocity.z * DRIFT_RETENTION);
    }
}
