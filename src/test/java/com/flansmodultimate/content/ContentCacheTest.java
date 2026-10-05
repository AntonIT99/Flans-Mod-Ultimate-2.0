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
    void virtualModulePathsHaveSeparatePersistentIndexesAcrossStarts() throws Exception
    {
        Path a = root.resolve("a.zip"), b = root.resolve("b.zip");
        Map<Path, Map<String, Map<String, Map<String, String>>>> legacy = new java.util.HashMap<>();
        Map<Path, ModernAssetAliases.Assets> modern = new java.util.HashMap<>();
        for (Path source : java.util.List.of(a, b))
        {
            try (var fs = java.nio.file.FileSystems.newFileSystem(source, Map.of("create", "true")))
            {
                var provider = virtualModule(source, fs);
                Path texture = provider.getTextureSourcePath(fs).resolve("armor/shared_1.png");
                Files.createDirectories(texture.getParent());
                var image = new java.awt.image.BufferedImage(2, 2, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                image.setRGB(0, 0, source.equals(a) ? 0xffff0000 : 0xff0000ff);
                try (var out = Files.newOutputStream(texture)) { javax.imageio.ImageIO.write(image, "png", out); }
                Path model = provider.getAssetsPath(fs).resolve("models/item/shared.json");
                Files.createDirectories(model.getParent());
                Files.writeString(model, "{\"marker\":\"" + (source.equals(a) ? "A" : "B") + "\"}");
                // Zip entries reopen with whole-second timestamps; pin an even-second time so the
                // stamps recorded while this filesystem is open still match after it is reopened.
                FileTime stable = FileTime.from(java.time.Instant.parse("2020-01-01T00:00:00Z"));
                Files.setLastModifiedTime(texture, stable);
                Files.setLastModifiedTime(model, stable);
                legacy.put(source, PackAssetIndex.legacy(provider));
                modern.put(source, PackAssetIndex.modern(provider));
                // Simulate unreadable source contents with identical per-file metadata. A warm
                // load must reuse both indexes, even though both virtual roots print the same path.
                FileTime textureTime = Files.getLastModifiedTime(texture), modelTime = Files.getLastModifiedTime(model);
                Files.write(texture, new byte[(int)Files.size(texture)]);
                Files.setLastModifiedTime(texture, textureTime);
                Files.writeString(model, " ".repeat((int)Files.size(model)));
                Files.setLastModifiedTime(model, modelTime);
            }
        }
        assertNotEquals(legacy.get(a), legacy.get(b));
        assertNotEquals(modern.get(a), modern.get(b));
        var before = ContentFileCache.snapshot(root.resolve("cache"));
        for (Path source : java.util.List.of(a, b))
            try (var fs = java.nio.file.FileSystems.newFileSystem(source))
            {
                var provider = virtualModule(source, fs);
                assertEquals(legacy.get(source), PackAssetIndex.legacyNames(provider));
                assertEquals(legacy.get(source), PackAssetIndex.legacy(provider));
                assertEquals(modern.get(source), PackAssetIndex.modern(provider));
            }
        assertEquals(before, ContentFileCache.snapshot(root.resolve("cache")));
    }

    private static PackagedContentProvider virtualModule(Path physicalSource, java.nio.file.FileSystem fs)
    {
        Path assets = fs.getPath("/assets/flansmod");
        return new PackagedContentProvider(physicalSource.getFileName().toString(), "module", "module", physicalSource,
            fs.getPath("/"), assets, fs.getPath("/"), "/", "/assets/flansmod", "/", false, true);
    }

    @Test
    void archiveNameIndexUpgradesOnlyTheRequestedGroups() throws Exception
    {
        Path archive = root.resolve("pack.zip");
        var image = new java.awt.image.BufferedImage(2, 2, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        var bytes = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, "png", bytes);
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive)))
        {
            for (String name : java.util.List.of("shared_1", "unique_1"))
            {
                zip.putNextEntry(new ZipEntry("assets/flansmod/armor/" + name + ".png"));
                zip.write(bytes.toByteArray());
                zip.closeEntry();
            }
        }
        ContentPack pack = new ContentPack("pack.zip", archive);
        assertEquals("", PackAssetIndex.legacyNames(pack).get("armor").get("shared").get("_1"));
        var selective = PackAssetIndex.legacy(pack, Map.of("armor", java.util.Set.of("shared")));
        assertFalse(selective.get("armor").get("shared").get("_1").isEmpty());
        assertEquals("", selective.get("armor").get("unique").get("_1"));
        FileTime time = Files.getLastModifiedTime(archive);
        Files.write(archive, new byte[(int)Files.size(archive)]);
        Files.setLastModifiedTime(archive, time);
        assertEquals(selective, PackAssetIndex.legacy(pack, Map.of("armor", java.util.Set.of("shared"))));
    }

    @Test
    void nameOnlyIndexSkipsDecodingAndUpgradesWhenACollisionAppears() throws Exception
    {
        ContentPack pack = new ContentPack("pack", root.resolve("pack"));
        Path texture = pack.getAssetsPath().resolve("armor/uniform_1.png");
        Files.createDirectories(texture.getParent());
        var image = new java.awt.image.BufferedImage(2, 2, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, 0xffff0000);
        javax.imageio.ImageIO.write(image, "png", texture.toFile());
        assertEquals("", PackAssetIndex.legacyNames(pack).get("armor").get("uniform").get("_1"));
        assertEquals("", PackAssetIndex.legacy(pack, Map.of()).get("armor").get("uniform").get("_1"));
        var signatures = PackAssetIndex.legacy(pack, Map.of("armor", java.util.Set.of("uniform")));
        assertFalse(signatures.get("armor").get("uniform").get("_1").isEmpty());
        assertEquals(signatures, PackAssetIndex.legacy(pack));
        FileTime time = Files.getLastModifiedTime(texture);
        Files.write(texture, new byte[(int)Files.size(texture)]);
        Files.setLastModifiedTime(texture, time);
        assertEquals(signatures, PackAssetIndex.legacy(pack, Map.of("armor", java.util.Set.of("uniform"))));
        ContentFileCache.configure(root.resolve("cache"), true);
        assertEquals("", PackAssetIndex.legacy(pack, Map.of()).get("armor").get("uniform").get("_1"));
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
        try (var files = Files.list(root.resolve("cache").resolve("asset-index")))
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
        try (var files = Files.list(root.resolve("cache").resolve("generation")))
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
    void eachKindOfEntryIsKeptInItsOwnFolder() throws Exception
    {
        ContentPack pack = new ContentPack("pack", root.resolve("pack"));
        Files.createDirectories(pack.getPath());
        new ContentProcessingCache(pack, "definitions").remember(true, true, true, true);
        PackAssetIndex.modern(pack.getAssetsPath(), null);
        try (var folders = Files.list(root.resolve("cache")))
        {
            assertEquals(java.util.Set.of("generation", "asset-index"),
                folders.map(folder -> folder.getFileName().toString()).collect(java.util.stream.Collectors.toSet()));
        }
        try (var leftovers = Files.walk(root.resolve("cache")))
        {
            assertTrue(leftovers.noneMatch(file -> file.getFileName().toString().endsWith(".tmp")));
        }
    }

    @Test
    void snapshotMatchesStampingEachListedFile() throws Exception
    {
        Path tree = root.resolve("tree");
        for (String name : java.util.List.of("a.txt", "sub/b.png", "sub/deeper/c.json", "sub/empty.txt"))
        {
            Path file = tree.resolve(name);
            Files.createDirectories(file.getParent());
            Files.writeString(file, name.endsWith("empty.txt") ? "" : name);
        }
        Files.createDirectories(tree.resolve("empty-folder"));
        // What the walk-and-stamp form recorded, which existing cache entries were written with.
        Map<String, ContentFileCache.Stamp> expected = new java.util.TreeMap<>();
        try (var files = Files.walk(tree))
        {
            for (Path file : files.filter(Files::isRegularFile).toList())
                expected.put(tree.relativize(file).toString().replace('\\', '/'), ContentFileCache.stamp(file));
        }
        assertEquals(expected, ContentFileCache.snapshot(tree));
        assertEquals(Map.of(), ContentFileCache.snapshot(root.resolve("missing")));
    }

    @Test
    void streamedGenerationDigestEqualsTheHashOfTheConcatenatedInput()
    {
        var digest = ContentFileCache.newDigest();
        StringBuilder concatenated = new StringBuilder();
        for (String part : java.util.List.of("guns/a.txt:[ShortName a]\n", "Sturmgeschütz 坦克\n", "{a=a_2}"))
        {
            digest.update(part.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            concatenated.append(part);
        }
        assertEquals(ContentFileCache.hash(concatenated.toString()), java.util.HexFormat.of().formatHex(digest.digest()));
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
