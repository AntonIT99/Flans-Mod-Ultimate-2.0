package com.flansmodultimate.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.*;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TapPackDetectionTest
{
    @TempDir
    Path directory;

    private boolean detectInZip(String description) throws IOException
    {
        URI uri = URI.create("jar:" + directory.resolve(description.hashCode() + ".zip").toUri());
        try (FileSystem zip = FileSystems.newFileSystem(uri, Map.of("create", "true")))
        {
            Files.writeString(zip.getPath("/pack.mcmeta"), "{\"pack\":{\"pack_format\":1,\"description\":\"" + description + "\"}}");
            return TapCompatModels.readIsTapPack(zip.getPath("/"));
        }
    }

    @Test
    void detectsTapInsideAZippedPack() throws IOException
    {
        assertTrue(detectInZip("Tyrants And Plebeians Exorcism"));
        assertFalse(detectInZip("Modern Warfare Content Pack"));
    }

    @Test
    void readsTheNameAClassDeclaresRatherThanItsFileName() throws IOException
    {
        try (InputStream stream = TapPackDetectionTest.class.getResourceAsStream("TapPackDetectionTest.class"))
        {
            assertEquals(TapPackDetectionTest.class.getName(), ClassLoaderUtils.declaredClassName(stream.readAllBytes()));
        }
    }
}
