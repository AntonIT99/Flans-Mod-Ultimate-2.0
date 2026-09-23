package com.flansmodultimate.common.driveables.armor;

import net.minecraft.world.phys.Vec3;

/** Semantic faces in a driveable's collision frame. Front is local -Z. */
public enum EnumArmorFacing
{
    FRONT(new Vec3(0D, 0D, -1D)),
    REAR(new Vec3(0D, 0D, 1D)),
    LEFT(new Vec3(-1D, 0D, 0D)),
    RIGHT(new Vec3(1D, 0D, 0D)),
    TOP(new Vec3(0D, 1D, 0D)),
    BOTTOM(new Vec3(0D, -1D, 0D));

    private final Vec3 outwardNormal;

    EnumArmorFacing(Vec3 outwardNormal)
    {
        this.outwardNormal = outwardNormal;
    }

    public Vec3 outwardNormal()
    {
        return outwardNormal;
    }

    public static EnumArmorFacing fromOutwardNormal(Vec3 normal)
    {
        if (normal == null)
            return FRONT;
        double x = Math.abs(normal.x);
        double y = Math.abs(normal.y);
        double z = Math.abs(normal.z);
        if (y >= x && y >= z)
            return normal.y >= 0D ? TOP : BOTTOM;
        if (x >= z)
            return normal.x >= 0D ? RIGHT : LEFT;
        return normal.z >= 0D ? REAR : FRONT;
    }
}
