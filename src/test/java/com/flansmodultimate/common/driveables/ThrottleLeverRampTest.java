package com.flansmodultimate.common.driveables;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Progressive throttle lever: the step grows with the length of the hold. */
class ThrottleLeverRampTest
{
    private static final float MAX = ThrottleLeverRamp.VEHICLE_MAX_STEP_MULTIPLIER;
    private static final float INITIAL = ThrottleLeverRamp.INITIAL_STEP_MULTIPLIER;

    @Test
    void aTapMovesTheLeverByHalfTheAuthoredStep()
    {
        ThrottleLeverRamp ramp = new ThrottleLeverRamp();
        assertEquals(INITIAL, ramp.advance(1, MAX), 1.0E-6F);
        assertEquals(INITIAL, ramp.advance(0, MAX), 1.0E-6F, "released, so nothing is applied at all");
        assertEquals(INITIAL, ramp.advance(1, MAX), 1.0E-6F, "a fresh press starts over at the base step");
    }

    @Test
    void holdingTheLeverRampsToTheMaximumAndStaysThere()
    {
        ThrottleLeverRamp ramp = new ThrottleLeverRamp();
        float previous = 0F;
        for (int tick = 0; tick <= ThrottleLeverRamp.RAMP_TICKS; tick++)
        {
            float step = ramp.advance(1, MAX);
            assertTrue(step >= previous, "the step never goes backwards during a hold");
            previous = step;
        }
        assertEquals(MAX, previous, 1.0E-6F);
        // Held beyond the ramp, it saturates rather than growing without bound.
        assertEquals(MAX, ramp.advance(1, MAX), 1.0E-6F);
        assertEquals(MAX, ramp.advance(1, MAX), 1.0E-6F);
    }

    @Test
    void reversingOrReleasingTheLeverRestartsTheRamp()
    {
        ThrottleLeverRamp ramp = new ThrottleLeverRamp();
        for (int tick = 0; tick < ThrottleLeverRamp.RAMP_TICKS; tick++)
            ramp.advance(1, MAX);
        assertEquals(INITIAL, ramp.advance(-1, MAX), 1.0E-6F, "the other key starts its own ramp");

        ThrottleLeverRamp released = new ThrottleLeverRamp();
        for (int tick = 0; tick < ThrottleLeverRamp.RAMP_TICKS; tick++)
            released.advance(1, MAX);
        released.advance(0, MAX);
        assertEquals(INITIAL, released.advance(1, MAX), 1.0E-6F);
    }

    @Test
    void bothKeysAtOnceCancelOutAndHoldTheLeverStill()
    {
        int both = DriveableInput.THROTTLE_INCREASE | DriveableInput.THROTTLE_DECREASE;
        assertEquals(0, ThrottleLeverRamp.direction(both,
            DriveableInput.THROTTLE_INCREASE, DriveableInput.THROTTLE_DECREASE));
        assertEquals(1, ThrottleLeverRamp.direction(DriveableInput.THROTTLE_INCREASE,
            DriveableInput.THROTTLE_INCREASE, DriveableInput.THROTTLE_DECREASE));
        assertEquals(-1, ThrottleLeverRamp.direction(DriveableInput.THROTTLE_DECREASE,
            DriveableInput.THROTTLE_INCREASE, DriveableInput.THROTTLE_DECREASE));
        assertEquals(0, ThrottleLeverRamp.direction(0,
            DriveableInput.THROTTLE_INCREASE, DriveableInput.THROTTLE_DECREASE));
    }

    @Test
    void crossingZeroRestartsTheRampSoDriveToReverseIsNeverSwept()
    {
        ThrottleLeverRamp ramp = new ThrottleLeverRamp();
        for (int tick = 0; tick < ThrottleLeverRamp.RAMP_TICKS; tick++)
            ramp.advance(-1, MAX);
        assertEquals(MAX, ramp.advance(-1, MAX), 1.0E-6F);
        ramp.resetOnZeroCrossing(0.05F, -0.05F);
        assertEquals(INITIAL, ramp.advance(-1, MAX), 1.0E-6F, "the same hold continues, at the base step");
    }

    @Test
    void movingWithinOneDirectionDoesNotRestartTheRamp()
    {
        ThrottleLeverRamp ramp = new ThrottleLeverRamp();
        for (int tick = 0; tick < ThrottleLeverRamp.RAMP_TICKS; tick++)
            ramp.advance(1, MAX);
        // Leaving zero, and moving well clear of it, are not crossings.
        ramp.resetOnZeroCrossing(0F, 0.05F);
        ramp.resetOnZeroCrossing(0.5F, 0.55F);
        ramp.resetOnZeroCrossing(-0.5F, -0.55F);
        assertEquals(MAX, ramp.advance(1, MAX), 1.0E-6F);
    }

    @Test
    void degenerateInputsFallBackToTheBaseStep()
    {
        assertEquals(INITIAL, ThrottleLeverRamp.multiplier(0, MAX), 1.0E-6F);
        assertEquals(INITIAL, ThrottleLeverRamp.multiplier(-5, MAX), 1.0E-6F);
        assertEquals(INITIAL, ThrottleLeverRamp.multiplier(999, 0.2F), 1.0E-6F, "a max below the base is still the base");
        assertEquals(INITIAL, ThrottleLeverRamp.multiplier(999, Float.NaN), 1.0E-6F);
        ThrottleLeverRamp ramp = new ThrottleLeverRamp();
        ramp.advance(1, MAX);
        ramp.resetOnZeroCrossing(Float.NaN, 1F);
        assertTrue(ramp.advance(1, MAX) > INITIAL, "a non-finite throttle is not a zero crossing");
    }

    @Test
    void aircraftRampFurtherThanGroundVehiclesBecauseTheirBaseStepIsFiner()
    {
        assertTrue(ThrottleLeverRamp.PLANE_MAX_STEP_MULTIPLIER > ThrottleLeverRamp.VEHICLE_MAX_STEP_MULTIPLIER);
        // A cold aircraft throttle reaches full in a handful of seconds, not
        // the twenty five that the unramped 0.002 step took.
        float throttle = 0F;
        int ticks = 0;
        ThrottleLeverRamp ramp = new ThrottleLeverRamp();
        while (throttle < 1F && ticks < 20 * 60)
        {
            throttle += 0.002F * ramp.advance(1, ThrottleLeverRamp.PLANE_MAX_STEP_MULTIPLIER);
            ticks++;
        }
        assertTrue(ticks > 20, "still deliberate enough to set a cruise power by hand");
        assertTrue(ticks < 20 * 6, "but a full sweep no longer takes half a minute");
    }

    @Test
    void theLeverMovesSmoothlyEveryTickRatherThanInJumps()
    {
        // Every tick applies its own fractional step, and consecutive steps
        // differ by a fraction of one percent, so there is no stair to see.
        ThrottleLeverRamp ramp = new ThrottleLeverRamp();
        float previousStep = 0F;
        for (int tick = 0; tick <= ThrottleLeverRamp.RAMP_TICKS; tick++)
        {
            float step = ThrottleLeverRamp.VEHICLE_LEVER_BASE_STEP * ramp.advance(1, MAX);
            assertTrue(step > 0F, "the lever moves on every tick of the hold");
            if (tick > 0)
                assertTrue(step - previousStep < 0.005F, "the step itself grows smoothly, not in jumps");
            previousStep = step;
        }
    }

    @Test
    void theLeverStaysPreciseEnoughToSelectAnyCruiseSetting()
    {
        // A single tap is one percent of throttle, so every setting in the
        // range is reachable, and a full sweep still takes a couple of seconds.
        assertEquals(0.01F, ThrottleLeverRamp.VEHICLE_LEVER_BASE_STEP, 1.0E-6F);
        float throttle = 0F;
        int ticks = 0;
        ThrottleLeverRamp ramp = new ThrottleLeverRamp();
        while (throttle < 1F && ticks < 20 * 60)
        {
            throttle += ThrottleLeverRamp.VEHICLE_LEVER_BASE_STEP * ramp.advance(1, MAX);
            ticks++;
        }
        assertTrue(ticks > 20 && ticks < 20 * 3, "a full sweep lands between one and three seconds");
    }
}
