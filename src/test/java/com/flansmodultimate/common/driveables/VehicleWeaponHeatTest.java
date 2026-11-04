package com.flansmodultimate.common.driveables;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VehicleWeaponHeatTest
{
    @Test
    void disabledHeatAndFailedShotsNeverAccumulate()
    {
        VehicleWeaponHeat heat = new VehicleWeaponHeat();
        assertFalse(heat.fired(3, 0, 75));
        assertFalse(heat.fired(0, 10, 75));
        assertEquals(0, heat.getHeat());
        assertFalse(heat.locked(0));
    }

    @Test
    void thresholdIsStrictAndLockoutDoesNotRestartOnRejectedShots()
    {
        VehicleWeaponHeat heat = new VehicleWeaponHeat();
        assertFalse(heat.fired(2, 10, 3));
        assertEquals(10, heat.getHeat());
        assertTrue(heat.fired(1, 10, 3));
        assertTrue(heat.locked(10));
        assertFalse(heat.fired(1, 10, 3));
        assertEquals(15, heat.getHeat());
        heat.tick(10, 10);
        assertEquals(4, heat.getHeat());
        assertTrue(heat.locked(10));
        heat.tick(10, 10);
        assertTrue(heat.locked(10));
        heat.tick(10, 10);
        assertFalse(heat.locked(10));
        assertEquals(0, heat.getHeat());
    }

    @Test
    void stillHotWeaponStaysLockedAfterPenaltyExpires()
    {
        VehicleWeaponHeat heat = new VehicleWeaponHeat();
        assertTrue(heat.fired(4, 5, 1));
        heat.tick(5, 0);
        assertEquals(0, heat.getLockoutTicks());
        assertTrue(heat.locked(5));
        for (int i = 0; i < 14; i++)
            heat.tick(5, 0);
        assertFalse(heat.locked(5));
        assertEquals(5, heat.getHeat());
    }

    @Test
    void arithmeticCannotOverflowAndDisablingResetsState()
    {
        VehicleWeaponHeat heat = new VehicleWeaponHeat();
        assertTrue(heat.fired(Integer.MAX_VALUE, 10, 75));
        assertEquals(Integer.MAX_VALUE, heat.getHeat());
        heat.tick(10, Integer.MAX_VALUE);
        assertEquals(0, heat.getHeat());
        heat.tick(0, 0);
        assertFalse(heat.locked(10));
        assertEquals(0, heat.getLockoutTicks());
    }
}
