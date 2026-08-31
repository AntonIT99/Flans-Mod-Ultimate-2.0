package com.flansmodultimate.common.driveables;

import net.minecraft.util.Mth;

/**
 * Pure sizing curve for the fireball a crashing driveable leaves behind.
 *
 * <p>What burns after an impact is the fuel, not the airframe. A tank with
 * nothing left in it leaves wreckage and no fireball, so gliding in after a
 * flameout reads quite differently from a loaded aircraft going into a hillside,
 * and running a tank dry becomes a way to survive putting one down badly.
 */
public final class DriveableCrashExplosion
{
    /** Below this share of a full tank there is too little left to deflagrate. */
    private static final float MIN_FUEL_FRACTION = 0.02F;
    /** Below this share the fuel burns off without leaving the ground alight. */
    private static final float FIRE_FUEL_FRACTION = 0.25F;
    private static final float FIRE_RADIUS_SHARE = 0.4F;
    /** Even a marginal write-off still lets go of a good part of the tank. */
    private static final float SEVERITY_FLOOR = 0.35F;
    private static final float FUEL_FLOOR = 0.3F;
    /**
     * How much wider the fireball reads than it bites. A crash should look like
     * an airframe's worth of fuel going up, but the damaging and terrain
     * breaking radius stays exactly what the pack authorised, so the spectacle
     * costs no one their build.
     */
    private static final float VISUAL_SCALE = 2.5F;

    private DriveableCrashExplosion() {}

    /**
     * @param configuredRadius the type's {@code DeathExplosionRadius}
     * @param severity         how bad the impact was, 0 to 1
     * @param fuelFraction     how full the tank was, 0 to 1
     */
    public static Blast evaluate(float configuredRadius, float severity, float fuelFraction)
    {
        if (!Float.isFinite(configuredRadius) || configuredRadius <= 0F
            || !Float.isFinite(severity) || severity <= 0F
            || !Float.isFinite(fuelFraction) || fuelFraction < MIN_FUEL_FRACTION)
            return Blast.NONE;

        float fuel = Mth.clamp(fuelFraction, 0F, 1F);
        float radius = configuredRadius
            * (SEVERITY_FLOOR + (1F - SEVERITY_FLOOR) * Mth.clamp(severity, 0F, 1F))
            * (FUEL_FLOOR + (1F - FUEL_FLOOR) * fuel);
        return new Blast(radius, fuel >= FIRE_FUEL_FRACTION ? radius * FIRE_RADIUS_SHARE : 0F,
            radius * VISUAL_SCALE);
    }

    /**
     * @param radius       damage and block breaking radius
     * @param fireRadius   how far the wreck sets the ground alight
     * @param visualRadius how far the fireball and its debris are thrown, which
     *                     is deliberately wider than the radius that does harm
     */
    public record Blast(float radius, float fireRadius, float visualRadius)
    {
        private static final Blast NONE = new Blast(0F, 0F, 0F);

        public boolean happens()
        {
            return radius > 0F;
        }
    }
}
