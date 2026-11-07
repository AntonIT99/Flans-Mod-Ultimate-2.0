package com.flansmodultimate.common.driveables.damage;

import net.minecraft.util.Mth;

/**
 * Pure severity curve for a ground vehicle landing after a drop.
 *
 * <p>
 * Legacy Flan's ran this out of {@code fall()}, dealing a flat
 * {@code -fallDistance * 50} to the core and a fifth of it to the first wheel.
 * A flat figure only ever bit because legacy packs authored part health in the
 * low hundreds: the same pack ported forward authors thousands, and
 * {@code UseRealisticVehicleHealth} derives a tank's hitpoints from its mass, so
 * a fixed number is a scratch on one hull and instant death on another. Severity
 * is therefore reported as a share of the part's own maximum health, exactly as
 * {@link PlaneCrashDamage} does for airframes, which keeps a drop equally
 * consequential whatever hitpoints a pack scales to.
 *
 * <p>
 * The curve is calibrated against the legacy outcome for the hulls legacy
 * actually had: a ten-block drop costs a core about a fifth of its health, which
 * is what the old flat formula took off the roughly 120 hitpoint cores that
 * period packs authored.
 */
public final class VehicleLandingDamage
{
    /** Descent speed in blocks a tick the running gear absorbs unharmed, which is about a two-block drop. */
    public static final double SAFE_DESCENT_SPEED = 0.65D;
    /** Descent speed at which the impact alone accounts for the whole hull, which is about a thirty-block drop. */
    private static final double WRITE_OFF_DESCENT_SPEED = 2.4D;
    /** Share of the core's health the write-off speed destroys, before {@code FallDamageFactor}. */
    private static final double CORE_HEALTH_LOSS = 1.6D;
    /** Legacy gave the first wheel a fifth of what the hull took. */
    private static final float RUNNING_GEAR_SHARE = 0.2F;
    /** Below this share the landing is a thump rather than damage, and reporting it would only spam part feedback. */
    private static final float MIN_HEALTH_LOSS = 0.005F;
    /** At this much relative loss the impact has accounted for a part at full health. */
    private static final float WRITE_OFF_HEALTH_LOSS = 1F;

    private VehicleLandingDamage()
    {}

    /**
     * @param descentSpeed
     *            downward speed at contact in blocks per tick, positive downwards
     * @param fallDamageFactor
     *            the type's {@code FallDamageFactor}; zero makes a type immune,
     *            as it did in legacy
     */
    public static Impact evaluate(double descentSpeed, float fallDamageFactor)
    {
        if (!Double.isFinite(descentSpeed) || descentSpeed <= SAFE_DESCENT_SPEED || !Float.isFinite(fallDamageFactor) || fallDamageFactor <= 0F)
            return Impact.NONE;

        double severity = Mth.clamp((descentSpeed - SAFE_DESCENT_SPEED) / (WRITE_OFF_DESCENT_SPEED - SAFE_DESCENT_SPEED), 0D, 1D);
        // Squared, so clearing a ledge stays cheap while a genuine fall is not
        // merely proportionally worse. Energy grows with the square of the speed
        // and so does what the suspension cannot absorb.
        float loss = (float) (severity * severity * CORE_HEALTH_LOSS * fallDamageFactor);
        if (!Float.isFinite(loss) || loss < MIN_HEALTH_LOSS)
            return Impact.NONE;
        return new Impact((float) severity, loss);
    }

    /**
     * @param severity
     *            0 to 1 description of how hard the landing was
     * @param coreHealthFraction
     *            share of the core's maximum health the landing destroys
     */
    public record Impact(float severity, float coreHealthFraction)
    {
        private static final Impact NONE = new Impact(0F, 0F);

        public boolean damaging()
        {
            return coreHealthFraction > 0F;
        }

        /** Share of its own maximum health the running gear that met the ground loses. */
        public float runningGearHealthFraction()
        {
            return coreHealthFraction * RUNNING_GEAR_SHARE;
        }

        /**
         * Whether the impact alone accounts for a part at full health, in which
         * case the hull is written off rather than merely damaged. Read off the
         * scaled loss rather than off severity, so a forgiving
         * {@code FallDamageFactor} keeps a type survivable.
         */
        public boolean writeOff()
        {
            return coreHealthFraction >= WRITE_OFF_HEALTH_LOSS;
        }
    }
}
