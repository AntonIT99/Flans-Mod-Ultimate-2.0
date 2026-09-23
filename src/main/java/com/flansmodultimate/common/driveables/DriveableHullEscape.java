package com.flansmodultimate.common.driveables;

import org.jetbrains.annotations.NotNull;

import net.minecraft.world.entity.Entity;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Notices entities that ended up trapped inside one driveable's hull and lets
 * them out again.
 *
 * <p>Shaped collision stops entities from walking into a hull, but a hull that
 * moves onto an entity, a destroyed part coming back, a teleport or a mount
 * dismounting into a wall can still leave one inside. Pushing such an entity
 * along the shortest way out fails whenever terrain, another hull or the hull
 * closing again blocks that direction: the push is undone every tick and the
 * entity stays sealed in a solid wall, unable to move because the same hull is
 * also stopping its own movement.</p>
 *
 * <p>So penetration that persists without getting shallower is treated as
 * being trapped. For as long as that lasts this hull is suspended for that one
 * entity, which can then simply walk out while the hull keeps nudging it
 * towards the nearest opening. Only the pair is suspended: other hulls, blocks
 * and every other entity stay solid. Collision resumes a few ticks after the
 * entity stops overlapping, so brushing the surface on the way out cannot trap
 * it again, and immediately if it comes to rest on a surface it can stand on.</p>
 *
 * <p>One instance per driveable; only its level's thread touches it.</p>
 */
public final class DriveableHullEscape
{
    /** Overlap shallower than this is ordinary surface contact, not being trapped. */
    static final double MIN_TRAPPED_DEPTH = 0.15D;
    /** Depth an entity has to lose in a tick for the ordinary push-out to count as working. */
    private static final double PROGRESS_PER_TICK = 0.02D;
    /** Ticks of deep overlap without progress before the hull gives way. */
    private static final int TRAPPED_TICKS = 10;
    /** Ticks the hull stays suspended after the entity last overlapped it. */
    private static final int GRACE_TICKS = 5;
    /** Ticks after which an entry nothing reports on any more is dropped. */
    private static final int FORGET_TICKS = 60;

    private final Map<Entity, Entry> entries = new WeakHashMap<>();

    /** Whether this hull is currently suspended for {@code entity}, letting it move out freely. */
    public boolean isSuspended(@NotNull Entity entity)
    {
        if (entries.isEmpty())
            return false;
        Entry entry = entries.get(entity);
        return entry != null && entry.isSuspended(entity.level().getGameTime());
    }

    /**
     * Records this tick's overlap of {@code entity} with the hull.
     *
     * @param depth      how far the entity is sunk in, or {@code 0} when it only rests on the hull
     * @param supported  whether the entity stands on a surface of the hull, which is never being trapped
     * @return whether the entity is trapped, so the hull is suspended for it
     */
    public boolean report(@NotNull Entity entity, double depth, boolean supported)
    {
        long now = entity.level().getGameTime();
        Entry entry = entries.get(entity);
        if (entry == null)
        {
            if (supported || depth < MIN_TRAPPED_DEPTH)
                return false;
            prune(now);
            entry = new Entry(now, depth);
            entries.put(entity, entry);
        }
        return entry.update(now, depth, supported);
    }

    /** Drops entries nothing has reported on for a while, for entities that left or died. */
    private void prune(long now)
    {
        for (Iterator<Entry> iterator = entries.values().iterator(); iterator.hasNext(); )
        {
            if (now - iterator.next().lastReport > FORGET_TICKS)
                iterator.remove();
        }
    }

    /** How one entity's overlap with this hull has been developing. */
    private static final class Entry
    {
        private long lastReport;
        private double bestDepth;
        private int stuckTicks;
        private boolean suspended;

        private Entry(long now, double depth)
        {
            lastReport = now - 1L;
            bestDepth = depth;
        }

        private boolean isSuspended(long now)
        {
            return suspended && now - lastReport <= GRACE_TICKS;
        }

        private boolean update(long now, double depth, boolean supported)
        {
            if (now <= lastReport)
                // Several hulls of the same driveable overlap the entity in one tick.
                return suspended && depth >= MIN_TRAPPED_DEPTH;
            if (now - lastReport > GRACE_TICKS)
            {
                // The entity was clear in between; nothing that happened before still counts.
                stuckTicks = 0;
                bestDepth = depth;
                suspended = false;
            }
            lastReport = now;
            if (supported)
            {
                // Standing on the hull is a safe place to be; make it solid again at once.
                stuckTicks = 0;
                bestDepth = depth;
                suspended = false;
                return false;
            }
            if (depth < MIN_TRAPPED_DEPTH)
            {
                // Almost out. Keep any suspension until the entity is clear of the hull.
                stuckTicks = 0;
                bestDepth = depth;
                return suspended;
            }
            if (depth <= bestDepth - PROGRESS_PER_TICK)
            {
                bestDepth = depth;
                stuckTicks = 0;
            }
            else if (++stuckTicks >= TRAPPED_TICKS)
                suspended = true;
            return suspended;
        }
    }
}
