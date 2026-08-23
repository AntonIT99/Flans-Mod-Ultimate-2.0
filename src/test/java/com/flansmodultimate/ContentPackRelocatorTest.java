package com.flansmodultimate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentPackRelocatorTest
{
    @TempDir
    Path tempDir;

    @Test
    void movesProcessedStandaloneContentPackFromModsToFlan() throws Exception
    {
        Path mods = Files.createDirectory(tempDir.resolve("mods"));
        Path flan = Files.createDirectory(tempDir.resolve("flan"));
        Path archive = createArchive(mods.resolve("modern-pack.zip"), Map.of(
            "guns/TestGun.txt", "ShortName TestGun",
            "assets/flansmod/textures/items/test.png", "png"
        ));

        ContentPackRelocator.RelocationResult result = reconcile(mods, flan);

        assertEquals(1, result.movedContentPacks());
        assertFalse(Files.exists(archive));
        assertTrue(Files.isRegularFile(flan.resolve("modern-pack.zip")));
        assertFalse(result.restartRequired());
    }

    @Test
    void recognizesUnprocessedLegacyPackWithoutGeneratedAssets() throws Exception
    {
        Path archive = createArchive(tempDir.resolve("legacy-pack.jar"), Map.of(
            "guns/LegacyGun.txt", "ShortName LegacyGun",
            "models/ModelLegacyGun.java", "class ModelLegacyGun {}"
        ));

        assertEquals(ContentPackRelocator.ArchiveKind.CONTENT_PACK,
            ContentPackRelocator.classify(archive));
    }

    @Test
    void movesExplicitPackModBundleToModsAndRequiresRestart() throws Exception
    {
        Path mods = Files.createDirectory(tempDir.resolve("mods"));
        Path flan = Files.createDirectory(tempDir.resolve("flan"));
        createArchive(flan.resolve("third-party-bundle.jar"), Map.of(
            "META-INF/mods.toml", "modLoader=\"javafml\"",
            ContentPackRelocator.DESCRIPTOR_PATH, bundleDescriptor(),
            "custom_content/pack/guns/Test.txt", "ShortName Test"
        ));

        ContentPackRelocator.RelocationResult result = reconcile(mods, flan);

        assertEquals(1, result.movedBundles());
        assertTrue(result.restartRequired());
        assertTrue(Files.isRegularFile(mods.resolve("third-party-bundle.jar")));
    }

    @Test
    void recognizesKnownBundleWithoutDescriptor() throws Exception
    {
        Path archive = createArchive(tempDir.resolve("official.jar"), Map.of(
            "META-INF/mods.toml", "modLoader=\"javafml\"",
            "flans_content/pack_names.json", "{}"
        ));

        assertEquals(ContentPackRelocator.ArchiveKind.PACK_MOD_BUNDLE,
            ContentPackRelocator.classify(archive));
    }

    @Test
    void neverLetsDescriptorOverrideLoaderMetadataRules() throws Exception
    {
        Path modClaimingStandalone = createArchive(tempDir.resolve("bad-mod.jar"), Map.of(
            "META-INF/mods.toml", "modLoader=\"javafml\"",
            ContentPackRelocator.DESCRIPTOR_PATH, contentPackDescriptor()
        ));
        Path bundleWithoutMetadata = createArchive(tempDir.resolve("bad-bundle.jar"), Map.of(
            ContentPackRelocator.DESCRIPTOR_PATH, bundleDescriptor()
        ));

        assertEquals(ContentPackRelocator.ArchiveKind.UNKNOWN,
            ContentPackRelocator.classify(modClaimingStandalone));
        assertEquals(ContentPackRelocator.ArchiveKind.UNKNOWN,
            ContentPackRelocator.classify(bundleWithoutMetadata));
    }

    @Test
    void leavesUnrelatedModsAndAmbiguousArchivesUntouched() throws Exception
    {
        Path mods = Files.createDirectory(tempDir.resolve("mods"));
        Path flan = Files.createDirectory(tempDir.resolve("flan"));
        Path unrelatedMod = createArchive(mods.resolve("unrelated.jar"), Map.of(
            "META-INF/mods.toml", "modLoader=\"javafml\"",
            "assets/flansmod/textures/items/compatibility.png", "png"
        ));
        Path ambiguous = createArchive(mods.resolve("ambiguous.zip"), Map.of(
            "guns/notes.txt", "not enough evidence"
        ));

        ContentPackRelocator.RelocationResult result = reconcile(mods, flan);

        assertEquals(0, result.movedContentPacks());
        assertTrue(Files.exists(unrelatedMod));
        assertTrue(Files.exists(ambiguous));
    }

    @Test
    void neverOverwritesDestination() throws Exception
    {
        Path mods = Files.createDirectory(tempDir.resolve("mods"));
        Path flan = Files.createDirectory(tempDir.resolve("flan"));
        Path source = createArchive(mods.resolve("pack.zip"), Map.of(
            "guns/Test.txt", "ShortName Test",
            "assets/flansmod/test", "test"
        ));
        Path destination = createArchive(flan.resolve("pack.zip"), Map.of("keep.txt", "keep"));
        long destinationSize = Files.size(destination);

        ContentPackRelocator.RelocationResult result = reconcile(mods, flan);

        assertEquals(0, result.movedContentPacks());
        assertTrue(Files.exists(source));
        assertEquals(destinationSize, Files.size(destination));
        assertTrue(result.warnings().stream().anyMatch(message -> message.contains("Nothing was overwritten")));
    }

    @Test
    void leavesZipBundleInFlanBecauseLoaderRequiresJar() throws Exception
    {
        Path mods = Files.createDirectory(tempDir.resolve("mods"));
        Path flan = Files.createDirectory(tempDir.resolve("flan"));
        Path bundle = createArchive(flan.resolve("bundle.zip"), Map.of(
            "META-INF/mods.toml", "modLoader=\"javafml\"",
            ContentPackRelocator.DESCRIPTOR_PATH, bundleDescriptor()
        ));

        ContentPackRelocator.RelocationResult result = reconcile(mods, flan);

        assertEquals(0, result.movedBundles());
        assertTrue(Files.exists(bundle));
        assertTrue(result.excludedFromContentLoading().contains(bundle.toAbsolutePath().normalize()));
        assertTrue(result.warnings().stream().anyMatch(message -> message.contains("is not a JAR")));
    }

    @Test
    void reusesCachedClassificationsForUnchangedArchives() throws Exception
    {
        Path mods = Files.createDirectory(tempDir.resolve("mods"));
        Path flan = Files.createDirectory(tempDir.resolve("flan"));
        createArchive(mods.resolve("unrelated.jar"), Map.of(
            "META-INF/mods.toml", "modLoader=\"javafml\""
        ));

        ContentPackRelocator.RelocationResult first = reconcile(mods, flan);
        ContentPackRelocator.RelocationResult second = reconcile(mods, flan);

        assertEquals(1, first.inspectedArchives());
        assertEquals(0, second.inspectedArchives());
        assertTrue(Files.isRegularFile(tempDir.resolve(ContentPackRelocator.CACHE_FILE_NAME)));
    }

    @Test
    void rejectsOverlappingFolders() throws Exception
    {
        Path mods = Files.createDirectory(tempDir.resolve("mods"));
        Path nestedFlan = Files.createDirectory(mods.resolve("flan"));

        ContentPackRelocator.RelocationResult result = ContentPackRelocator.reconcile(
            mods, nestedFlan, tempDir.resolve(ContentPackRelocator.CACHE_FILE_NAME));

        assertTrue(result.warnings().stream().anyMatch(message -> message.contains("overlap")));
    }

    private ContentPackRelocator.RelocationResult reconcile(Path mods, Path flan)
    {
        return ContentPackRelocator.reconcile(mods, flan,
            tempDir.resolve(ContentPackRelocator.CACHE_FILE_NAME));
    }

    private static Path createArchive(Path path, Map<String, String> entries) throws IOException
    {
        Map<String, String> orderedEntries = new LinkedHashMap<>(entries);
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(path)))
        {
            for (Map.Entry<String, String> entry : orderedEntries.entrySet())
            {
                output.putNextEntry(new ZipEntry(entry.getKey()));
                output.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                output.closeEntry();
            }
        }
        return path;
    }

    private static String bundleDescriptor()
    {
        return """
            {"formatVersion":1,"kind":"pack_mod_bundle","contentRoots":["custom_content"]}
            """;
    }

    private static String contentPackDescriptor()
    {
        return """
            {"formatVersion":1,"kind":"content_pack"}
            """;
    }
}
