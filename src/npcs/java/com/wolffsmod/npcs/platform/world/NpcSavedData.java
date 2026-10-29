package com.wolffsmod.npcs.platform.world;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Version boundary for the module's level saved data, written and read with registry context. Minecraft 1.21 already
 * passes the registries to {@code save} and loads through a {@link SavedData.Factory}; the 1.20.1 counterpart adapts its
 * one-argument {@code save} and the data storage's function pair.
 */
public abstract class NpcSavedData extends SavedData
{
    /** Loads the saved data of the level, or creates it when the level has none yet. */
    public static <T extends SavedData> T computeIfAbsent(ServerLevel level, String id, Supplier<T> constructor, BiFunction<CompoundTag, HolderLookup.Provider, T> loader)
    {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(constructor, loader), id);
    }
}
