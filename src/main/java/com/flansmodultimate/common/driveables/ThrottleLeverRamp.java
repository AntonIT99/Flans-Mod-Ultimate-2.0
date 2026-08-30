package com.flansmodultimate.common.driveables;

/**
 * Progressive throttle lever: holding the key moves the lever faster the longer
 * it is held.
 *
 * <p>The authored per-tick step stays what it always was. This only scales it,
 * from half at the moment of the press up to a per-vehicle maximum reached
 * after {@link #RAMP_TICKS}, so a tap is a fine nudge and fine adjustments
 * are still made by tapping while a long hold sweeps the whole range in a few
 * seconds instead of tens of them.
 *
 * <p>The ramp restarts whenever the lever is released or reversed, and, for
 * ground vehicles, whenever it passes through zero, so crossing from drive into
 * reverse never happens at sweep speed.
 *
 * <p>One instance belongs to one driveable and {@link #advance} must be called
 * exactly once per tick, because the hold is counted in calls.
 */
public final class ThrottleLeverRamp
{
    /** Ticks of continuous hold after which the step reaches its maximum multiplier. */
    public static final int RAMP_TICKS = 30;
    /**
     * Multiplier at the moment of the press, before any hold has accumulated.
     */
    public static final float INITIAL_STEP_MULTIPLIER = 0.5F;
    /**
     * Aircraft sweep the furthest, because their authored step is a very fine
     * 0.002 per tick: at full ramp a cold throttle reaches military power in
     * roughly four seconds rather than twenty five.
     */
    public static final float PLANE_MAX_STEP_MULTIPLIER = 8F;
    /** Ground vehicles reach four times their base lever step at full ramp. */
    public static final float VEHICLE_MAX_STEP_MULTIPLIER = 4F;
    /**
     * Base per-tick travel of the ground-vehicle lever, as a fraction of full
     * throttle.
     *
     * <p>The lever deliberately does not share the pedals' rate. Under the
     * real-world profile the pedal travels a coarse 5% of its range per tick,
     * which is fine for a momentary pedal but far too granular for a lever that
     * holds its setting: it can only ever select multiples of 5%, and ramping
     * that would sweep the whole range in well under a second. One percent per
     * tick keeps every setting reachable and keeps the ramp smooth.
     */
    public static final float VEHICLE_LEVER_BASE_STEP = 0.01F;

    /** +1 while increasing, -1 while decreasing, 0 while released. */
    private int direction;
    private int heldTicks;

    /**
     * Advances the hold by one tick and returns the multiplier to apply to this
     * tick's authored throttle step.
     *
     * @param requestedDirection +1 for increase, -1 for decrease, 0 for released
     *                           or for both keys held at once
     * @param maxStepMultiplier  the multiplier reached after {@link #RAMP_TICKS}
     * @return the multiplier for this tick, from {@link #INITIAL_STEP_MULTIPLIER} upward
     */
    public float advance(int requestedDirection, float maxStepMultiplier)
    {
        if (requestedDirection == 0 || requestedDirection != direction)
        {
            direction = requestedDirection;
            heldTicks = 0;
        }
        if (direction == 0)
            return INITIAL_STEP_MULTIPLIER;
        // The hold is counted after the step is read, so the tick immediately
        // following a reset is always at the base step.
        float step = multiplier(heldTicks, maxStepMultiplier);
        if (heldTicks < RAMP_TICKS)
            heldTicks++;
        return step;
    }

    /**
     * Restarts the ramp when the lever has just passed through zero, so the
     * transition between forward and reverse is always made at the base step.
     *
     * @param before the throttle before this tick's step
     * @param after  the throttle after it
     */
    public void resetOnZeroCrossing(float before, float after)
    {
        if (!Float.isFinite(before) || !Float.isFinite(after))
            return;
        if ((before > 0F && after <= 0F) || (before < 0F && after >= 0F))
            heldTicks = 0;
    }

    /** Drops the ramp back to the base step without forgetting the held direction. */
    public void reset()
    {
        heldTicks = 0;
    }

    /** The multiplier for a given hold length, ramped linearly and clamped. */
    public static float multiplier(int heldTicks, float maxStepMultiplier)
    {
        float max = Float.isFinite(maxStepMultiplier) ? Math.max(INITIAL_STEP_MULTIPLIER, maxStepMultiplier)
            : INITIAL_STEP_MULTIPLIER;
        float progress = Math.min(1F, Math.max(0, heldTicks) / (float) RAMP_TICKS);
        return INITIAL_STEP_MULTIPLIER + (max - INITIAL_STEP_MULTIPLIER) * progress;
    }

    /** Direction from a raw input mask, with both keys down cancelling out. */
    public static int direction(int input, int increaseFlag, int decreaseFlag)
    {
        boolean up = DriveableInput.isDown(input, increaseFlag);
        boolean down = DriveableInput.isDown(input, decreaseFlag);
        if (up == down)
            return 0;
        return up ? 1 : -1;
    }
}
