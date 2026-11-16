package com.flansmodultimate.common.driveables.physics;

import com.flansmod.common.vector.Vector3f;

import java.util.List;

/** Shared travel for track links and faster texture-swap tread cycles. */
public final class TrackAnimationPhysics
{
    private static final double LOOPS_PER_BLOCK = 0.25D;
    /** Model pixels per block at model scale 1. */
    private static final double PIXELS_PER_BLOCK = 16D;

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

    /**
     * Multiplier on the shared loop steps that makes fancy track links cover ground at hull speed:
     * {@code 16 / modelScale} model pixels per block, whatever the authored loop length. Without it a
     * long loop scrolled several times faster than the ground and aliased into slow or backward links.
     * Returns 1 for types without a link path, so texture-swap treads keep their cadence.
     */
    public static float linkLoopScale(List<Vector3f> points, float trackLinkLength, float modelScale)
    {
        if (points == null || points.size() < 2 || !(trackLinkLength > 0F) || !(modelScale > 0F))
            return 1F;
        // XY only, exactly as the link path measures its loop.
        double length = 0D;
        int count = points.size();
        for (int i = 0; i < count; i++)
        {
            Vector3f point = points.get(i);
            Vector3f previous = points.get(i == 0 ? count - 1 : i - 1);
            if (point != null && previous != null)
                length += Math.hypot(point.x - previous.x, point.y - previous.y);
        }
        return length > 1E-4D ? (float) (PIXELS_PER_BLOCK / (modelScale * length * LOOPS_PER_BLOCK)) : 1F;
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
