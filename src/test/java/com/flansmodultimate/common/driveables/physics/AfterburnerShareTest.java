package com.flansmodultimate.common.driveables.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AfterburnerShareTest
{
    @Test
    void authoredRatingWins()
    {
        assertEquals(0.6D, AircraftPerformancePhysics.afterburnerDryShare(60D, 100D, 0.5D, true, 0.62D), 1E-9D);
    }

    @Test
    void cruiseRatioIsSquaredForJetsAndCubedForPropellers()
    {
        assertEquals(0.64D, AircraftPerformancePhysics.afterburnerDryShare(0D, 100D, 0.8D, true, 0.62D), 1E-9D);
        assertEquals(0.512D, AircraftPerformancePhysics.afterburnerDryShare(0D, 100D, 0.8D, false, 0.87D), 1E-9D);
    }

    @Test
    void fallsBackToTheConfiguredShare()
    {
        assertEquals(0.87D, AircraftPerformancePhysics.afterburnerDryShare(0D, 2000D, 0D, false, 0.87D), 1E-9D);
    }

    @Test
    void afterburnerOnlyHeatsTheGaugesWhileBurning()
    {
        EngineGaugeHeat burning = new EngineGaugeHeat();
        EngineGaugeHeat dry = new EngineGaugeHeat();
        burning.advance(1F, true, true, true, 20000);
        dry.advance(1F, true, true, false, 20000);
        assertTrue(burning.getOil() > dry.getOil() + 0.1F);
    }
}
