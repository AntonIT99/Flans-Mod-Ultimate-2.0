package com.flansmodultimate.common.types;

import com.flansmodultimate.ContentPack;
import com.flansmodultimate.IContentProvider;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.driveables.armor.EnumArmorFacing;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DriveableTypeArmorHealthTest
{
    @Test
    void legacyDefinitionsKeepAuthoredHealthAndNoArmourGate()
    {
        VehicleType type = vehicle(
            "SetupPart core 1000 0 0 0 16 16 16",
            "SetupPart turret 250 0 16 0 16 16 16");
        assertEquals(1000F, type.getHealth().get(EnumDriveablePart.CORE).getHealth());
        assertFalse(type.getResolvedHealth().enabled());
        assertEquals(1250F, type.getTotalHp());
        assertFalse(type.getResolvedArmor().isConfigured());
    }

    @Test
    void armourAndNormalizedHealthAreIndependentOptIns()
    {
        VehicleType armouredLegacyHp = vehicle(
            "SetupPart core 1000 0 0 0 16 16 16", "ArmorFrontMm 80 55");
        assertEquals(1000F, armouredLegacyHp.getHealth().get(EnumDriveablePart.CORE).getHealth());
        assertFalse(armouredLegacyHp.getResolvedHealth().enabled());
        assertEquals(80F, armouredLegacyHp.getResolvedArmor()
            .plate(EnumDriveablePart.CORE, EnumArmorFacing.FRONT).authored().thicknessMm());

        VehicleType normalizedSoft = vehicle(
            "SetupPart core 100 0 0 0 16 16 16", "SetupPart turret 50 0 16 0 16 16 16",
            "RealMassKg 1000", "UseRealisticVehicleHealth true");
        assertTrue(normalizedSoft.getResolvedHealth().enabled());
        assertEquals(normalizedSoft.getResolvedHealth().totalHp(), normalizedSoft.getTotalHp());
        assertFalse(normalizedSoft.getResolvedArmor().isConfigured());
    }

    @Test
    void normalizedAllocationIsIdempotentAcrossRepeatedFinalization()
    {
        VehicleType type = vehicle(
            "SetupPart core 100 0 0 0 16 16 16",
            "SetupPart turret 50 0 16 0 16 16 16",
            "SetupPart leftTrack 25 -16 0 0 16 16 16",
            "SetupPart rightTrack 25 16 0 0 16 16 16",
            "SetupPart barrel 0 0 16 -16 16 16 16",
            "RealMassKg 1000", "UseRealisticVehicleHealth true");
        float firstCore = type.getHealth().get(EnumDriveablePart.CORE).getHealth();
        type.finishDerivedValues();
        assertEquals(firstCore, type.getHealth().get(EnumDriveablePart.CORE).getHealth(), 1.0E-4F);
        assertEquals(250F, firstCore, 1.0E-3F);
        assertEquals(0F, type.getHealth().get(EnumDriveablePart.BARREL).getHealth());
    }

    private static VehicleType vehicle(String... lines)
    {
        List<String> definition = new ArrayList<>(List.of("Driver 0 0 0"));
        definition.addAll(List.of(lines));
        IContentProvider pack = new ContentPack("test", Path.of("build", "test-packs", "armor-health"));
        VehicleType type = new VehicleType();
        type.read(new TypeFile("synthetic", EnumType.VEHICLE, pack, definition));
        return type;
    }
}
