package com.flansmodultimate.common.guns;

import net.minecraft.util.Mth;

/**
 * The shared shot-cooldown rule for every weapon this mod fires: handheld guns,
 * deployed guns, AA guns, driveable weapon banks, passenger gun mounts and mecha
 * hand guns all keep their cadence the same way.
 *
 * <p>A cooldown is a tick counter that may hold a fraction. It loses one tick per
 * game tick and the weapon is ready whenever it has run down to zero or below.
 * Firing charges one shot's delay back onto whatever is left, rather than
 * overwriting it, so a delay shorter than a tick leaves the counter still at or
 * below zero and the weapon fires again inside the same tick. That is what makes
 * fire rates above 1200 RPM expressible, and it also stops rates that do not
 * divide evenly into ticks from drifting: the remainder is carried instead of
 * being rounded away.
 *
 * <p>A cooldown that has run down is deliberately <em>not</em> clamped back up to
 * zero while the weapon sits idle, because the leftover fraction is the carry. It
 * cannot run away, because a tick only ever subtracts from a positive counter, so
 * an idle weapon settles in {@code (-1, 0]} and a single tick can never produce
 * more than {@code 1 / MIN_DELAY + 1} shots.
 */
public final class ShotCooldown
{
    /**
     * The shortest delay any weapon may declare, in ticks; 0.05 ticks is 24000 RPM.
     *
     * <p>This is a guard rather than a balance figure. A delay of zero, which a
     * content pack can reach with an attachment multiplier of zero or by declaring
     * neither a delay nor a fire rate, would let a firing loop charge nothing back
     * onto the cooldown and spin forever.
     */
    public static final float MIN_DELAY = 0.05F;

    private ShotCooldown() {}

    /** A declared shot delay, held to something a firing loop can terminate on. */
    public static float clampDelay(float delay)
    {
        return Math.max(MIN_DELAY, delay);
    }

    /**
     * The cadence a weapon's own keys describe: its {@code RoundsPerMin} if it
     * states one, otherwise its legacy {@code ShootDelay}, otherwise the fallback
     * its kind of weapon uses.
     *
     * <p>Handheld guns, deployed guns and AA guns all read the same two keys with
     * the same precedence, so they resolve them here instead of each keeping its
     * own copy for the sets to drift apart.
     *
     * @param roundsPerMin the weapon's declared rate of fire, or zero for none
     * @param shootDelay   the weapon's declared tick delay, or zero for none
     * @param fallback     the cadence to use when the weapon declares neither
     */
    public static float baseDelay(float roundsPerMin, float shootDelay, float fallback)
    {
        if (roundsPerMin != 0F)
            return clampDelay(1200F / roundsPerMin);
        return shootDelay > 0F ? clampDelay(shootDelay) : clampDelay(fallback);
    }

    /** Advances a cooldown by one game tick. */
    public static float tick(float cooldown)
    {
        return cooldown > 0F ? cooldown - 1F : cooldown;
    }

    /** Whether a weapon holding this cooldown may fire now. */
    public static boolean isReady(float cooldown)
    {
        return cooldown <= 0F;
    }

    /** Charges one shot's worth of delay onto a cooldown, carrying any remainder. */
    public static float charge(float cooldown, float delay)
    {
        return cooldown + clampDelay(delay);
    }

    /**
     * A cooldown as whole ticks remaining, for the HUD, for saved data and for
     * anything else that counts a weapon down in ticks.
     */
    public static int displayTicks(float cooldown)
    {
        return cooldown <= 0F ? 0 : Mth.ceil(cooldown);
    }
}
