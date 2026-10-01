package com.flansmodultimate.common.command;

import com.flansmodultimate.platform.world.LevelFilePlatform;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.RegionFile;

import java.io.DataOutputStream;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SavedFlanEntityScannerTest
{
    @TempDir Path directory;

    @Test
    void readsSavedPlanesWithoutLoadingChunks() throws Exception
    {
        ChunkPos chunk = new ChunkPos(-1, 3);
        Path regionPath = directory.resolve("r.-1.0.mca");
        CompoundTag plane = new CompoundTag();
        plane.putString("id", "flansmodultimate:plane");
        plane.putString("driveable_type", "ec665");
        ListTag position = new ListTag();
        position.add(net.minecraft.nbt.DoubleTag.valueOf(-4.5D));
        position.add(net.minecraft.nbt.DoubleTag.valueOf(80D));
        position.add(net.minecraft.nbt.DoubleTag.valueOf(50.25D));
        plane.put("Pos", position);
        CompoundTag unrelated = plane.copy();
        unrelated.putString("id", "minecraft:pig");
        ListTag entities = new ListTag();
        entities.add(plane);
        entities.add(unrelated);
        CompoundTag root = new CompoundTag();
        root.put("Entities", entities);
        ResourceLocation dimension = ResourceLocation.fromNamespaceAndPath("minecraft", "overworld");
        try (RegionFile region = LevelFilePlatform.openEntityRegion(regionPath, directory, dimension);
             DataOutputStream output = region.getChunkDataOutputStream(chunk))
        {
            NbtIo.write(root, output);
        }

        SavedFlanEntityScanner.Result result = SavedFlanEntityScanner.scan(
            List.of(new SavedFlanEntityScanner.Source(dimension, directory)));

        assertEquals(0, result.failedChunks());
        assertEquals(List.of(new SavedFlanEntityScanner.Entry(dimension, "flansmodultimate:plane", "ec665",
            -4.5D, 80D, 50.25D, -1, 3)), result.entries());
    }
}
