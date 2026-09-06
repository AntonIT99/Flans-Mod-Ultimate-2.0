package com.flansmodultimate.common;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * How an explosion's radii grow with the charge, in one place so the law cannot drift between the
 * item tooltip, the fired round and the vehicle damage model.
 * <p>
 * Each radius follows {@code reference * mass^exponent} up to {@link #FLATTEN_KNEE_MASS_KG}, then
 * continues from there at a much smaller exponent. The two regimes answer different questions.
 * <p>
 * Below the knee the exponents are fitted to measured ordnance, from a 1 g .50 cal HE filler up to
 * a 4.4 kg 150 mm shell. Textbook Hopkinson-Cranz scaling would make every radius a pure cube root
 * of the charge, but the quoted effect radii of real rounds do not behave that way across that
 * range: the scaled distance itself creeps up with the charge, because what is quoted is a
 * casualty or destruction radius rather than one fixed overpressure. A least-squares fit in
 * log-log space over those rounds gives ~0.37 for cratering and ~0.40 for the broader blast and
 * fragmentation envelope, which lands every one of them inside its quoted band.
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
    /** Cratering growth past the knee. Tuned so a 2.25 t bomb craters ~49 blocks. */
    public static final float CRATER_FLATTEN_EXPONENT = 0.26F;

    /** Fitted growth of the blast radius with the charge. */
    public static final float BLAST_EXPONENT = 0.40F;
    /** Blast growth past the knee. Tuned so a 2.25 t bomb reaches ~126 blocks. */
    public static final float BLAST_FLATTEN_EXPONENT = 0.18F;

    /** Fragmentation follows the same fitted growth as blast below the knee. */
    public static final float FRAG_EXPONENT = 0.40F;
    /**
     * Fragments flatten more gently than blast, so a heavy charge throws them past its own
     * overpressure envelope the way real HE does, reaching ~161 blocks on a 2.25 t bomb.
     */
    public static final float FRAG_FLATTEN_EXPONENT = 0.22F;

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

    /** Fragmentation radius in blocks for a charge in kg TNT equivalent. */
    public static float fragRadius(double reference, float massKg)
    {
        return radius(reference, massKg, FRAG_EXPONENT, FRAG_FLATTEN_EXPONENT);
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
}
