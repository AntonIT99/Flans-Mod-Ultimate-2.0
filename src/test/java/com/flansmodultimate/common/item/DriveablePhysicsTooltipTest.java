package com.flansmodultimate.common.item;

import com.flansmodultimate.ContentPack;
import com.flansmodultimate.IContentProvider;
import com.flansmodultimate.common.driveables.armor.ArmorPlate;
import com.flansmodultimate.common.driveables.armor.EnumArmorFacing;
import com.flansmodultimate.common.types.EnumType;
import com.flansmodultimate.common.types.TypeFile;
import com.flansmodultimate.common.types.VehicleType;
import org.junit.jupiter.api.Test;

import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DriveablePhysicsTooltipTest
{
    @Test
    void armorSummaryUsesFrontSideRearOrderAndCompactSlopeNotation()
    {
        EnumMap<EnumArmorFacing, ArmorPlate> plates = new EnumMap<>(EnumArmorFacing.class);
        plates.put(EnumArmorFacing.FRONT, new ArmorPlate(100F, 9F));
        plates.put(EnumArmorFacing.LEFT, new ArmorPlate(80F, 0F));
        plates.put(EnumArmorFacing.RIGHT, new ArmorPlate(80F, 0F));
        plates.put(EnumArmorFacing.REAR, new ArmorPlate(80F, 9F));

        assertEquals("100 (9°) / 80 / 80 (9°)\u00a0mm",
            DriveablePhysicsTooltip.formatArmorSummary(plates));
    }

    @Test
    void armorSummaryMarksMissingSemanticFaces()
    {
        EnumMap<EnumArmorFacing, ArmorPlate> plates = new EnumMap<>(EnumArmorFacing.class);
        plates.put(EnumArmorFacing.FRONT, new ArmorPlate(100F, 0F));

        assertEquals("100 / — / —\u00a0mm", DriveablePhysicsTooltip.formatArmorSummary(plates));
    }

    @Test
    void legacyDriveablesAlwaysShowSummedTotalHp()
    {
        IContentProvider pack = new ContentPack("test", Path.of("build", "test-packs", "tooltip-health"));
        TestVehicleType type = new TestVehicleType();
        type.readDefinition(new TypeFile("synthetic", EnumType.VEHICLE, pack, List.of(
            "Driver 0 0 0",
            "SetupPart core 1000 0 0 0 16 16 16",
            "SetupPart turret 250 0 16 0 16 16 16")));

        List<Component> tooltip = new ArrayList<>();
        DriveablePhysicsTooltip.append(type, tooltip);

        assertTrue(tooltip.stream().map(Component::getString)
            .anyMatch(line -> line.equals(TooltipKeys.PHYSICS_TOTAL_HP + ": 1250")));
    }

    private static final class TestVehicleType extends VehicleType
    {
        private void readDefinition(TypeFile file)
        {
            read(file);
        }
    }
}
