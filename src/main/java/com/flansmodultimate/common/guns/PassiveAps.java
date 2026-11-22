package com.flansmodultimate.common.guns;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * The Labjac fork's soft-kill active protection, a Shtora-style dazzler: a vehicle carrying it throws an incoming wire-
 * or beam-guided missile off course, and the missile corkscrews away from where its operator is aiming.
 *
 * <p>
 * Only missiles steered by their operator can be jammed: SACLOS and manually guided ones. A missile homing on a lock of
 * its own is not, and neither is one whose type bypasses the system.
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PassiveAps
{
    /** How far from a protected vehicle a missile is looked for, in blocks. */
    public static final double SCAN_RANGE = 80D;
    /** Closer than this the missile is too near to be thrown off, in blocks. */
    public static final double MIN_RANGE = 10D;
    /** How directly the missile must be flying at the vehicle: the cosine of the largest angle off. */
    public static final double MIN_APPROACH_COSINE = 0.35D;
    /** Blocks added to the vehicle's own extent for how close the missile must pass to count as a threat. */
    public static final double INTERCEPT_MARGIN = 4D;
    /** Smallest extent assumed for a vehicle, in blocks, for definitions with few hitboxes. */
    public static final double MIN_VEHICLE_RADIUS = 4D;

    /**
     * @param toVehicle from the missile to the middle of the vehicle
     * @param velocity the missile's velocity
     * @param vehicleRadius how far the vehicle's hitboxes reach from its middle, in blocks
     * @return whether the missile is past the minimum range and on a course that passes close enough to hit the vehicle
     */
    public static boolean threatens(Vec3 toVehicle, Vec3 velocity, double vehicleRadius)
    {
        double distanceSq = toVehicle.lengthSqr();
        double speedSq = velocity.lengthSqr();
        if (distanceSq <= MIN_RANGE * MIN_RANGE || speedSq < 1.0E-4D)
            return false;
        double along = toVehicle.dot(velocity);
        if (along <= 0D || along / Math.sqrt(distanceSq * speedSq) < MIN_APPROACH_COSINE)
            return false;
        double missSq = Math.max(0D, distanceSq - along * along / speedSq);
        double intercept = Math.max(MIN_VEHICLE_RADIUS, vehicleRadius) + INTERCEPT_MARGIN;
        return missSq <= intercept * intercept;
    }

    /**
     * @param vehicleForward the vehicle's hull forward direction
     * @param toMissile from the vehicle to the missile
     * @param allAspect whether the system covers every side
     * @return whether the system can see a missile coming from this side: the front half, or anywhere when all-aspect
     */
    public static boolean covers(Vec3 vehicleForward, Vec3 toMissile, boolean allAspect)
    {
        return allAspect || vehicleForward.dot(toMissile) >= 0D;
    }

    /** @return a seed that is the same on every side for this missile and vehicle */
    public static int seed(int missileId, int vehicleId)
    {
        int seed = missileId * 73471 ^ vehicleId * 193;
        return seed == 0 ? 1 : seed;
    }

    /**
     * @param velocity the jammed missile's velocity
     * @param seed the jamming seed, which picks the corkscrew's phase and which way it drifts
     * @param ticks ticks since the missile was jammed
     * @return the velocity turned along the corkscrew, at the same speed: it widens over the first seconds and drifts to one side
     */
    public static Vec3 spiral(Vec3 velocity, int seed, int ticks)
    {
        double speed = velocity.length();
        if (speed < 1.0E-4D)
            return velocity;
        Vec3 forward = velocity.scale(1D / speed);
        Vec3 reference = Math.abs(forward.y) < 0.9D ? new Vec3(0D, 1D, 0D) : new Vec3(1D, 0D, 0D);
        Vec3 right = forward.cross(reference).normalize();
        Vec3 up = right.cross(forward).normalize();
        float phase = ticks * 0.82F + (seed & 255) * 0.03125F;
        float strength = Math.min(0.28F, 0.055F + ticks * 0.009F);
        float drift = ((seed >>> 8) & 1) == 0 ? 0.045F : -0.045F;
        Vec3 turned = forward.add(right.scale(Mth.sin(phase) * strength + drift)).add(up.scale(Mth.cos(phase) * strength)).normalize();
        return turned.scale(speed);
    }
}
