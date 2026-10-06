package com.wolffsmod.npcs.combat;

/** Fractional native item cadence; idle ticks retain the fraction without accumulating missed shots. */
public final class NpcItemCadence
{
    private double remaining;

    public void tick()
    {
        if (remaining > 0D)
            remaining -= 1D;
    }

    public boolean ready()
    {
        return remaining <= 0D;
    }

    public void fired(double interval)
    {
        remaining += Double.isFinite(interval) ? Math.max(0.05D, interval) : 1D;
    }

    public void reset()
    {
        remaining = 0D;
    }
}
