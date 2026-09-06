package com.flansmodultimate.common.driveables.armor;

import com.flansmodultimate.common.ExplosionScaling;

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

    /**
     * TNT equivalent, in kg, of a legacy explosive that never declared {@code ExplosiveMass}.
     * <p>
     * Without this a legacy definition cannot scratch an armoured vehicle at all, because the
     * pressure model has no charge to work from. The crater radius is the one legacy quantity
     * with a physical meaning here, so inverting the curve the new system uses to derive it
     * recovers the charge the author was implicitly asking for. {@code ExplosionPower} then
     * scales it, since it is documented as a multiplier where 1 is vanilla behaviour.
     *
     * @param explosionRadius       legacy {@code ExplosionRadius} / {@code Explosion} in blocks
     * @param explosionPower        legacy {@code ExplosionPower} multiplier, 1 being vanilla
     * @param craterRadiusReference {@code newDamageSystemExplosiveRadiusReference}
     * @return the equivalent charge in kg TNT, or 0 when the legacy values describe no explosion
     */
    public static float legacyTntEquivalentKg(float explosionRadius, float explosionPower,
                                              double craterRadiusReference)
    {
        float charge = ExplosionScaling.chargeForCraterRadius(explosionRadius, craterRadiusReference);
        if (charge <= 0F)
            return 0F;

        double power = Float.isFinite(explosionPower) && explosionPower > 0F ? explosionPower : 1D;
        double equivalent = charge * power;
        if (!Double.isFinite(equivalent) || equivalent <= 0D)
            return 0F;
        return (float) Math.min(equivalent, Float.MAX_VALUE);
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
