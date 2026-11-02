package com.wolffsmod.npcs.diag;

import com.wolffsmod.npcs.NpcsCommonConfig;
import com.wolffsmod.npcs.diag.NpcServerSettings.Setting;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;

/**
 * The share of target and path searches every NPC together may start in one server tick, when an operator limits them. A
 * search over the limit waits for a later tick. Searches waiting a second share a small additional allowance, so overdue
 * requests make progress without all bypassing the limit together. Server thread.
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
    /** Ticks before a deferred search qualifies for the bounded overdue allowance. */
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
    private static int overdueTargetSearches;
    private static int overduePathSearches;
    private static final Priority targetPriority = new Priority();
    private static final Priority pathPriority = new Priority();
    private static final Priority coverPriority = new Priority();
    private static int coverRays;
    private static int urgentCoverRays;
    private static int overdueCoverRays;
    /** Cover planning reserves a conservative upper bound on its raycasts; safety rechecks remain immediate. */
    private static final int COVER_RAYS = 2048;
    private static final int URGENT_COVER_RAYS = 512;
    private static final int OVERDUE_COVER_RAYS = 256;
    private static int targetDeferred;
    private static int pathDeferred;
    private static int lastTargetDeferred;
    private static int lastPathDeferred;
    private static double autoTargetAllowance = MAX_AUTO_TARGET_SEARCHES;
    private static double autoPathAllowance = MAX_AUTO_PATH_SEARCHES;
    /** The limits in force this tick: the settings' own, or the worked-out ones; 0 for no limit. */
    private static int targetLimit;
    private static int pathLimit;

    /** Remembers only the oldest denied age, so later NPCs can overtake recently served ones without a request queue. */
    private static final class Priority
    {
        private long oldestDeferred = Long.MAX_VALUE;
        private long minimumWait = MAX_WAIT;

        private void start(int now)
        {
            minimumWait = oldestDeferred == Long.MAX_VALUE ? MAX_WAIT : Math.max(MAX_WAIT, now - oldestDeferred);
            oldestDeferred = Long.MAX_VALUE;
        }

        private boolean allows(long waited)
        {
            return waited >= minimumWait;
        }

        private void defer(long waited)
        {
            oldestDeferred = Math.min(oldestDeferred, tick - waited);
        }

        private void clear()
        {
            oldestDeferred = Long.MAX_VALUE;
            minimumWait = MAX_WAIT;
        }
    }

    /**
     * @param waited
     *            ticks this NPC's search has been put off so far
     * @return whether a band search may start now; one that may is counted
     */
    public static boolean targetSearch(MinecraftServer server, long waited)
    {
        start(server);
        if (targetLimit > 0 && targetSearches >= targetLimit)
        {
            if (!overdue(waited, overdueTargetSearches, targetLimit) || !targetPriority.allows(waited))
            {
                targetPriority.defer(waited);
                targetDeferred++;
                return false;
            }
            overdueTargetSearches++;
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
        if (pathLimit > 0 && pathSearches >= pathLimit)
        {
            if (!overdue(waited, overduePathSearches, pathLimit) || !pathPriority.allows(waited))
            {
                pathPriority.defer(waited);
                pathDeferred++;
                return false;
            }
            overduePathSearches++;
        }
        pathSearches++;
        return true;
    }

    /** Older requests get at most a quarter of the limit in extra starts, with one place even at the smallest limit. */
    static boolean overdue(long waited, int used, int limit)
    {
        return waited >= MAX_WAIT && used < Math.max(1, limit / 4);
    }

    /** Reserves enough rays for one cover candidate, so a deferred candidate is never mistaken for exposed ground. */
    public static boolean coverSearch(MinecraftServer server, int rays, long waited, boolean urgent)
    {
        start(server);
        if (coverRays + rays <= COVER_RAYS)
        {
            coverRays += rays;
            return true;
        }
        if (urgent && urgentCoverRays + rays <= URGENT_COVER_RAYS)
        {
            urgentCoverRays += rays;
            return true;
        }
        if (coverPriority.allows(waited) && overdueCoverRays + rays <= OVERDUE_COVER_RAYS)
        {
            overdueCoverRays += rays;
            return true;
        }
        coverPriority.defer(waited);
        return false;
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
        targetPriority.clear();
        pathPriority.clear();
        coverPriority.clear();
    }

    private static void start(MinecraftServer server)
    {
        if (server.getTickCount() == tick)
            return;
        tick = server.getTickCount();
        targetPriority.start(tick);
        pathPriority.start(tick);
        coverPriority.start(tick);
        lastTargetDeferred = targetDeferred;
        lastPathDeferred = pathDeferred;
        targetSearches = 0;
        pathSearches = 0;
        overdueTargetSearches = 0;
        overduePathSearches = 0;
        coverRays = 0;
        urgentCoverRays = 0;
        overdueCoverRays = 0;
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
        double average = server.getCurrentSmoothedTickTime();
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
