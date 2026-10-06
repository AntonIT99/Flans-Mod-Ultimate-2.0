package com.wolffsmod.npcs.properties;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NpcTypeCadenceTest
{
    @Test
    void carriesFractionalTickIntervalsWithoutAddingNpcBurstGaps()
    {
        NpcTypeCadence cadence = new NpcTypeCadence();
        int elapsed = 0;
        for (int shot = 0; shot < 100; shot++)
        {
            int cooldown = cadence.next(2.5);
            do
            {
                elapsed++;
            } while (cooldown-- > 0);
        }
        assertEquals(250, elapsed);
    }

    @Test
    void resettingAndSubtickIntervalsKeepNpcTickLimitsExplicit()
    {
        NpcTypeCadence cadence = new NpcTypeCadence();
        assertEquals(1, cadence.next(2.5));
        cadence.reset();
        assertEquals(1, cadence.next(2.5));
        assertEquals(0, cadence.next(0.2));
        assertEquals(0, cadence.next(Double.NaN));
    }
}
