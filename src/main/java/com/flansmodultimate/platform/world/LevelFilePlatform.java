package com.flansmodultimate.platform.world;

import net.minecraft.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.storage.RegionFile;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;

import java.io.IOException;
import java.nio.file.Path;

/** Version boundary for NBT files in a world folder, such as {@code level.dat} and region files. */
public final class LevelFilePlatform
{
    /** Labels the profiler events of region files opened outside a level's own storage. */
    private static final String REGION_PROFILER_LEVEL = "flansmodultimate";

    private LevelFilePlatform() {}

    public static CompoundTag readCompressed(Path file) throws IOException
    {
        return NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
    }

    public static void writeCompressed(CompoundTag tag, Path file) throws IOException
    {
        NbtIo.writeCompressed(tag, file);
    }

    /** Replaces {@code current} with {@code latest}, keeping the previous file as {@code backup}. */
    public static void safeReplaceFile(Path current, Path latest, Path backup)
    {
        Util.safeReplaceFile(current, latest, backup);
    }

    /**
     * Opens one region file of a dimension's {@code entities} directory without syncing writes. Minecraft 1.21
     * also takes a {@link RegionStorageInfo}, which only labels the file's profiler events.
     */
    public static RegionFile openEntityRegion(Path file, Path directory, ResourceLocation dimension) throws IOException
    {
        RegionStorageInfo info = new RegionStorageInfo(REGION_PROFILER_LEVEL,
            ResourceKey.create(Registries.DIMENSION, dimension), "entities");
        return new RegionFile(info, file, directory, false);
    }
}
