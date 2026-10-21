package com.flansmodultimate.common.entity;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Finds the seat aim that points a seat gun along a world direction.
 *
 * <p>
 * A seat aims in angles relative to its driveable, and how those turn into a world direction depends on the
 * driveable's orientation, the turret frame and whether it flies. Rather than invert each of those, this treats the
 * driveable's own aim-to-direction mapping as a black box and solves it with a few Newton steps on the world heading
 * and elevation of the result, estimating their derivatives by finite differences. Pure arithmetic, any thread.
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class SeatAimSolver
{
    /** How the driveable turns a seat aim into a world direction. */
    @FunctionalInterface
    interface AimDirection
    {
        Vec3 direction(float yaw, float pitch);
    }

    /** A seat aim, in degrees. */
    record Aim(float yaw, float pitch)
    {}

    private static final int MAX_STEPS = 6;
    /** Degrees the aim is nudged by to estimate how the direction changes with it. */
    private static final float PROBE = 0.5F;
    /** Heading and elevation errors, in degrees, below which the aim is good enough. */
    private static final float TOLERANCE = 0.05F;
    /** Largest change one step may make, so a poor estimate cannot throw the aim around. */
    private static final float MAX_STEP = 90F;
    private static final double SINGULAR = 1.0E-6D;

    /**
     * @param mapping
     *            the driveable's aim-to-direction mapping
     * @param target
     *            the world direction to point along; need not be normalised
     * @param startYaw
     *            the aim to start from, usually the current one
     * @param startPitch
     *            the aim to start from, usually the current one
     * @return the aim whose direction comes closest to the target, unbounded by the seat's limits
     */
    static Aim solve(AimDirection mapping, Vec3 target, float startYaw, float startPitch)
    {
        if (target.lengthSqr() < 1.0E-12D)
            return new Aim(startYaw, startPitch);
        float targetHeading = heading(target);
        float targetElevation = elevation(target);
        float yaw = startYaw;
        float pitch = startPitch;
        for (int step = 0; step < MAX_STEPS; step++)
        {
            Vec3 current = mapping.direction(yaw, pitch);
            float headingError = Mth.wrapDegrees(targetHeading - heading(current));
            float elevationError = targetElevation - elevation(current);
            if (Math.abs(headingError) < TOLERANCE && Math.abs(elevationError) < TOLERANCE)
                break;

            Vec3 yawProbe = mapping.direction(yaw + PROBE, pitch);
            Vec3 pitchProbe = mapping.direction(yaw, pitch + PROBE);
            double headingByYaw = Mth.wrapDegrees(heading(yawProbe) - heading(current)) / PROBE;
            double headingByPitch = Mth.wrapDegrees(heading(pitchProbe) - heading(current)) / PROBE;
            double elevationByYaw = (elevation(yawProbe) - elevation(current)) / PROBE;
            double elevationByPitch = (elevation(pitchProbe) - elevation(current)) / PROBE;
            double determinant = headingByYaw * elevationByPitch - headingByPitch * elevationByYaw;

            double yawStep;
            double pitchStep;
            if (Math.abs(determinant) > SINGULAR)
            {
                yawStep = (headingError * elevationByPitch - headingByPitch * elevationError) / determinant;
                pitchStep = (headingByYaw * elevationError - headingError * elevationByYaw) / determinant;
            }
            else
            {
                // Looking straight up or down the heading means nothing; correct each angle on its own.
                yawStep = Math.abs(headingByYaw) > SINGULAR ? headingError / headingByYaw : 0D;
                pitchStep = Math.abs(elevationByPitch) > SINGULAR ? elevationError / elevationByPitch : 0D;
            }
            yaw = Mth.wrapDegrees(yaw + (float) Mth.clamp(yawStep, -MAX_STEP, MAX_STEP));
            pitch = Mth.clamp(pitch + (float) Mth.clamp(pitchStep, -MAX_STEP, MAX_STEP), -90F, 90F);
        }
        return new Aim(yaw, pitch);
    }

    /** World heading of a direction, in degrees, as Minecraft measures yaw. */
    static float heading(Vec3 direction)
    {
        return (float) (Mth.atan2(-direction.x, direction.z) * Mth.RAD_TO_DEG);
    }

    /** World elevation of a direction, in degrees, positive upwards. */
    static float elevation(Vec3 direction)
    {
        double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        return (float) (Mth.atan2(direction.y, horizontal) * Mth.RAD_TO_DEG);
    }
}
