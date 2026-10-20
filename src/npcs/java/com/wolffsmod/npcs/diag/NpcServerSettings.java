package com.wolffsmod.npcs.diag;

import com.flansmodultimate.platform.world.FlanSavedData;
import com.flansmodultimate.platform.world.SavedDataPlatform;
import com.wolffsmod.npcs.NpcsMod;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/**
 * Settings that hold for every NPC of a world rather than for one: the target-search intervals NPCs use unless they
 * state their own, whether target searches are profiled, and the aggro and firing ranges a new NPC starts with, which
 * an operator can also hand to every NPC of the world at once. Saved with the world, edited from the Server page of the
 * Wolff's Mod tab by operators.
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
    /** The farthest range saved; each NPC is held to the server's NPC tracking range besides. */
    public static final int MAX_RANGE = 512;
    private static final String NAME = NpcsMod.MOD_ID + "_settings";
    private static final String NEAR_KEY = "SearchNearInterval";
    private static final String FAR_KEY = "SearchFarInterval";
    private static final String AGGRO_KEY = "AggroRange";
    private static final String FIRING_KEY = "FiringRange";
    private static final String GENERATION_KEY = "RangeGeneration";
    /** Ticks between two profiling summaries in the server log. */
    private static final int PROFILE_INTERVAL = 200;

    /** Ticks between searches of the first 32 blocks, for NPCs without an interval of their own. */
    @Getter
    private int searchNearInterval = DEFAULT_NEAR_INTERVAL;
    /** Ticks between searches of the farthest band, for NPCs without an interval of their own. */
    @Getter
    private int searchFarInterval = DEFAULT_FAR_INTERVAL;
    /** Whether target searches are counted and summarised in the server log. Not saved: it is a diagnostic. */
    @Getter
    private boolean profiling;
    /** The aggro range of a new NPC, and of every NPC once the ranges are applied to all. */
    @Getter
    private int aggroRange = DEFAULT_AGGRO_RANGE;
    /** The firing range of a new NPC, and of every NPC once the ranges are applied to all. */
    @Getter
    private int firingRange = DEFAULT_FIRING_RANGE;
    /**
     * How many times the ranges were applied to every NPC. An NPC remembers the count it last took the ranges at, so an
     * NPC that was not loaded at the time takes them when it is.
     */
    @Getter
    private int rangeGeneration;

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
        if (tag.contains(NEAR_KEY, Tag.TAG_INT))
            settings.searchNearInterval = clamp(tag.getInt(NEAR_KEY), MAX_NEAR_INTERVAL);
        if (tag.contains(FAR_KEY, Tag.TAG_INT))
            settings.searchFarInterval = clamp(tag.getInt(FAR_KEY), MAX_FAR_INTERVAL);
        if (tag.contains(AGGRO_KEY, Tag.TAG_INT))
            settings.aggroRange = clamp(tag.getInt(AGGRO_KEY), MAX_RANGE);
        if (tag.contains(FIRING_KEY, Tag.TAG_INT))
            settings.firingRange = clamp(tag.getInt(FIRING_KEY), MAX_RANGE);
        settings.rangeGeneration = tag.getInt(GENERATION_KEY);
        return settings;
    }

    @Override
    @NotNull
    public CompoundTag save(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries)
    {
        tag.putInt(NEAR_KEY, searchNearInterval);
        tag.putInt(FAR_KEY, searchFarInterval);
        tag.putInt(AGGRO_KEY, aggroRange);
        tag.putInt(FIRING_KEY, firingRange);
        tag.putInt(GENERATION_KEY, rangeGeneration);
        return tag;
    }

    public void set(int near, int far, boolean profile, int aggro, int firing)
    {
        searchNearInterval = clamp(near, MAX_NEAR_INTERVAL);
        searchFarInterval = clamp(far, MAX_FAR_INTERVAL);
        profiling = profile;
        aggroRange = clamp(aggro, MAX_RANGE);
        firingRange = clamp(firing, MAX_RANGE);
        setDirty();
        NpcProfiler.setServer(profiling || ServerBenchmark.running());
    }

    /** Hands the ranges to every NPC of the world: loaded NPCs take them within a second, the others when they load. */
    public void applyRangesToAll()
    {
        rangeGeneration++;
        setDirty();
    }

    private static int clamp(int value, int maximum)
    {
        return Mth.clamp(value, 1, maximum);
    }

    /** Writes the profiled target searches to the server log every few seconds, then starts counting afresh. */
    public static void serverTick(MinecraftServer server)
    {
        if (server.getTickCount() % PROFILE_INTERVAL != 0 || ServerBenchmark.running() || !get(server).profiling)
            return;
        for (int band = 0; band < NpcProfiler.BANDS; band++)
        {
            if (NpcProfiler.band(band).searches() > 0 && NpcsMod.log.isInfoEnabled())
                NpcsMod.log.info("NPC target search band {}: {}", band, NpcProfiler.band(band).summary());
        }
        NpcProfiler.resetServer();
    }
}
