package com.flansmodultimate.common.driveables.physics;

/**
 * Forgiving uphill propulsion response derived from a ground vehicle's real-world
 * power-to-weight ratio and drive layout.
 *
 * <p>This only runs for a complete real-world ground profile. Legacy vehicles are
 * unchanged, while profiled vehicles require no separately authored climbing stat.
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
     * <p>Power-to-weight and drive layout set the angle at which the vehicle starts
     * to bog down. Propulsion then falls linearly but retains a crawl floor, which
     * lets suspension cross Minecraft block edges without a hard cutoff.
     *
     * @param pitchDegrees    vehicle pitch in degrees; positive is nose-up
     * @param throttleSign    sign of the driver's demand; only climbing is limited
     * @param powerToWeightKwPerKg resolved power-to-weight ratio in kW/kg
     * @param driveType resolved driven-wheel or track layout
     * @return a multiplier in {@code [0, 1]}
     */
    public static float propulsionFactor(float pitchDegrees, float throttleSign,
                                         float powerToWeightKwPerKg, EnumDriveType driveType)
    {
        if (!Float.isFinite(powerToWeightKwPerKg) || powerToWeightKwPerKg <= 0F || driveType == null)
            return 1F;
        if (!Float.isFinite(pitchDegrees) || !Float.isFinite(throttleSign) || throttleSign == 0F)
            return 1F;

        // Climbing means the nose is raised while driving forward, or lowered
        // while reversing up the same slope.
        float climbAngle = throttleSign > 0F ? pitchDegrees : -pitchDegrees;
        if (climbAngle <= 0F)
            return 1F;

        float powerToWeightKwPerTonne = powerToWeightKwPerKg * 1000F;
        float capability = powerToWeightKwPerTonne / VehiclePhysicsConstants.SLOPE_REFERENCE_POWER_TO_WEIGHT_KW_PER_T;
        capability *= driveType.slopeTractionFactor();
        capability = Math.max(VehiclePhysicsConstants.SLOPE_MIN_CAPABILITY,
            Math.min(VehiclePhysicsConstants.SLOPE_MAX_CAPABILITY, capability));

        float falloffStart = VehiclePhysicsConstants.SLOPE_BASE_FALLOFF_DEG
            + capability * VehiclePhysicsConstants.SLOPE_CAPABILITY_FALLOFF_DEG;
        if (climbAngle <= falloffStart)
            return 1F;

        float progress = (climbAngle - falloffStart) / VehiclePhysicsConstants.SLOPE_FALLOFF_SPAN_DEG;
        float factor = 1F - progress * (1F - VehiclePhysicsConstants.SLOPE_CRAWL_PROPULSION);
        return Math.max(VehiclePhysicsConstants.SLOPE_CRAWL_PROPULSION, Math.min(1F, factor));
    }
}
