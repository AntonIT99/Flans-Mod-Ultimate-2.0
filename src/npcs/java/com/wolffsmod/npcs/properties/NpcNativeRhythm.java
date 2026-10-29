package com.wolffsmod.npcs.properties;

import com.flansmodultimate.api.driveables.ReloadCycle;
import org.jetbrains.annotations.Nullable;

/**
 * The rhythm of an NPC firing its model's weapon with Flan ammunition, in Custom NPCs terms: a burst of triggers, then a
 * pause. Each part comes from the model unless the NPC keeps its own value for it, so what the editor shows is what the
 * NPC does.
 *
 * @param shotDelay
 *            ticks between triggers within a burst, possibly fractional
 * @param volleys
 *            triggers per burst, at least one
 * @param pause
 *            ticks waited after the burst's last trigger
 * @param reloads
 *            whether the pause is the model's reload, with its sounds, rather than an ordinary delay
 */
public record NpcNativeRhythm(double shotDelay, int volleys, double pause, boolean reloads)
{
    /** Passed for a part the NPC does not keep its own value for. */
    public static final int INHERITED = -1;

    /**
     * Without a reload cycle the model fires steadily: bursts of one trigger, paused for the firing interval.
     *
     * @param modelShotDelay
     *            the model weapon's firing interval in ticks
     * @param cycle
     *            the model's reload cycle with the NPC's ammunition, or null when the NPC follows none
     * @param ownBurstDelay
     *            the NPC's own burst delay in ticks, or {@link #INHERITED}
     * @param ownBurst
     *            the NPC's own burst count, or {@link #INHERITED}
     * @param ownDelay
     *            the NPC's own delay in ticks, or {@link #INHERITED}
     */
    public static NpcNativeRhythm of(double modelShotDelay, @Nullable ReloadCycle cycle, int ownBurstDelay, int ownBurst, int ownDelay)
    {
        double modelShot = Double.isFinite(modelShotDelay) && modelShotDelay > 0D ? modelShotDelay : 1D;
        double shot = ownBurstDelay == INHERITED ? modelShot : Math.max(1, ownBurstDelay);
        int inheritedVolleys = cycle == null ? 1 : cycle.volleys();
        double inheritedPause = cycle == null ? modelShot : cycle.reloadTicks();
        return new NpcNativeRhythm(shot, ownBurst == INHERITED ? inheritedVolleys : Math.max(1, ownBurst), ownDelay == INHERITED ? inheritedPause : Math.max(1, ownDelay), cycle != null);
    }
}
