package com.flansmodultimate.client.render.thermal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Team-mates pulse hot twice a second: ticks 0 to 3 and 6 to 9 of every 20. */
class ThermalTeamStrobeTest
{
    @Test
    void doublePulseEverySecond()
    {
        assertTrue(ThermalTeamStrobe.isFlashOn(0L, 0F));
        assertTrue(ThermalTeamStrobe.isFlashOn(2L, 0.9F));
        assertFalse(ThermalTeamStrobe.isFlashOn(3L, 0F));
        assertFalse(ThermalTeamStrobe.isFlashOn(5L, 0.5F));
        assertTrue(ThermalTeamStrobe.isFlashOn(6L, 0F));
        assertTrue(ThermalTeamStrobe.isFlashOn(8L, 0.5F));
        assertFalse(ThermalTeamStrobe.isFlashOn(9L, 0F));
        assertFalse(ThermalTeamStrobe.isFlashOn(19L, 0.9F));
        assertTrue(ThermalTeamStrobe.isFlashOn(40L, 0.1F));
    }
}
