package com.wolffsmod.npcs.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NpcBombTargetsTest
{
    @Test
    void aTargetMoreThanFourBlocksAboveTheGroundIsInTheAirWhereNoBombHitsIt()
    {
        assertFalse(NpcBombTargets.airborne(64D, 64D));
        assertFalse(NpcBombTargets.airborne(68D, 64D), "on a low roof or a jump");
        assertTrue(NpcBombTargets.airborne(70D, 64D));
    }

    @Test
    void soldiersWithinOneBombsCircleOfTheTargetCountTowardsItsGroup()
    {
        assertTrue(NpcBombTargets.beside(0D, 0D, 0D), "the target itself");
        assertTrue(NpcBombTargets.beside(5D, 1D, 4D));
        assertFalse(NpcBombTargets.beside(8D, 0D, 0D), "too far across the ground");
    }

    @Test
    void aSoldierFarAboveOrBelowTheTargetIsNoPartOfItsGroup()
    {
        assertTrue(NpcBombTargets.beside(1D, -5D, 1D), "down a slope");
        assertFalse(NpcBombTargets.beside(1D, 9D, 1D), "a floor or two above");
    }

    @Test
    void aHeavyBombMayComeDownOffTheTargetAsFarAsItsBlastReaches()
    {
        assertEquals(4D, NpcBombTargets.releaseRadius(0D), "a bomb whose blast is not known has to come down close");
        assertEquals(4D, NpcBombTargets.releaseRadius(6D), "a small blast keeps the plain release circle");
        assertEquals(12D, NpcBombTargets.releaseRadius(24D));
    }
}
