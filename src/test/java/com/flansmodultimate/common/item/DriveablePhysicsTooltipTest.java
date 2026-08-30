package com.flansmodultimate.common.item;

import com.flansmodultimate.common.driveables.armor.ArmorPlate;
import com.flansmodultimate.common.driveables.armor.EnumArmorFacing;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
