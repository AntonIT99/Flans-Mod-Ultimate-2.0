package com.flansmodultimate.common.guns;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * The Labjac fork's seekers that find a target by what it gives off rather than by where the round points.
 *
 * <p>
 * A modern torpedo listens: a driveable's throttle is its noise, so the torpedo hears it within its minimum range when
 * silent and up to its full seeker range on top of that at full throttle. Running astern is half as loud, since the hull
 * is slower. An anti-radiation missile homes on a radar that is emitting.
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PassiveSeekers
{
    /** @return how loud a driveable at this throttle is, from 0 when stopped to 1 at full ahead */
    public static float noise(float throttle)
    {
        if (!Float.isFinite(throttle))
            return 0F;
        return throttle >= 0F ? Math.min(throttle, 1F) : Math.min(-throttle, 1F) / 2F;
    }

    /** @return the distance in blocks at which a modern torpedo hears a driveable at this throttle */
    public static double hearingRange(float minRange, float seekerRange, float throttle)
    {
        return Math.max(0F, minRange) + Math.max(0F, seekerRange) * noise(throttle);
    }

    /** @return whether a modern torpedo at this squared distance hears a driveable at this throttle */
    public static boolean hears(double distanceSquared, float minRange, float seekerRange, float throttle)
    {
        double range = hearingRange(minRange, seekerRange, throttle);
        return Double.isFinite(distanceSquared) && distanceSquared < range * range;
    }
}
