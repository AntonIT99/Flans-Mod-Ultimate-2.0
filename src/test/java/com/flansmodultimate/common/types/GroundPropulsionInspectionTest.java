package com.flansmodultimate.common.types;

import com.flansmodultimate.api.driveables.GroundPropulsion;
import com.flansmodultimate.api.entity.FlansEntityTypes;
import com.flansmodultimate.common.driveables.physics.*;
import com.flansmodultimate.config.ModCommonConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GroundPropulsionInspectionTest
{
    @Test
    void aRealWorldProfileTopsOutAtItsSpeedAndAcceleratesFromPowerAgainstMass()
    {
        try (var config = mockStatic(ModCommonConfig.class))
        {
            config.when(() -> ModCommonConfig.realisticSpeedScale(any())).thenReturn(1D);
            GroundPropulsion light = FlansEntityTypes.getGroundPropulsion(derived(400F, 20000F, 54F, null)).orElseThrow();
            GroundPropulsion heavy = FlansEntityTypes.getGroundPropulsion(derived(400F, 60000F, 54F, null)).orElseThrow();
            assertTrue(light.realistic());
            assertEquals(VehiclePhysicsUnits.kmhToBlocksPerTick(54D), light.topSpeed(), 1.0E-9D);
            assertEquals(60000D, heavy.massKg().orElseThrow());
            assertEquals(400D, heavy.enginePowerKw().orElseThrow());
            // The same engine pulls a lighter hull away faster, and every hull gains less the nearer it is to its top speed.
            assertTrue(light.acceleration(0D) > heavy.acceleration(0D));
            assertTrue(light.acceleration(light.topSpeed() * 0.5D) < light.acceleration(0D));
            assertEquals(0D, light.acceleration(light.topSpeed()));
            assertTrue(light.deceleration(light.topSpeed(), 1D) > light.deceleration(light.topSpeed(), 0D));
            assertTrue(light.deceleration(0D, 0D) > 0D);
            // Without its own reverse speed it reverses at the share its throttle allows.
            assertEquals(light.topSpeed() * 0.5D, light.reverseSpeed(), 1.0E-9D);
            assertEquals(VehiclePhysicsUnits.kmhToBlocksPerTick(9D), FlansEntityTypes.getGroundPropulsion(derived(400F, 20000F, 54F, 9F)).orElseThrow().reverseSpeed(), 1.0E-9D);
            config.when(() -> ModCommonConfig.realisticSpeedScale(any())).thenReturn(0.5D);
            assertEquals(light.topSpeed() * 0.5D, FlansEntityTypes.getGroundPropulsion(derived(400F, 20000F, 54F, null)).orElseThrow().topSpeed(), 1.0E-9D);
        }
    }

    @Test
    void theSpeedCapLowersTheTopSpeedButNotTheResistanceOfTheProfile()
    {
        try (var config = mockStatic(ModCommonConfig.class))
        {
            config.when(() -> ModCommonConfig.realisticSpeedScale(any())).thenReturn(1D);
            config.when(ModCommonConfig::maxVehicleSpeedKmh).thenReturn(36D);
            GroundPropulsion capped = FlansEntityTypes.getGroundPropulsion(derived(400F, 20000F, 54F, null)).orElseThrow();
            assertEquals(VehiclePhysicsUnits.kmhToBlocksPerTick(36D), capped.topSpeed(), 1.0E-9D);
            assertEquals(0D, capped.acceleration(capped.topSpeed()));
            assertTrue(capped.acceleration(capped.topSpeed() * 0.9D) > 0D);
        }
    }

    @Test
    void aLegacyDefinitionSettlesBelowItsThrottleSpeedAndRampsItsPedal()
    {
        VehicleType type = new VehicleType();
        type.maxThrottle = 2F;
        type.maxNegativeThrottle = 1F;
        GroundPropulsion legacy = FlansEntityTypes.getGroundPropulsion(type).orElseThrow();
        assertFalse(legacy.realistic());
        assertTrue(legacy.massKg().isEmpty());
        // Each tick the velocity closes 22% of the gap to the wanted one and then keeps 98% of itself.
        double settle = 0.22D * 0.98D / (1D - 0.98D * 0.78D);
        assertEquals(2D * 0.32D * settle, legacy.topSpeed(), 1.0E-9D);
        assertEquals(0.32D * settle, legacy.reverseSpeed(), 1.0E-9D);
        assertEquals(0.01D * legacy.topSpeed(), legacy.acceleration(0D), 1.0E-9D);
        assertEquals(0D, legacy.acceleration(legacy.topSpeed()), 1.0E-9D);
        assertTrue(legacy.deceleration(legacy.topSpeed(), 1D) > legacy.deceleration(legacy.topSpeed(), 0D));
        type.useRealisticAcceleration = true;
        type.mass = 4000F;
        GroundPropulsion heavy = FlansEntityTypes.getGroundPropulsion(type).orElseThrow();
        assertEquals(4000D, heavy.massKg().orElseThrow());
        assertEquals(10D / 4000D * heavy.topSpeed(), heavy.acceleration(0D), 1.0E-9D);
    }

    @Test
    void aDefinitionThatCannotReverseOrIsNoGroundVehicleOffersNothingToReverseOrDrive()
    {
        VehicleType type = new VehicleType();
        type.maxNegativeThrottle = 0F;
        assertEquals(0D, FlansEntityTypes.getGroundPropulsion(type).orElseThrow().reverseSpeed());
        type.maxThrottle = 0F;
        assertTrue(FlansEntityTypes.getGroundPropulsion(type).isEmpty());
        assertTrue(FlansEntityTypes.getGroundPropulsion(new PlaneType()).isEmpty());
    }

    private static VehicleType derived(float powerKw, float massKg, float speedKmh, Float reverseKmh)
    {
        VehicleType type = new VehicleType();
        type.maxThrottle = 2F;
        type.maxNegativeThrottle = 1F;
        type.resolvedPhysics = new ResolvedVehiclePhysics(EnumVehiclePhysicsMode.REAL_WORLD_PROFILE, EnumVehicleCategory.GROUND, RealWorldVehicleSpec.EMPTY, VehicleGeometry.EMPTY, true, false, false,
            powerKw, 0F, massKg, speedKmh, powerKw / massKg, 0F, 0F, 1F, EnumDriveType.AWD, true, reverseKmh, null);
        return type;
    }
}
