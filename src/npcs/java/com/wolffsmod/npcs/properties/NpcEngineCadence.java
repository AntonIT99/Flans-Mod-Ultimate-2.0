package com.wolffsmod.npcs.properties;

import com.flansmodultimate.api.entity.EngineSound;

/** Transient clip timer: pauses cancel immediately, resumed movement starts fresh, edits replace audio without overlap. */
public final class NpcEngineCadence
{
    private EngineSound playing;
    private int remaining;

    public void reset()
    {
        playing = null;
        remaining = 0;
    }

    public boolean needsStop(EngineSound desired, boolean moving)
    {
        return playing != null && (!moving || !playing.equals(desired));
    }

    public boolean tick(EngineSound desired, boolean moving)
    {
        if (!moving || desired == null)
        {
            playing = null;
            remaining = 0;
            return false;
        }
        if (!desired.equals(playing))
            remaining = 0;
        playing = desired;
        if (remaining > 0)
            --remaining;
        if (remaining > 0)
            return false;
        remaining = Math.max(1, desired.repeatTicks());
        return true;
    }
}
