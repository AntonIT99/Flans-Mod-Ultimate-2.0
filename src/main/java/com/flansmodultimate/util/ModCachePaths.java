package com.flansmodultimate.util;

import com.flansmodultimate.platform.PlatformPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Shared cache layout and migration of the caches used by version 2.1. */
public final class ModCachePaths
{
    public static final String DIRECTORY_NAME = ".flansmod-cache";
    public static final String PACK_LOCATIONS_FILE_NAME = "pack-locations.json";

    private ModCachePaths() {}

    public static Path root()
    {
        return root(PlatformPaths.gameDir());
    }

    public static Path root(Path gameDir)
    {
        return gameDir.resolve(DIRECTORY_NAME);
    }

    /** Generation state and asset, sound and model-source indexes of the content packs, one subfolder per kind. */
    public static Path contentPacks(Path gameDir)
    {
        return root(gameDir).resolve("content-packs");
    }

    /** Migrate only the two 2.1 cache files, retaining any existing new-layout entries. */
    public static void migrate(Path gameDir)
    {
        Path cache = root(gameDir);
        Path oldMuzzleDirectory = gameDir.resolve(".flancache");
        moveMissing(oldMuzzleDirectory.resolve("muzzle-measurements.json"), cache.resolve("muzzle-measurements.json"));
        FileUtils.deleteDirectoryIfEmpty(oldMuzzleDirectory);
        moveMissing(gameDir.resolve(".flansmod-pack-locations-cache.json"), cache.resolve(PACK_LOCATIONS_FILE_NAME));
    }

    private static void moveMissing(Path source, Path target)
    {
        if (!Files.isRegularFile(source) || Files.isSymbolicLink(source) || Files.exists(target))
            return;
        try
        {
            Files.createDirectories(target.getParent());
            Files.move(source, target);
        }
        catch (IOException | RuntimeException e)
        {
            FlansLog.log.warn("Could not migrate cache '{}' to '{}': {}", source, target, e.toString());
        }
    }
}
