package com.flansmodultimate.common.driveables.physics;

/** Shared travel for track links and faster texture-swap tread cycles. */
public final class TrackAnimationPhysics
{
    private static final double LOOPS_PER_BLOCK = 0.25D;

    private TrackAnimationPhysics()
    {}

    /** Retains 0.075 loop/tick at 0.3 blocks/tick, using completed horizontal displacement. */
    public static float travelStep(double displacementX, double displacementZ, double forwardX, double forwardZ)
    {
        double length = Math.hypot(forwardX, forwardZ);
        if (length < 1E-8)
            return 0F;
        return (float) ((displacementX * forwardX + displacementZ * forwardZ) / length * LOOPS_PER_BLOCK);
    }

    /** Actual hull rotation drives opposite belt travel; a single surviving belt travels twice as far. */
    public static Steps steps(float travel, float yawDeltaDegrees, double trackWidth, boolean leftIntact, boolean rightIntact)
    {
        double width = Double.isFinite(trackWidth) && trackWidth > 0D ? trackWidth : 1D;
        float turn = (float) (Math.toRadians(yawDeltaDegrees) * width * 0.5D * LOOPS_PER_BLOCK);
        if (leftIntact != rightIntact)
            turn *= 2F;
        return new Steps(leftIntact ? travel - turn : 0F, rightIntact ? travel + turn : 0F);
    }

    public record Steps(float left, float right)
    {}

    /** Four tread-pattern cycles per shared loop; integer scaling keeps the wrap continuous. */
    public static float framePhase(float travel)
    {
        float phase = travel * 4F;
        return phase - (float) Math.floor(phase);
    }
}
