package com.flansmodultimate.common.driveables.physics;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.util.Mth;

/**
 * Depth control and oxygen of the Labjac fork's diving vehicles ({@code CanDive}). No world access.
 *
 * <p>
 * Holding dive or surface moves a depth rate from -1 to 1, which settles back to zero once released, so a submerged
 * hull holds its depth. Out of oxygen, the rate is driven up and the hull surfaces. Rates and speeds are in blocks per
 * tick, as the fork moves its hull by half the authored speed each tick.
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SubmarineDiving
{
    /** Depth below the water surface, in blocks, at which the hull counts as submerged and stops floating. */
    public static final double SUBMERGED_DEPTH = 1.5D;
    /** Depth below the water surface, in blocks, beyond which the crew breathes stored oxygen. */
    public static final double SNORKEL_DEPTH = 2D;
    private static final float RATE_STEP = 0.02F;
    private static final float RATE_SETTLE = 0.01F;
    private static final float BRAKE_RETENTION = 0.7F;
    private static final float OXYGEN_REFILL_PER_TICK = 15F;

    /** @return the depth rate after one tick of input */
    public static float stepRate(float rate, boolean dive, boolean surface, boolean brake, boolean oxygenLeft)
    {
        if (surface || !oxygenLeft)
            rate += RATE_STEP;
        if (dive && oxygenLeft)
            rate -= RATE_STEP;
        if (oxygenLeft)
        {
            if (brake)
                rate *= BRAKE_RETENTION;
            // Released, the rate settles back to zero instead of cruising up or down
            rate = rate > 0F ? Math.max(0F, rate - RATE_SETTLE) : Math.min(0F, rate + RATE_SETTLE);
        }
        return Mth.clamp(rate, -1F, 1F);
    }

    /** @return the vertical velocity a depth rate gives, in blocks per tick */
    public static double verticalVelocity(float rate, float diveSpeed, float surfaceSpeed)
    {
        return 0.5D * (rate < 0F ? diveSpeed : surfaceSpeed) * rate;
    }

    /** @return the oxygen left after one tick at a depth below the surface: used up deep down, refilled near the surface */
    public static float stepOxygen(float oxygen, double depth, float maxOxygen, boolean unlimited)
    {
        if (depth > SNORKEL_DEPTH)
            oxygen -= unlimited ? 0F : 1F;
        else
            oxygen += OXYGEN_REFILL_PER_TICK;
        return Mth.clamp(oxygen, 0F, Math.max(0F, maxOxygen));
    }
}
