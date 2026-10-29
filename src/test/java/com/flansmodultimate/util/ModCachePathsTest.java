package com.flansmodultimate.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import static org.junit.jupiter.api.Assertions.*;

class ModCachePathsTest
{
    @TempDir
    Path gameDir;

    @Test
    void migratesVersion21CachesWithoutChangingContentsOrTimestamps() throws Exception
    {
        Path oldMuzzles = gameDir.resolve(".flancache/muzzle-measurements.json");
        Path oldLocations = gameDir.resolve(".flansmod-pack-locations-cache.json");
        FileTime modified = FileTime.fromMillis(123456789000L);
        for (Path file : java.util.List.of(oldMuzzles, oldLocations))
        {
            Files.createDirectories(file.getParent());
            Files.writeString(file, "cached data");
            Files.setLastModifiedTime(file, modified);
        }
        ModCachePaths.migrate(gameDir);
        for (Path file : java.util.List.of(ModCachePaths.root(gameDir).resolve("muzzle-measurements.json"), ModCachePaths.packsManager(gameDir).resolve(ModCachePaths.PACK_LOCATIONS_FILE_NAME)))
        {
            assertEquals("cached data", Files.readString(file));
            assertEquals(modified, Files.getLastModifiedTime(file));
        }
        assertFalse(Files.exists(gameDir.resolve(".flancache")));
        assertFalse(Files.exists(oldLocations));
        ModCachePaths.migrate(gameDir);
        assertEquals(modified, Files.getLastModifiedTime(ModCachePaths.root(gameDir).resolve("muzzle-measurements.json")));
    }

    @Test
    void doesNotOverwriteExistingNewLayoutCacheEntries() throws Exception
    {
        Path oldRoot = gameDir.resolve(".flancache"), newRoot = ModCachePaths.root(gameDir);
        Files.createDirectories(oldRoot);
        Files.createDirectories(newRoot);
        Files.writeString(oldRoot.resolve("muzzle-measurements.json"), "old");
        Files.writeString(newRoot.resolve("muzzle-measurements.json"), "current");
        Files.writeString(gameDir.resolve(".flansmod-pack-locations-cache.json"), "old locations");
        Files.createDirectories(ModCachePaths.packsManager(gameDir));
        Files.writeString(ModCachePaths.packsManager(gameDir).resolve(ModCachePaths.PACK_LOCATIONS_FILE_NAME), "current locations");
        ModCachePaths.migrate(gameDir);
        assertEquals("current", Files.readString(newRoot.resolve("muzzle-measurements.json")));
        assertEquals("current locations", Files.readString(ModCachePaths.packsManager(gameDir).resolve(ModCachePaths.PACK_LOCATIONS_FILE_NAME)));
        assertEquals("old", Files.readString(oldRoot.resolve("muzzle-measurements.json")));
        assertEquals("old locations", Files.readString(gameDir.resolve(".flansmod-pack-locations-cache.json")));
    }

    @Test
    void migratesPreviousCacheRootLocations() throws Exception
    {
        Path previous = ModCachePaths.root(gameDir).resolve(ModCachePaths.PACK_LOCATIONS_FILE_NAME);
        Files.createDirectories(previous.getParent());
        Files.writeString(previous, "locations");
        ModCachePaths.migrate(gameDir);
        assertEquals("locations", Files.readString(ModCachePaths.packsManager(gameDir).resolve(ModCachePaths.PACK_LOCATIONS_FILE_NAME)));
        assertFalse(Files.exists(previous));
    }

    @Test
    void leavesUnknownAndPost21DevelopmentCachesInTheirOriginalLocations() throws Exception
    {
        Path unknown = gameDir.resolve(".flancache/later-cache.json");
        Path development = gameDir.resolve(".flansmod-content-cache/index.json");
        Files.createDirectories(unknown.getParent());
        Files.createDirectories(development.getParent());
        Files.writeString(unknown, "unknown");
        Files.writeString(development, "development");
        ModCachePaths.migrate(gameDir);
        assertEquals("unknown", Files.readString(unknown));
        assertEquals("development", Files.readString(development));
        assertFalse(Files.exists(ModCachePaths.root(gameDir)));
    }
}
