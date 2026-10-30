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
}
