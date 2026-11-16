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
        return propulsionFactor(pitchDegrees, throttleSign, powerToWeightKwPerKg, driveType, 1F);
    }

    /**
     * As above, with the grip the surface under the wheels leaves: a vehicle that bogs down at a grade on
     * firm ground bogs down sooner on loose or slick ground.
     *
     * @param surfaceGrip grip multiplier from {@link SurfaceTraction}, 1 on firm ground
     */
    public static float propulsionFactor(float pitchDegrees, float throttleSign,
                                         float powerToWeightKwPerKg, EnumDriveType driveType, float surfaceGrip)
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

        float falloffStart = falloffStart(powerToWeightKwPerKg, driveType, surfaceGrip);
        if (climbAngle <= falloffStart)
            return 1F;

        float progress = (climbAngle - falloffStart) / VehiclePhysicsConstants.SLOPE_FALLOFF_SPAN_DEG;
        float factor = 1F - progress * (1F - VehiclePhysicsConstants.SLOPE_CRAWL_PROPULSION);
        return Math.max(VehiclePhysicsConstants.SLOPE_CRAWL_PROPULSION, Math.min(1F, factor));
    }

    /**
     * The grade at which a vehicle is down to its crawl on firm ground: the steepest terrain its suspension has any
     * reason to follow, since it drives nothing steeper.
     *
     * @return degrees, or 0 without a usable power-to-weight ratio and drive layout
     */
    public static float crawlAngle(float powerToWeightKwPerKg, EnumDriveType driveType)
    {
        if (!Float.isFinite(powerToWeightKwPerKg) || powerToWeightKwPerKg <= 0F || driveType == null)
            return 0F;
        return falloffStart(powerToWeightKwPerKg, driveType, 1F) + VehiclePhysicsConstants.SLOPE_FALLOFF_SPAN_DEG;
    }

    /**
     * Speed multiplier for a vehicle driving down a grade. Nothing in the ground model pulls a vehicle downhill, but
     * nothing held it back either: it took a steep descent at the speed of a level road and left the ground at every
     * edge. A driver holds it back on the engine instead, the more the steeper it goes down.
     *
     * @param pitchDegrees vehicle pitch in degrees; positive is nose-up
     * @param travelSign   sign of the driver's demand; only descending is limited
     * @return a multiplier in {@code [DESCENT_MIN_SPEED, 1]}
     */
    public static float descentFactor(float pitchDegrees, float travelSign)
    {
        if (!Float.isFinite(pitchDegrees) || !Float.isFinite(travelSign) || travelSign == 0F)
            return 1F;
        // Descending means the nose is lowered while driving forward, or raised while reversing down the same slope.
        float descentAngle = travelSign > 0F ? -pitchDegrees : pitchDegrees;
        if (descentAngle <= VehiclePhysicsConstants.DESCENT_FALLOFF_START_DEG)
            return 1F;
        float progress = (descentAngle - VehiclePhysicsConstants.DESCENT_FALLOFF_START_DEG) / VehiclePhysicsConstants.DESCENT_FALLOFF_SPAN_DEG;
        return Math.max(VehiclePhysicsConstants.DESCENT_MIN_SPEED, 1F - progress * (1F - VehiclePhysicsConstants.DESCENT_MIN_SPEED));
    }

    /** Grade up to which the vehicle keeps its full propulsion, in degrees. */
    private static float falloffStart(float powerToWeightKwPerKg, EnumDriveType driveType, float surfaceGrip)
    {
        float powerToWeightKwPerTonne = powerToWeightKwPerKg * 1000F;
        float capability = powerToWeightKwPerTonne / VehiclePhysicsConstants.SLOPE_REFERENCE_POWER_TO_WEIGHT_KW_PER_T;
        capability *= driveType.slopeTractionFactor();
        if (Float.isFinite(surfaceGrip))
            capability *= Math.max(0F, Math.min(1F, surfaceGrip));
        capability = Math.max(VehiclePhysicsConstants.SLOPE_MIN_CAPABILITY,
            Math.min(VehiclePhysicsConstants.SLOPE_MAX_CAPABILITY, capability));
        return VehiclePhysicsConstants.SLOPE_BASE_FALLOFF_DEG
            + capability * VehiclePhysicsConstants.SLOPE_CAPABILITY_FALLOFF_DEG;
    }
}
