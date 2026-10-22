package com.wolffsmod.npcs.sim;

/** Server-local retry budget retained across return-goal restarts, so failed paths cannot cause a search every tick. */
public final class NpcReturnHome
{
    private static final int RETRY_TICKS = 40;
    private long nextAttempt = Long.MIN_VALUE;
    private boolean detour;

    public boolean ready(long tick)
    {
        return tick >= nextAttempt;
    }

    public void attempted(long tick)
    {
        nextAttempt = tick + RETRY_TICKS;
    }

    /** Alternate direct and intermediate-point searches using the dependency's existing destination logic. */
    public boolean nextDetour()
    {
        detour = !detour;
        return detour;
    }
}
