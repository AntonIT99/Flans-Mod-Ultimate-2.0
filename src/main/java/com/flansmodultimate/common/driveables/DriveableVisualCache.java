package com.flansmodultimate.common.driveables;

/** Invalidates inventory-derived visuals without scanning unchanged stacks each tick. */
public final class DriveableVisualCache
{
    private Object source;
    private long revision;
    private int primarySlot;
    private int secondarySlot;
    private int lastRefreshTick;

    public boolean needsRefresh(Object inventory, long inventoryRevision, int primary, int secondary, int tick)
    {
        // Periodically re-read stacks for integrations that mutate NBT without calling setChanged.
        // Each entity measures from its own last refresh, rather than a global tick phase.
        if (source == inventory && revision == inventoryRevision && primarySlot == primary
            && secondarySlot == secondary && tick - lastRefreshTick >= 0 && tick - lastRefreshTick < 20)
            return false;
        source = inventory;
        revision = inventoryRevision;
        primarySlot = primary;
        secondarySlot = secondary;
        lastRefreshTick = tick;
        return true;
    }
}
