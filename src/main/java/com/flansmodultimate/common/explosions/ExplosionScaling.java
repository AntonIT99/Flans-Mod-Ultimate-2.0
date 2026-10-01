package com.flansmodultimate.common.explosions;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * How an explosion's radii and peak damage grow with the charge, in one place so the law cannot
 * drift between the item tooltip, the fired round and the vehicle damage model.
 * <p>
 * Each radius follows {@code reference * mass^exponent} up to {@link #FLATTEN_KNEE_MASS_KG}, then
 * continues from there at a much smaller exponent. The two regimes answer different questions.
 * <p>
 * Below the knee the exponents are fitted to measured ordnance, from a 1 g .50 cal HE filler up to
 * a 4.4 kg 150 mm shell. The radius curves govern cratering and blast only. Casing fragments use
 * {@link FragmentationModel}, which computes a practical range from count, spread and energy.
 * <p>
 * Above the knee - beyond anything in that reference set - the curve is deliberately flattened for
 * playability. Continuing the fitted growth would give a 2.25 t bomb a blast radius over 400
 * blocks, which is not something Minecraft's scale can carry. Heavier charges still reach further,
 * with steeply diminishing returns.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ExplosionScaling
{
    /**
     * Charge mass beyond which radius growth flattens. It sits just above the heaviest round in the
     * reference set, so the fitted curves cover exactly the range they were measured over and
     * everything past it is shaped for gameplay instead.
     */
    public static final float FLATTEN_KNEE_MASS_KG = 5F;

    /** Fitted growth of the cratering radius with the charge. */
    public static final float CRATER_EXPONENT = 0.37F;
    /**
     * Cratering growth past the knee: the Hopkinson-Cranz cube root, which is how structural damage
     * radii scale for large charges. A 2.25 t bomb craters ~76 blocks and an 11 t MOAB ~130.
     */
    public static final float CRATER_FLATTEN_EXPONENT = 1F / 3F;

    /** Fitted growth of the blast radius with the charge. */
    public static final float BLAST_EXPONENT = 0.40F;
    /** Blast growth past the knee. Tuned so a 2.25 t bomb reaches ~126 blocks. */
    public static final float BLAST_FLATTEN_EXPONENT = 0.18F;

    /**
     * Growth of the peak blast damage with the charge. Peak overpressure at a fixed scaled
     * distance follows Hopkinson-Cranz, so this is the textbook cube root.
     */
    public static final double BLAST_DAMAGE_EXPONENT = 1D / 3D;
    /** Peak blast damage for a charge in kg TNT equivalent; {@code reference} is that of 1 kg. */
    public static float blastDamage(double reference, float massKg)
    {
        return damage(reference, massKg, BLAST_DAMAGE_EXPONENT);
    }

    /** Pure distance falloff shared by the explosion simulation and balance scenarios. */
    public static double blastFalloff(double distanceMeters, double radiusMeters, double sharpness)
    {
        double normalizedDistance = distanceMeters / Math.max(0.001D, radiusMeters);
        double falloff = 1D / (1D + Math.pow(normalizedDistance * sharpness, 3D));
        double edge = Math.max(1D - normalizedDistance, 0D);
        return falloff * edge * edge;
    }

    /** Peak damage is a property of the fragments/casing, not the explosive charge mass. */
    public static float fragPeakDamage(double casingDamage)
    {
        return Double.isFinite(casingDamage) && casingDamage > 0D
            ? (float) Math.min(casingDamage, Float.MAX_VALUE) : 0F;
    }

    /** Cratering radius in blocks for a charge in kg TNT equivalent. */
    public static float craterRadius(double reference, float massKg)
    {
        return radius(reference, massKg, CRATER_EXPONENT, CRATER_FLATTEN_EXPONENT);
    }

    /** Blast (overpressure) radius in blocks for a charge in kg TNT equivalent. */
    public static float blastRadius(double reference, float massKg)
    {
        return radius(reference, massKg, BLAST_EXPONENT, BLAST_FLATTEN_EXPONENT);
    }

    /**
     * Inverse of {@link #craterRadius}: the charge that would have produced this crater. Used to
     * recover an implied charge from a legacy definition that only authored a radius.
     */
    public static float chargeForCraterRadius(float craterRadius, double reference)
    {
        if (!Float.isFinite(craterRadius) || craterRadius <= 0F || !Double.isFinite(reference) || reference <= 0D)
            return 0F;

        double radiusAtKnee = reference * Math.pow(FLATTEN_KNEE_MASS_KG, CRATER_EXPONENT);
        double charge = craterRadius <= radiusAtKnee
            ? Math.pow(craterRadius / reference, 1D / CRATER_EXPONENT)
            : FLATTEN_KNEE_MASS_KG * Math.pow(craterRadius / radiusAtKnee, 1D / CRATER_FLATTEN_EXPONENT);

        if (!Double.isFinite(charge) || charge <= 0D)
            return 0F;
        return (float) Math.min(charge, Float.MAX_VALUE);
    }

    private static float radius(double reference, float massKg, float exponent, float flattenExponent)
    {
        if (!Float.isFinite(massKg) || massKg <= 0F || !Double.isFinite(reference) || reference <= 0D)
            return 0F;

        if (massKg <= FLATTEN_KNEE_MASS_KG)
            return (float) (reference * Math.pow(massKg, exponent));

        double atKnee = reference * Math.pow(FLATTEN_KNEE_MASS_KG, exponent);
        return (float) (atKnee * Math.pow(massKg / FLATTEN_KNEE_MASS_KG, flattenExponent));
    }

    private static float damage(double reference, float massKg, double exponent)
    {
        if (!Float.isFinite(massKg) || massKg <= 0F || !Double.isFinite(reference) || reference <= 0D)
            return 0F;
        return (float) Math.min(reference * Math.pow(massKg, exponent), Float.MAX_VALUE);
    }
}
