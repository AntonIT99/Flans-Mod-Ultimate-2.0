package com.flansmodultimate.common.driveables;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlaneCrashDamageTest
{
    @Test
    void normalLevelLandingDoesNoDamage()
    {
        assertFalse(PlaneCrashDamage.evaluate(0.3D, 1D, 1F).damaging());
        assertFalse(PlaneCrashDamage.evaluate(0.5D, 1D, 1F).damaging());
    }

    @Test
    void badAngleMakesTheSameDescentMoreDestructive()
    {
        PlaneCrashDamage.Impact level = PlaneCrashDamage.evaluate(0.8D, 1D, 1F);
        PlaneCrashDamage.Impact tilted = PlaneCrashDamage.evaluate(0.8D, 0.45D, 1F);

        assertTrue(tilted.damaging());
        assertTrue(tilted.damage() > level.damage());
    }

    @Test
    void fallDamageFactorScalesCrashDamage()
    {
        float normal = PlaneCrashDamage.evaluate(0.9D, 0.4D, 1F).damage();
        float softened = PlaneCrashDamage.evaluate(0.9D, 0.4D, 0.5F).damage();
        assertEquals(normal * 0.5F, softened, 1.0E-4F);
    }
}
