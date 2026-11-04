package com.wolffsmod.npcs.sim;

import com.wolffsmod.npcs.sim.NpcSubmarine.Trim;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NpcSubmarineTest
{
    private static final int RUNNING = 20;

    @Test
    void aSubmarineOnItsWayRunsAtTheDepthItWasGiven()
    {
        assertEquals(RUNNING, NpcSubmarine.trimDepth(Trim.TRANSIT, RUNNING));
    }

    @Test
    void aSubmarineComesUpToPeriscopeDepthToFight()
    {
        assertEquals(NpcSubmarine.PERISCOPE_DEPTH, NpcSubmarine.trimDepth(Trim.ATTACK, RUNNING));
        // One whose running depth is already shallower stays where it is rather than rising above it.
        assertEquals(1, NpcSubmarine.trimDepth(Trim.ATTACK, 1));
        assertEquals(2, NpcSubmarine.trimDepth(Trim.ATTACK, 2));
    }

    @Test
    void aSubmarineThatHasBeenHitGoesBelowItsRunningDepth()
    {
        assertEquals(RUNNING + NpcSubmarine.EVADE_DEPTH, NpcSubmarine.trimDepth(Trim.EVADE, RUNNING));
        assertTrue(NpcSubmarine.trimDepth(Trim.EVADE, RUNNING) > NpcSubmarine.trimDepth(Trim.TRANSIT, RUNNING));
    }

    @Test
    void anyTrimStillKeepsTheHullOffTheBottomInShallowWater()
    {
        // Whatever depth the trim asks for, the water it is in has the last word.
        assertEquals(3, NpcSubmarine.effectiveDepth(NpcSubmarine.trimDepth(Trim.EVADE, RUNNING), 4));
        assertEquals(1, NpcSubmarine.effectiveDepth(NpcSubmarine.trimDepth(Trim.TRANSIT, RUNNING), 1));
    }

    @Test
    void theDiveControllerClosesOnItsDepthAndThenHoldsIt()
    {
        assertTrue(NpcSubmarine.dive(0D, 4D) > 0D, "below its depth it rises");
        assertTrue(NpcSubmarine.dive(0D, -4D) < 0D, "above it it sinks");
        assertEquals(0.045D, NpcSubmarine.dive(0.1D, 0D), 1.0E-9D, "on its depth it bleeds off what motion it has");
    }

    @Test
    void aDiveRateCapsHowFastItChangesDepth()
    {
        assertEquals(0.05D, NpcSubmarine.dive(0D, 40D, 0.05D), 1.0E-9D);
        assertEquals(-0.05D, NpcSubmarine.dive(0D, -40D, 0.05D), 1.0E-9D);
    }
}
