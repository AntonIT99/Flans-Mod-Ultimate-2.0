package com.flansmodultimate.common.driveables.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LandingFlapDragTest
{
    private static double deceleration(double speedMs, double wingArea, double effectiveness, boolean gear)
    {
        return AircraftPerformancePhysics.landingFlapDecelerationMs2(speedMs, wingArea, 5000D, 1D, effectiveness, 1.5D, gear, 1.5D);
    }

    @Test
    void vanishesAtAStandstill()
    {
        assertEquals(0D, deceleration(0D, 30D, 1D, true));
    }

    @Test
    void growsWithSpeed()
    {
        assertTrue(deceleration(120D, 30D, 1D, false) > deceleration(60D, 30D, 1D, false));
    }

    @Test
    void lowersGearAddsDrag()
    {
        assertEquals(deceleration(80D, 30D, 1D, false) * 1.5D, deceleration(80D, 30D, 1D, true), 1E-9D);
    }

    @Test
    void assistsAircraftWithoutWingData()
    {
        assertEquals(1.5D, deceleration(100D, 0D, 1D, false), 1E-9D);
    }

    @Test
    void zeroEffectivenessDisablesTheDrag()
    {
        assertEquals(0D, deceleration(100D, 30D, 0D, true));
    }
}
