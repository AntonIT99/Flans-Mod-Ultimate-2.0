package com.flansmodultimate.common.driveables;

/**
 * Allocation-free suspension response functions shared by driveable physics.
 * The response is deliberately over-damped: terrain displacement is converted
 * into a bounded target speed instead of being added to the current velocity.
 */
public final class SuspensionPhysics
{
    private SuspensionPhysics() {}

    public static double dampVerticalVelocity(double velocity, double supportError, float springStrength,
                                              double horizontalSpeed)
    {
        if (!Double.isFinite(velocity) || !Double.isFinite(supportError) || !Double.isFinite(horizontalSpeed))
            return 0D;

        double spring = clamp(springStrength, 0D, 1D);
        double response = 0.16D + spring * 0.30D;
        double maximumRise = 0.16D + spring * 0.07D
            + Math.min(0.12D, Math.max(0D, horizontalSpeed) * 0.25D);
        double targetVelocity = clamp(supportError * response, -0.18D, maximumRise);
        double damping = 0.62D + spring * 0.18D;
        return velocity + (targetVelocity - velocity) * damping;
    }

    public static float terrainAngle(double heightDifference, double distance)
    {
        if (!Double.isFinite(heightDifference) || !Double.isFinite(distance))
            return 0F;
        return (float) clamp(Math.toDegrees(Math.atan2(heightDifference, Math.max(0.5D, distance))), -35D, 35D);
    }

    public static float smoothTerrainAngle(float current, float target, float springStrength)
    {
        if (!Float.isFinite(current) || !Float.isFinite(target))
            return 0F;
        float spring = (float) clamp(springStrength, 0D, 1D);
        float response = 0.08F + spring * 0.18F;
        float desired = current + (target - current) * response;
        float maximumChange = 2F + spring * 2F;
        return (float) clamp(desired, current - maximumChange, current + maximumChange);
    }

    private static double clamp(double value, double minimum, double maximum)
    {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
