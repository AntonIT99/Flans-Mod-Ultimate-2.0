package com.wolffsmod.npcs.diag;

import com.wolffsmod.npcs.NpcsCommonConfig;
import com.wolffsmod.npcs.diag.NpcServerSettings.Setting;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;

/**
 * The share of target and path searches every NPC together may start in one server tick, when an operator limits them. A
 * search over the limit waits for a later tick; one that has waited a second goes ahead regardless, so no NPC is starved
 * while the server keeps above its limit. Server thread.
 *
 * <p>
 * With a tick time target configured the limits are worked out instead of set: while the server stays under the target the
 * allowance grows, above it the allowance is cut back, so a battle loses search cadence gradually rather than tick rate,
 * and an operator need not guess numbers that depend on their hardware. The allowance never falls below what keeps the
 * nearest NPCs searching, and at its ceiling it means no limit at all.
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcBudgets
{
    /** Ticks a search may be put off before it goes ahead whatever the limit. */
    public static final int MAX_WAIT = 20;

    /** Bounds of the worked-out allowances. At the ceiling the allowance counts as no limit. */
    static final int MIN_AUTO_TARGET_SEARCHES = 8;
    static final int MAX_AUTO_TARGET_SEARCHES = 4096;
    static final int MIN_AUTO_PATH_SEARCHES = 2;
    static final int MAX_AUTO_PATH_SEARCHES = 512;
    /** Growth per tick under the target, and the share kept per tick over it. */
    private static final double GROWTH = 1.05D;
    private static final double DECAY = 0.8D;

    private static int tick = -1;
    private static int targetSearches;
    private static int pathSearches;
    private static int targetDeferred;
    private static int pathDeferred;
    private static int lastTargetDeferred;
    private static int lastPathDeferred;
    private static double autoTargetAllowance = MAX_AUTO_TARGET_SEARCHES;
    private static double autoPathAllowance = MAX_AUTO_PATH_SEARCHES;
    /** The limits in force this tick: the settings' own, or the worked-out ones; 0 for no limit. */
    private static int targetLimit;
    private static int pathLimit;

    /**
     * @param waited
     *            ticks this NPC's search has been put off so far
     * @return whether a band search may start now; one that may is counted
     */
    public static boolean targetSearch(MinecraftServer server, long waited)
    {
        start(server);
        if (targetLimit > 0 && targetSearches >= targetLimit && waited < MAX_WAIT)
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
        if (pathLimit > 0 && pathSearches >= pathLimit && waited < MAX_WAIT)
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

    /** Forgets the worked-out allowances, as a stopping server does. */
    public static void clear()
    {
        tick = -1;
        autoTargetAllowance = MAX_AUTO_TARGET_SEARCHES;
        autoPathAllowance = MAX_AUTO_PATH_SEARCHES;
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
        chooseLimits(server);
    }

    /**
     * Sets this tick's limits: the settings' own, or ones worked out from how long the server's ticks are taking. The
     * allowance is steered towards the target rather than set from it, so one slow tick does not stall every NPC and one
     * fast tick does not undo a cutback.
     */
    private static void chooseLimits(MinecraftServer server)
    {
        double target = NpcsCommonConfig.budgetTargetMillisPerTick();
        if (target <= 0D)
        {
            NpcServerSettings settings = NpcServerSettings.get(server);
            targetLimit = settings.get(Setting.SEARCH_BUDGET);
            pathLimit = settings.get(Setting.PATH_BUDGET);
            return;
        }
        double average = server.getAverageTickTime();
        autoTargetAllowance = steer(autoTargetAllowance, average, target, MIN_AUTO_TARGET_SEARCHES, MAX_AUTO_TARGET_SEARCHES);
        autoPathAllowance = steer(autoPathAllowance, average, target, MIN_AUTO_PATH_SEARCHES, MAX_AUTO_PATH_SEARCHES);
        targetLimit = limitOf(autoTargetAllowance, MAX_AUTO_TARGET_SEARCHES);
        pathLimit = limitOf(autoPathAllowance, MAX_AUTO_PATH_SEARCHES);
    }

    /**
     * @param average
     *            the server's average tick time in milliseconds
     * @param target
     *            the tick time it is to keep
     * @return the allowance after one tick: grown a little while the server stays under the target, cut back while it is
     *         over, so one slow tick stalls no NPC and one fast tick undoes no cutback
     */
    static double steer(double allowance, double average, double target, int minimum, int maximum)
    {
        return Mth.clamp(allowance * (average > target ? DECAY : GROWTH), minimum, maximum);
    }

    /** @return the limit an allowance stands for; at its ceiling it means no limit at all */
    static int limitOf(double allowance, int maximum)
    {
        return allowance >= maximum ? 0 : (int) allowance;
    }
}
