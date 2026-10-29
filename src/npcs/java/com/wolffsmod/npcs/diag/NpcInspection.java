package com.wolffsmod.npcs.diag;

import com.wolffsmod.npcs.combat.NpcEquipment;
import com.wolffsmod.npcs.combat.NpcWeaponOptions;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.combat.NpcWeaponSettings;
import com.wolffsmod.npcs.diag.NpcServerSettings.Setting;
import com.wolffsmod.npcs.platform.CustomNpcsPlatform;
import com.wolffsmod.npcs.properties.NpcTypeProperties;
import com.wolffsmod.npcs.properties.NpcTypeProperty;
import com.wolffsmod.npcs.sim.NpcActivity;
import com.wolffsmod.npcs.sim.NpcDormancy;
import com.wolffsmod.npcs.sim.NpcMobility;
import com.wolffsmod.npcs.sim.NpcTargetSearch;
import com.wolffsmod.npcs.sim.NpcTuning;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import com.wolffsmod.npcs.tactics.NpcTacticState;
import com.wolffsmod.npcs.tactics.NpcTactics;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * What the Diagnosis page of the Wolff's Mod tab shows of one NPC, worked out by the server, which alone knows what the
 * model covers and what the server's settings make of the NPC's own: the values it acts on with where each comes from,
 * and what it did since the page last asked. Server thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcInspection
{
    private static final String KEY = "wolffsmodnpcs.diagnosis.";
    private static final double NANOS_PER_MILLI = 1.0E6D;

    /** @return a line per value the NPC acts on, saying where it comes from */
    public static List<Component> effective(EntityNPCInterface npc, MinecraftServer server)
    {
        NpcServerSettings settings = NpcServerSettings.get(server);
        NpcWeaponOptions options = ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions();
        NpcTuning tuning = NpcTuning.of(npc);
        NpcTypeProperties properties = NpcTypeProperties.of(npc);
        List<Component> lines = new ArrayList<>();
        lines.add(range("aggro", npc.stats.aggroRange, properties.covers(NpcTypeProperty.AGGRO_RANGE)));
        lines.add(range("firing", npc.stats.ranged.getRange(), properties.covers(NpcTypeProperty.RANGE)));
        if (options.enabled(Feature.PROGRESSIVE_SEARCH))
        {
            lines.add(Component.translatable(KEY + "search", interval(tuning.get(Value.SEARCH_NEAR_INTERVAL), settings.getSearchNearInterval()),
                interval(tuning.get(Value.SEARCH_FAR_INTERVAL), settings.getSearchFarInterval())));
            String bands = NpcTargetSearch.bandIntervals(npc, settings).stream().map(String::valueOf).collect(Collectors.joining(" / "));
            lines.add(Component.translatable(KEY + "bands", bands, Component.translatable(KEY + (options.enabled(Feature.STAGGER_NEAR_SEARCH) ? "staggered" : "together"))));
        }
        else
            lines.add(Component.translatable(KEY + "search_native"));
        lines.add(activity(npc, settings, options, tuning));
        lines.add(Component.translatable(KEY + "tactic", Component.translatable("wolffsmodnpcs.tuning.tactic." + NpcTactics.of(npc).name().toLowerCase(Locale.ROOT))));
        boolean consumes = NpcEquipment.enabled(npc, Feature.CONSUME_AMMO);
        lines.add(Component.translatable(KEY + (consumes ? "ammo_used" : "ammo_kept"), source(settings.ammoPolicy() == NpcServerSettings.AmmoPolicy.PER_NPC)));
        lines.add(Component.translatable(KEY + "health", Math.round(npc.getMaxHealth()), settings.get(Setting.HEALTH_PERCENT)));
        if (CustomNpcsPlatform.frozen())
            lines.add(Component.translatable(KEY + "frozen"));
        int searchBudget = settings.get(Setting.SEARCH_BUDGET);
        int pathBudget = settings.get(Setting.PATH_BUDGET);
        if (searchBudget > 0 || pathBudget > 0)
            lines.add(Component.translatable(KEY + "budgets", limit(searchBudget), limit(pathBudget)));
        return lines;
    }

    /** @return a line per thing the NPC did since the page last asked; the first request only starts the count */
    public static List<Component> live(EntityNPCInterface npc, MinecraftServer server)
    {
        int now = server.getTickCount();
        NpcWatch.Counters counters = NpcWatch.drain(npc, now);
        NpcWatch.watch(npc, now);
        List<Component> lines = new ArrayList<>();
        LivingEntity target = npc.getTarget();
        if (target == null)
            lines.add(Component.translatable(KEY + "live_no_target"));
        else
            lines.add(Component.translatable(KEY + "live_target", target.getName(), decimal(Math.sqrt(npc.distanceToSqr(target))),
                Component.translatable(KEY + (npc.getSensing().hasLineOfSight(target) ? "sight" : "no_sight"))));
        NpcTacticState tactic = NpcTacticState.of(npc);
        lines.add(Component.translatable(KEY + "live_tactic", tactic.getPhase().name().toLowerCase(Locale.ROOT),
            Component.translatable(KEY + (tactic.isReloading(npc.level().getGameTime()) ? "reloading" : "ready"))));
        lines.add(state(npc));
        if (counters == null || now - counters.getSince() <= 0)
        {
            lines.add(Component.translatable(KEY + "live_counting"));
            return lines;
        }
        double seconds = (now - counters.getSince()) / 20D;
        lines.add(Component.translatable(KEY + "live_window", decimal(seconds)));
        lines.add(Component.translatable(KEY + "live_ticks", counters.getTicks(), millis(counters.getTicks() == 0 ? 0L : counters.getTickNanos() / counters.getTicks())));
        lines.add(Component.translatable(KEY + "live_targets", counters.getTargetSearches(), millis(counters.getTargetNanos())));
        lines.add(Component.translatable(KEY + "live_paths", counters.getPathSearches(), counters.getPathFailures(), millis(counters.getPathNanos()), lastPath(counters.getLastPathNodes())));
        lines.add(Component.translatable(KEY + "live_attacks", counters.getAttacks()));
        return lines;
    }

    private static Component range(String name, int value, boolean model)
    {
        Component from = Component.translatable(KEY + (model ? "from_model" : "from_npc"));
        if (value >= NpcMobility.rangeLimit())
            from = Component.translatable(KEY + "from_limit", from, NpcMobility.rangeLimit());
        return Component.translatable(KEY + name, value, from);
    }

    /** @return the interval the NPC follows and where it comes from */
    private static Component interval(int own, int server)
    {
        return own > 0 ? Component.translatable(KEY + "ticks_own", own) : Component.translatable(KEY + "ticks_server", server);
    }

    private static Component activity(EntityNPCInterface npc, NpcServerSettings settings, NpcWeaponOptions options, NpcTuning tuning)
    {
        Component stay;
        if (!options.enabled(Feature.STAY_ACTIVE))
            stay = Component.translatable(KEY + "stay_off");
        else
        {
            boolean held = npc.level() instanceof ServerLevel level && NpcActivity.held(level).ids().contains(npc.getId());
            int limit = settings.get(Setting.STAY_ACTIVE_LIMIT);
            stay = Component.translatable(KEY + (held ? "stay_held" : "stay_idle"), tuning.get(Value.ACTIVE_RANGE), limit(limit));
        }
        int dormant = tuning.get(Value.DORMANT_RANGE);
        Component rest = dormant <= 0 ? Component.translatable(KEY + "dormant_off") : Component.translatable(KEY + "dormant_on", dormant);
        return Component.translatable(KEY + "activity", stay, rest);
    }

    private static Component state(EntityNPCInterface npc)
    {
        if (CustomNpcsPlatform.frozen())
            return Component.translatable(KEY + "live_frozen");
        if (NpcDormancy.dormant(npc))
            return Component.translatable(KEY + "live_dormant");
        boolean held = npc.level() instanceof ServerLevel level && NpcActivity.held(level).ids().contains(npc.getId());
        return Component.translatable(KEY + (held ? "live_held" : "live_awake"));
    }

    private static Component source(boolean own)
    {
        return Component.translatable(KEY + (own ? "from_npc" : "from_server"));
    }

    private static Component limit(int limit)
    {
        return limit > 0 ? Component.literal(Integer.toString(limit)) : Component.translatable(KEY + "no_limit");
    }

    private static Component lastPath(int nodes)
    {
        if (nodes < 0)
            return Component.translatable(KEY + "path_unknown");
        return nodes == 0 ? Component.translatable(KEY + "path_none") : Component.translatable(KEY + "path_nodes", nodes);
    }

    private static String millis(long nanos)
    {
        return String.format(Locale.ROOT, "%.3f", nanos / NANOS_PER_MILLI);
    }

    private static String decimal(double value)
    {
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
