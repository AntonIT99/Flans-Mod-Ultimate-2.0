package com.flansmodultimate.common.driveables.physics;

/**
 * Uphill propulsion limiting from the optional {@code RealMaxSlopeDeg} key.
 *
 * <p>This is one of the independently usable parameters: it works whether or not
 * the vehicle has a complete real-world propulsion profile, and when the key is
 * absent every method here returns the neutral value so behaviour is unchanged.
 *
 * <p>The grade is read from the pitch the suspension already computes in
 * {@code Driveable.applyWheelContactPhysics}, so no new terrain sampling is
 * introduced and suspension geometry is left alone.
 */
public final class GroundSlopePhysics
{
    private GroundSlopePhysics() {}

    /**
     * Propulsion multiplier for a vehicle attempting to drive up a grade.
     *
     * <p>Full propulsion is retained until the grade reaches
     * {@link VehiclePhysicsConstants#SLOPE_FALLOFF_START} of the limit, then falls
     * off linearly to a small residual at the limit and zero beyond it. The ramp
     * is what keeps the vehicle from switching between full power and none within
     * a single tick as the suspension pitch oscillates.
     *
     * @param pitchDegrees    vehicle pitch in degrees; positive is nose-up
     * @param throttleSign    sign of the driver's demand; only climbing is limited
     * @param maxSlopeDegrees authored slope limit, or a non-positive value for "no limit"
     * @return a multiplier in {@code [0, 1]}
     */
    public static float propulsionFactor(float pitchDegrees, float throttleSign, float maxSlopeDegrees)
    {
        if (!Float.isFinite(maxSlopeDegrees) || maxSlopeDegrees <= 0F)
            return 1F;
        if (!Float.isFinite(pitchDegrees) || !Float.isFinite(throttleSign) || throttleSign == 0F)
            return 1F;

        // Climbing means the nose is raised while driving forward, or lowered
        // while reversing up the same slope.
        float climbAngle = throttleSign > 0F ? pitchDegrees : -pitchDegrees;
        if (climbAngle <= 0F)
            return 1F;

        float falloffStart = maxSlopeDegrees * VehiclePhysicsConstants.SLOPE_FALLOFF_START;
        if (climbAngle <= falloffStart)
            return 1F;
        if (climbAngle >= maxSlopeDegrees)
            return 0F;

        float span = maxSlopeDegrees - falloffStart;
        float progress = (climbAngle - falloffStart) / span;
        float factor = 1F - progress * (1F - VehiclePhysicsConstants.SLOPE_LIMIT_RESIDUAL);
        return Math.max(0F, Math.min(1F, factor));
    }
}
