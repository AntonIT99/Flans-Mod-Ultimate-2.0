package com.flansmodultimate.common.driveables;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DriveableDataTest
{
    @Test
    void migratesPilotFirstAmmoSlotsToLegacyPassengerFirstOrder()
    {
        assertEquals(2, DriveableData.remapPilotFirstAmmoIndex(0, 2, 2));
        assertEquals(3, DriveableData.remapPilotFirstAmmoIndex(1, 2, 2));
        assertEquals(0, DriveableData.remapPilotFirstAmmoIndex(2, 2, 2));
        assertEquals(1, DriveableData.remapPilotFirstAmmoIndex(3, 2, 2));
        assertEquals(2, DriveableData.remapPilotFirstAmmoIndex(0, 1, 2));
        assertEquals(0, DriveableData.remapPilotFirstAmmoIndex(1, 1, 2));
        assertEquals(1, DriveableData.remapPilotFirstAmmoIndex(2, 1, 2));
    }

    @Test
    void rejectsInvalidHistoricalAmmoIndices()
    {
        assertEquals(-1, DriveableData.remapPilotFirstAmmoIndex(-1, 2, 2));
        assertEquals(-1, DriveableData.remapPilotFirstAmmoIndex(4, 2, 2));
    }

    @Test
    void honoursLegacyFilterAmmunitionInputSemantics()
    {
        assertTrue(DriveableData.allowsAmmunitionInput(false, false));
        assertTrue(DriveableData.allowsAmmunitionInput(false, true));
        assertFalse(DriveableData.allowsAmmunitionInput(true, false));
        assertTrue(DriveableData.allowsAmmunitionInput(true, true));
    }
}
