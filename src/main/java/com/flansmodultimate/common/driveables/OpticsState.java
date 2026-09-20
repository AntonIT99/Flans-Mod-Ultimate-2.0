package com.flansmodultimate.common.driveables;

import lombok.Getter;

/** Server-owned transient state, with tick-budgeted debouncing independent of packet frequency. */
@Getter
public final class OpticsState
{
    private boolean active;
    private int sight;
    private boolean thermal;
    private long nextToggle;
    private long nextCycle;

    public void reset()
    {
        active = false;
        sight = 0;
        thermal = false;
        nextToggle = nextCycle = 0;
    }

    public void update(VehicleOptics definition, boolean allowed, long tick, boolean toggle, boolean cycle)
    {
        if (!allowed)
        {
            reset();
            return;
        }
        boolean wasActive = active;
        sight = Math.floorMod(sight, definition.sightCount());
        if (definition.forced()) active = true;
        else if (toggle && tick >= nextToggle)
        {
            active = !active;
            nextToggle = tick + (definition.isOpticsMode() ? 4 : 10);
        }
        if (cycle && tick >= nextCycle)
        {
            if (definition.sightCount() > 1)
                sight = (sight + 1) % definition.sightCount();
            else if (definition.isThermalSight() || definition.thermal(sight))
                thermal = !thermal;
            nextCycle = tick + 3;
        }
        if (definition.sightCount() > 1 || !wasActive && active)
            thermal = definition.thermal(sight);
        if (!active) thermal = false;
    }
}
