package com.wolffsmod.npcs.diag;

import com.flansmodultimate.platform.world.FlanSavedData;
import com.flansmodultimate.platform.world.SavedDataPlatform;
import com.wolffsmod.npcs.NpcsMod;
import com.wolffsmod.npcs.platform.CustomNpcsPlatform;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

import java.util.Arrays;
import java.util.List;

/**
 * Settings that hold for every NPC of a world rather than for one: the target-search intervals NPCs use unless they
 * state their own, which an operator can also hand to every NPC at once, whether target searches are profiled, the aggro
 * and firing ranges a new NPC starts with, which an operator can hand to every NPC as well, the ranges an AA gun,
 * vehicle, plane or mecha model lends the NPC wearing it, the limits that keep many NPCs from overloading the server,
 * whether every NPC is frozen, and the health and ammunition rules every NPC follows. Saved with the world, edited from
 * the Server page of the Wolff's Mod tab by operators.
 */
public final class NpcServerSettings extends FlanSavedData
{
    public static final int DEFAULT_NEAR_INTERVAL = 10;
    public static final int DEFAULT_FAR_INTERVAL = 60;
    public static final int MAX_NEAR_INTERVAL = 200;
    public static final int MAX_FAR_INTERVAL = 600;
    /** Blocks within which a new NPC notices an enemy, until an operator chooses otherwise; Custom NPCs uses 16. */
    public static final int DEFAULT_AGGRO_RANGE = 64;
    /** Blocks within which a new NPC fires at its target, until an operator chooses otherwise; Custom NPCs uses 15. */
    public static final int DEFAULT_FIRING_RANGE = 64;
    /**
     * Blocks within which an NPC wearing an AA gun, vehicle, plane or mecha model notices and fires at an enemy, while it
     * inherits the model's ranges: machines engage farther out than infantry.
     */
    public static final int DEFAULT_MODEL_RANGE = 128;
    /** The farthest range saved; each NPC is held to the server's NPC tracking range besides. */
    public static final int MAX_RANGE = 512;
    /** Blocks within which players see the name tags of NPCs, until an operator chooses otherwise; vanilla uses 64. */
    public static final int DEFAULT_NAME_TAG_RANGE = 64;
    private static final String NAME = NpcsMod.MOD_ID + "_settings";
    private static final String GENERATION_KEY = "RangeGeneration";
    private static final String SEARCH_GENERATION_KEY = "SearchGeneration";
    /** Ticks between two profiling summaries in the server log. */
    private static final int PROFILE_INTERVAL = 200;

    /**
     * One setting of the whole server: its value for a new world, its bounds, and the key it is saved under, or none for one
     * that is not saved. A switch holds 1 for on; a choice holds its index. Packets carry the values in this order, so a
     * new setting goes at the end.
     */
    public enum Setting
    {
        NEAR_INTERVAL(DEFAULT_NEAR_INTERVAL, 1, MAX_NEAR_INTERVAL, "SearchNearInterval"), FAR_INTERVAL(DEFAULT_FAR_INTERVAL, 1, MAX_FAR_INTERVAL, "SearchFarInterval"),
        /** Whether target searches are counted and summarised in the server log. Not saved: it is a diagnostic. */
        PROFILING(0, 0, 1, null), AGGRO_RANGE(DEFAULT_AGGRO_RANGE, 1, MAX_RANGE, "AggroRange"), FIRING_RANGE(DEFAULT_FIRING_RANGE, 1, MAX_RANGE, "FiringRange"), MODEL_AGGRO_RANGE(DEFAULT_MODEL_RANGE,
            1, MAX_RANGE, "ModelAggroRange"), MODEL_FIRING_RANGE(DEFAULT_MODEL_RANGE, 1, MAX_RANGE, "ModelFiringRange"),
        /** Band searches all NPCs together may start in one tick; 0 for no limit. */
        SEARCH_BUDGET(0, 0, 10000, "SearchBudget"),
        /** Path searches all NPCs together may start in one tick; 0 for no limit. */
        PATH_BUDGET(0, 0, 1000, "PathBudget"),
        /** NPCs that Stay active may keep their chunks ticking at once; 0 for no limit. */
        STAY_ACTIVE_LIMIT(0, 0, 4096, "StayActiveLimit"),
        /** Whether every NPC stands still: the switch Custom NPCs' remote calls Freeze NPCs, saved with the world. */
        FREEZE_AI(0, 0, 1, "FreezeNpcs"),
        /** Percent of its own most health every NPC has. */
        HEALTH_PERCENT(100, 10, 1000, "HealthPercent"),
        /** Index into {@link AmmoPolicy}. */
        AMMO_POLICY(0, 0, AmmoPolicy.values().length - 1, "AmmoPolicy"),
        /** Blocks within which players see the name tags of NPCs; Custom NPCs uses about 22.6. Sent to every player. */
        NAME_TAG_RANGE(DEFAULT_NAME_TAG_RANGE, 0, MAX_RANGE, "NameTagRange");

        @Getter
        private final int fallback;
        @Getter
        private final int minimum;
        @Getter
        private final int maximum;
        @Nullable
        private final String key;

        Setting(int fallback, int minimum, int maximum, @Nullable String key)
        {
            this.fallback = fallback;
            this.minimum = minimum;
            this.maximum = maximum;
            this.key = key;
        }

        public int clamp(int value)
        {
            return Mth.clamp(value, minimum, maximum);
        }

        /** @return the values of a new world, in order */
        public static int[] fallbacks()
        {
            return Arrays.stream(values()).mapToInt(Setting::getFallback).toArray();
        }
    }

    /** Whether NPCs use up the ammunition they fire. */
    public enum AmmoPolicy
    {
        /** As each NPC's own Use up ammunition switch says. */
        PER_NPC,
        /** No NPC uses up its ammunition. */
        NEVER,
        /** Every NPC uses up its ammunition. */
        ALWAYS
    }

    private final int[] values = Setting.fallbacks();
    /**
     * How many times the ranges were applied to every NPC. An NPC remembers the count it last took the ranges at, so an
     * NPC that was not loaded at the time takes them when it is.
     */
    @Getter
    private int rangeGeneration;
    /** How many times the search intervals were handed to every NPC, remembered by each NPC the same way. */
    @Getter
    private int searchGeneration;
    /** The freeze switch last handed to Custom NPCs, 1 for frozen, or -1 before the first server tick of the world. */
    private int appliedFreeze = -1;

    public static NpcServerSettings get(MinecraftServer server)
    {
        return SavedDataPlatform.computeIfAbsent(server.overworld(), NAME, NpcServerSettings::new, NpcServerSettings::load);
    }

    /**
     * @return the settings of the server the entity is on, or null on the client or off the server thread, where the saved
     *         data must not be touched
     */
    @Nullable
    public static NpcServerSettings of(Entity entity)
    {
        MinecraftServer server = entity.getServer();
        return server != null && server.isSameThread() ? get(server) : null;
    }

    // SavedDataPlatform passes the registries; these settings hold no registry data.
    @SuppressWarnings("unused")
    private static NpcServerSettings load(CompoundTag tag, HolderLookup.Provider registries)
    {
        NpcServerSettings settings = new NpcServerSettings();
        for (Setting setting : Setting.values())
        {
            if (setting.key != null && tag.contains(setting.key, Tag.TAG_INT))
                settings.values[setting.ordinal()] = setting.clamp(tag.getInt(setting.key));
        }
        settings.rangeGeneration = tag.getInt(GENERATION_KEY);
        settings.searchGeneration = tag.getInt(SEARCH_GENERATION_KEY);
        return settings;
    }

    @Override
    @NotNull
    public CompoundTag save(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries)
    {
        for (Setting setting : Setting.values())
        {
            if (setting.key != null)
                tag.putInt(setting.key, get(setting));
        }
        tag.putInt(GENERATION_KEY, rangeGeneration);
        tag.putInt(SEARCH_GENERATION_KEY, searchGeneration);
        return tag;
    }

    public int get(Setting setting)
    {
        return values[setting.ordinal()];
    }

    /** @return every value, in the order of {@link Setting} */
    public List<Integer> all()
    {
        return Arrays.stream(values).boxed().toList();
    }

    public int getSearchNearInterval()
    {
        return get(Setting.NEAR_INTERVAL);
    }

    public int getSearchFarInterval()
    {
        return get(Setting.FAR_INTERVAL);
    }

    public boolean isProfiling()
    {
        return get(Setting.PROFILING) == 1;
    }

    public int getAggroRange()
    {
        return get(Setting.AGGRO_RANGE);
    }

    public int getFiringRange()
    {
        return get(Setting.FIRING_RANGE);
    }

    public int getModelAggroRange()
    {
        return get(Setting.MODEL_AGGRO_RANGE);
    }

    public int getModelFiringRange()
    {
        return get(Setting.MODEL_FIRING_RANGE);
    }

    public boolean isFrozen()
    {
        return get(Setting.FREEZE_AI) == 1;
    }

    public AmmoPolicy ammoPolicy()
    {
        return AmmoPolicy.values()[get(Setting.AMMO_POLICY)];
    }

    /**
     * Takes the values a client sent, in the order of {@link Setting}; a missing value keeps the current one.
     *
     * @return whether the share of health every NPC has changed
     */
    public boolean set(List<Integer> sent)
    {
        int health = get(Setting.HEALTH_PERCENT);
        for (Setting setting : Setting.values())
        {
            if (setting.ordinal() < sent.size())
                values[setting.ordinal()] = setting.clamp(sent.get(setting.ordinal()));
        }
        setDirty();
        NpcProfiler.setServer(isProfiling() || ServerBenchmark.running());
        return health != get(Setting.HEALTH_PERCENT);
    }

    /** Hands the ranges to every NPC of the world: loaded NPCs take them within a second, the others when they load. */
    public void applyRangesToAll()
    {
        rangeGeneration++;
        setDirty();
    }

    /**
     * Makes every NPC of the world follow the server's search intervals: loaded NPCs drop their own within a second, the
     * others when they load.
     */
    public void applySearchToAll()
    {
        searchGeneration++;
        setDirty();
    }

    /**
     * Keeps Custom NPCs' freeze switch and the saved one in step: the saved one is handed over when the world loads and
     * when an operator turns it here, and a turn of Custom NPCs' remote is saved.
     */
    private void syncFreeze()
    {
        int saved = get(Setting.FREEZE_AI);
        if (appliedFreeze != saved)
        {
            CustomNpcsPlatform.setFrozen(saved == 1);
            appliedFreeze = saved;
        }
        else if (CustomNpcsPlatform.frozen() != (saved == 1))
        {
            appliedFreeze = CustomNpcsPlatform.frozen() ? 1 : 0;
            values[Setting.FREEZE_AI.ordinal()] = appliedFreeze;
            setDirty();
        }
    }

    /**
     * Writes the profiled target searches to the server log every few seconds, then starts counting afresh, and keeps the
     * saved freeze switch in step with the one Custom NPCs' remote turns.
     */
    public static void serverTick(MinecraftServer server)
    {
        NpcServerSettings settings = get(server);
        settings.syncFreeze();
        if (server.getTickCount() % PROFILE_INTERVAL != 0 || ServerBenchmark.running() || !settings.isProfiling())
            return;
        for (int band = 0; band < NpcProfiler.BANDS; band++)
        {
            if (NpcProfiler.band(band).searches() > 0 && NpcsMod.log.isInfoEnabled())
                NpcsMod.log.info("NPC target search band {}: {}", band, NpcProfiler.band(band).summary());
        }
        NpcProfiler.resetServer();
    }
}
