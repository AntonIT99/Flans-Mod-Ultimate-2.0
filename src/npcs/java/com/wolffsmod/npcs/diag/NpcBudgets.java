package com.wolffsmod.npcs.diag;

import com.wolffsmod.npcs.diag.NpcServerSettings.Setting;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.server.MinecraftServer;

/**
 * The share of target and path searches every NPC together may start in one server tick, when an operator limits them. A
 * search over the limit waits for a later tick; one that has waited a second goes ahead regardless, so no NPC is starved
 * while the server keeps above its limit. Server thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcBudgets
{
    /** Ticks a search may be put off before it goes ahead whatever the limit. */
    public static final int MAX_WAIT = 20;

    private static int tick = -1;
    private static int targetSearches;
    private static int pathSearches;
    private static int targetDeferred;
    private static int pathDeferred;
    private static int lastTargetDeferred;
    private static int lastPathDeferred;

    /**
     * @param waited
     *            ticks this NPC's search has been put off so far
     * @return whether a band search may start now; one that may is counted
     */
    public static boolean targetSearch(MinecraftServer server, long waited)
    {
        start(server);
        int limit = NpcServerSettings.get(server).get(Setting.SEARCH_BUDGET);
        if (limit > 0 && targetSearches >= limit && waited < MAX_WAIT)
        {
            targetDeferred++;
            return false;
        }
        targetSearches++;
        return true;
    }

    /**
     * @param waited
     *            ticks this NPC's path search has been put off so far
     * @return whether a path search may start now; one that may is counted
     */
    public static boolean pathSearch(MinecraftServer server, long waited)
    {
        start(server);
        int limit = NpcServerSettings.get(server).get(Setting.PATH_BUDGET);
        if (limit > 0 && pathSearches >= limit && waited < MAX_WAIT)
        {
            pathDeferred++;
            return false;
        }
        pathSearches++;
        return true;
    }

    /**
     * Searches the limits put off in the last tick that searched.
     *
     * @param targets
     *            band searches put off
     * @param paths
     *            path searches put off
     */
    public record Deferred(int targets, int paths)
    {}

    /** @return the searches put off in the last tick that searched, for the census */
    public static Deferred deferred()
    {
        return new Deferred(lastTargetDeferred, lastPathDeferred);
    }

    private static void start(MinecraftServer server)
    {
        if (server.getTickCount() == tick)
            return;
        tick = server.getTickCount();
        lastTargetDeferred = targetDeferred;
        lastPathDeferred = pathDeferred;
        targetSearches = 0;
        pathSearches = 0;
        targetDeferred = 0;
        pathDeferred = 0;
    }
}
