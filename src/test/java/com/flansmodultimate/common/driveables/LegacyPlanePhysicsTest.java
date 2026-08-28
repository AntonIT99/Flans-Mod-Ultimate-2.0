package com.flansmodultimate.common.driveables;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LegacyPlanePhysicsTest
{
    private static final float EPSILON = 1.0E-6F;

    @Test
    void flapInputAccumulatesAndReturnsLikeLegacy()
    {
        float flap = 0F;
        for (int tick = 0; tick < 100; tick++)
            flap = LegacyPlanePhysics.flap(flap, 1F);
        assertEquals(9F, flap, 0.001F);
        assertEquals(8.1F, LegacyPlanePhysics.flap(flap, 0F), 0.001F);
    }

    @Test
    void fixedWingControlAuthorityDependsOnAirspeed()
    {
        var stopped = LegacyPlanePhysics.controlRates(EnumPlaneMode.PLANE, 0F, 1F, 1F,
            0F, 10F, 10F, 2F, 2F, 3F, 3F, 4F, 4F);
        var flying = LegacyPlanePhysics.controlRates(EnumPlaneMode.PLANE, 1F, 1F, 1F,
            0F, 10F, 10F, 2F, 2F, 3F, 3F, 4F, 4F);
        assertEquals(0F, stopped.pitch(), EPSILON);
        assertEquals(3.75F, flying.pitch(), EPSILON);
        assertEquals(5F, flying.roll(), EPSILON);
    }

    @Test
    void legacyPowerIsConvertedToPerTickThrustOnce()
    {
        assertEquals(0.09F, LegacyPlanePhysics.thrust(1F, 8F, 0F, 0F, 1F, false), EPSILON);
        assertEquals(0.015F, LegacyPlanePhysics.thrust(-1F, 8F, 0.5F, 0F, 1F, false), EPSILON);
        assertEquals(0.95F, LegacyPlanePhysics.drag(1F), EPSILON);
    }

    @Test
    void angularMomentumApproachesControlInsteadOfSnapping()
    {
        assertEquals(1F, LegacyPlanePhysics.approachMomentum(0F, 8F), EPSILON);
        assertEquals(7F, LegacyPlanePhysics.approachMomentum(8F, 0F), EPSILON);
    }

    @Test
    void wheelSupportReleasesOnlyForARealFixedWingTakeoff()
    {
        assertTrue(LegacyPlanePhysics.isLiftingOff(EnumPlaneMode.PLANE, 0.6D, 0.5F, 0.15D, 0.04D));
        assertFalse(LegacyPlanePhysics.isLiftingOff(EnumPlaneMode.PLANE, 0.4D, 0.5F, 0.15D, 0.04D));
        assertFalse(LegacyPlanePhysics.isLiftingOff(EnumPlaneMode.PLANE, 0.6D, 0.5F, -0.15D, 0.04D));
        assertFalse(LegacyPlanePhysics.isLiftingOff(EnumPlaneMode.HELI, 0.6D, 0.5F, 0.15D, 0.04D));
    }
}
