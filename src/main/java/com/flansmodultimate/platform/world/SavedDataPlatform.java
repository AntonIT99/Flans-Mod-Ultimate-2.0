package com.flansmodultimate.platform.world;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.function.BiFunction;
import java.util.function.Supplier;

/** Version boundary for loading or creating level saved data. */
public final class SavedDataPlatform
{
    private SavedDataPlatform() {}

    public static <T extends SavedData> T computeIfAbsent(ServerLevel level, String id, Supplier<T> constructor,
                                                          BiFunction<CompoundTag, HolderLookup.Provider, T> loader)
    {
        return level.getDataStorage().computeIfAbsent(tag -> loader.apply(tag, level.registryAccess()), constructor, id);
    }
}
