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
    /**
     * Whether the projectile met armour. Read from the resolved thickness rather than the authored plate, so a
     * face given no protection against HEAT counts as unarmoured for a HEAT hit even when it stops kinetic rounds.
     */
    public boolean isArmoured()
    {
        return Float.isFinite(effectiveArmorMm) && effectiveArmorMm > 0F;
    }
}
