package com.wolffsmod.npcs.sim;

import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.combat.NpcWeaponSettings;
import com.wolffsmod.npcs.diag.NpcProfiler;
import com.wolffsmod.npcs.diag.NpcServerSettings;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;

import java.util.Comparator;
import java.util.List;

/**
 * Looks for a target in distance bands, each on its own schedule, instead of testing everything within aggro range on
 * every search.
 *
 * <p>
 * The surroundings up to 32 blocks are searched often and on a cadence shared by all NPCs, so a formation reacts
 * together. Each band farther out is searched more rarely and at a moment of its own per NPC, so a crowd does not
 * search the horizon in the same tick. Within a band the nearest candidates are tested first and the search stops at
 * the first one that qualifies, which saves the line-of-sight checks on everything behind it.
 * </p>
 */
public final class NpcTargetSearch
{
    /** Outer edge of each band, in blocks. */
    private static final double[] BANDS = {32D, 64D, 128D, 256D, 384D, 512D};
    /** Tactical manoeuvres carry an NPC at least this far past its aggro range before it gives its target up. */
    private static final int MIN_TACTICAL_MARGIN = 8;

    private final long[] searched = new long[BANDS.length];
    private boolean started;

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
        long now = npc.level().getGameTime();
        if (!started)
        {
            started = true;
            searched[0] = Long.MIN_VALUE;
            for (int band = 1; band < BANDS.length; band++)
                searched[band] = now - Math.floorMod(npc.getId() * 7919L, interval(band, near, far));
        }
        conditions.range(range);
        double inner = 0D;
        for (int band = 0; band < BANDS.length && inner < range; band++)
        {
            double outer = Math.min(BANDS[band], range);
            if (due(band, now, near, far))
            {
                searched[band] = now;
                LivingEntity found = scan(npc, conditions, band, inner, outer);
                if (found != null)
                    return found;
            }
            inner = outer;
        }
        return null;
    }

    private boolean due(int band, long now, int near, int far)
    {
        // The near band runs once per shared time slot rather than at a fixed age, so every NPC searches it together.
        if (band == 0)
            return searched[0] == Long.MIN_VALUE || Math.floorDiv(now, (long) near) != Math.floorDiv(searched[0], (long) near);
        return now - searched[band] >= interval(band, near, far);
    }

    /** @return ticks between searches of a band: the near interval for the first, rising evenly to the far one */
    static int interval(int band, int near, int far)
    {
        return Math.max(1, near + Math.round((far - near) * (float) band / (BANDS.length - 1)));
    }

    @Nullable
    private static LivingEntity scan(EntityNPCInterface npc, TargetingConditions conditions, int band, double inner, double outer)
    {
        long started = NpcProfiler.server() ? System.nanoTime() : 0L;
        double innerSquared = inner * inner;
        double outerSquared = outer * outer;
        List<LivingEntity> candidates = npc.level().getEntitiesOfClass(LivingEntity.class, npc.getBoundingBox().inflate(outer), entity ->
        {
            double distance = npc.distanceToSqr(entity);
            return entity != npc && entity.isAlive() && distance > innerSquared && distance <= outerSquared;
        });
        candidates.sort(Comparator.comparingDouble(npc::distanceToSqr));
        LivingEntity found = null;
        int tested = 0;
        for (int index = 0; index < candidates.size() && found == null; index++)
        {
            tested++;
            if (conditions.test(npc, candidates.get(index)))
                found = candidates.get(index);
        }
        if (started != 0L)
            NpcProfiler.targetSearch(band, candidates.size(), tested, found != null, System.nanoTime() - started);
        return found;
    }

    /** @return the NPC's own interval, or the server's when the NPC states none */
    static int interval(int own, int server)
    {
        return own > 0 ? own : Math.max(1, server);
    }

    /**
     * @return blocks beyond its aggro range at which the NPC keeps the target it has. Left to itself, an NPC that
     *         circles, stalks or dodges keeps it for as far as its manoeuvre carries it.
     */
    public static int margin(Mob mob)
    {
        if (!(mob instanceof EntityNPCInterface npc) || npc.getTarget() == null)
            return 0;
        int tactical = npc.ais.tacticalVariant == 0 ? 0 : Math.max(MIN_TACTICAL_MARGIN, npc.ais.getTacticalRange());
        return Math.max(tactical, NpcTuning.of(npc).get(Value.TARGET_MARGIN));
    }
}
