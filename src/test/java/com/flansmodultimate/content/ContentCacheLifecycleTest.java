package com.flansmodultimate.content;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ContentCacheLifecycleTest
{
    @TempDir Path root;

    @BeforeEach void configure() { ContentFileCache.configure(root.resolve("cache")); }
    @AfterEach void reset() { ContentFileCache.releaseRunSnapshots(); ContentFileCache.configure(null); }

    @Test
    void aPackIsWalkedOnceWhileLoadingAndAgainAfterSomethingWritesToIt() throws Exception
    {
        ContentPack pack = new ContentPack("pack", root.resolve("pack"));
        write(pack.getPath().resolve("guns/gun.txt"));
        write(pack.getAssetsPath().resolve("sounds/shot.ogg"));
        Path sounds = pack.getAssetsPath().resolve("sounds");

        ContentFileCache.beginRun();
        ContentFileCache.prefetch(pack);
        Map<String, ContentFileCache.Stamp> whole = ContentFileCache.snapshot(pack.getPath());
        // Added behind the remembered walk: still unseen, as nothing announced the write.
        write(sounds.resolve("reload.ogg"));
        assertEquals(Map.of("shot.ogg", whole.get("assets/flansmod/sounds/shot.ogg")),
            ContentFileCache.snapshot(sounds));

        ContentFileCache.forget(pack.getPath());
        assertEquals(List.of("reload.ogg", "shot.ogg"), List.copyOf(ContentFileCache.snapshot(sounds).keySet()));
        assertEquals(3, ContentFileCache.freshSnapshot(pack.getPath()).size());

        ContentFileCache.endRun(false);
        write(sounds.resolve("empty.ogg"));
        assertEquals(3, ContentFileCache.snapshot(sounds).size(), "outside loading every snapshot is a new walk");
    }

    @Test
    void aFolderSpeltInAnotherCaseIsWalkedRatherThanFoundEmpty() throws Exception
    {
        ContentPack pack = new ContentPack("pack", root.resolve("pack"));
        write(pack.getPath().resolve("Assets/flansmod/sounds/shot.ogg"));
        ContentFileCache.beginRun();
        ContentFileCache.prefetch(pack);
        Path lowerCase = pack.getPath().resolve("assets/flansmod/sounds");
        // On a case-insensitive file system the folder exists under the other spelling.
        int expected = Files.isDirectory(lowerCase) ? 1 : 0;
        assertEquals(expected, ContentFileCache.snapshot(lowerCase).size());
    }

    @Test
    void entriesStoreAFingerprintAndTheirSourceFirst() throws Exception
    {
        ContentPack pack = new ContentPack("pack", root.resolve("pack"));
        write(pack.getPath().resolve("guns/gun.txt"));
        new ContentProcessingCache(pack, "definitions").remember(true, true, true, true);
        Path entry;
        try (var files = Files.list(root.resolve("cache/generation")))
        {
            entry = files.findFirst().orElseThrow();
        }
        var json = JsonParser.parseString(Files.readString(entry)).getAsJsonObject();
        assertEquals("source", json.keySet().iterator().next());
        assertEquals(pack.getPath().toAbsolutePath().normalize().toString(), ContentFileCache.readSource(entry));
        assertTrue(json.get("files").getAsString().matches("[0-9a-f]{64}"));
        assertTrue(new ContentProcessingCache(pack, "definitions").assetsCurrent());
    }

    @Test
    void fingerprintsDifferExactlyWhenSnapshotsDo()
    {
        var a = new ContentFileCache.Stamp(1, "2020-01-01T00:00:00Z", "null");
        var b = new ContentFileCache.Stamp(2, "2020-01-01T00:00:00Z", "null");
        assertEquals(ContentFileCache.fingerprint(Map.of("x", a, "y", b)), ContentFileCache.fingerprint(Map.of("y", b, "x", a)));
        assertNotEquals(ContentFileCache.fingerprint(Map.of("x", a)), ContentFileCache.fingerprint(Map.of("x", b)));
        assertNotEquals(ContentFileCache.fingerprint(Map.of("x", a)), ContentFileCache.fingerprint(Map.of("z", a)));
    }

    @Test
    void pruningRemovesEntriesOfMissingSourcesOnlyAndAtMostWeekly() throws Exception
    {
        ContentPack kept = new ContentPack("kept", root.resolve("kept"));
        ContentPack removed = new ContentPack("removed", root.resolve("removed"));
        for (ContentPack pack : List.of(kept, removed))
        {
            write(pack.getPath().resolve("guns/gun.txt"));
            new ContentProcessingCache(pack, "definitions").remember(true, true, true, true);
        }
        Path converted = root.resolve("converted.jar");
        Files.writeString(root.resolve("converted.zip"), "zip");
        Path folder = root.resolve("cache/generation");
        Files.writeString(folder.resolve("converted.json"), "{\"source\":\"" + json(converted) + "\"}");
        Files.writeString(folder.resolve("virtual.json"), "{\"source\":\"\"}");
        Files.writeString(folder.resolve("old-layout.json"), "{\"version\":1}");
        Path abandoned = folder.resolve("entry.json123.tmp");
        Files.writeString(abandoned, "partial");
        Files.setLastModifiedTime(abandoned, FileTime.from(Instant.now().minus(Duration.ofDays(2))));
        Path running = folder.resolve("entry.json456.tmp");
        Files.writeString(running, "partial");

        deleteTree(removed.getPath());
        ContentFileCache.pruneIfDue();

        assertTrue(new ContentProcessingCache(kept, "definitions").assetsCurrent());
        try (var files = Files.list(folder))
        {
            List<String> names = files.map(file -> file.getFileName().toString()).sorted().toList();
            assertEquals(4, names.size(), names::toString);
            assertTrue(names.containsAll(List.of("converted.json", "virtual.json", "entry.json456.tmp")), names::toString);
            assertFalse(names.contains("old-layout.json"));
            assertFalse(names.contains("entry.json123.tmp"));
        }

        Files.writeString(folder.resolve("old-layout.json"), "{\"version\":1}");
        ContentFileCache.pruneIfDue();
        assertTrue(Files.exists(folder.resolve("old-layout.json")), "the next pruning waits a week");
    }

    private static String json(Path path)
    {
        return path.toAbsolutePath().normalize().toString().replace("\\", "\\\\");
    }

    private static void write(Path file) throws Exception
    {
        Files.createDirectories(file.getParent());
        Files.writeString(file, file.getFileName().toString());
    }

    private static void deleteTree(Path path) throws Exception
    {
        try (var files = Files.walk(path))
        {
            for (Path file : files.sorted(java.util.Comparator.reverseOrder()).toList())
                Files.delete(file);
        }
    }
}
