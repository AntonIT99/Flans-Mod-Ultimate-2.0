package com.flansmodultimate.common.driveables.damage;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.util.Mth;

/**
 * How strongly damage and burning show, as plain numbers both sides agree on.
 *
 * <p>
 * Damage only starts to show once a part has lost a good share of its hit points, so that a scratched vehicle looks
 * whole, and then grows smoothly to full strength as the part is lost. A wreck burns in stages over its lifetime:
 * a heavy fire, a smoke column once that dies down, thin smoke, and smouldering at the end.
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DamageVisualCurves
{
    /** Share of hit points left above which a part shows no damage. */
    public static final float DAMAGE_THRESHOLD = 0.6F;
    /** The darkest a burnt-out wreck is drawn, as a multiplier of its colour. */
    public static final float WRECK_TINT = 0.3F;
    /** The darkest a still-working part is drawn. */
    public static final float DAMAGED_TINT = 0.62F;
    /** Ticks over which a fresh wreck chars down to its final colour. */
    public static final int CHAR_TICKS = 60;
    /** The longest a wreck's heavy fire lasts, in ticks, however long the wreck itself stays. */
    public static final int MAX_FIRE_TICKS = 900;
    /** Share of the wreck's lifetime its heavy fire lasts, when that is shorter than the cap. */
    private static final float FIRE_SHARE = 0.35F;
    /** Share of the lifetime by which the smoke has thinned to a wisp. */
    private static final float THIN_SMOKE_SHARE = 0.8F;
    private static final float THIN_SMOKE = 0.25F;
    private static final float SMOULDER = 0.1F;
    /** Ticks a mecha takes to fall once it is wrecked. */
    public static final int COLLAPSE_TICKS = 24;
    /** The least heat a wreck shows on a thermal sight. */
    public static final float MIN_WRECK_HEAT = 0.15F;

    /**
     * @param healthFraction
     *            share of the part's hit points left, from 0 to 1
     * @return how damaged the part looks, from 0 for untouched to 1 for lost
     */
    public static float damage(float healthFraction)
    {
        if (!Float.isFinite(healthFraction))
            return 0F;
        float t = Mth.clamp(1F - healthFraction / DAMAGE_THRESHOLD, 0F, 1F);
        return t * t * (3F - 2F * t);
    }

    /** @return ticks the heavy fire of a wreck lasts */
    public static int fireTicks(int lifetimeTicks)
    {
        return lifetimeTicks <= 0 ? MAX_FIRE_TICKS : Math.min(MAX_FIRE_TICKS, Math.max(1, Mth.floor(lifetimeTicks * FIRE_SHARE)));
    }

    /**
     * @param lifetimeTicks
     *            how long the wreck stays, or 0 or less when it stays until removed
     * @return how strongly the wreck burns at this age, from 0 to 1
     */
    public static float wreckFire(int ageTicks, int lifetimeTicks)
    {
        if (ageTicks < 0)
            return 0F;
        int fire = fireTicks(lifetimeTicks);
        if (ageTicks >= fire)
            return 0F;
        float t = ageTicks / (float) fire;
        // Full strength for the first half, then dying away.
        return t < 0.5F ? 1F : 1F - (t - 0.5F) * 2F;
    }

    /** @return how much smoke the wreck gives off at this age, from 0 to 1 */
    public static float wreckSmoke(int ageTicks, int lifetimeTicks)
    {
        if (ageTicks < 0)
            return 0F;
        int fire = fireTicks(lifetimeTicks);
        if (ageTicks < fire)
            return 1F;
        if (lifetimeTicks <= 0)
            return THIN_SMOKE;
        int thin = Math.max(fire + 1, Mth.floor(lifetimeTicks * THIN_SMOKE_SHARE));
        if (ageTicks < thin)
            return Mth.lerp((ageTicks - fire) / (float) (thin - fire), 1F, THIN_SMOKE);
        return ageTicks < lifetimeTicks ? Mth.lerp((ageTicks - thin) / (float) Math.max(1, lifetimeTicks - thin), THIN_SMOKE, SMOULDER) : SMOULDER;
    }

    /**
     * @return how hot the wreck shows on a thermal sight at this age: white-hot while it burns, cooling as the smoke
     *         thins, and never quite cold before it is gone
     */
    public static float wreckHeat(int ageTicks, int lifetimeTicks)
    {
        if (ageTicks < 0)
            return 1F;
        float heat = Math.max(wreckFire(ageTicks, lifetimeTicks), 0.2F + 0.5F * wreckSmoke(ageTicks, lifetimeTicks));
        return Mth.clamp(heat, MIN_WRECK_HEAT, 1F);
    }

    /**
     * @param healthFraction
     *            share of the part's hit points left
     * @param burning
     *            whether the part is alight
     * @param wreckAge
     *            ticks since the driveable was wrecked, or negative while it still works
     * @return the multiplier the part's colour is drawn with
     */
    public static float tint(float healthFraction, boolean burning, int wreckAge)
    {
        float alive = 1F - (1F - DAMAGED_TINT) * damage(healthFraction) * (burning ? 1F : 0.6F);
        if (burning)
            alive = Math.min(alive, 0.85F);
        if (wreckAge < 0)
            return Mth.clamp(alive, DAMAGED_TINT, 1F);
        return Mth.lerp(Mth.clamp(wreckAge / (float) CHAR_TICKS, 0F, 1F), Math.max(alive, WRECK_TINT), WRECK_TINT);
    }

    /** @return how far a wrecked mecha has fallen, from 0 standing to 1 down, with a small settle at the end */
    public static float collapse(float wreckAge)
    {
        if (wreckAge <= 0F)
            return 0F;
        float t = Mth.clamp(wreckAge / COLLAPSE_TICKS, 0F, 1.25F);
        if (t <= 1F)
            return t * t;
        // A small rebound as it lands, settling back down.
        return 1F - Mth.sin((t - 1F) * 4F * Mth.PI) * 0.04F;
    }
}
