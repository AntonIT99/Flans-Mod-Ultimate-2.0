package com.flansmodultimate.common.driveables.armor;

import net.minecraft.world.phys.Vec3;

/** Immutable definition-time plate metadata. */
public record ResolvedArmorPlate(ArmorPlate authored, Vec3 virtualNormal)
{
    public static ResolvedArmorPlate unarmoured(EnumArmorFacing facing)
    {
        return new ResolvedArmorPlate(ArmorPlate.UNARMOURED, facing.outwardNormal());
    }
}
