package com.wolffsmod.npcs.properties;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NpcTypeHealthTest
{
    @Test
    void oldHealthyNpcsStayHealthyAndExistingDamageSurvivesInheritance()
    {
        assertEquals(200F, NpcTypeHealth.restore(20F, 20F, 200F));
        assertEquals(100F, NpcTypeHealth.restore(10F, 20F, 200F));
        assertEquals(100F, NpcTypeHealth.restore(100F, 200F, 200F));
        assertEquals(200F, NpcTypeHealth.restore(100F, 200F, 400F));
        assertEquals(10F, NpcTypeHealth.restore(100F, 200F, 20F));
    }

    @Test
    void deadNpcsAndOutOfRangeSavedHealthDoNotGainExtraHealth()
    {
        assertEquals(0F, NpcTypeHealth.restore(0F, 200F, 400F));
        assertEquals(200F, NpcTypeHealth.restore(220F, 200F, 200F));
        assertEquals(0F, NpcTypeHealth.restore(-1F, 20F, 200F));
    }
}
