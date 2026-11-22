package com.flansmodultimate.common.driveables.physics;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * The service ceiling of the Labjac fork's aircraft ({@code flightCeiling}). No world access.
 *
 * <p>
 * Within {@link #MARGIN} blocks of its ceiling an aircraft whose nose is up is pitched down by {@link #PUSH_DEGREES} a
 * tick, so it levels off however hard it is pulled up. Like the HUD altitude, the ceiling counts from sea level. Pitch is
 * the simulation's, negative nose up.
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FlightCeiling
{
    /** Blocks below the ceiling at which the nose starts to be pushed down. */
    public static final float MARGIN = 10F;
    /** Degrees the nose is pushed down each tick. */
    public static final float PUSH_DEGREES = 1F;

    /** @return whether the ceiling is set and this altitude above sea level is close enough to it to push the nose down */
    public static boolean atCeiling(double altitude, float ceiling)
    {
        return ceiling > 0F && Double.isFinite(altitude) && altitude > ceiling - MARGIN;
    }

    /** @return the pitch after this tick's push: nose-up attitudes at the ceiling are brought towards level, never past it */
    public static float limitPitch(float pitch, double altitude, float ceiling)
    {
        if (pitch >= 0F || !atCeiling(altitude, ceiling))
            return pitch;
        return Math.min(0F, pitch + PUSH_DEGREES);
    }
}
