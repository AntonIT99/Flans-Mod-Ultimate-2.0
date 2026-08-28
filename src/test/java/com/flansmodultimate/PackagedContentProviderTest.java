package com.flansmodultimate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PackagedContentProviderTest
{
    @TempDir
    Path tempDir;

    @Test
    void developmentProviderUsesResolvedResourceDirectories() throws Exception
    {
        Path content = Files.createDirectories(tempDir.resolve("resources/flans_content/parts"));
        Path assets = Files.createDirectories(tempDir.resolve("resources/assets/flansmod"));
        Path models = Files.createDirectories(tempDir.resolve("resources/flans_models"));
        PackagedContentProvider provider = provider(tempDir, content, assets, models, false);

        assertTrue(provider.isDirectory());
        assertFalse(provider.isArchive());
        assertEquals("Flan's Mod Official Content Packs", provider.getConflictDisplayName());
        assertEquals(content, provider.getContentRoot(null));
        assertEquals(assets.resolve("textures"), provider.getTextureSourcePath(null));
        assertEquals(models.resolve("com/flansmod/client/model/ModelTest.class"),
            provider.getModelPath("com.flansmod.client.model.ModelTest", null));
    }

    @Test
    void productionProviderResolvesStablePathsInsideJarFileSystem() throws Exception
    {
        Path jar = tempDir.resolve("official-packs.jar");
        try (FileSystem fs = FileSystems.newFileSystem(URI.create("jar:" + jar.toUri()), Map.of("create", "true")))
        {
            Files.createDirectories(fs.getPath("/flans_content/parts"));
            Files.createDirectories(fs.getPath("/assets/flansmod"));
            Files.createDirectories(fs.getPath("/flans_models"));
        }

        PackagedContentProvider provider = provider(jar, tempDir, tempDir, tempDir, true);
        assertTrue(provider.isArchive());
        assertFalse(provider.isDirectory());
        assertThrows(IllegalStateException.class, () -> provider.getContentRoot(null));

        try (FileSystem fs = FileSystems.newFileSystem(jar))
        {
            assertEquals(fs.getPath("/flans_content/parts"), provider.getContentRoot(fs));
            assertEquals(fs.getPath("/assets/flansmod/textures"), provider.getTextureSourcePath(fs));
            assertEquals(fs.getPath("/flans_models/com/flansmod/client/model/ModelTest.class"),
                provider.getModelPath("com.flansmod.client.model.ModelTest", fs));
        }
    }

    @Test
    void readsJsonPackDisplayNames() throws Exception
    {
        Path mapping = tempDir.resolve("pack_names.json");
        Files.writeString(mapping, """
            {
              "modernwarfare": "Modern Warfare Content Pack",
              "parts": "Parts Content Pack"
            }
            """);

        assertEquals(Map.of(
            "modernwarfare", "Modern Warfare Content Pack",
            "parts", "Parts Content Pack"
        ), PackagedContentPackApi.loadDisplayNames(mapping));
    }

    private static PackagedContentProvider provider(Path modulePath, Path content, Path assets,
                                                     Path models, boolean archiveBacked)
    {
        return new PackagedContentProvider(
            "Parts (Official)", "Flan's Mod Official Content Packs", "parts", modulePath,
            content, assets, models,
            "flans_content/parts", "assets/flansmod", "flans_models",
            archiveBacked, true
        );
    }
}
