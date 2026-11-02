package com.flansmodultimate.client.damage;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The one slow drift every column of smoke in a level leans with.
 *
 * <p>
 * Smoke rising dead straight reads as a game effect; a field of wrecks all leaning the same way reads as a
 * battlefield. The drift is worked out from the level's own clock alone, so every client leans every column the same
 * way without anything being sent for it, and it turns slowly enough that the sky over a battle looks settled rather
 * than stirred.
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class SmokeWind
{
    /** How fast the smoke is carried along, in blocks a tick. */
    private static final double SPEED = 0.03D;
    /** Share of that speed which comes and goes in gusts. */
    private static final double GUST_SHARE = 0.45D;
    /** Ticks the drift takes to box the compass: twenty minutes, so it never looks like it is being steered. */
    private static final int TURN_TICKS = 24_000;
    /** Ticks between gusts. */
    private static final int GUST_TICKS = 170;
    /** How much harder the wind blows in the rain. */
    private static final double RAIN_GAIN = 1.7D;

    /** @return the drift to add to a rising smoke particle's velocity, in blocks a tick */
    static Vec3 of(Level level)
    {
        long time = level.getGameTime();
        double turn = time * (Mth.TWO_PI / TURN_TICKS);
        double gust = 1D - GUST_SHARE + GUST_SHARE * (0.5D + 0.5D * Math.sin(time * (Mth.TWO_PI / GUST_TICKS)));
        double speed = SPEED * gust * (level.isRaining() ? RAIN_GAIN : 1D);
        return new Vec3(Math.cos(turn) * speed, 0D, Math.sin(turn) * speed);
    }
}
