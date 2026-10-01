package com.flansmodultimate.common.driveables.armor;

/**
 * Authored nominal thickness and virtual slope. Both values use real-world units.
 *
 * <p>{@code heatThicknessMm} is the same face's protection against a shaped-charge (HEAT) jet. Composite and
 * spaced arrays resist a jet far better than their equivalent against kinetic rounds, so a definition may author it
 * separately; it shares the plate's slope and otherwise equals {@code thicknessMm}.</p>
 */
public record ArmorPlate(float thicknessMm, float slopeDeg, float heatThicknessMm)
{
    public static final ArmorPlate UNARMOURED = new ArmorPlate(0F, 0F);

    public ArmorPlate(float thicknessMm, float slopeDeg)
    {
        this(thicknessMm, slopeDeg, thicknessMm);
    }

    public boolean isArmoured()
    {
        return Float.isFinite(thicknessMm) && thicknessMm > 0F;
    }

    /** Nominal thickness a projectile of the given warhead has to defeat. */
    public float thicknessAgainst(boolean shapedCharge)
    {
        return shapedCharge ? heatThicknessMm : thicknessMm;
    }

    /** Whether the face was authored with protection against HEAT that differs from its kinetic value. */
    public boolean hasDistinctHeatProtection()
    {
        return Float.compare(heatThicknessMm, thicknessMm) != 0;
    }

    public ArmorPlate withHeatThickness(float heatMm)
    {
        return new ArmorPlate(thicknessMm, slopeDeg, heatMm);
    }
}
