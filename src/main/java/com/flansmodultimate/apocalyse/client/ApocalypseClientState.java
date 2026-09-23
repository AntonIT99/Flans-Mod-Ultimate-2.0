package com.flansmodultimate.apocalyse.client;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Client-side view of the apocalypse countdown.
 *
 * <p>The server only sends a value once a second, so the client counts the ticks in between
 * itself and the displayed timer runs smoothly instead of stepping.</p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApocalypseClientState
{
    private static int countdownTicks;

    public static void setCountdownTicks(int ticks)
    {
        countdownTicks = Math.max(0, ticks);
    }

    public static int getCountdownTicks()
    {
        return countdownTicks;
    }

    public static boolean isCountingDown()
    {
        return countdownTicks > 0;
    }

    /** Advances the local estimate between server updates. */
    public static void tick()
    {
        if (countdownTicks > 0)
            --countdownTicks;
    }

    /** Forgets the countdown when leaving a world, so it cannot leak into the next one. */
    public static void reset()
    {
        countdownTicks = 0;
    }
}
