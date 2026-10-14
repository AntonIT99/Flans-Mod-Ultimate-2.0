package com.flansmodultimate.common.command;

import com.flansmodultimate.platform.world.LevelFilePlatform;

import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.RegionFile;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** Read-only snapshot of Flan entities in entity region files; never loads world chunks. */
final class SavedFlanEntityScanner
{
    private static final Pattern REGION_NAME = Pattern.compile("r\\.(-?\\d+)\\.(-?\\d+)\\.mca");

    record Source(ResourceLocation dimension, Path entitiesDirectory)
    {}
    record Entry(ResourceLocation dimension, String id, String type, double x, double y, double z, int chunkX, int chunkZ)
    {}
    record Result(List<Entry> entries, int failedChunks)
    {}

    private SavedFlanEntityScanner()
    {}

    static Result scan(List<Source> sources)
    {
        List<Entry> entries = new ArrayList<>();
        int failures = 0;
        for (Source source : sources)
        {
            if (Files.isDirectory(source.entitiesDirectory()))
                failures += scanDirectory(source, entries);
        }
        return new Result(List.copyOf(entries), failures);
    }

    /** Scans every region file of one dimension and returns how many regions or chunks could not be read. */
    private static int scanDirectory(Source source, List<Entry> entries)
    {
        int failures = 0;
        try (Stream<Path> paths = Files.list(source.entitiesDirectory()))
        {
            for (Path file : paths.filter(Files::isRegularFile).toList())
                failures += scanRegion(source, file, entries);
        }
        catch (IOException exception)
        {
            failures++;
        }
        return failures;
    }

    private static int scanRegion(Source source, Path file, List<Entry> entries)
    {
        Matcher matcher = REGION_NAME.matcher(file.getFileName().toString());
        if (!matcher.matches())
            return 0;
        int failures = 0;
        try (RegionFile region = LevelFilePlatform.openEntityRegion(file, source.entitiesDirectory(), source.dimension()))
        {
            int regionX = Integer.parseInt(matcher.group(1));
            int regionZ = Integer.parseInt(matcher.group(2));
            for (int localZ = 0; localZ < 32; localZ++)
            {
                for (int localX = 0; localX < 32; localX++)
                {
                    ChunkPos chunk = new ChunkPos(regionX * 32 + localX, regionZ * 32 + localZ);
                    if (region.hasChunk(chunk) && !scanChunk(region, chunk, source.dimension(), entries))
                        failures++;
                }
            }
        }
        catch (IOException | RuntimeException exception)
        {
            failures++;
        }
        return failures;
    }

    /** Returns false when the chunk could not be read. */
    private static boolean scanChunk(RegionFile region, ChunkPos chunk, ResourceLocation dimension, List<Entry> entries)
    {
        try (DataInputStream input = region.getChunkDataInputStream(chunk))
        {
            if (input != null)
                readChunk(NbtIo.read(input), dimension, chunk, entries);
            return true;
        }
        catch (IOException | RuntimeException exception)
        {
            return false;
        }
    }

    private static void readChunk(CompoundTag root, ResourceLocation dimension, ChunkPos chunk, List<Entry> entries)
    {
        ListTag entities = root.getList("Entities", Tag.TAG_COMPOUND);
        for (int index = 0; index < entities.size(); index++)
        {
            CompoundTag tag = entities.getCompound(index);
            String id = tag.getString("id");
            if (!isFlanEntity(id))
                continue;
            ListTag position = tag.getList("Pos", Tag.TAG_DOUBLE);
            if (position.size() < 3)
                continue;
            String legacyTypeKey = tag.contains("Type", Tag.TAG_STRING) ? "Type" : "type";
            String type = tag.getString(tag.contains("driveable_type", Tag.TAG_STRING) ? "driveable_type" : legacyTypeKey);
            entries.add(new Entry(dimension, id, type, position.getDouble(0), position.getDouble(1), position.getDouble(2), chunk.x, chunk.z));
        }
    }

    private static boolean isFlanEntity(String id)
    {
        return switch (id)
        {
            case "flansmodultimate:vehicle", "flansmodultimate:plane", "flansmodultimate:mecha", "flansmodultimate:deployed_gun", "flansmodultimate:aa_gun" -> true;
            default -> false;
        };
    }
}
