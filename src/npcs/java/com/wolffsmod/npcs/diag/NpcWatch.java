package com.wolffsmod.npcs.diag;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;

/**
 * Counters of the NPCs whose live statistics an editor is showing: their ticks, target searches, path searches and
 * attacks since the editor last asked. A watch ends a few seconds after the last request, so closing the editor stops
 * the counting. While nothing is watched, a hook pays for one map size check. Server thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcWatch
{
    /** Server ticks a watch lasts after the editor last asked. */
    private static final int LINGER = 60;

    private static final Map<Integer, Counters> watched = new HashMap<>();

    /** What one NPC did since the editor last asked. */
    @Getter
    public static final class Counters
    {
        private long expires;
        /** Server tick the counting started at. */
        private int since;
        private int ticks;
        private long tickNanos;
        private int targetSearches;
        private long targetNanos;
        private int pathSearches;
        private int pathFailures;
        private long pathNanos;
        /** Nodes of the last path found, 0 for none, or -1 before any search. */
        private int lastPathNodes = -1;
        private int attacks;

        private Counters copy()
        {
            Counters copy = new Counters();
            copy.since = since;
            copy.ticks = ticks;
            copy.tickNanos = tickNanos;
            copy.targetSearches = targetSearches;
            copy.targetNanos = targetNanos;
            copy.pathSearches = pathSearches;
            copy.pathFailures = pathFailures;
            copy.pathNanos = pathNanos;
            copy.lastPathNodes = lastPathNodes;
            copy.attacks = attacks;
            return copy;
        }
    }

    /** Starts or extends the watch of an NPC; a new watch counts from now. */
    public static void watch(Entity npc, int now)
    {
        Counters counters = watched.computeIfAbsent(npc.getId(), id ->
        {
            Counters fresh = new Counters();
            fresh.since = now;
            return fresh;
        });
        counters.expires = (long) now + LINGER;
    }

    public static boolean watched(Entity npc)
    {
        return !watched.isEmpty() && watched.containsKey(npc.getId());
    }

    /**
     * @return what the NPC did since the last call, which starts the next count; null while it is not watched
     * @param now
     *            the current server tick
     */
    @Nullable
    public static Counters drain(Entity npc, int now)
    {
        Counters counters = watched.get(npc.getId());
        if (counters == null)
            return null;
        Counters drained = counters.copy();
        int last = counters.lastPathNodes;
        long expires = counters.expires;
        Counters fresh = new Counters();
        fresh.since = now;
        fresh.expires = expires;
        // The last path is what the NPC follows, whether or not it searched again since.
        fresh.lastPathNodes = last;
        watched.put(npc.getId(), fresh);
        return drained;
    }

    /** Ends the watches nobody asked about lately. At the end of every server tick. */
    public static void serverTick(int now)
    {
        if (!watched.isEmpty())
            watched.values().removeIf(counters -> counters.expires < now);
    }

    public static void clear()
    {
        watched.clear();
    }

    public static void tick(Entity npc, long nanos)
    {
        Counters counters = watched.get(npc.getId());
        if (counters == null)
            return;
        counters.ticks++;
        counters.tickNanos += nanos;
    }

    public static void targetSearch(Entity npc, long nanos)
    {
        Counters counters = watched.get(npc.getId());
        if (counters == null)
            return;
        counters.targetSearches++;
        counters.targetNanos += nanos;
    }

    /**
     * @param nodes
     *            nodes of the path found, or 0 for none
     */
    public static void pathSearch(Entity npc, long nanos, int nodes)
    {
        Counters counters = watched.get(npc.getId());
        if (counters == null)
            return;
        counters.pathSearches++;
        counters.pathNanos += nanos;
        if (nodes == 0)
            counters.pathFailures++;
        counters.lastPathNodes = nodes;
    }

    /** Counts an attack of a watched NPC; an attack the client plays is not counted. */
    public static void attack(Entity npc)
    {
        if (watched.isEmpty() || npc.level().isClientSide)
            return;
        Counters counters = watched.get(npc.getId());
        if (counters != null)
            counters.attacks++;
    }
}
