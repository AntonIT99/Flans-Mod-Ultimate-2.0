package com.flansmodultimate.content;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class ContentCacheTest
{
    @TempDir Path root;

    @BeforeEach void configure() { ContentFileCache.configure(root.resolve("cache")); }
    @AfterEach void reset() { ContentFileCache.configure(null); }

    @Test
    void unchangedGenerationIsReusedWithoutRewritingCacheOrAliasFiles() throws Exception
    {
        ContentPack pack = new ContentPack("pack", root.resolve("pack"));
        Files.createDirectories(pack.getPath());
        Files.writeString(pack.getPath().resolve("definition.txt"), "definition");
        AliasFileManager.writeToAliasMappingFile("id_alias.json", pack, Map.of());
        FileTime aliasTime = Files.getLastModifiedTime(pack.getPath().resolve("id_alias.json"));
        new ContentProcessingCache(pack, "definitions").remember(true, true, true, true);
        Map<String, ContentFileCache.Stamp> before = ContentFileCache.snapshot(root.resolve("cache"));
        ContentProcessingCache warm = new ContentProcessingCache(pack, "definitions");
        assertTrue(warm.assetsCurrent());
        assertTrue(warm.dataCurrent());
        warm.remember(false, false, false, false);
        AliasFileManager.writeToAliasMappingFile("id_alias.json", pack, Map.of());
        assertEquals(before, ContentFileCache.snapshot(root.resolve("cache")));
        assertEquals(aliasTime, Files.getLastModifiedTime(pack.getPath().resolve("id_alias.json")));
    }

    @Test
    void changesToDefinitionsCategoriesOrIndividualOutputsInvalidateProcessing() throws Exception
    {
        ContentPack pack = new ContentPack("pack", root.resolve("pack"));
        Files.createDirectories(pack.getPath());
        Path input = pack.getPath().resolve("definition.txt");
        Files.writeString(input, "first");
        new ContentProcessingCache(pack, "categoryA").remember(true, true, true, true);
        assertFalse(new ContentProcessingCache(pack, "categoryB").assetsCurrent());
        Files.writeString(input, "other");
        Files.setLastModifiedTime(input, FileTime.fromMillis(System.currentTimeMillis() + 2000));
        assertFalse(new ContentProcessingCache(pack, "categoryA").dataCurrent());
        new ContentProcessingCache(pack, "categoryA").remember(true, true, true, true);
        Files.delete(input);
        assertFalse(new ContentProcessingCache(pack, "categoryA").assetsCurrent());
    }

    @Test
    void serverDataCacheDoesNotSkipTheFirstClientGeneration() throws Exception
    {
        ContentPack pack = new ContentPack("pack", root.resolve("pack"));
        Files.createDirectories(pack.getPath());
        new ContentProcessingCache(pack, "definitions").remember(false, true, false, true);
        ContentProcessingCache client = new ContentProcessingCache(pack, "definitions");
        assertTrue(client.dataCurrent());
        assertFalse(client.assetsCurrent());
        client.remember(true, false, true, false);
        assertTrue(new ContentProcessingCache(pack, "definitions").assetsCurrent());
    }

    @Test
    void cachedAssetIndexUsesMetadataAndRebuildsAfterAChange() throws Exception
    {
        Path assets = root.resolve("assets"), model = assets.resolve("models/item/model.json");
        Files.createDirectories(model.getParent());
        Files.writeString(model, "{\"parent\":\"minecraft:item/generated\"}");
        ModernAssetAliases.Assets cold = PackAssetIndex.modern(assets, null);
        FileTime time = Files.getLastModifiedTime(model);
        // Keeping all metadata equal demonstrates that the warm path never reads/parses source JSON.
        Files.writeString(model, " ".repeat((int)Files.size(model)));
        Files.setLastModifiedTime(model, time);
        ModernAssetAliases.Assets warm = PackAssetIndex.modern(assets, null);
        assertEquals(cold.models(), warm.models());
        Files.setLastModifiedTime(model, FileTime.fromMillis(time.toMillis() + 2000));
        assertTrue(PackAssetIndex.modern(assets, null).models().isEmpty());
    }

    @Test
    void forceRegenerationBypassesAssetSignaturesEvenWhenMetadataIsPreserved() throws Exception
    {
        Path assets = root.resolve("assets"), model = assets.resolve("models/item/model.json");
        Files.createDirectories(model.getParent());
        Files.writeString(model, "{\"marker\":\"A\"}");
        var original = PackAssetIndex.modern(assets, null);
        FileTime time = Files.getLastModifiedTime(model);
        Files.writeString(model, "{\"marker\":\"B\"}");
        Files.setLastModifiedTime(model, time);
        assertEquals(original, PackAssetIndex.modern(assets, null));
        ContentFileCache.configure(root.resolve("cache"), true);
        assertNotEquals(original, PackAssetIndex.modern(assets, null));
    }

    @Test
    void incompleteAssetCacheRebuildsInsteadOfLosingAllResourceAliases() throws Exception
    {
        Path assets = root.resolve("assets"), model = assets.resolve("models/item/model.json");
        Files.createDirectories(model.getParent());
        Files.writeString(model, "{\"marker\":\"A\"}");
        var original = PackAssetIndex.modern(assets, null);
        try (var files = Files.list(root.resolve("cache")))
        {
            Path cache = files.findFirst().orElseThrow();
            var json = com.google.gson.JsonParser.parseString(Files.readString(cache)).getAsJsonObject();
            json.getAsJsonObject("modern").remove("textures");
            Files.writeString(cache, json.toString());
        }
        assertEquals(original, PackAssetIndex.modern(assets, null));
    }

    @Test
    void corruptCacheIsDisposable() throws Exception
    {
        ContentPack pack = new ContentPack("pack", root.resolve("pack"));
        Files.createDirectories(pack.getPath());
        new ContentProcessingCache(pack, "definitions").remember(true, true, true, true);
        try (var files = Files.list(root.resolve("cache")))
        {
            for (Path file : files.toList()) Files.writeString(file, "invalid JSON");
        }
        assertFalse(new ContentProcessingCache(pack, "definitions").assetsCurrent());
    }

    @Test
    void failedForcedRegenerationDoesNotInheritEarlierSuccess() throws Exception
    {
        ContentPack pack = new ContentPack("pack", root.resolve("pack"));
        Files.createDirectories(pack.getPath());
        new ContentProcessingCache(pack, "definitions").remember(true, true, true, true);
        new ContentProcessingCache(pack, "definitions").remember(false, true, true, true);
        assertFalse(new ContentProcessingCache(pack, "definitions").assetsCurrent());
        assertTrue(new ContentProcessingCache(pack, "definitions").dataCurrent());
    }

    @Test
    void generationFingerprintIncludesEffectiveValuesBeyondRawDefinitionText()
    {
        ContentPack pack = new ContentPack("pack", root.resolve("pack"));
        var file = new com.flansmodultimate.common.types.TypeFile("part.txt",
            com.flansmodultimate.common.types.EnumType.PART, pack, java.util.List.of("Name First"));
        String before = file.getGenerationInput();
        file.getConfigLines("Name").set(0, "Category name");
        assertNotEquals(before, file.getGenerationInput());
        assertEquals(java.util.List.of("Name First"), file.getLines());
    }

    @Test
    void unchangedArchiveUsesBothAssetIndexesWithoutOpeningTheZip() throws Exception
    {
        Path archive = root.resolve("pack.zip");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive)))
        {
            zip.putNextEntry(new ZipEntry("assets/flansmod/models/item/model.json"));
            zip.write("{\"parent\":\"minecraft:item/generated\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        ContentPack pack = new ContentPack("pack.zip", archive);
        var legacy = PackAssetIndex.legacy(pack);
        var modern = PackAssetIndex.modern(pack);
        Map<String, ContentFileCache.Stamp> before = ContentFileCache.snapshot(root.resolve("cache"));
        FileTime time = Files.getLastModifiedTime(archive);
        // A ZIP reader would fail on this data. Equal metadata must select the persisted indexes.
        Files.write(archive, new byte[(int)Files.size(archive)]);
        Files.setLastModifiedTime(archive, time);
        assertEquals(legacy, PackAssetIndex.legacy(pack));
        assertEquals(modern, PackAssetIndex.modern(pack));
        assertEquals(before, ContentFileCache.snapshot(root.resolve("cache")));
    }
}
