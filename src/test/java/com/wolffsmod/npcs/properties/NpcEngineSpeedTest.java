package com.wolffsmod.npcs.properties;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcEngineSpeedTest
{
    @Test
    void speedGlidesTowardsTheMeasuredSpeedAndSettles()
    {
        float speed = 0F;
        float previous = speed;
        for (int tick = 0; tick < 40; tick++)
        {
            speed = NpcEngineAudio.smoothSpeed(speed, 1F);
            assertTrue(speed >= previous);
            previous = speed;
        }
        assertEquals(1F, speed);
    }

    @Test
    void steadySpeedStaysUnchanged()
    {
        float steady = 32F / NpcEngineAudio.SPEED_STEPS;
        assertEquals(steady, NpcEngineAudio.smoothSpeed(steady, 0.5F));
    }

    @Test
    void oneTickSpikeMovesThePitchOnlyPartly()
    {
        assertTrue(NpcEngineAudio.smoothSpeed(0F, 1F) <= 0.25F);
    }
}
