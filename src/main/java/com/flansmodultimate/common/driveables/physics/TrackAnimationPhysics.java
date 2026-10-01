package com.flansmodultimate.common.driveables.physics;

/** Shared travel for track links and faster texture-swap tread cycles. */
public final class TrackAnimationPhysics
{
    private TrackAnimationPhysics() {}

    /** Retains 0.075 loop/tick at 0.3 blocks/tick, but follows actual signed motion. */
    public static float travelStep(double velocityX, double velocityZ, double forwardX, double forwardZ)
    {
        double length = Math.hypot(forwardX, forwardZ);
        if (length < 1E-8) return 0F;
        return (float)((velocityX * forwardX + velocityZ * forwardZ) / length * 0.25D);
    }

    /** Four tread-pattern cycles per shared loop; integer scaling keeps the wrap continuous. */
    public static float framePhase(float travel)
    {
        float phase = travel * 4F;
        return phase - (float)Math.floor(phase);
    }
}
