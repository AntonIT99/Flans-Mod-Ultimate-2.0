package com.flansmodultimate.platform.world;

import com.flansmodultimate.platform.item.ItemStackData;
import org.jetbrains.annotations.NotNull;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Saved data that is written with registry context. 1.20.1 saves without one, so this adapts its
 * one-argument {@code save} to the built-in registries; item stacks do not need more on this version.
 */
public abstract class FlanSavedData extends SavedData
{
    @Override
    @NotNull
    public final CompoundTag save(@NotNull CompoundTag tag)
    {
        return save(tag, ItemStackData.builtInRegistries());
    }

    @NotNull
    public abstract CompoundTag save(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries);
}
