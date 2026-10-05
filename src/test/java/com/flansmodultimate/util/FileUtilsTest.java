package com.flansmodultimate.util;

import com.flansmodultimate.content.ContentPack;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FileUtilsTest
{
    @Test
    void jarConversionCannotReplaceAnotherPackZip() throws Exception
    {
        Path packs = Files.createDirectories(tempDir.resolve("flan"));
        Path jar = packs.resolve("Pack.jar"), zip = packs.resolve("Pack.zip");
        Files.writeString(jar, "original jar");
        Files.writeString(zip, "original zip");
        ContentPack pack = new ContentPack("Pack.jar", jar);
        Files.createDirectories(pack.getExtractedPath());
        Files.writeString(pack.getExtractedPath().resolve("generated.txt"), "generated");
        assertFalse(FileUtils.repackArchive(pack));
        assertEquals("original jar", Files.readString(jar));
        assertEquals("original zip", Files.readString(zip));
        assertEquals(jar.toAbsolutePath().normalize(), pack.getPath());
    }
    @TempDir
    Path tempDir;

    @Test
    void archiveFileSystemsAreReusedAndClosedWithScope() throws Exception
    {
        Path archive = tempDir.resolve("content.zip");
        try (FileSystem fs = FileSystems.newFileSystem(URI.create("jar:" + archive.toUri()), Map.of("create", "true")))
        {
            Files.createDirectories(fs.getPath("/guns"));
            Files.writeString(fs.getPath("/guns/example.txt"), "ShortName Example");
        }

        ContentPack provider = new ContentPack(archive.getFileName().toString(), archive);
        FileSystem cached;
        try (FileUtils.ArchiveFileSystemCache ignored = FileUtils.cacheArchiveFileSystems())
        {
            cached = FileUtils.createFileSystem(provider);
            assertSame(cached, FileUtils.createFileSystem(provider));

            try (FileUtils.ArchiveFileSystemCache ignoredNested = FileUtils.cacheArchiveFileSystems())
            {
                assertSame(cached, FileUtils.createFileSystem(provider));
            }
            assertTrue(cached.isOpen());

            FileUtils.closeFileSystem(cached, provider);
            assertTrue(cached.isOpen());

            try (DirectoryStream<Path> stream = FileUtils.createDirectoryStream(provider))
            {
                assertTrue(stream.iterator().hasNext());
            }
            assertTrue(cached.isOpen());
        }

        assertFalse(cached.isOpen());
    }

    @Test
    void archiveDirectoryStreamStillClosesFileSystemOutsideScope() throws Exception
    {
        Path archive = tempDir.resolve("content.zip");
        try (FileSystem fs = FileSystems.newFileSystem(URI.create("jar:" + archive.toUri()), Map.of("create", "true")))
        {
            Files.createDirectories(fs.getPath("/guns"));
        }

        ContentPack provider = new ContentPack(archive.getFileName().toString(), archive);
        FileSystem opened;
        try (DirectoryStream<Path> stream = FileUtils.createDirectoryStream(provider))
        {
            Path entry = stream.iterator().next();
            opened = entry.getFileSystem();
            assertTrue(opened.isOpen());
        }

        assertFalse(opened.isOpen());
    }
}
