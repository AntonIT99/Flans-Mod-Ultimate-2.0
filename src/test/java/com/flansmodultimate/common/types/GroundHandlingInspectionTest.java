package com.flansmodultimate.common.types;

import com.flansmodultimate.api.FlansEntityTypes;
import com.flansmodultimate.common.driveables.physics.RealWorldVehicleSpec;
import com.flansmodultimate.config.ModCommonConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GroundHandlingInspectionTest
{
    @Test
    void absoluteSteeringAndReverseSpeedsProduceMatchingRadiusAndRatio()
    {
        VehicleType type = vehicle(60F, 72F, 18F);
        try (var config = mockStatic(ModCommonConfig.class))
        {
            config.when(() -> ModCommonConfig.realisticSpeedScale(any())).thenReturn(1D);
            var result = FlansEntityTypes.getGroundHandling(type);
            assertEquals(60D, result.turnRate().orElseThrow());
            assertEquals(1D / Math.toRadians(3D), result.turningRadius().orElseThrow(), 1.0E-6D);
            assertEquals(0.25D, result.reverseShare().orElseThrow());
            config.when(() -> ModCommonConfig.realisticSpeedScale(any())).thenReturn(0.5D);
            assertEquals(result.turningRadius().orElseThrow() * 0.5D, FlansEntityTypes.getGroundHandling(type).turningRadius().orElseThrow(), 1.0E-6D);
        }
    }

    @Test
    void trackedVehiclesNeedNoReferenceSpeedButHaveNoRollingRadius()
    {
        VehicleType type = vehicle(45F, null, null);
        type.tank = true;
        var result = FlansEntityTypes.getGroundHandling(type);
        assertEquals(45D, result.turnRate().orElseThrow());
        assertTrue(result.turningRadius().isEmpty());
        assertTrue(result.reverseShare().isEmpty());
    }

    @Test
    void legacyCoefficientsAreNotInventedAbsoluteLimitsAndReverseProhibitionIsKept()
    {
        VehicleType type = vehicle(0F, null, null);
        type.turnLeftModifier = 3F;
        type.maxNegativeThrottle = 0F;
        var result = FlansEntityTypes.getGroundHandling(type);
        assertTrue(result.turnRate().isEmpty());
        assertTrue(result.turningRadius().isEmpty());
        assertEquals(0D, result.reverseShare().orElseThrow());
        assertTrue(FlansEntityTypes.getGroundHandling(new PlaneType()).turnRate().isEmpty());
    }

    @Test
    void forceLegacyAndCrewPushedTypesDoNotOfferInactiveAbsoluteSteering()
    {
        VehicleType type = vehicle(60F, 72F, 18F);
        type.pushSpeedKmh = 5F;
        assertTrue(FlansEntityTypes.getGroundHandling(type).turnRate().isEmpty());
        type.pushSpeedKmh = 0F;
        try (var config = mockStatic(ModCommonConfig.class))
        {
            config.when(ModCommonConfig::forceLegacyVehiclePhysics).thenReturn(true);
            var result = FlansEntityTypes.getGroundHandling(type);
            assertTrue(result.turnRate().isEmpty());
            assertTrue(result.reverseShare().isEmpty());
        }
    }

    private static VehicleType vehicle(float rate, Float speed, Float reverse)
    {
        VehicleType type = new VehicleType();
        type.realTurnRateDegPerSec = rate;
        type.maxNegativeThrottle = 1F;
        type.realWorldSpec = new RealWorldVehicleSpec(null, speed, null, null, RealWorldVehicleSpec.Aircraft.EMPTY, new RealWorldVehicleSpec.Ground(null, reverse), RealWorldVehicleSpec.Marine.EMPTY);
        return type;
    }
}
