package com.wolffsmod.npcs.properties;

import com.flansmodultimate.api.EngineSound;

/** Transient clip timer: pauses cancel immediately, resumed movement starts fresh, edits replace audio without overlap. */
public final class NpcEngineCadence
{
    private EngineSound playing;
    private float remaining;

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
        return tick(desired, moving, 1F);
    }

    public boolean tick(EngineSound desired, boolean moving, float playbackRate)
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
            remaining -= Float.isFinite(playbackRate) ? Math.max(0.5F, Math.min(2F, playbackRate)) : 1F;
        if (remaining > 0)
            return false;
        remaining = Math.max(1, desired.repeatTicks());
        return true;
    }
}
