package com.wolffsmod.npcs.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NpcSupportBurstTest
{
    @Test
    void evenAFastGunPausesAfterEightActualRounds()
    {
        NpcSupportBurst burst = new NpcSupportBurst();
        for (int round = 0; round < 8; round++)
        {
            assertTrue(burst.ready(0L));
            burst.fired(0L);
        }
        assertFalse(burst.ready(0L));
        assertFalse(burst.ready(11L));
        assertTrue(burst.ready(12L));
    }

    @Test
    void slowerAutomaticFireAlsoHasABoundedBurstDuration()
    {
        NpcSupportBurst burst = new NpcSupportBurst();
        burst.fired(10L);
        assertTrue(burst.ready(39L));
        assertFalse(burst.ready(40L));
        assertTrue(burst.ready(52L));
        burst.fired(52L);
        assertTrue(burst.ready(53L));
    }
}
