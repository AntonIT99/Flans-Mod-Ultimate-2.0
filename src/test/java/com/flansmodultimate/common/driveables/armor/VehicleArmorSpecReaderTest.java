package com.flansmodultimate.common.driveables.armor;

import com.flansmodultimate.ContentPack;
import com.flansmodultimate.IContentProvider;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.types.EnumType;
import com.flansmodultimate.common.types.TypeFile;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class VehicleArmorSpecReaderTest
{
    @Test
    void readsThicknessAndOptionalVirtualSlopeFromOneSemanticLine()
    {
        VehicleArmorSpec spec = read("ArmorFrontMm 80 55", "ArmorRearMm 40").spec();
        assertEquals(new ArmorPlate(80F, 55F), spec.hull().get(EnumArmorFacing.FRONT));
        assertEquals(new ArmorPlate(40F, 0F), spec.hull().get(EnumArmorFacing.REAR));
    }

    @Test
    void sideKeysPopulateBothSemanticSides()
    {
        VehicleArmorSpec spec = read("ArmorSideMm 40 10", "TurretArmorSideMm 25").spec();
        assertEquals(spec.hull().get(EnumArmorFacing.LEFT), spec.hull().get(EnumArmorFacing.RIGHT));
        assertEquals(spec.turret().get(EnumArmorFacing.LEFT), spec.turret().get(EnumArmorFacing.RIGHT));
    }

    @Test
    void explicitZeroRemainsPresentAndDistinctFromMissing()
    {
        VehicleArmorSpec spec = read("ArmorTopMm 0").spec();
        assertTrue(spec.hull().containsKey(EnumArmorFacing.TOP));
        assertEquals(0F, spec.hull().get(EnumArmorFacing.TOP).thicknessMm());
        assertFalse(spec.hull().containsKey(EnumArmorFacing.BOTTOM));
    }

    @Test
    void readsUniformPartOverridesAndLastDeclarationWins()
    {
        VehicleArmorSpec spec = read("PartArmorMm leftTrack 15", "PartArmorMm leftTrack 0").spec();
        assertEquals(new ArmorPlate(0F, 0F), spec.partOverrides().get(EnumDriveablePart.LEFT_TRACK));
    }

    @Test
    void invalidValuesWarnAndRemainAbsent()
    {
        VehicleArmorSpecReader.Result result = read(
            "ArmorFrontMm -1", "ArmorRearMm NaN", "ArmorTopMm 20 95", "PartArmorMm imaginary 10");
        assertTrue(result.spec().isEmpty());
        assertEquals(4, result.warnings().size());
    }

    @Test
    void aNavalKeyCoversEveryPartOfItsStructure()
    {
        VehicleArmorSpec spec = read("ArmorDeckMm 121", "ArmorBulkheadMm 287").spec();
        assertEquals(new ArmorPlate(121F, 0F), spec.partOverrides().get(EnumDriveablePart.DECK));
        assertEquals(new ArmorPlate(121F, 0F), spec.partOverrides().get(EnumDriveablePart.DECK_2));
        assertEquals(new ArmorPlate(121F, 0F), spec.partOverrides().get(EnumDriveablePart.DECK_3));
        assertEquals(new ArmorPlate(287F, 0F), spec.partOverrides().get(EnumDriveablePart.BULKHEAD));
        assertEquals(new ArmorPlate(287F, 0F), spec.partOverrides().get(EnumDriveablePart.BULKHEAD_2));
    }

    @Test
    void aNavalKeyAcceptsAnInclinedPlate()
    {
        VehicleArmorSpec spec = read("ArmorBeltMm 310 19").spec();
        assertEquals(new ArmorPlate(310F, 19F), spec.partOverrides().get(EnumDriveablePart.BELT));
        assertEquals(new ArmorPlate(310F, 19F), spec.partOverrides().get(EnumDriveablePart.PORT));
        assertEquals(new ArmorPlate(310F, 19F), spec.partOverrides().get(EnumDriveablePart.STARBOARD));
    }

    @Test
    void machineryCoversEveryEngineAndBoilerRoomAndTheSteeringGear()
    {
        VehicleArmorSpec spec = read("ArmorMachineryMm 40").spec();
        assertEquals(new ArmorPlate(40F, 0F), spec.partOverrides().get(EnumDriveablePart.ENGINE_ROOM_1));
        assertEquals(new ArmorPlate(40F, 0F), spec.partOverrides().get(EnumDriveablePart.ENGINE_ROOM_8));
        assertEquals(new ArmorPlate(40F, 0F), spec.partOverrides().get(EnumDriveablePart.BOILER_ROOM_1));
        assertEquals(new ArmorPlate(40F, 0F), spec.partOverrides().get(EnumDriveablePart.STEERING));
    }

    @Test
    void anExplicitPartOverrideBeatsTheNavalFamilyItBelongsTo()
    {
        VehicleArmorSpec after = read("ArmorDeckMm 121", "PartArmorMm deck2 37").spec();
        assertEquals(new ArmorPlate(121F, 0F), after.partOverrides().get(EnumDriveablePart.DECK));
        assertEquals(new ArmorPlate(37F, 0F), after.partOverrides().get(EnumDriveablePart.DECK_2));

        VehicleArmorSpec before = read("PartArmorMm deck2 37", "ArmorDeckMm 121").spec();
        assertEquals(new ArmorPlate(37F, 0F), before.partOverrides().get(EnumDriveablePart.DECK_2),
            "the more specific key must win regardless of declaration order");
    }

    @Test
    void everyPartBelongsToAtMostOneNavalFamily()
    {
        // The families are applied in map order, so two keys claiming the same
        // part would make the result depend on that order. Keep them disjoint.
        Set<EnumDriveablePart> seen = EnumSet.noneOf(EnumDriveablePart.class);
        for (String key : VehicleArmorSpecReader.navalKeys())
        {
            VehicleArmorSpec spec = read(key + " 100").spec();
            assertFalse(spec.partOverrides().isEmpty(), key + " covers no part at all");
            for (EnumDriveablePart part : spec.partOverrides().keySet())
                assertTrue(seen.add(part), part + " is claimed by more than one naval key");
        }
    }

    @Test
    void navalKeysAreAbsentFromADefinitionThatDoesNotUseThem()
    {
        assertTrue(read("ArmorFrontMm 80 55").spec().partOverrides().isEmpty());
    }

    private static VehicleArmorSpecReader.Result read(String... lines)
    {
        IContentProvider pack = new ContentPack("test", Path.of("build", "test-packs", "armor"));
        return VehicleArmorSpecReader.read(new TypeFile("synthetic", EnumType.VEHICLE, pack, List.of(lines)));
    }
}
