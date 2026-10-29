package com.flansmodultimate.common.types;

import com.flansmodultimate.api.driveables.FlightPerformance;
import com.flansmodultimate.api.driveables.GroundPropulsion;
import com.flansmodultimate.api.entity.FlansEntityTypes;
import com.flansmodultimate.common.driveables.EnumPlaneMode;
import com.flansmodultimate.common.driveables.physics.*;
import com.flansmodultimate.config.ModCommonConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

class DriveableMobilityInspectionTest
{
    @Test
    void aMechaWalksAtItsResolvedSpeedAndJumpsOnlyWhenItsDefinitionSaysHowHigh()
    {
        MechaType mecha = new MechaType();
        mecha.moveSpeed = 2F;
        var mobility = FlansEntityTypes.getMechaMobility(mecha).orElseThrow();
        assertEquals(2D * MechaPhysics.LEGACY_SPEED_PER_TICK, mobility.walkSpeed(), 1.0E-9D);
        assertTrue(mobility.jumpHeight().isEmpty());
        mecha.jumpHeight = 3F;
        assertEquals(3D, FlansEntityTypes.getMechaMobility(mecha).orElseThrow().jumpHeight().orElseThrow(), 1.0E-9D);
        assertTrue(FlansEntityTypes.getMechaMobility(new VehicleType()).isEmpty());
    }

    @Test
    void wheelsOfALandVehicleClimbAWholeBlockAndAMechaClimbsItsOwnStepHeight()
    {
        VehicleType vehicle = new VehicleType();
        vehicle.wheelStepHeight = 0.5F;
        assertEquals(1D, FlansEntityTypes.getStepHeight(vehicle).orElseThrow(), 1.0E-9D);
        vehicle.placeableOnLand = false;
        assertEquals(0.5D, FlansEntityTypes.getStepHeight(vehicle).orElseThrow(), 1.0E-9D);
        MechaType mecha = new MechaType();
        mecha.stepHeight = 3;
        assertEquals(3D, FlansEntityTypes.getStepHeight(mecha).orElseThrow(), 1.0E-9D);
        mecha.stepHeight = 20;
        assertEquals(8D, FlansEntityTypes.getStepHeight(mecha).orElseThrow(), 1.0E-9D);
        assertTrue(FlansEntityTypes.getStepHeight(new PlaneType()).isEmpty());
    }

    @Test
    void aLegacyPlaneTopsOutAtItsMaxSpeedAndNeedsItsTakeoffSpeedToFly()
    {
        PlaneType plane = new PlaneType();
        plane.maxSpeed = 1.5F;
        plane.takeoffSpeed = 0.4F;
        plane.rollLeftModifier = 2F;
        plane.rollRightModifier = 1F;
        FlightPerformance performance = FlansEntityTypes.getFlightPerformance(plane).orElseThrow();
        assertFalse(performance.realistic());
        assertFalse(performance.rotorcraft());
        assertEquals(1.5D, performance.topSpeed(), 1.0E-6D);
        assertEquals(0.4D, performance.minimumSpeed(), 1.0E-6D);
        assertEquals(1.5D, performance.rollAuthority(), 1.0E-6D);
        assertEquals(1D, performance.pitchAuthority(), 1.0E-6D);
        assertTrue(performance.climbRate().isEmpty());
        // Without NewFlightControl the legacy model never flies faster than two blocks a tick, supersonic or not.
        plane.maxSpeed = 3F;
        plane.supersonic = true;
        assertEquals(2D, FlansEntityTypes.getFlightPerformance(plane).orElseThrow().topSpeed(), 1.0E-6D);
        plane.newFlightControl = true;
        assertEquals(4.5D, FlansEntityTypes.getFlightPerformance(plane).orElseThrow().topSpeed(), 1.0E-6D);
    }

    @Test
    void aRotorcraftHoversAndASixDegreeCraftIsNotCovered()
    {
        PlaneType helicopter = new PlaneType();
        helicopter.mode = EnumPlaneMode.HELI;
        FlightPerformance performance = FlansEntityTypes.getFlightPerformance(helicopter).orElseThrow();
        assertTrue(performance.rotorcraft());
        assertEquals(0D, performance.minimumSpeed());
        helicopter.mode = EnumPlaneMode.SIXDOF;
        assertTrue(FlansEntityTypes.getFlightPerformance(helicopter).isEmpty());
        assertTrue(FlansEntityTypes.getFlightPerformance(new VehicleType()).isEmpty());
    }

    @Test
    void aRealWorldAircraftFliesAtItsTopSpeedAndClimbsAtItsRate()
    {
        try (var config = mockStatic(ModCommonConfig.class))
        {
            config.when(() -> ModCommonConfig.realisticSpeedScale(any())).thenReturn(1D);
            config.when(ModCommonConfig::realisticAircraftReferenceSpeedScale).thenReturn(1D);
            PlaneType plane = new PlaneType();
            RealWorldVehicleSpec spec = new RealWorldVehicleSpec(3000F, 540F, 900F, null, new RealWorldVehicleSpec.Aircraft(11F, 22F, 15F), RealWorldVehicleSpec.Ground.EMPTY,
                RealWorldVehicleSpec.Marine.EMPTY);
            plane.resolvedPhysics = new ResolvedVehiclePhysics(EnumVehiclePhysicsMode.REAL_WORLD_PROFILE, EnumVehicleCategory.AIRCRAFT, spec, VehicleGeometry.EMPTY, false, true, false, 900F, 0F,
                3000F, 540F, 0.3F, 0F, 0F, 1F, EnumDriveType.AWD, false, null, null);
            FlightPerformance performance = FlansEntityTypes.getFlightPerformance(plane).orElseThrow();
            assertTrue(performance.realistic());
            assertEquals(VehiclePhysicsUnits.kmhToBlocksPerTick(540D), performance.topSpeed(), 1.0E-9D);
            assertEquals(15D / 20D, performance.climbRate().orElseThrow(), 1.0E-9D);
            assertTrue(performance.minimumSpeed() > 0D && performance.minimumSpeed() < performance.topSpeed());
            // The plane speed cap applies as it does to the plane itself.
            config.when(ModCommonConfig::maxPlaneSpeedKmh).thenReturn(360D);
            assertEquals(VehiclePhysicsUnits.kmhToBlocksPerTick(360D), FlansEntityTypes.getFlightPerformance(plane).orElseThrow().topSpeed(), 1.0E-9D);
        }
    }

    @Test
    void aShipFiledAsAPlaneSailsUnderHerMarineProfileAndMakesFortyPercentAstern()
    {
        try (var config = mockStatic(ModCommonConfig.class))
        {
            config.when(() -> ModCommonConfig.realisticSpeedScale(any())).thenReturn(1D);
            PlaneType ship = new PlaneType();
            ship.epicShip = true;
            ship.resolvedPhysics = new ResolvedVehiclePhysics(EnumVehiclePhysicsMode.REAL_WORLD_PROFILE, EnumVehicleCategory.GROUND, RealWorldVehicleSpec.EMPTY, VehicleGeometry.EMPTY, false, false,
                true, 2000F, 0F, 400000F, 54F, 0.005F, 0F, 0F, 1F, EnumDriveType.MARINE, true, null, 3F);
            GroundPropulsion water = FlansEntityTypes.getWaterPropulsion(ship).orElseThrow();
            assertEquals(VehiclePhysicsUnits.kmhToBlocksPerTick(54D), water.topSpeed(), 1.0E-9D);
            assertEquals(water.topSpeed() * 0.4D, water.reverseSpeed(), 1.0E-9D);
            assertTrue(water.acceleration(0D) > 0D);
            assertEquals(0.4D, FlansEntityTypes.getWaterHandling(ship).reverseShare().orElseThrow(), 1.0E-9D);
            // Nothing climbs in water, and on land she is no vehicle.
            assertEquals(1D, water.climbFactor(30D));
            assertTrue(FlansEntityTypes.getGroundPropulsion(ship).isEmpty());
            ship.epicShip = false;
            assertTrue(FlansEntityTypes.getWaterPropulsion(ship).isEmpty());
        }
    }

    @Test
    void aRealWorldVehicleBogsDownOnSteepGradesAndALegacyOneDoesNot()
    {
        try (var config = mockStatic(ModCommonConfig.class))
        {
            config.when(() -> ModCommonConfig.realisticSpeedScale(any())).thenReturn(1D);
            VehicleType type = new VehicleType();
            type.maxThrottle = 2F;
            type.maxNegativeThrottle = 1F;
            type.resolvedPhysics = new ResolvedVehiclePhysics(EnumVehiclePhysicsMode.REAL_WORLD_PROFILE, EnumVehicleCategory.GROUND, RealWorldVehicleSpec.EMPTY, VehicleGeometry.EMPTY, true, false,
                false, 400F, 0F, 20000F, 54F, 0.02F, 0F, 0F, 1F, EnumDriveType.AWD, true, null, null);
            GroundPropulsion realistic = FlansEntityTypes.getGroundPropulsion(type).orElseThrow();
            assertEquals(1D, realistic.climbFactor(0D));
            assertEquals(1D, realistic.climbFactor(-30D));
            assertTrue(realistic.climbFactor(60D) < 1D);
            assertTrue(realistic.climbFactor(60D) > 0D);
            assertEquals(GroundSlopePhysics.propulsionFactor(45F, 1F, 0.02F, EnumDriveType.AWD), realistic.climbFactor(45D), 1.0E-6D);
        }
        VehicleType legacy = new VehicleType();
        legacy.maxThrottle = 2F;
        assertEquals(1D, FlansEntityTypes.getGroundPropulsion(legacy).orElseThrow().climbFactor(60D));
    }
}
