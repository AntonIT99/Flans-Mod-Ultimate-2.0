package com.flansmodultimate.common.driveables.armor;

/** Authored nominal thickness and virtual slope. Both values use real-world units. */
public record ArmorPlate(float thicknessMm, float slopeDeg)
{
    public static final ArmorPlate UNARMOURED = new ArmorPlate(0F, 0F);

    public boolean isArmoured()
    {
        return Float.isFinite(thicknessMm) && thicknessMm > 0F;
    }
}
