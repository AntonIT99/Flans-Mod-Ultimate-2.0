package com.flansmodultimate.common.driveables.physics;

import com.flansmodultimate.ContentPack;
import com.flansmodultimate.IContentProvider;
import com.flansmodultimate.common.types.EnumType;
import com.flansmodultimate.common.types.TypeFile;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The reader has to be safe against anything a content pack can contain. A
 * malformed optional value must warn and be dropped, never abort loading and
 * never turn into an arbitrary physics number.
 */
class RealWorldSpecReaderTest
{
    @Test
    void aDefinitionWithNoRealKeysProducesTheEmptySpec()
    {
        RealWorldSpecReader.Result result = read("Model Spitfire", "MaxThrottle 1.0");
        assertTrue(result.spec().isEmpty());
        assertFalse(result.hasWarnings());
    }

    @Test
    void legacyMassIsNotReadAsARealWorldMass()
    {
        // Six shipped vehicle definitions declare "Mass 1.0" with the legacy
        // meaning. Reading that as one kilogram would be catastrophic.
        RealWorldSpecReader.Result result = read("Mass 1.0");
        assertNull(result.spec().massKg());
        assertTrue(result.spec().isEmpty());
    }

    @Test
    void legacyMaxSpeedIsNotReadAsKilometresPerHour()
    {
        RealWorldSpecReader.Result result = read("MaxSpeed 2.0", "WingArea 1.0", "EnginePower 10");
        assertNull(result.spec().maxSpeedKmh());
        assertNull(result.spec().enginePowerKw());
        assertNull(result.spec().aircraft().wingAreaM2());
        assertTrue(result.spec().isEmpty());
    }

    @Test
    void readsEveryCommonAndCategoryKey()
    {
        RealWorldSpecReader.Result result = read(
            "RealMassKg 2890",
            "RealMaxSpeedKmh 635",
            "RealEnginePowerKw 993",
            "RealEngineThrustKn 79.6",
            "RealWingSpanM 11.23",
            "RealWingAreaM2 22.48",
            "RealClimbRateMs 17.0",
            "DriveType TRACKED",
            "RealMaxReverseSpeedKmh 8",
            "RealDraftM 1.5");

        assertEquals(2890F, result.spec().massKg());
        assertEquals(635F, result.spec().maxSpeedKmh());
        assertEquals(993F, result.spec().enginePowerKw());
        assertEquals(79.6F, result.spec().engineThrustKn());
        assertEquals(11.23F, result.spec().aircraft().wingSpanM());
        assertEquals(22.48F, result.spec().aircraft().wingAreaM2());
        assertEquals(17F, result.spec().aircraft().climbRateMs());
        assertEquals(EnumDriveType.TRACKED, result.spec().ground().driveType());
        assertEquals(8F, result.spec().ground().maxReverseSpeedKmh());
        assertEquals(1.5F, result.spec().marine().draftM());
        // Declaring both power and thrust is legal but worth a note.
        assertTrue(result.hasWarnings());
    }

    @Test
    void enginePowerHpIsConvertedToKilowatts()
    {
        // 993 kW is the Spitfire's rating; roughly 1332 hp is the horsepower
        // figure most reference books actually print for the same engine.
        RealWorldSpecReader.Result result = read("RealEnginePowerHp 1332");
        assertEquals(993.35F, result.spec().enginePowerKw(), 0.5F);
        assertFalse(result.hasWarnings());
    }

    @Test
    void enginePowerHpOverridesEnginePowerKwWhenBothAreDeclared()
    {
        RealWorldSpecReader.Result result = read("RealEnginePowerKw 500", "RealEnginePowerHp 1332");
        assertEquals(993.35F, result.spec().enginePowerKw(), 0.5F);
    }

    @Test
    void enginePowerHpIsValidatedTheSameWayAsEnginePowerKw()
    {
        assertNull(read("RealEnginePowerHp not-a-number").spec().enginePowerKw());
        assertNull(read("RealEnginePowerHp -5").spec().enginePowerKw());
        assertNull(read("RealEnginePowerHp 0").spec().enginePowerKw());
        assertTrue(read("RealEnginePowerHp not-a-number").hasWarnings());
    }

    @Test
    void enginePowerPSIsConvertedToKilowatts()
    {
        // 993 kW is the Spitfire's rating; roughly 1350 PS is the metric
        // horsepower figure a German-language reference would print for it.
        RealWorldSpecReader.Result result = read("RealEnginePowerPS 1350");
        assertEquals(993.0F, result.spec().enginePowerKw(), 0.5F);
        assertFalse(result.hasWarnings());
    }

    @Test
    void enginePowerPSOverridesBothEnginePowerKwAndEnginePowerHpWhenAllAreDeclared()
    {
        RealWorldSpecReader.Result result = read(
            "RealEnginePowerKw 500", "RealEnginePowerHp 1332", "RealEnginePowerPS 1350");
        assertEquals(993.0F, result.spec().enginePowerKw(), 0.5F);
    }

    @Test
    void enginePowerPSIsValidatedTheSameWayAsEnginePowerKw()
    {
        assertNull(read("RealEnginePowerPS not-a-number").spec().enginePowerKw());
        assertNull(read("RealEnginePowerPS -5").spec().enginePowerKw());
        assertNull(read("RealEnginePowerPS 0").spec().enginePowerKw());
        assertTrue(read("RealEnginePowerPS not-a-number").hasWarnings());
    }

    @Test
    void keysArMatchedCaseInsensitivelyLikeEveryOtherLegacyKey()
    {
        RealWorldSpecReader.Result result = read("realmasskg 2890", "REALMAXSPEEDKMH 635");
        assertEquals(2890F, result.spec().massKg());
        assertEquals(635F, result.spec().maxSpeedKmh());
    }

    @Test
    void aKeyPresentWithoutAValueMeansNotSpecified()
    {
        RealWorldSpecReader.Result result = read("RealMassKg", "RealMaxSpeedKmh   ");
        assertNull(result.spec().massKg());
        assertNull(result.spec().maxSpeedKmh());
        assertFalse(result.hasWarnings());
    }

    @Test
    void trailingCommentsAreIgnored()
    {
        RealWorldSpecReader.Result result = read("RealMassKg 2890 // empty weight");
        assertEquals(2890F, result.spec().massKg());
        assertFalse(result.hasWarnings());
    }

    @Test
    void nonNumericValuesWarnAndAreDropped()
    {
        RealWorldSpecReader.Result result = read("RealMassKg heavy");
        assertNull(result.spec().massKg());
        assertEquals(1, result.warnings().size());
        assertTrue(result.warnings().get(0).contains("RealMassKg"));
    }

    @Test
    void zeroNegativeAndNonFiniteValuesWarnAndAreDropped()
    {
        assertNull(read("RealMassKg 0").spec().massKg());
        assertNull(read("RealMassKg -100").spec().massKg());
        assertNull(read("RealMassKg NaN").spec().massKg());
        assertNull(read("RealMassKg Infinity").spec().massKg());
        assertNull(read("RealEnginePowerKw -5").spec().enginePowerKw());
        assertNull(read("RealEngineThrustKn -5").spec().engineThrustKn());
        assertNull(read("RealMaxSpeedKmh -5").spec().maxSpeedKmh());
        assertNull(read("RealWingAreaM2 0").spec().aircraft().wingAreaM2());
        assertNull(read("RealDraftM -1").spec().marine().draftM());
        assertTrue(read("RealMassKg 0").hasWarnings());
    }

    @Test
    void unknownDriveTypesWarnAndFallBackWithoutCrashing()
    {
        RealWorldSpecReader.Result result = read("DriveType HOVERCRAFT");
        assertNull(result.spec().ground().driveType());
        assertEquals(1, result.warnings().size());
        assertTrue(result.spec().isEmpty());
    }

    @Test
    void removedMaxSlopeKeyNoLongerActivatesPhysics()
    {
        RealWorldSpecReader.Result result = read("RealMaxSlopeDeg 35");
        assertTrue(result.spec().isEmpty());
        assertFalse(result.hasWarnings());
    }

    @Test
    void driveTypeAcceptsTheCommonSpellings()
    {
        assertEquals(EnumDriveType.AWD, read("DriveType 4WD").spec().ground().driveType());
        assertEquals(EnumDriveType.AWD, read("DriveType all-wheel-drive").spec().ground().driveType());
        assertEquals(EnumDriveType.TRACKED, read("DriveType tracks").spec().ground().driveType());
        assertEquals(EnumDriveType.RWD, read("DriveType rwd").spec().ground().driveType());
        assertEquals(EnumDriveType.FWD, read("DriveType FWD").spec().ground().driveType());
    }

    @Test
    void theLastDeclarationWinsAsItDoesForEveryOtherKey()
    {
        RealWorldSpecReader.Result result = read("RealMassKg 1000", "RealMassKg 2890");
        assertEquals(2890F, result.spec().massKg());
    }

    @Test
    void completenessRulesMatchTheDocumentedRequiredSets()
    {
        RealWorldVehicleSpec ground = read(
            "RealMassKg 2400", "RealEnginePowerKw 140", "RealMaxSpeedKmh 113").spec();
        assertTrue(ground.hasCompleteGroundProfile());
        assertFalse(ground.hasCompleteAircraftProfile());

        RealWorldVehicleSpec partialGround = read("RealMassKg 2400", "RealEnginePowerKw 140").spec();
        assertFalse(partialGround.hasCompleteGroundProfile());

        RealWorldVehicleSpec aircraft = read(
            "RealMassKg 2890", "RealMaxSpeedKmh 635",
            "RealWingSpanM 11.23", "RealWingAreaM2 22.48", "RealEnginePowerKw 993").spec();
        assertTrue(aircraft.hasCompleteAircraftProfile());

        RealWorldVehicleSpec jet = read(
            "RealMassKg 12700", "RealMaxSpeedKmh 2120",
            "RealWingSpanM 9.96", "RealWingAreaM2 30", "RealEngineThrustKn 79.6").spec();
        assertTrue(jet.hasCompleteAircraftProfile(), "thrust alone must satisfy the propulsion requirement");

        RealWorldVehicleSpec noClimbRate = read(
            "RealMassKg 2890", "RealMaxSpeedKmh 635",
            "RealWingSpanM 11.23", "RealWingAreaM2 22.48", "RealEnginePowerKw 993").spec();
        assertNull(noClimbRate.aircraft().climbRateMs());
        assertTrue(noClimbRate.hasCompleteAircraftProfile(), "climb rate is a calibration input, not a requirement");
    }

    @Test
    void aNullFileIsToleratedRatherThanThrowing()
    {
        RealWorldSpecReader.Result result = RealWorldSpecReader.read(null);
        assertNotNull(result);
        assertTrue(result.spec().isEmpty());
    }

    private static RealWorldSpecReader.Result read(String... lines)
    {
        IContentProvider pack = new ContentPack("test", Path.of("build", "test-packs", "test"));
        return RealWorldSpecReader.read(new TypeFile("testVehicle", EnumType.VEHICLE, pack, List.of(lines)));
    }
}
