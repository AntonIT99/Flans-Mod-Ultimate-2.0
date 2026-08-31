package com.flansmodultimate.common.guns.penetration;

import org.jetbrains.annotations.Nullable;

/** Resolves an authored penetration rating against effective armour thickness. */
public final class PenetrationCalculator
{
    private PenetrationCalculator() {}

    public static PenetrationResult resolve(@Nullable Float penetrationAt100m, float effectiveArmorMm)
    {
        if (effectiveArmorMm <= 0F || !Float.isFinite(effectiveArmorMm))
            return new PenetrationResult(true, false, 0F, 0F, 0F);

        float penetration = currentPenetrationMm(penetrationAt100m);
        float overmatch = penetration > 0F ? penetration / effectiveArmorMm : 0F;
        boolean penetrated = Float.isFinite(overmatch) && penetration >= effectiveArmorMm;
        return new PenetrationResult(penetrated, true, penetration, effectiveArmorMm, overmatch);
    }

    public static float currentPenetrationMm(@Nullable Float penetrationAt100m)
    {
        if (penetrationAt100m == null || !Float.isFinite(penetrationAt100m) || penetrationAt100m <= 0F)
            return 0F;
        return penetrationAt100m;
    }
}
