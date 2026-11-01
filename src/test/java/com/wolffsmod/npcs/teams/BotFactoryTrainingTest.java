package com.wolffsmod.npcs.teams;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BotFactoryTrainingTest
{
    @Test
    void aFactorOfOneLeavesTheStatExactlyAsItWas()
    {
        assertEquals(0, BotFactory.scaled(0, 1F, 1));
        assertEquals(37, BotFactory.scaled(37, 1F, 1));
    }

    @Test
    void otherFactorsScaleTheStatAndKeepItAboveItsLeast()
    {
        assertEquals(51, BotFactory.scaled(85, 0.6F, 0));
        assertEquals(1, BotFactory.scaled(0, 1.5F, 1));
        assertEquals(30, BotFactory.scaled(20, 1.5F, 1));
    }
}
