package com.flansmodultimate.common.driveables.physics;

/** Motion-derived animation values for vehicle wheels. */
public final class WheelAnimationPhysics
{
    /** Preserves the previous 18 degree step at a representative 0.3 blocks/tick. */
    static final double DEGREES_PER_BLOCK = 60D;
    private static final double DIRECTION_EPSILON = 1.0E-8D;

    private WheelAnimationPhysics()
    {
    }

    /**
     * Returns a wheel rotation step proportional to horizontal speed. The step
     * reverses only when motion has a meaningful component behind the vehicle.
     */
    public static float angularStepDegrees(double velocityX, double velocityZ,
                                           double forwardX, double forwardZ)
    {
        double speed = Math.hypot(velocityX, velocityZ);
        if (speed < DIRECTION_EPSILON)
            return 0F;
        double longitudinalSpeed = velocityX * forwardX + velocityZ * forwardZ;
        double direction = longitudinalSpeed < -DIRECTION_EPSILON ? -1D : 1D;
        return (float) (speed * DEGREES_PER_BLOCK * direction);
    }
}
