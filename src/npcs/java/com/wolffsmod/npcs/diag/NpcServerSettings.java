package com.wolffsmod.npcs.diag;

import com.wolffsmod.npcs.NpcsMod;
import lombok.Getter;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.NotNull;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Settings that hold for every NPC of a world rather than for one: the target-search intervals NPCs use unless they
 * state their own, and whether target searches are profiled. Saved with the world, edited from the Server page of the
 * Wolff's Mod tab by operators.
 */
public final class NpcServerSettings extends SavedData
{
    public static final int DEFAULT_NEAR_INTERVAL = 10;
    public static final int DEFAULT_FAR_INTERVAL = 60;
    public static final int MAX_NEAR_INTERVAL = 200;
    public static final int MAX_FAR_INTERVAL = 600;
    private static final String NAME = NpcsMod.MOD_ID + "_settings";
    private static final String NEAR_KEY = "SearchNearInterval";
    private static final String FAR_KEY = "SearchFarInterval";
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

    public static NpcServerSettings get(MinecraftServer server)
    {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(NpcServerSettings::new, NpcServerSettings::load), NAME);
    }

    private static NpcServerSettings load(CompoundTag tag, HolderLookup.Provider registries)
    {
        NpcServerSettings settings = new NpcServerSettings();
        if (tag.contains(NEAR_KEY, Tag.TAG_INT))
            settings.searchNearInterval = clamp(tag.getInt(NEAR_KEY), MAX_NEAR_INTERVAL);
        if (tag.contains(FAR_KEY, Tag.TAG_INT))
            settings.searchFarInterval = clamp(tag.getInt(FAR_KEY), MAX_FAR_INTERVAL);
        return settings;
    }

    @Override
    @NotNull
    public CompoundTag save(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries)
    {
        tag.putInt(NEAR_KEY, searchNearInterval);
        tag.putInt(FAR_KEY, searchFarInterval);
        return tag;
    }

    public void set(int near, int far, boolean profile)
    {
        searchNearInterval = clamp(near, MAX_NEAR_INTERVAL);
        searchFarInterval = clamp(far, MAX_FAR_INTERVAL);
        profiling = profile;
        setDirty();
        NpcProfiler.setServer(profiling || ServerBenchmark.running());
    }

    private static int clamp(int interval, int maximum)
    {
        return Math.clamp(interval, 1, maximum);
    }

    /** Writes the profiled target searches to the server log every few seconds, then starts counting afresh. */
    public static void serverTick(ServerTickEvent.Post event)
    {
        MinecraftServer server = event.getServer();
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
