package com.flansmodultimate.common.driveables;

import com.flansmodultimate.common.driveables.armor.ArmorPlate;
import com.flansmodultimate.common.driveables.armor.EnumArmorFacing;
import com.flansmodultimate.common.driveables.armor.ResolvedArmorHit;
import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DriveableDamageDebugTest
{
    @Test
    void armorDescriptionNamesTheFaceTheNominalPlateAndTheImpactAngle()
    {
        ResolvedArmorHit hit = new ResolvedArmorHit(EnumDriveablePart.CORE, EnumArmorFacing.FRONT,
            new ArmorPlate(80F, 55F), new Vec3(0D, 0D, -1D), 55F, 139.47F);
        assertEquals("effective armor 139.47 mm: front face, 80.00 mm nominal at 55.0 deg",
            DriveableDamageDebug.describeArmor(hit));
    }
}
