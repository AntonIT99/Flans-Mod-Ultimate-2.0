package com.flansmodultimate.common.driveables.armor;

import com.flansmodultimate.common.driveables.EnumDriveablePart;
import net.minecraft.world.phys.Vec3;

/** Per-hit armour values derived from immutable plate metadata and the incoming ray. */
public record ResolvedArmorHit(
    EnumDriveablePart part,
    EnumArmorFacing facing,
    ArmorPlate authored,
    Vec3 virtualNormal,
    float impactAngleDeg,
    float effectiveArmorMm)
{
    public boolean isArmoured()
    {
        return authored.isArmoured();
    }
}
