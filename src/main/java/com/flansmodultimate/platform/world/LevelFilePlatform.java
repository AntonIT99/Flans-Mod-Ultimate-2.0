package com.flansmodultimate.platform.world;

import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;

import java.io.IOException;
import java.nio.file.Path;

/** Version boundary for compressed NBT files in a world folder, such as {@code level.dat}. */
public final class LevelFilePlatform
{
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
}
