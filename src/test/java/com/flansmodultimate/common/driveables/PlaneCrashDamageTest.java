package com.flansmodultimate.common.driveables;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlaneCrashDamageTest
{
    @Test
    void normalLevelLandingDoesNoDamage()
    {
        assertFalse(PlaneCrashDamage.evaluate(0.4D, 0.3D, 1D, 1F).damaging());
        assertFalse(PlaneCrashDamage.evaluate(0.7D, 0.5D, 1D, 1F).damaging());
    }

    @Test
    void fastLevelTouchdownStaysSurvivable()
    {
        PlaneCrashDamage.Impact impact = PlaneCrashDamage.evaluate(3D, 0.5D, 1D, 1F);
        assertFalse(impact.damaging());
    }

    @Test
    void badAngleMakesTheSameDescentMoreDestructive()
    {
        PlaneCrashDamage.Impact level = PlaneCrashDamage.evaluate(1D, 0.8D, 1D, 1F);
        PlaneCrashDamage.Impact tilted = PlaneCrashDamage.evaluate(1D, 0.8D, 0.45D, 1F);

        assertTrue(tilted.damaging());
        assertTrue(tilted.healthFraction() > level.healthFraction());
    }

    @Test
    void speedAmplifiesAnAlreadyBadTouchdown()
    {
        PlaneCrashDamage.Impact slow = PlaneCrashDamage.evaluate(1D, 0.9D, 0.9D, 1F);
        PlaneCrashDamage.Impact fast = PlaneCrashDamage.evaluate(4D, 0.9D, 0.9D, 1F);

        assertTrue(fast.healthFraction() > slow.healthFraction());
    }

    @Test
    void highSpeedDiveIntoTheGroundIsCatastrophic()
    {
        // A 45 degree dive at four blocks per tick, roughly 290 km/h.
        PlaneCrashDamage.Impact impact = PlaneCrashDamage.evaluate(4D, 2.83D, 0.707D, 1F);

        assertTrue(impact.catastrophic());
    }

    @Test
    void hardButUprightLandingIsNotCatastrophic()
    {
        PlaneCrashDamage.Impact impact = PlaneCrashDamage.evaluate(2D, 0.9D, 1D, 1F);

        assertTrue(impact.damaging());
        assertFalse(impact.catastrophic());
    }

    @Test
    void fallDamageFactorScalesCrashDamage()
    {
        float normal = PlaneCrashDamage.evaluate(2D, 0.9D, 0.4D, 1F).healthFraction();
        float softened = PlaneCrashDamage.evaluate(2D, 0.9D, 0.4D, 0.5F).healthFraction();
        assertEquals(normal * 0.5F, softened, 1.0E-4F);
    }

    @Test
    void aForgivingFallDamageFactorKeepsEvenADiveSurvivable()
    {
        PlaneCrashDamage.Impact impact = PlaneCrashDamage.evaluate(4D, 2.83D, 0.707D, 0.4F);

        assertTrue(impact.damaging());
        assertFalse(impact.catastrophic());
    }
}
