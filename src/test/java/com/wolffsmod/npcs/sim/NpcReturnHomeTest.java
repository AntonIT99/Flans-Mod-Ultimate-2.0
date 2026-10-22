package com.wolffsmod.npcs.sim;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NpcReturnHomeTest
{
    @Test
    void initialSearchAndRestartShareTheSameFortyTickBudget()
    {
        NpcReturnHome state = new NpcReturnHome();
        assertTrue(state.ready(100));
        state.attempted(100);
        for (long tick = 100; tick < 140; tick++)
            assertFalse(state.ready(tick));
        assertTrue(state.ready(140));
        state.attempted(140);
        assertFalse(state.ready(179));
        assertTrue(state.ready(180));
    }

    @Test
    void repeatedFailuresRemainBoundedBeyondTheOldTeleportTimeout()
    {
        NpcReturnHome state = new NpcReturnHome();
        int attempts = 0;
        for (int tick = 0; tick < 1200; tick++)
            if (state.ready(tick))
            {
                state.attempted(tick);
                attempts++;
            }
        assertEquals(30, attempts);
        assertTrue(state.nextDetour());
        assertFalse(state.nextDetour());
        assertTrue(state.nextDetour());
    }
}
