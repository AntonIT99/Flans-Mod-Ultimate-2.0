package com.flansmodultimate.common.driveables;

/**
 * Pure block strike curve for the collision point sweep.
 *
 * <p>Legacy Flan's dealt {@code hardness * hardness * speed} flat hitpoints to
 * whichever part carried the collision point that struck a block. The shape is
 * kept - a soft block barely scuffs an airframe while stone or metal tears it
 * apart, and both scale with how fast the point was travelling - but the result
 * is a fraction of the struck part's own health so it stays meaningful for packs
 * that author hitpoints in the thousands.
 */
public final class DriveableImpactDamage
{
    /** Below this a driveable is manoeuvring or taxiing rather than crashing. */
    public static final double MIN_IMPACT_SPEED = 0.3D;
    /** Legacy threshold: foliage, snow and other soft blocks never damaged anything. */
    private static final float MIN_DAMAGING_HARDNESS = 0.2F;
    /** Chosen so a strike costs the same share of a part as it did in legacy packs. */
    private static final double HEALTH_FRACTION_PER_STRIKE = 0.01D;
    /** One point cannot destroy a healthy part on its own; a sustained plough can. */
    private static final float MAX_FRACTION_PER_STRIKE = 0.35F;

    private DriveableImpactDamage() {}

    /**
     * @param hardness the struck block's destroy speed; unbreakable blocks report
     *                 a negative value and, as in legacy, do no damage
     * @param speed    the impact speed in blocks per tick
     * @return the share of the struck part's maximum health lost this tick
     */
    public static float blockStrikeHealthFraction(float hardness, double speed)
    {
        if (!Float.isFinite(hardness) || hardness <= MIN_DAMAGING_HARDNESS
            || !Double.isFinite(speed) || speed < MIN_IMPACT_SPEED)
            return 0F;
        return (float) Math.min(MAX_FRACTION_PER_STRIKE,
            hardness * hardness * speed * HEALTH_FRACTION_PER_STRIKE);
    }
}
