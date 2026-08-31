package com.flansmodultimate.common.driveables;

import net.minecraft.util.Mth;

/**
 * Pure severity curve for aircraft ground impacts.
 *
 * <p>Legacy Flan's crashed an aircraft through {@code fall()}, dealing a flat
 * {@code -fallDistance * 50} to the core. That only ever bit because legacy
 * packs authored part health in the low hundreds; the same pack ported forward
 * now authors thousands, so any flat figure is a scratch. Severity is therefore
 * reported as a fraction of the struck part's own maximum health, which keeps a
 * crash equally lethal whatever hitpoints a pack scales to.
 *
 * <p>Both halves of the legacy behaviour survive: descent speed decides how hard
 * the touchdown was, and the total impact speed decides how much energy went
 * into it. Attitude gates the pair, so a fast but level landing stays free while
 * a dive at the same speed writes the airframe off.
 */
public final class PlaneCrashDamage
{
    static final double SAFE_DESCENT_SPEED = 0.35D;
    private static final double FULL_DAMAGE_DESCENT_SPEED = 2D;
    private static final double SAFE_IMPACT_SPEED = 0.6D;
    private static final double FULL_IMPACT_SPEED = 4D;
    private static final double SAFE_UPRIGHT_COSINE = Math.cos(Math.toRadians(25D));
    private static final double WORST_UPRIGHT_COSINE = Math.cos(Math.toRadians(75D));
    private static final double MIN_DAMAGE_SEVERITY = 0.16D;
    /** How much a hard touchdown weighs against a badly angled one. */
    private static final double DESCENT_WEIGHT = 0.65D;
    private static final double ANGLE_WEIGHT = 0.35D;
    /** Forward energy amplifies an already bad touchdown; it never creates one. */
    private static final double SPEED_AMPLIFICATION = 0.9D;
    /** Fraction of the epicentre part's health destroyed by a full severity impact. */
    private static final double EPICENTRE_HEALTH_LOSS = 2D;
    /** At this much relative structural loss the airframe is written off outright. */
    private static final float CATASTROPHIC_HEALTH_LOSS = 1F;

    private PlaneCrashDamage() {}

    /**
     * @param impactSpeed   total speed at contact, in blocks per tick
     * @param descentSpeed  downward speed at contact, in blocks per tick, positive downwards
     * @param uprightCosine how upright the aircraft is: 1 level, 0 on its nose or a wingtip
     */
    public static Impact evaluate(double impactSpeed, double descentSpeed, double uprightCosine, float fallDamageFactor)
    {
        if (!Double.isFinite(impactSpeed) || !Double.isFinite(descentSpeed) || !Double.isFinite(uprightCosine)
            || descentSpeed <= SAFE_DESCENT_SPEED || fallDamageFactor <= 0F)
            return Impact.NONE;

        double descent = Mth.clamp((descentSpeed - SAFE_DESCENT_SPEED)
            / (FULL_DAMAGE_DESCENT_SPEED - SAFE_DESCENT_SPEED), 0D, 1D);
        double angle = Mth.clamp((SAFE_UPRIGHT_COSINE - uprightCosine)
            / (SAFE_UPRIGHT_COSINE - WORST_UPRIGHT_COSINE), 0D, 1D);
        double speed = Mth.clamp((impactSpeed - SAFE_IMPACT_SPEED)
            / (FULL_IMPACT_SPEED - SAFE_IMPACT_SPEED), 0D, 1D);

        // Speed multiplies rather than adds so a fast, gentle, wheels-first
        // touchdown - which is simply how a jet lands - stays harmless, while
        // the same speed carried into a dive or a wingtip is what kills.
        double severity = Mth.clamp((descent * DESCENT_WEIGHT + angle * ANGLE_WEIGHT)
            * (1D + speed * SPEED_AMPLIFICATION), 0D, 1D);
        if (severity < MIN_DAMAGE_SEVERITY)
            return Impact.NONE;
        return new Impact((float) severity,
            (float) (severity * severity * EPICENTRE_HEALTH_LOSS * fallDamageFactor));
    }

    /**
     * @param severity       0 to 1 description of how bad the impact was
     * @param healthFraction share of the epicentre part's maximum health destroyed
     */
    public record Impact(float severity, float healthFraction)
    {
        private static final Impact NONE = new Impact(0F, 0F);

        public boolean damaging()
        {
            return healthFraction > 0F;
        }

        /**
         * Whether the impact alone would obliterate a part at full health, in
         * which case the airframe is lost rather than merely broken. Reading it
         * off the scaled loss rather than off severity lets a forgiving
         * {@code FallDamageFactor} keep a type survivable.
         */
        public boolean catastrophic()
        {
            return healthFraction >= CATASTROPHIC_HEALTH_LOSS;
        }
    }
}
