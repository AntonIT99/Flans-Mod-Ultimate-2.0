package com.flansmodultimate.content;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class IncrementalContentArchiveTest
{
    @TempDir Path root;

    @Test
    void appliesGeneratedChangesAndKeepsEverythingElse() throws Exception
    {
        Path archive = zip("pack.zip", Map.of("guns/a.txt", "gun", "com/flansmod/Model.class", "class", "data/flansmod/recipes/old.json", "old", "data/flansmod/recipes/same.json", "same",
            "data/flansmod/recipes/changed.json", "before", "__MACOSX/._a.txt", "metadata"));
        Path extracted = root.resolve("extracted");
        IncrementalContentArchive incremental = open(archive);

        try (ContentLoadingWorkers workers = ContentLoadingWorkers.create(4))
        {
            assertTrue(incremental.extract(extracted, name -> name.startsWith("data/"), workers));
            assertFalse(Files.exists(extracted.resolve("guns/a.txt")));
            assertEquals("same", Files.readString(extracted.resolve("data/flansmod/recipes/same.json")));

            Files.delete(extracted.resolve("data/flansmod/recipes/old.json"));
            Files.writeString(extracted.resolve("data/flansmod/recipes/changed.json"), "after, and longer");
            Files.createDirectories(extracted.resolve("data/flansmod/advancements"));
            Files.writeString(extracted.resolve("data/flansmod/advancements/new.json"), "new");

            Path output = root.resolve("output.zip");
            incremental.write(output, extracted, workers);

            Map<String, String> contents = read(output);
            assertEquals("gun", contents.get("guns/a.txt"));
            assertEquals("class", contents.get("com/flansmod/Model.class"));
            assertEquals("same", contents.get("data/flansmod/recipes/same.json"));
            assertEquals("after, and longer", contents.get("data/flansmod/recipes/changed.json"));
            assertEquals("new", contents.get("data/flansmod/advancements/new.json"));
            assertFalse(contents.containsKey("data/flansmod/recipes/old.json"));
            assertFalse(contents.containsKey("__MACOSX/._a.txt"));
        }
    }

    @Test
    void outputCanBeProcessedAgain() throws Exception
    {
        Path archive = zip("pack.zip", Map.of("guns/a.txt", "gun", "data/a.json", "a"));
        Path extracted = root.resolve("extracted");
        Path first = root.resolve("first.zip");
        IncrementalContentArchive incremental = open(archive);
        assertTrue(incremental.extract(extracted, name -> name.startsWith("data/"), ContentLoadingWorkers.sequential()));
        Files.writeString(extracted.resolve("data/b.json"), "b".repeat(4096));
        incremental.write(first, extracted, ContentLoadingWorkers.sequential());

        Path extractedAgain = root.resolve("again");
        Path second = root.resolve("second.zip");
        IncrementalContentArchive next = open(first);
        assertTrue(next.extract(extractedAgain, name -> true, ContentLoadingWorkers.sequential()));
        next.write(second, extractedAgain, ContentLoadingWorkers.sequential());

        assertEquals(read(first), read(second));
        assertEquals("b".repeat(4096), read(second).get("data/b.json"));
    }

    @Test
    void javaSourcesPullInTheWholePack() throws Exception
    {
        Path archive = zip("pack.zip", Map.of("com/flansmod/Model.java", "source", "com/flansmod/Other.class", "class", "guns/a.txt", "gun"));
        Path extracted = root.resolve("extracted");

        assertTrue(open(archive).extract(extracted, name -> false, ContentLoadingWorkers.sequential()));

        assertTrue(Files.isRegularFile(extracted.resolve("com/flansmod/Other.class")));
        assertTrue(Files.isRegularFile(extracted.resolve("guns/a.txt")));
    }

    @Test
    void renamingAnExtractedFileReplacesItsEntry() throws Exception
    {
        Path archive = zip("pack.zip", Map.of("assets/Model.json", "model"));
        Path extracted = root.resolve("extracted");
        IncrementalContentArchive incremental = open(archive);
        assertTrue(incremental.extract(extracted, name -> true, ContentLoadingWorkers.sequential()));
        Path temporary = extracted.resolve("assets/model.tmp");
        Files.move(extracted.resolve("assets/Model.json"), temporary);
        Files.move(temporary, extracted.resolve("assets/model.json"));

        Path output = root.resolve("output.zip");
        incremental.write(output, extracted, ContentLoadingWorkers.sequential());

        Map<String, String> contents = read(output);
        assertEquals("model", contents.get("assets/model.json"));
        assertFalse(contents.containsKey("assets/Model.json"));
    }

    @Test
    void blocksEntriesEscapingTheExtractionDirectory() throws Exception
    {
        Path archive = zip("pack.zip", Map.of("../escaped.txt", "bad"));

        assertFalse(open(archive).extract(root.resolve("nested/extracted"), name -> true, ContentLoadingWorkers.sequential()));
        assertFalse(Files.exists(root.resolve("nested/escaped.txt")));
    }

    @Test
    void leavesUnusualArchivesToTheFullRoute() throws Exception
    {
        Path archive = zip("pack.zip", Map.of("a.txt", "a"));
        byte[] zip = Files.readAllBytes(archive);
        byte[] prefixed = new byte[zip.length + 16];
        System.arraycopy(zip, 0, prefixed, 16, zip.length);
        Path withPrefix = root.resolve("prefixed.zip");
        Files.write(withPrefix, prefixed);

        byte[] encrypted = Arrays.copyOf(zip, zip.length);
        encrypted[centralHeader(encrypted) + 8] |= 1;
        Path withEncryption = root.resolve("encrypted.zip");
        Files.write(withEncryption, encrypted);

        assertNull(IncrementalContentArchive.open(withPrefix));
        assertNull(IncrementalContentArchive.open(withEncryption));
        assertNull(IncrementalContentArchive.open(root.resolve("missing.zip")));
    }

    @Test
    void failsOnCorruptedExtractedData() throws Exception
    {
        Path archive = root.resolve("stored.zip");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive)))
        {
            ZipEntry entry = new ZipEntry("a.txt");
            byte[] content = "content".getBytes(StandardCharsets.UTF_8);
            entry.setMethod(ZipEntry.STORED);
            entry.setSize(content.length);
            entry.setCompressedSize(content.length);
            java.util.zip.CRC32 crc = new java.util.zip.CRC32();
            crc.update(content);
            entry.setCrc(crc.getValue());
            zip.putNextEntry(entry);
            zip.write(content);
            zip.closeEntry();
        }
        byte[] bytes = Files.readAllBytes(archive);
        bytes[30 + "a.txt".length()] ^= 1;
        Files.write(archive, bytes);

        assertFalse(open(archive).extract(root.resolve("extracted"), name -> true, ContentLoadingWorkers.sequential()));
    }

    private static int centralHeader(byte[] zip)
    {
        for (int i = zip.length - 4; i >= 0; i--)
            if (zip[i] == 0x50 && zip[i + 1] == 0x4b && zip[i + 2] == 0x01 && zip[i + 3] == 0x02)
                return i;
        throw new IllegalStateException("No central directory");
    }

    private static IncrementalContentArchive open(Path archive)
    {
        IncrementalContentArchive incremental = IncrementalContentArchive.open(archive);
        assertNotNull(incremental);
        return incremental;
    }

    private Path zip(String name, Map<String, String> files) throws IOException
    {
        Path archive = root.resolve(name);
        try (OutputStream output = Files.newOutputStream(archive); ZipOutputStream zip = new ZipOutputStream(output))
        {
            for (Map.Entry<String, String> file : new LinkedHashMap<>(new TreeMap<>(files)).entrySet())
            {
                zip.putNextEntry(new ZipEntry(file.getKey()));
                zip.write(file.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return archive;
    }

    private static Map<String, String> read(Path archive) throws IOException
    {
        Map<String, String> contents = new TreeMap<>();
        try (ZipFile zip = new ZipFile(archive.toFile()))
        {
            for (ZipEntry entry : java.util.Collections.list(zip.entries()))
            {
                if (!entry.isDirectory())
                    contents.put(entry.getName(), new String(zip.getInputStream(entry).readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        return contents;
    }
}
