package com.flansmodultimate.common.driveables;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * Keeps a driveable's extremities out of terrain.
 *
 * <p>A driveable collides with the world through one compact body box, which on a long or wide model
 * covers only its middle: the nose of a tank, the outer wheels of a truck or a crewman sitting at the
 * front could otherwise be driven into a wall until that middle box touched it. In 1.7.10 every wheel was
 * its own 1x1 entity moving through the world, and occupied seats ray-traced back to the wheels, so a
 * vehicle's corners and crew stopped at walls. These probes restore that: small boxes at the wheels and the
 * occupied seats, lifted above the height the driveable can step, limit each horizontal move to what all of
 * them can make.</p>
 *
 * <p>The geometry here is pure so that it can be tested without a world; the caller supplies the terrain
 * queries.</p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DriveableTerrainProbes
{
    /** Width of a wheel probe. 1.7.10 wheels were 1x1 entities; a slimmer box lets tracks hug walls. */
    static final double WHEEL_PROBE_WIDTH = 0.6D;
    /** Height of a wheel probe above the climbable step. */
    static final double WHEEL_PROBE_HEIGHT = 1.0D;
    /** Width of a seat probe, about a seated body. */
    static final double SEAT_PROBE_WIDTH = 0.5D;
    /** Height of a seat probe, from just above the seat to the occupant's chest. */
    static final double SEAT_PROBE_HEIGHT = 1.0D;
    /** Gap between the seat and the bottom of its probe. */
    static final double SEAT_PROBE_OFFSET = 0.2D;
    /** Smallest step a probe ignores, so slabs and uneven ground never catch a driveable with no step height. */
    public static final double MIN_STEP_LIFT = 0.6D;
    /** Margin above the step, so a block exactly as tall as the step is climbed rather than hit. */
    public static final double STEP_CLEARANCE = 0.05D;
    /** Movement changes smaller than this are rounding, not contact. */
    private static final double CONTACT_EPSILON = 1.0E-4D;

    /** Height above a wheel's anchor below which terrain counts as a step the driveable climbs. */
    public static double stepLift(double stepHeight)
    {
        return Math.max(Double.isFinite(stepHeight) ? stepHeight : 0D, MIN_STEP_LIFT) + STEP_CLEARANCE;
    }

    /** The probe of a wheel anchored at {@code wheel}: terrain above the step height around it. */
    public static AABB wheelProbe(Vec3 wheel, double stepLift)
    {
        double half = WHEEL_PROBE_WIDTH / 2D;
        double bottom = wheel.y + stepLift;
        return new AABB(wheel.x - half, bottom, wheel.z - half, wheel.x + half, bottom + WHEEL_PROBE_HEIGHT, wheel.z + half);
    }

    /**
     * The probe of an occupied seat at {@code seat}. It never reaches below {@code minimumBottom}, the step
     * height above the driveable's feet, so a low seat does not stop the driveable at a step it can climb.
     */
    public static AABB seatProbe(Vec3 seat, double minimumBottom)
    {
        double half = SEAT_PROBE_WIDTH / 2D;
        double bottom = Math.max(seat.y + SEAT_PROBE_OFFSET, minimumBottom);
        return new AABB(seat.x - half, bottom, seat.z - half, seat.x + half, bottom + SEAT_PROBE_HEIGHT, seat.z + half);
    }

    /**
     * Limits the horizontal part of {@code velocity} to what every probe can travel.
     *
     * <p>Each probe is moved through the terrain on its own and the smallest travel along each horizontal axis
     * wins, so a driveable slides along a wall rather than sticking to it. A probe already inside terrain is
     * left out: blocking it would leave a driveable that was pushed or spawned into a wall unable to drive
     * out of it.</p>
     *
     * @param inTerrain whether a box intersects terrain
     * @param collide   how far a box can travel by a horizontal motion before terrain stops it
     */
    public static Vec3 clampHorizontal(Vec3 velocity, List<AABB> probes, Predicate<AABB> inTerrain,
                                       BiFunction<AABB, Vec3, Vec3> collide)
    {
        Vec3 horizontal = new Vec3(velocity.x, 0D, velocity.z);
        if (probes.isEmpty() || horizontal.lengthSqr() < CONTACT_EPSILON * CONTACT_EPSILON)
            return velocity;

        double x = velocity.x;
        double z = velocity.z;
        for (AABB probe : probes)
        {
            if (inTerrain.test(probe))
                continue;
            Vec3 allowed = collide.apply(probe, horizontal);
            x = towardsZero(x, allowed.x);
            z = towardsZero(z, allowed.z);
        }
        return x == velocity.x && z == velocity.z ? velocity : new Vec3(x, velocity.y, z);
    }

    /** Whether the clamp stopped the driveable short of where it was going. */
    public static boolean blocked(Vec3 requested, Vec3 allowed)
    {
        return Math.abs(requested.x - allowed.x) > CONTACT_EPSILON || Math.abs(requested.z - allowed.z) > CONTACT_EPSILON;
    }

    /** How many probes intersect terrain. */
    public static int countInTerrain(List<AABB> probes, Predicate<AABB> inTerrain)
    {
        int count = 0;
        for (AABB probe : probes)
            if (inTerrain.test(probe))
                count++;
        return count;
    }

    /** The smaller of two travels along one axis, never reversing the requested direction. */
    private static double towardsZero(double requested, double allowed)
    {
        if (requested > 0D)
            return Math.max(0D, Math.min(requested, allowed));
        if (requested < 0D)
            return Math.min(0D, Math.max(requested, allowed));
        return 0D;
    }
}
