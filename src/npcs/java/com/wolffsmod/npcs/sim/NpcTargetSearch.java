package com.wolffsmod.npcs.sim;

import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.combat.NpcWeaponSettings;
import com.wolffsmod.npcs.diag.NpcBudgets;
import com.wolffsmod.npcs.diag.NpcProfiler;
import com.wolffsmod.npcs.diag.NpcServerSettings;
import com.wolffsmod.npcs.diag.NpcWatch;
import com.wolffsmod.npcs.platform.CustomNpcsPlatform;
import com.wolffsmod.npcs.sim.NpcTuning.Tactic;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import com.wolffsmod.npcs.tactics.NpcTactics;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Looks for a target in distance bands, each on its own schedule, instead of testing everything within aggro range on
 * every search.
 *
 * <p>
 * By default the surroundings up to 32 blocks are searched often and on a cadence shared by all NPCs, so a formation reacts
 * together. Optional overrides give each band its own interval and stagger nearby searches too. Each band farther out is searched at a moment of its own per NPC, so a crowd does not
 * search the horizon in the same tick. Within a band the nearest candidates are tested first and the search stops at
 * the first one that qualifies, which saves the line-of-sight checks on everything behind it.
 * </p>
 */
public final class NpcTargetSearch
{
    /** Outer edge of each band, in blocks. */
    private static final double[] BANDS = {32D, 64D, 128D, 256D, 384D, 512D};
    private static final Value[] BAND_INTERVALS = {Value.SEARCH_BAND_0_INTERVAL, Value.SEARCH_BAND_1_INTERVAL, Value.SEARCH_BAND_2_INTERVAL, Value.SEARCH_BAND_3_INTERVAL, Value.SEARCH_BAND_4_INTERVAL,
        Value.SEARCH_BAND_5_INTERVAL};
    /** Tactical manoeuvres carry an NPC at least this far past its aggro range before it gives its target up. */
    private static final int MIN_TACTICAL_MARGIN = 8;
    /** Custom NPCs' tactical variants that add no manoeuvre of their own. */
    private static final int RUSH = 0;
    private static final int NO_TACTIC = 6;

    /** Ranks the candidates of a band ahead of their distance, lower first; set by the Teams bots, which prefer some targets. */
    @FunctionalInterface
    public interface Priority
    {
        // The default ranks everything alike; the Teams bots install a ranking of their own at runtime.
        @SuppressWarnings("SameReturnValue")
        int rank(EntityNPCInterface npc, LivingEntity candidate);
    }

    private static final AtomicReference<Priority> PRIORITY = new AtomicReference<>((npc, candidate) -> 0);
    /** Candidates in the order a band tests them: by rank, then nearest first. */
    private static final Comparator<Ranked> RANKED_ORDER = Comparator.comparingInt(Ranked::rank).thenComparingDouble(Ranked::distance);

    private final long[] searched = new long[BANDS.length];
    private boolean started;
    /** Game time a due search was first put off by the server's limit, or {@link Long#MIN_VALUE} while none waits. */
    private long waitingSince = Long.MIN_VALUE;

    /**
     * A candidate of a band search.
     *
     * @param distance
     *            squared blocks from the searching NPC
     */
    private record Ranked(LivingEntity entity, int rank, double distance)
    {}

    /** Sets how candidates are ranked ahead of their distance. */
    public static void setPriority(Priority priority)
    {
        PRIORITY.set(priority);
    }

    public static boolean enabled(Mob mob)
    {
        return mob instanceof EntityNPCInterface npc && ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions().enabled(Feature.PROGRESSIVE_SEARCH);
    }

    /**
     * Searches the bands that are due.
     *
     * @param conditions
     *            the target goal's own conditions; their range is brought up to date with the given one
     * @param range
     *            the NPC's aggro range
     * @return the nearest qualifying entity of the nearest due band that has one, or null
     */
    @Nullable
    public LivingEntity find(EntityNPCInterface npc, TargetingConditions conditions, double range)
    {
        NpcTuning tuning = NpcTuning.of(npc);
        // An NPC without intervals of its own follows the ones set for the whole server.
        NpcServerSettings server = npc.getServer() == null ? null : NpcServerSettings.get(npc.getServer());
        int near = interval(tuning.get(Value.SEARCH_NEAR_INTERVAL), server == null ? NpcServerSettings.DEFAULT_NEAR_INTERVAL : server.getSearchNearInterval());
        int far = Math.max(near, interval(tuning.get(Value.SEARCH_FAR_INTERVAL), server == null ? NpcServerSettings.DEFAULT_FAR_INTERVAL : server.getSearchFarInterval()));
        var options = ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions();
        boolean advanced = options.enabled(Feature.ADVANCED_SEARCH_INTERVALS);
        boolean synchronizedNear = !options.enabled(Feature.STAGGER_NEAR_SEARCH);
        long now = npc.level().getGameTime();
        if (!started)
            initialize(npc, now, near, far, advanced, synchronizedNear);
        conditions.range(range);
        double inner = 0D;
        for (int band = 0; band < BANDS.length && inner < range; band++)
        {
            double outer = Math.min(BANDS[band], range);
            if (due(band, now, interval(band, near, far, tuning, advanced), synchronizedNear))
            {
                // Over the server's limit the band stays due, and the search waits for a later tick.
                if (!mayStart(npc, now))
                    return null;
                searched[band] = now;
                LivingEntity found = scan(npc, conditions, band, inner, outer);
                if (found != null)
                    return found;
            }
            inner = outer;
        }
        return null;
    }

    /**
     * @param server
     *            the server's settings, or null for their defaults
     * @return the ticks between searches of each band the NPC searches with, nearest first
     */
    public static List<Integer> bandIntervals(EntityNPCInterface npc, @Nullable NpcServerSettings server)
    {
        NpcTuning tuning = NpcTuning.of(npc);
        int near = interval(tuning.get(Value.SEARCH_NEAR_INTERVAL), server == null ? NpcServerSettings.DEFAULT_NEAR_INTERVAL : server.getSearchNearInterval());
        int far = Math.max(near, interval(tuning.get(Value.SEARCH_FAR_INTERVAL), server == null ? NpcServerSettings.DEFAULT_FAR_INTERVAL : server.getSearchFarInterval()));
        boolean advanced = ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions().enabled(Feature.ADVANCED_SEARCH_INTERVALS);
        List<Integer> intervals = new ArrayList<>(BANDS.length);
        for (int band = 0; band < BANDS.length; band++)
            intervals.add(interval(band, near, far, tuning, advanced));
        return List.copyOf(intervals);
    }

    /** @return whether the server's limit lets a due search start now; one that may not remembers since when it waits */
    private boolean mayStart(EntityNPCInterface npc, long now)
    {
        MinecraftServer server = npc.getServer();
        if (server == null || NpcBudgets.targetSearch(server, waitingSince == Long.MIN_VALUE ? 0L : now - waitingSince))
        {
            waitingSince = Long.MIN_VALUE;
            return true;
        }
        if (waitingSince == Long.MIN_VALUE)
            waitingSince = now;
        return false;
    }

    /** Shared nearby searches start immediately; other bands begin at their per-NPC phase. */
    private void initialize(EntityNPCInterface npc, long now, int near, int far, boolean advanced, boolean synchronizedNear)
    {
        started = true;
        searched[0] = Long.MIN_VALUE;
        NpcTuning tuning = NpcTuning.of(npc);
        for (int band = synchronizedNear ? 1 : 0; band < BANDS.length; band++)
            searched[band] = now - Math.floorMod(npc.getId() * 7919L, interval(band, near, far, tuning, advanced));
    }

    private boolean due(int band, long now, int ticks, boolean synchronizedNear)
    {
        // The near band runs once per shared time slot rather than at a fixed age, so every NPC searches it together.
        if (band == 0 && synchronizedNear)
            return searched[0] == Long.MIN_VALUE || Math.floorDiv(now, (long) ticks) != Math.floorDiv(searched[0], (long) ticks);
        return searched[band] == Long.MIN_VALUE || now - searched[band] >= ticks;
    }

    /** Zero inherits this band's interpolation; explicit intervals need not rise with distance. */
    static int interval(int band, int near, int far, NpcTuning tuning, boolean advanced)
    {
        return advanced ? interval(tuning.get(BAND_INTERVALS[band]), interval(band, near, far)) : interval(band, near, far);
    }

    /** @return ticks between searches of a band: the near interval for the first, rising evenly to the far one */
    static int interval(int band, int near, int far)
    {
        return Math.max(1, near + Math.round((far - near) * (float) band / (BANDS.length - 1)));
    }

    @Nullable
    private static LivingEntity scan(EntityNPCInterface npc, TargetingConditions conditions, int band, double inner, double outer)
    {
        boolean watched = NpcWatch.watched(npc);
        long started = NpcProfiler.server() || watched ? System.nanoTime() : 0L;
        double innerSquared = inner * inner;
        double outerSquared = outer * outer;
        List<LivingEntity> candidates = npc.level().getEntitiesOfClass(LivingEntity.class, npc.getBoundingBox().inflate(outer), entity ->
        {
            double distance = npc.distanceToSqr(entity);
            return entity != npc && entity.isAlive() && distance > innerSquared && distance <= outerSquared;
        });
        Priority priority = PRIORITY.get();
        // Each candidate's rank and distance are worked out once, not on every comparison of the sort.
        Ranked[] ranked = new Ranked[candidates.size()];
        for (int index = 0; index < ranked.length; index++)
        {
            LivingEntity candidate = candidates.get(index);
            ranked[index] = new Ranked(candidate, priority.rank(npc, candidate), npc.distanceToSqr(candidate));
        }
        Arrays.sort(ranked, RANKED_ORDER);
        LivingEntity found = null;
        int tested = 0;
        for (int index = 0; index < ranked.length && found == null; index++)
        {
            tested++;
            if (conditions.test(npc, ranked[index].entity()))
                found = ranked[index].entity();
        }
        if (started != 0L)
        {
            long nanos = System.nanoTime() - started;
            if (NpcProfiler.server())
                NpcProfiler.targetSearch(band, candidates.size(), tested, found != null, nanos);
            if (watched)
                NpcWatch.targetSearch(npc, nanos);
        }
        return found;
    }

    /**
     * @return blocks out to which the mob looks for a target, and keeps one before any margin: an NPC's aggro range, or its
     *         follow range where that reaches further. Custom NPCs resets the follow range to its navigation range whenever
     *         an NPC loads, and searching only that far in a straight line leaves an aircraft high above its targets blind
     *         to all but those almost under it, and drops a target as soon as its turn carries it out that far.
     */
    public static double range(Mob mob)
    {
        double follow = mob.getAttributeValue(Attributes.FOLLOW_RANGE);
        return mob instanceof EntityNPCInterface npc ? Math.max(follow, npc.stats.aggroRange) : follow;
    }

    /** @return the NPC's own interval, or the server's when the NPC states none */
    static int interval(int own, int server)
    {
        return own > 0 ? own : Math.max(1, server);
    }

    /**
     * @return blocks beyond its aggro range at which the NPC keeps the target it has. Left to itself, an NPC that
     *         circles, stalks or dodges keeps it for as far as its manoeuvre carries it, and one of the mod's own tactics for
     *         as far as its tactic distance. A plane keeps it for as far as coming round in its tightest circle carries it.
     */
    public static int margin(Mob mob)
    {
        if (!(mob instanceof EntityNPCInterface npc) || npc.getTarget() == null)
            return 0;
        Tactic tactic = NpcTactics.of(npc);
        int tactical;
        if (tactic != Tactic.CUSTOM_NPCS)
            tactical = tactic.usesDistance() ? Math.max(MIN_TACTICAL_MARGIN, NpcTactics.reach(npc, tactic)) : 0;
        else
            tactical = manoeuvres(CustomNpcsPlatform.tacticalVariant(npc)) ? Math.max(MIN_TACTICAL_MARGIN, CustomNpcsPlatform.tacticalRange(npc)) : 0;
        int turn = (int) Math.ceil(NpcVehicleState.of(npc).getFlight().keepMargin());
        return Math.max(Math.max(tactical, turn), NpcTuning.of(npc).get(Value.TARGET_MARGIN));
    }

    /** @return whether a Custom NPCs tactical variant moves the NPC beyond where it would attack from: not Rush, not None */
    static boolean manoeuvres(int tacticalVariant)
    {
        return tacticalVariant != RUSH && tacticalVariant != NO_TACTIC;
    }
}
