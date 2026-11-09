package com.flansmodultimate.common.driveables.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EngineGaugeHeatTest
{
    private static EngineGaugeHeat running(float throttle, int ticks)
    {
        EngineGaugeHeat heat = new EngineGaugeHeat();
        heat.advance(throttle, true, ticks);
        return heat;
    }

    @Test
    void warmsGraduallyRatherThanFollowingTheThrottle()
    {
        EngineGaugeHeat heat = running(1F, 20);
        assertTrue(heat.getOil() > 0F && heat.getOil() < 0.1F);
    }

    @Test
    void sustainedFullThrottleRunsHotterThanCruise()
    {
        assertTrue(running(1F, 20000).getOil() > running(0.7F, 20000).getOil() + 0.1F);
    }

    @Test
    void waterLagsOilUnderSustainedFullPower()
    {
        EngineGaugeHeat heat = running(1F, 8000);
        assertTrue(heat.getWater() < heat.getOil());
    }

    @Test
    void oilHoldsAnIdleReadingThatWaterDoesNot()
    {
        EngineGaugeHeat heat = running(0.1F, 20000);
        assertTrue(heat.getOil() > heat.getWater());
    }

    @Test
    void stoppedEngineDrainsTheGauges()
    {
        EngineGaugeHeat heat = running(1F, 4000);
        heat.advance(1F, false, 60);
        assertEquals(0F, heat.getOil());
        assertEquals(0F, heat.getWater());
    }
}
