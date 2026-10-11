package com.wolffsmod.npcs.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcThrowablesTest
{
    @Test
    void npcAccuracyBecomesVanillaInaccuracyWithAHandWobbleAtBest()
    {
        assertEquals(1F, NpcThrowables.inaccuracy(100));
        assertEquals(12F, NpcThrowables.inaccuracy(0));
        assertEquals(6.5F, NpcThrowables.inaccuracy(50));
        assertTrue(NpcThrowables.inaccuracy(90) < NpcThrowables.inaccuracy(60));
        assertEquals(1F, NpcThrowables.inaccuracy(250));
        assertEquals(12F, NpcThrowables.inaccuracy(-20));
    }
}
