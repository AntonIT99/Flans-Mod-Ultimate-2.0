package com.flansmodultimate.common.guns.penetration;

import org.jetbrains.annotations.Nullable;

/** Velocity-scaled P100 penetration and definition-time 100 metre velocity reference. */
public final class PenetrationCalculator
{
    public static final double METRES_PER_BLOCK = 1D;
    public static final double TICKS_PER_SECOND = 20D;

    private PenetrationCalculator() {}

    public static PenetrationResult resolve(@Nullable Float penetrationAt100m,
                                            double impactVelocityMetersPerSecond,
                                            double referenceVelocityAt100mMetersPerSecond,
                                            double exponent, float effectiveArmorMm)
    {
        if (!(effectiveArmorMm > 0F) || !Float.isFinite(effectiveArmorMm))
            return new PenetrationResult(true, false, 0F, 0F, 0F);

        float penetration = currentPenetrationMm(penetrationAt100m, impactVelocityMetersPerSecond,
            referenceVelocityAt100mMetersPerSecond, exponent);
        float overmatch = penetration > 0F ? penetration / effectiveArmorMm : 0F;
        boolean penetrated = Float.isFinite(overmatch) && penetration >= effectiveArmorMm;
        return new PenetrationResult(penetrated, true, penetration, effectiveArmorMm, overmatch);
    }

    public static float currentPenetrationMm(@Nullable Float penetrationAt100m,
                                             double impactVelocityMetersPerSecond,
                                             double referenceVelocityAt100mMetersPerSecond,
                                             double exponent)
    {
        if (penetrationAt100m == null || !Float.isFinite(penetrationAt100m) || penetrationAt100m <= 0F
            || !Double.isFinite(impactVelocityMetersPerSecond) || impactVelocityMetersPerSecond <= 0D
            || !Double.isFinite(referenceVelocityAt100mMetersPerSecond)
            || referenceVelocityAt100mMetersPerSecond <= 0D)
            return 0F;
        double safeExponent = Double.isFinite(exponent) && exponent > 0D ? exponent : 1.43D;
        double penetration = penetrationAt100m
            * Math.pow(impactVelocityMetersPerSecond / referenceVelocityAt100mMetersPerSecond, safeExponent);
        return Double.isFinite(penetration) && penetration > 0D
            ? (float) Math.min(penetration, Float.MAX_VALUE) : 0F;
    }

    /**
     * Steps the authored air-drag model once at definition finalization. Gravity
     * is deliberately excluded: P100 describes retained forward velocity, not
     * speed gained by falling.
     */
    public static float referenceVelocityAt100m(float initialVelocityBlocksPerTick, float dragInAir)
    {
        if (!Float.isFinite(initialVelocityBlocksPerTick) || initialVelocityBlocksPerTick <= 0F
            || !Float.isFinite(dragInAir) || dragInAir < 0F || dragInAir > 1F)
            return 0F;
        double speed = initialVelocityBlocksPerTick;
        double travelled = 0D;
        for (int tick = 0; tick < 100_000 && travelled < 100D; tick++)
        {
            travelled += speed * METRES_PER_BLOCK;
            if (travelled >= 100D)
                return (float) (speed * TICKS_PER_SECOND);
            speed *= dragInAir;
            if (!Double.isFinite(speed) || speed < 1.0E-9D)
                return 0F;
        }
        return 0F;
    }
}
