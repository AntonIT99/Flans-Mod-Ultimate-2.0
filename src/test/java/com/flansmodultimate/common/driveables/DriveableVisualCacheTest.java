package com.flansmodultimate.common.driveables;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DriveableVisualCacheTest
{
    @Test
    void refreshesIdleInventoryOnlyOncePerSecond()
    {
        DriveableVisualCache cache = new DriveableVisualCache();
        Object inventory = new Object();
        int refreshes = 0;
        for (int tick = 0; tick < 100; tick++)
            if (cache.needsRefresh(inventory, 0, -1, -1, tick)) ++refreshes;
        assertEquals(5, refreshes);
    }

    @Test
    void immediatelyRefreshesConsumptionRestockingAndChamberSwitchesEvenInTheSameTick()
    {
        DriveableVisualCache cache = new DriveableVisualCache();
        Object inventory = new Object();
        assertTrue(cache.needsRefresh(inventory, 0, -1, -1, 1));
        assertFalse(cache.needsRefresh(inventory, 0, -1, -1, 1));
        assertTrue(cache.needsRefresh(inventory, 1, -1, -1, 1));
        assertTrue(cache.needsRefresh(inventory, 2, -1, -1, 1));
        assertTrue(cache.needsRefresh(inventory, 2, 0, -1, 1));
        assertTrue(cache.needsRefresh(inventory, 2, 0, 3, 1));
        assertTrue(cache.needsRefresh(inventory, 2, -1, 3, 1));
    }

    @Test
    void inventoryReplacementAndTickResetCannotReuseStaleVisuals()
    {
        DriveableVisualCache cache = new DriveableVisualCache();
        Object inventory = new Object();
        assertTrue(cache.needsRefresh(new Object(), 0, 0, 0, 200));
        assertTrue(cache.needsRefresh(inventory, 0, 0, 0, 200));
        assertTrue(cache.needsRefresh(inventory, 0, 0, 0, 0));
    }
}
