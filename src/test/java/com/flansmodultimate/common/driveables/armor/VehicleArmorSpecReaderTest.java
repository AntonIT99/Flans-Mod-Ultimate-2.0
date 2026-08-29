package com.flansmodultimate.common.driveables.armor;

import com.flansmodultimate.ContentPack;
import com.flansmodultimate.IContentProvider;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.types.EnumType;
import com.flansmodultimate.common.types.TypeFile;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

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

    private static VehicleArmorSpecReader.Result read(String... lines)
    {
        IContentProvider pack = new ContentPack("test", Path.of("build", "test-packs", "armor"));
        return VehicleArmorSpecReader.read(new TypeFile("synthetic", EnumType.VEHICLE, pack, List.of(lines)));
    }
}
