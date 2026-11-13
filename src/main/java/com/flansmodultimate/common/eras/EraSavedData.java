package com.flansmodultimate.common.eras;

import com.flansmodultimate.platform.world.FlanSavedData;
import com.flansmodultimate.platform.world.SavedDataPlatform;
import com.flansmodultimate.util.FlansLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/** The world's server-wide and per-dimension era restrictions, kept with the overworld. */
public final class EraSavedData extends FlanSavedData
{
    private static final String NAME = "flansmodultimate_eras";
    private static final String NBT_SERVER = "server";
    private static final String NBT_DIMENSIONS = "dimensions";

    @Nullable
    private EraRestriction server;
    private final Map<String, EraRestriction> dimensions = new TreeMap<>();

    public static EraSavedData get(MinecraftServer minecraftServer)
    {
        return SavedDataPlatform.computeIfAbsent(minecraftServer.overworld(), NAME, EraSavedData::new, (tag, registries) -> load(tag));
    }

    public static EraSavedData load(CompoundTag tag)
    {
        EraSavedData data = new EraSavedData();
        if (tag.contains(NBT_SERVER, Tag.TAG_STRING))
            data.server = read(tag.getString(NBT_SERVER), "server");
        CompoundTag dimensions = tag.getCompound(NBT_DIMENSIONS);
        for (String dimension : dimensions.getAllKeys())
        {
            EraRestriction restriction = read(dimensions.getString(dimension), dimension);
            if (restriction != null)
                data.dimensions.put(dimension, restriction);
        }
        return data;
    }

    public Optional<EraRestriction> getServer()
    {
        return Optional.ofNullable(server);
    }

    public Optional<EraRestriction> getDimension(String dimension)
    {
        return Optional.ofNullable(dimensions.get(dimension));
    }

    public Map<String, EraRestriction> getDimensions()
    {
        return Collections.unmodifiableMap(dimensions);
    }

    public void setServer(@Nullable EraRestriction restriction)
    {
        server = restriction;
        setDirty();
    }

    public void setDimension(String dimension, @Nullable EraRestriction restriction)
    {
        if (restriction == null)
            dimensions.remove(dimension);
        else
            dimensions.put(dimension, restriction);
        setDirty();
    }

    @Override
    @NotNull
    public CompoundTag save(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries)
    {
        if (server != null)
            tag.putString(NBT_SERVER, server.toString());
        CompoundTag dimensionTag = new CompoundTag();
        dimensions.forEach((dimension, restriction) -> dimensionTag.putString(dimension, restriction.toString()));
        tag.put(NBT_DIMENSIONS, dimensionTag);
        return tag;
    }

    @Nullable
    private static EraRestriction read(String text, String scope)
    {
        try
        {
            return EraRestriction.parse(text);
        }
        catch (IllegalArgumentException exception)
        {
            FlansLog.log.warn("Dropping the saved {} era restriction '{}': {}", scope, text, exception.getMessage());
            return null;
        }
    }
}
