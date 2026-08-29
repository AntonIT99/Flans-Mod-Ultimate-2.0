package com.flansmodultimate.common.types;

import com.flansmodultimate.ContentPack;
import com.flansmodultimate.IContentProvider;
import com.flansmodultimate.common.driveables.physics.EnumDriveType;
import com.flansmodultimate.common.driveables.physics.EnumVehicleCategory;
import com.flansmodultimate.common.driveables.physics.EnumVehiclePhysicsMode;
import com.flansmodultimate.common.driveables.physics.ResolvedVehiclePhysics;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end checks through the real parser: a definition that declares no
 * {@code Real*} key must come out of parsing byte-for-byte as it did before, and
 * the legacy keys that share a concept with the new ones must keep their old
 * meaning.
 */
class DriveableTypeRealWorldPhysicsTest
{
    // ------------------------------------------------------------- legacy

    @Test
    void aLegacyVehicleParsesToPureLegacyPhysics()
    {
        VehicleType type = vehicle(
            "MaxThrottle 0.45",
            "MaxNegativeThrottle 0.25",
            "TurnLeftSpeed 0.25",
            "Drag 1.0");
        ResolvedVehiclePhysics physics = type.getResolvedPhysics();
        assertNotNull(physics);
        assertEquals(EnumVehiclePhysicsMode.LEGACY, physics.mode());
        assertEquals(EnumVehicleCategory.GROUND, physics.category());
        assertFalse(physics.hasGroundPropulsion());
        assertFalse(physics.hasReverseSpeedOverride());
        assertFalse(physics.hasSlopeLimit());
        assertFalse(physics.hasDraft());
        // The legacy fields themselves are untouched.
        assertEquals(0.45F, type.getMaxThrottle());
        assertEquals(0.25F, type.getMaxNegativeThrottle());
        assertEquals(0.25F, type.getTurnLeftModifier());
    }

    @Test
    void legacyMassStaysLegacyAndNeverBecomesARealWorldMass()
    {
        // Humvee.txt and five other shipped definitions declare exactly this.
        VehicleType type = vehicle("Mass 1.0");
        assertEquals(1F, type.getMass(), "the legacy field keeps its legacy value");
        assertTrue(type.getRealWorldSpec().isEmpty());
        assertEquals(EnumVehiclePhysicsMode.LEGACY, type.getResolvedPhysics().mode());
        assertEquals(0F, type.getResolvedPhysics().massKg(),
            "a legacy Mass must never be promoted into the real-world model");
    }

    @Test
    void legacyPlaneFieldsStayLegacy()
    {
        PlaneType type = plane("MaxSpeed 2.0", "Mass 3000", "WingArea 1.5", "MaxThrust 50", "Lift 1.0");
        assertEquals(2F, type.getMaxSpeed(), "MaxSpeed stays blocks per tick");
        assertEquals(3000F, type.getMass());
        assertEquals(1.5F, type.getWingArea());
        assertTrue(type.getRealWorldSpec().isEmpty());
        assertFalse(type.getResolvedPhysics().hasAircraftProfile());
    }

    @Test
    void aLegacyPlaneKeepsTheHistoricalMovementClamp()
    {
        assertEquals(8D, plane("Model Spitfire").getResolvedPhysics().movementClampBlocksPerTick());
    }

    // ---------------------------------------------------- complete profiles

    @Test
    void aCompleteGroundProfileParsesAndActivates()
    {
        VehicleType type = vehicle(
            "MaxThrottle 1.0",
            "RealMassKg 2400",
            "RealEnginePowerKw 140",
            "RealMaxSpeedKmh 113");
        ResolvedVehiclePhysics physics = type.getResolvedPhysics();
        assertEquals(EnumVehiclePhysicsMode.REAL_WORLD_PROFILE, physics.mode());
        assertTrue(physics.hasGroundPropulsion());
        assertEquals(113D / 72D, physics.maxSpeedBlocksPerTick(1D), 1.0E-9D);
        assertEquals(32D, physics.movementClampBlocksPerTick());
    }

    @Test
    void aCompleteAircraftProfileParsesAndActivates()
    {
        PlaneType type = plane(
            "RealMassKg 2890",
            "RealMaxSpeedKmh 635",
            "RealEnginePowerKw 993",
            "RealWingSpanM 11.23",
            "RealWingAreaM2 22.48",
            "RealClimbRateMs 17.0");
        ResolvedVehiclePhysics physics = type.getResolvedPhysics();
        assertEquals(EnumVehiclePhysicsMode.REAL_WORLD_PROFILE, physics.mode());
        assertTrue(physics.hasAircraftProfile());
        assertEquals(128.56F, physics.wingLoadingKgPerM2(), 0.01F);
        assertEquals(0.3436F, physics.powerToWeightKwPerKg(), 1.0E-4F);
    }

    @Test
    void newValuesCoexistWithLegacyOnesWithoutTheLegacyOnesBeingReinterpreted()
    {
        VehicleType type = vehicle(
            "Mass 1.0",
            "MaxThrottle 0.45",
            "RealMassKg 2400",
            "RealEnginePowerKw 140",
            "RealMaxSpeedKmh 113");
        assertEquals(1F, type.getMass(), "the legacy field is still the legacy field");
        assertEquals(2400F, type.getResolvedPhysics().massKg(), "the new model uses the new field");
        assertTrue(type.getResolvedPhysics().hasGroundPropulsion());
    }

    // --------------------------------------------------------- overrides

    @Test
    void independentOverridesActivateWithoutAPropulsionProfile()
    {
        VehicleType slope = vehicle("RealMaxSlopeDeg 35");
        assertEquals(EnumVehiclePhysicsMode.LEGACY_WITH_OVERRIDES, slope.getResolvedPhysics().mode());
        assertFalse(slope.getResolvedPhysics().hasGroundPropulsion());
        assertTrue(slope.getResolvedPhysics().hasSlopeLimit());

        VehicleType reverse = vehicle("MaxNegativeThrottle 0.25", "RealMaxReverseSpeedKmh 8");
        assertTrue(reverse.getResolvedPhysics().hasReverseSpeedOverride());
        assertFalse(reverse.getResolvedPhysics().hasGroundPropulsion());

        VehicleType drive = vehicle("DriveType AWD");
        assertTrue(drive.getResolvedPhysics().driveTypeExplicit());
        assertEquals(EnumDriveType.AWD, drive.getResolvedPhysics().driveType());
    }

    @Test
    void aReverseOverrideCannotEnableReverseOnAVehicleThatForbidsIt()
    {
        VehicleType type = vehicle("MaxNegativeThrottle 0", "RealMaxReverseSpeedKmh 8");
        assertFalse(type.getResolvedPhysics().hasReverseSpeedOverride());
        assertEquals(EnumVehiclePhysicsMode.LEGACY, type.getResolvedPhysics().mode());
    }

    @Test
    void anAbsentDriveTypeIsInferredButStaysImplicitSoTractionIsUnchanged()
    {
        assertEquals(EnumDriveType.TRACKED, vehicle("Tank true").getResolvedPhysics().driveType());
        assertEquals(EnumDriveType.AWD, vehicle("FourWheelDrive true").getResolvedPhysics().driveType());
        assertEquals(EnumDriveType.RWD, vehicle("Model Jeep").getResolvedPhysics().driveType());
        assertFalse(vehicle("Tank true").getResolvedPhysics().driveTypeExplicit());
    }

    @Test
    void draftOnlyAppliesToAHullThatFloats()
    {
        assertTrue(vehicle("Boat", "RealDraftM 1.5").getResolvedPhysics().hasDraft());
        assertFalse(vehicle("RealDraftM 1.5").getResolvedPhysics().hasDraft());
    }

    // ---------------------------------------------------------- geometry

    @Test
    void geometryIsDerivedFromTheCoreBoxAndWheelPositionsWithNoNewKeys()
    {
        // The S-100's real core box and hull wheel layout.
        VehicleType type = vehicle(
            "SetupPart core 8000 -247 -25 -42 561 86 84",
            "WheelPosition 0 -150 10 -18",
            "WheelPosition 1 -150 10 18",
            "WheelPosition 2 150 10 18",
            "WheelPosition 3 150 10 -18");
        assertEquals(35.0625F, type.getResolvedPhysics().geometry().lengthM(), 1.0E-3F);
        assertEquals(5.25F, type.getResolvedPhysics().geometry().widthM(), 1.0E-3F);
        // 300 model pixels of wheel spread is 18.75 blocks.
        assertEquals(18.75F, type.getResolvedPhysics().geometry().wheelbaseM(), 1.0E-3F);
        assertEquals(2.25F, type.getResolvedPhysics().geometry().trackWidthM(), 1.0E-3F);
    }

    // ------------------------------------------------------ malformed data

    /**
     * Malformed values are covered exhaustively in RealWorldSpecReaderTest, which
     * can exercise them without the parser's logging path. Here the concern is
     * only that an incomplete profile leaves legacy propulsion entirely alone.
     */
    @Test
    void anIncompleteProfileLeavesLegacyPropulsionUntouched()
    {
        VehicleType type = vehicle(
            "MaxThrottle 0.45",
            "RealMassKg 2400",
            "RealEnginePowerKw 140");
        assertEquals(0.45F, type.getMaxThrottle());
        assertEquals(EnumVehiclePhysicsMode.LEGACY, type.getResolvedPhysics().mode());
        assertFalse(type.getResolvedPhysics().hasGroundPropulsion());
        assertEquals(0F, type.getResolvedPhysics().massKg(),
            "a partial profile must not leak half-derived values into the runtime");
    }

    // ------------------------------------------------------------ helpers

    private static VehicleType vehicle(String... lines)
    {
        VehicleType type = new VehicleType();
        type.read(file(EnumType.VEHICLE, "testVehicle", lines));
        return type;
    }

    private static PlaneType plane(String... lines)
    {
        PlaneType type = new PlaneType();
        type.read(file(EnumType.PLANE, "testPlane", lines));
        return type;
    }

    /** A driver line keeps the parser off its logging path, as in the other type tests. */
    private static TypeFile file(EnumType type, String name, String... lines)
    {
        List<String> definition = new ArrayList<>(List.of("Driver 0 0 0"));
        definition.addAll(List.of(lines));
        IContentProvider pack = new ContentPack("test", Path.of("build", "test-packs", "test"));
        return new TypeFile(name, type, pack, definition);
    }
}
