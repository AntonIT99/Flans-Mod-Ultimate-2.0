package com.wolffsmod.npcs.platform.world;

import org.jetbrains.annotations.NotNull;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Version boundary for the module's level saved data, written and read with registry context. 1.20.1 saves without one, so
 * this adapts its one-argument {@code save} to the built-in registries, and loads through the data storage's function
 * pair where 1.21 takes a factory.
 */
public abstract class NpcSavedData extends SavedData
{
    /** Loads the saved data of the level, or creates it when the level has none yet. */
    public static <T extends SavedData> T computeIfAbsent(ServerLevel level, String id, Supplier<T> constructor, BiFunction<CompoundTag, HolderLookup.Provider, T> loader)
    {
        return level.getDataStorage().computeIfAbsent(tag -> loader.apply(tag, level.registryAccess()), constructor, id);
    }

    @Override
    @NotNull
    public final CompoundTag save(@NotNull CompoundTag tag)
    {
        return save(tag, RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
    }

    // Part of the cross-version platform signature; the module's stores hold no registry data, item stacks on 1.21 would.
    @SuppressWarnings("unused")
    @NotNull
    public abstract CompoundTag save(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries);
}
