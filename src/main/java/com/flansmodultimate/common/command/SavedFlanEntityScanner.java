package com.flansmodultimate.common.command;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
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

    record Source(ResourceLocation dimension, Path entitiesDirectory) {}
    record Entry(ResourceLocation dimension, String id, String type, double x, double y, double z,
        int chunkX, int chunkZ) {}
    record Result(List<Entry> entries, int failedChunks) {}

    private SavedFlanEntityScanner() {}

    static Result scan(List<Source> sources)
    {
        List<Entry> entries = new ArrayList<>();
        int failures = 0;
        for (Source source : sources)
        {
            Path directory = source.entitiesDirectory();
            if (!Files.isDirectory(directory))
                continue;
            try (Stream<Path> paths = Files.list(directory))
            {
                for (Path file : paths.filter(Files::isRegularFile).toList())
                {
                    Matcher matcher = REGION_NAME.matcher(file.getFileName().toString());
                    if (!matcher.matches())
                        continue;
                    try (RegionFile region = new RegionFile(file, directory, false))
                    {
                        int regionX = Integer.parseInt(matcher.group(1));
                        int regionZ = Integer.parseInt(matcher.group(2));
                        for (int localZ = 0; localZ < 32; localZ++)
                        {
                            for (int localX = 0; localX < 32; localX++)
                            {
                                ChunkPos chunk = new ChunkPos(regionX * 32 + localX, regionZ * 32 + localZ);
                                if (!region.hasChunk(chunk))
                                    continue;
                                try (DataInputStream input = region.getChunkDataInputStream(chunk))
                                {
                                    if (input != null)
                                        readChunk(NbtIo.read(input), source.dimension(), chunk, entries);
                                }
                                catch (IOException | RuntimeException exception)
                                {
                                    failures++;
                                }
                            }
                        }
                    }
                    catch (IOException | RuntimeException exception)
                    {
                        failures++;
                    }
                }
            }
            catch (IOException exception)
            {
                failures++;
            }
        }
        return new Result(List.copyOf(entries), failures);
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
            String type = tag.contains("driveable_type", Tag.TAG_STRING) ? tag.getString("driveable_type")
                : tag.contains("Type", Tag.TAG_STRING) ? tag.getString("Type") : tag.getString("type");
            entries.add(new Entry(dimension, id, type, position.getDouble(0), position.getDouble(1),
                position.getDouble(2), chunk.x, chunk.z));
        }
    }

    private static boolean isFlanEntity(String id)
    {
        return switch (id)
        {
            case "flansmodultimate:vehicle", "flansmodultimate:plane", "flansmodultimate:mecha",
                "flansmodultimate:deployed_gun", "flansmodultimate:aa_gun" -> true;
            default -> false;
        };
    }
}
