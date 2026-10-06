package com.wolffsmod.npcs.properties;

/** Carries the fractional part of a type's interval instead of rounding every shot independently. */
public final class NpcTypeCadence
{
    private double remainder;

    public int next(double ticks)
    {
        double interval = Double.isFinite(ticks) ? Math.max(1D, ticks) : 1D;
        double delay = interval + remainder;
        int whole = (int) Math.min(Integer.MAX_VALUE, Math.floor(delay));
        remainder = delay - whole;
        // Custom NPCs tests cooldown-- <= 0, so an N-tick interval needs a counter of N-1.
        return whole - 1;
    }

    public void reset()
    {
        remainder = 0D;
    }
}
