package com.wolffsmod.npcs.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcItemCadenceTest
{
    @Test
    void automaticWeaponsCanFireAboveTwentyRoundsPerSecond()
    {
        NpcItemCadence cadence = new NpcItemCadence();
        int shots = 0;
        for (int tick = 0; tick < 20; tick++)
        {
            cadence.tick();
            while (cadence.ready())
            {
                cadence.fired(0.5D);
                shots++;
            }
        }
        assertEquals(39, shots);
    }

    @Test
    void fractionalDelaysDoNotDriftAndIdleTimeDoesNotQueueMissedShots()
    {
        NpcItemCadence cadence = new NpcItemCadence();
        cadence.fired(2.5D);
        cadence.tick();
        cadence.tick();
        assertFalse(cadence.ready());
        cadence.tick();
        assertTrue(cadence.ready());
        for (int tick = 0; tick < 100; tick++)
            cadence.tick();
        cadence.fired(2.5D);
        cadence.tick();
        assertFalse(cadence.ready());
        cadence.tick();
        assertTrue(cadence.ready());
    }

    @Test
    void invalidIntervalsCannotCreateAnInfiniteFiringLoop()
    {
        NpcItemCadence cadence = new NpcItemCadence();
        cadence.fired(0D);
        assertFalse(cadence.ready());
        cadence.reset();
        cadence.fired(Double.NaN);
        assertFalse(cadence.ready());
        cadence.tick();
        assertTrue(cadence.ready());
    }
}
