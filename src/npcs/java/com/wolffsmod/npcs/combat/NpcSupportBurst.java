package com.wolffsmod.npcs.combat;

/** A support gun fires bounded bursts without changing its native shot cadence or reload. One per hand, server thread. */
final class NpcSupportBurst
{
    private static final int MAX_ROUNDS = 8;
    private static final long PAUSE = 12L;
    private int rounds;
    private long started;
    private long readyAt;

    boolean ready(long now)
    {
        if (rounds > 0 && now - started >= 30L)
            pause(now);
        return now >= readyAt;
    }

    void fired(long now)
    {
        if (rounds == 0)
            started = now;
        if (++rounds >= MAX_ROUNDS)
            pause(now);
    }

    private void pause(long now)
    {
        rounds = 0;
        readyAt = now + PAUSE;
    }
}
