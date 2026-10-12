package com.wolffsmod.npcs.diag;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.*;

/**
 * Counters the NPC hooks feed while something is measuring: how long NPCs take to tick, how long their path searches
 * and target searches take, and which path searches were slow. While nothing measures, a hook pays for one boolean
 * read. The server counters belong to the server thread and the render counters to the render thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcProfiler
{
    /** Distance bands of the progressive target search, as counted here. */
    public static final int BANDS = 6;
    /** A path search slower than this many milliseconds is recorded on its own. */
    public static final double SLOW_PATH_MILLIS = 2D;
    private static final int MAX_SLOW_PATHS = 200;
    private static final double NANOS_PER_MILLI = 1.0E6D;

    // Primitive flags, read by hooks on other threads than the one that sets them.
    private static volatile boolean server;
    private static volatile boolean client;

    // Server thread
    private static long npcTickNanos;
    private static int npcTicks;
    private static long pathNanos;
    private static int pathSearches;
    private static long targetNanos;
    private static final Band[] bandTotals = new Band[BANDS];
    private static final List<SlowPath> slowPaths = new ArrayList<>();
    private static int slowPathsDropped;

    // Render thread
    private static long npcRenderNanos;
    private static int npcRenders;

    static
    {
        for (int band = 0; band < BANDS; band++)
            bandTotals[band] = new Band();
    }

    /** Totals of one target-search band since the counters were last reset. */
    public static final class Band
    {
        private int searches;
        private long candidates;
        private long tested;
        private int acquired;
        private long nanos;

        public int searches()
        {
            return searches;
        }

        /** @return one line: searches, entities in the band, entities fully tested, targets found, time */
        public String summary()
        {
            return String.format(Locale.ROOT, "%d searches, %d candidates, %d tested, %d acquired, %.3f ms", searches, candidates, tested, acquired, nanos / NANOS_PER_MILLI);
        }

        private void clear()
        {
            searches = 0;
            candidates = 0L;
            tested = 0L;
            acquired = 0;
            nanos = 0L;
        }
    }

    /**
     * A path search that took long.
     *
     * @param distance
     *            blocks from the NPC to its nearest goal
     * @param found
     *            whether the search produced a path
     */
    public record SlowPath(long gameTime, String npc, int x, int y, int z, double distance, double millis, boolean found)
    {
        public String line()
        {
            return String.format(Locale.ROOT, "tick %d: %s at %d %d %d, goal %.0f blocks away, %.2f ms, %s", gameTime, npc, x, y, z, distance, millis, found ? "path found" : "no path");
        }
    }

    /** What the server hooks collected during one server tick. */
    public record ServerSample(double npcMillis, int npcTicks, double pathMillis, int pathSearches, double targetMillis)
    {}

    /** What the render hooks collected during one frame. */
    public record ClientSample(double npcMillis, int npcs)
    {}

    public static boolean server()
    {
        return server;
    }

    public static boolean client()
    {
        return client;
    }

    /** Starts or stops the server counters; starting clears them. Server thread. */
    public static void setServer(boolean active)
    {
        if (active && !server)
            resetServer();
        server = active;
    }

    /** Starts or stops the render counters. Render thread. */
    public static void setClient(boolean active)
    {
        if (active && !client)
        {
            npcRenderNanos = 0L;
            npcRenders = 0;
        }
        client = active;
    }

    public static void resetServer()
    {
        drainServer();
        for (Band band : bandTotals)
            band.clear();
        slowPaths.clear();
        slowPathsDropped = 0;
    }

    public static void npcTick(long nanos)
    {
        npcTickNanos += nanos;
        npcTicks++;
    }

    public static void pathSearch(long gameTime, String npc, int x, int y, int z, double distance, long nanos, boolean found)
    {
        pathNanos += nanos;
        pathSearches++;
        double millis = nanos / NANOS_PER_MILLI;
        if (millis < SLOW_PATH_MILLIS)
            return;
        if (slowPaths.size() < MAX_SLOW_PATHS)
            slowPaths.add(new SlowPath(gameTime, npc, x, y, z, distance, millis, found));
        else
            slowPathsDropped++;
    }

    public static void targetSearch(int band, int candidates, int tested, boolean acquired, long nanos)
    {
        targetNanos += nanos;
        Band totals = bandTotals[Math.max(0, Math.min(BANDS - 1, band))];
        totals.searches++;
        totals.candidates += candidates;
        totals.tested += tested;
        totals.nanos += nanos;
        if (acquired)
            totals.acquired++;
    }

    public static void npcRender(long nanos)
    {
        npcRenderNanos += nanos;
        npcRenders++;
    }

    /** @return what was collected since the last call, which starts the next tick's collection */
    public static ServerSample drainServer()
    {
        ServerSample sample = new ServerSample(npcTickNanos / NANOS_PER_MILLI, npcTicks, pathNanos / NANOS_PER_MILLI, pathSearches, targetNanos / NANOS_PER_MILLI);
        npcTickNanos = 0L;
        npcTicks = 0;
        pathNanos = 0L;
        pathSearches = 0;
        targetNanos = 0L;
        return sample;
    }

    /** @return what was collected since the last call, which starts the next frame's collection */
    public static ClientSample drainClient()
    {
        ClientSample sample = new ClientSample(npcRenderNanos / NANOS_PER_MILLI, npcRenders);
        npcRenderNanos = 0L;
        npcRenders = 0;
        return sample;
    }

    public static Band band(int band)
    {
        return bandTotals[band];
    }

    public static List<SlowPath> slowPaths()
    {
        return List.copyOf(slowPaths);
    }

    public static int slowPathsDropped()
    {
        return slowPathsDropped;
    }
}
