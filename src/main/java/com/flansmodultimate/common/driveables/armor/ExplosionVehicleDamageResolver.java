package com.flansmodultimate.common.driveables.armor;

import net.minecraft.util.Mth;

/** Pure separation of blast and fragmentation channels for one resolved vehicle part. */
public final class ExplosionVehicleDamageResolver
{
    private ExplosionVehicleDamageResolver() {}

    public record DamageChannels(float blastDamage, float fragmentationDamage,
                                 double pressureKPa, float blastMultiplier)
    {
        public float totalDamage()
        {
            return blastDamage + fragmentationDamage;
        }
    }

    public static DamageChannels resolve(float nominalArmorMm, Float explosiveMassKg,
                                         double actualDistanceMeters, float existingBlastDamage,
                                         float existingFragmentationDamage,
                                         double resistanceKPaPerMm, double minimumDistanceMeters)
    {
        float blast = finiteNonNegative(existingBlastDamage);
        float fragmentation = finiteNonNegative(existingFragmentationDamage);
        if (!(nominalArmorMm > 0F) || !Float.isFinite(nominalArmorMm))
            return new DamageChannels(blast, fragmentation, 0D, blast > 0F ? 1F : 0F);

        // Any nominal armour blocks the simple fragmentation channel.
        fragmentation = 0F;
        if (explosiveMassKg == null || !Float.isFinite(explosiveMassKg) || explosiveMassKg <= 0F)
            return new DamageChannels(0F, 0F, 0D, 0F);

        double resistance = Double.isFinite(resistanceKPaPerMm) && resistanceKPaPerMm > 0D
            ? resistanceKPaPerMm : 150D;
        double minimumDistance = Double.isFinite(minimumDistanceMeters) && minimumDistanceMeters > 0D
            ? minimumDistanceMeters : 0.5D;
        double pressure = peakPressureKPa(explosiveMassKg, actualDistanceMeters, minimumDistance);
        double required = resistance * nominalArmorMm;
        if (!Double.isFinite(required) || pressure <= required)
            return new DamageChannels(0F, 0F, pressure, 0F);

        double ratio = pressure / required;
        float multiplier = (float) Mth.clamp((ratio - 1D) / 2D, 0D, 1D);
        return new DamageChannels(blast * multiplier, 0F, pressure, multiplier);
    }

    public static double peakPressureKPa(double explosiveMassKg, double actualDistanceMeters,
                                         double minimumDistanceMeters)
    {
        if (!Double.isFinite(explosiveMassKg) || explosiveMassKg <= 0D)
            return 0D;
        double minimum = Double.isFinite(minimumDistanceMeters) && minimumDistanceMeters > 0D
            ? minimumDistanceMeters : 0.5D;
        double distance = Double.isFinite(actualDistanceMeters)
            ? Math.max(actualDistanceMeters, minimum) : minimum;
        double scaledDistance = distance / Math.cbrt(explosiveMassKg);
        if (!Double.isFinite(scaledDistance) || scaledDistance <= 0D)
            return 0D;
        double pressure = 1772D / Math.pow(scaledDistance, 3D)
            + 114D / Math.pow(scaledDistance, 2D)
            + 108D / scaledDistance;
        if (Double.isNaN(pressure) || pressure < 0D)
            return 0D;
        return Double.isInfinite(pressure) ? Double.MAX_VALUE : pressure;
    }

    private static float finiteNonNegative(float value)
    {
        return Float.isFinite(value) ? Math.max(0F, value) : 0F;
    }
}
