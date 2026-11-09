package com.flansmodultimate.content;

import com.flansmodultimate.common.types.BlockType;
import com.flansmodultimate.common.types.EnumType;
import com.flansmodultimate.util.DynamicReference;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ContentPackGeneratorsTest
{
    @TempDir Path root;

    @Test
    void carriesCustomModelsAndMultipartStatesThroughAnIdAlias() throws Exception
    {
        ContentPack provider = new ContentPack("test", root);
        TestBlock type = new TestBlock(provider, "crate", "crate_2");
        Path assets = provider.getAssetsPath();
        String block = "{\"textures\":{\"particle\":\"flansmod:block/metal\"},\"elements\":[]}";
        String item = "{\"parent\":\"flansmod:block/crate\",\"display\":{\"gui\":{\"scale\":[1,1,1]}}}";
        String state = "{\"multipart\":[{\"when\":{\"facing\":\"north\"},\"apply\":{\"model\":\"flansmod:block/crate\"}}]}";
        write(assets.resolve("models/block/crate.json"), block);
        write(assets.resolve("models/item/crate.json"), item);
        write(assets.resolve("blockstates/crate.json"), state);
        write(assets.resolve("blockstates/crate_2.json"), "{\"variants\":{\"\":{\"model\":\"flansmod:block/crate\"}},\"stale\":true}");

        ContentPackModels.generate(provider, List.of(type), List.of(type));

        assertEquals(block, Files.readString(assets.resolve("models/block/crate.json")));
        assertEquals(item, Files.readString(assets.resolve("models/item/crate.json")));
        assertEquals(state, Files.readString(assets.resolve("blockstates/crate.json")));
        ContentPackModels.generate(provider, List.of(type), List.of(type));
        assertEquals(block, Files.readString(assets.resolve("models/block/crate.json")));

        ModernAssetAliases.View view = ModernAssetAliases.plan(List.of(new ModernAssetAliases.Input(assets, false,
            java.util.Set.of("block/crate_2", "item/crate_2"), Map.of("block/crate", "block/crate_2", "item/crate", "item/crate_2")))).get(0);
        assertTrue(new String(view.json().get("models/item/crate_2.json"), StandardCharsets.UTF_8).contains("flansmod:block/crate_2"));
        assertTrue(new String(view.json().get("blockstates/crate_2.json"), StandardCharsets.UTF_8).contains("flansmod:block/crate_2"));
        assertFalse(new String(view.json().get("blockstates/crate_2.json"), StandardCharsets.UTF_8).contains("stale"));
    }

    @Test
    void regeneratesBasicModelsWhenDefinitionTexturesChange() throws Exception
    {
        ContentPack provider = new ContentPack("test", root);
        TestBlock type = new TestBlock(provider, "crate", "crate");
        ContentPackModels.generate(provider, List.of(type), List.of(type));
        type.changeTop("new_top");
        ContentPackModels.generate(provider, List.of(type), List.of(type));
        JsonObject model = json(provider.getAssetsPath().resolve("models/block/crate.json"));
        assertEquals("flansmod:block/new_top", model.getAsJsonObject("textures").get("top").getAsString());
        assertEquals("flansmod:block/crate", json(provider.getAssetsPath().resolve("models/item/crate.json"))
            .get("parent").getAsString());
    }

    @Test
    void convertsTranslationsUsingExplicitTypesWithoutChangingTheLegacyFile() throws Exception
    {
        ContentPack provider = new ContentPack("test", root);
        TestBlock type = new TestBlock(provider, "crate", "crate_2");
        Path lang = provider.getAssetsPath().resolve("lang/de_de.lang");
        String text = "\ufefftile.crate.name=Kiste\nitem.part.name=Bauteil\n";
        Files.createDirectories(lang.getParent());
        Files.writeString(lang, text, StandardCharsets.UTF_16LE);
        byte[] legacy = Files.readAllBytes(lang);
        ContentPackLocalization.generate(provider, List.of(type));
        JsonObject translated = json(lang.resolveSibling("de_de.json"));
        assertEquals("Kiste", translated.get("block.flansmod.crate_2").getAsString());
        assertFalse(translated.has("block.flansmod.crate"));
        assertEquals("Bauteil", translated.get("item.flansmod.part").getAsString());
        assertArrayEquals(legacy, Files.readAllBytes(lang));
    }

    @Test
    void generatedTextureCleanupPreservesAuthoredDestinationsAndLegacySources() throws Exception
    {
        Path source = root.resolve("legacy");
        Path destination = root.resolve("modern");
        png(source.resolve("Icon.png"), 0xffff0000);
        png(destination.resolve("icon.png"), 0xff0000ff);
        byte[] legacy = Files.readAllBytes(source.resolve("Icon.png"));
        byte[] authored = Files.readAllBytes(destination.resolve("icon.png"));
        GeneratedTextureFiles.copy(source, destination);
        assertTrue(Files.isRegularFile(destination.resolve("icon-1.png")));
        assertArrayEquals(legacy, Files.readAllBytes(source.resolve("Icon.png")));
        assertArrayEquals(authored, Files.readAllBytes(destination.resolve("icon.png")));
        GeneratedTextureFiles.copy(source, destination);
        assertFalse(GeneratedTextureFiles.needsUpdate(source, destination, Map.of(), false));
        assertFalse(Files.exists(destination.resolve("icon-2.png")));
        Files.delete(source.resolve("Icon.png"));
        Files.delete(source);
        GeneratedTextureFiles.copy(source, destination);
        assertFalse(Files.exists(destination.resolve("icon-1.png")));
        assertArrayEquals(authored, Files.readAllBytes(destination.resolve("icon.png")));
    }

    @Test
    void parallelTextureCopiesResolveCollisionsLikeSequentialCopies() throws Exception
    {
        Path source = root.resolve("legacy");
        for (int i = 0; i < 24; i++)
            png(source.resolve("Icon " + i + ".png"), 0xff000000 | i);
        png(source.resolve("Icon_0.png"), 0xffff0000);
        Path sequential = root.resolve("sequential");
        Path parallel = root.resolve("parallel");

        GeneratedTextureFiles.copy(source, sequential);
        try (ContentLoadingWorkers workers = ContentLoadingWorkers.create(4))
        {
            GeneratedTextureFiles.copy(source, parallel, Map.of(), false, workers);
        }

        try (var files = Files.list(sequential))
        {
            for (Path file : files.filter(path -> path.toString().endsWith(".png")).toList())
                assertArrayEquals(Files.readAllBytes(file), Files.readAllBytes(parallel.resolve(file.getFileName())));
        }
        assertTrue(Files.isRegularFile(parallel.resolve("icon_0-1.png")));
        assertFalse(GeneratedTextureFiles.needsUpdate(source, parallel, Map.of(), false));
    }

    @Test
    void generationInputsLeaveUnusedFilesInTheArchive()
    {
        var data = ContentPackAssets.generationInputs(false, true, false);
        assertTrue(data.test("id_alias.json"));
        assertTrue(data.test("data/flansmod/recipes/a.json"));
        assertFalse(data.test("assets/flansmod/textures/items/a.png"));
        assertFalse(data.test("guns/a.txt"));

        var assets = ContentPackAssets.generationInputs(true, false, false);
        assertTrue(assets.test("assets/flansmod/textures/items/a.png"));
        assertFalse(assets.test("data/flansmod/recipes/a.json"));
        assertFalse(assets.test("com/flansmod/client/model/a.class"));
        assertFalse(assets.test("assets/othermod/textures/a.png"));

        var sounds = ContentPackAssets.generationInputs(false, true, true);
        assertTrue(sounds.test("assets/flansmod/sound/a.ogg"));
        assertTrue(sounds.test("assets/flansmod/sounds.json"));
        assertFalse(sounds.test("assets/flansmod/skins/a.png"));
    }

    @Test
    void armorTextureAliasesKeepBothLayerSuffixes() throws Exception
    {
        Path source = root.resolve("armor");
        Path destination = root.resolve("modern");
        png(source.resolve("uniform_1.png"), 0xffff0000);
        png(source.resolve("uniform_2.png"), 0xff0000ff);
        GeneratedTextureFiles.copy(source, destination, Map.of("uniform", new DynamicReference("uniform_2")), true);
        assertTrue(Files.isRegularFile(destination.resolve("uniform_2_1.png")));
        assertTrue(Files.isRegularFile(destination.resolve("uniform_2_2.png")));
        assertTrue(Files.isRegularFile(source.resolve("uniform_1.png")));
        assertTrue(Files.isRegularFile(source.resolve("uniform_2.png")));
    }

    @Test
    void doesNotDeleteAuthoredCopiesOrAdoptAnIdenticalDestination() throws Exception
    {
        Path source = root.resolve("source"), destination = root.resolve("destination");
        png(source.resolve("icon.png"), 0xffff0000);
        Files.createDirectories(destination);
        Files.copy(source.resolve("icon.png"), destination.resolve("authored.png"));
        Files.copy(source.resolve("icon.png"), destination.resolve("icon.png"));
        GeneratedTextureFiles.copy(source, destination);
        assertTrue(Files.exists(destination.resolve("authored.png")));
        Files.delete(source.resolve("icon.png"));
        GeneratedTextureFiles.copy(source, destination);
        assertTrue(Files.exists(destination.resolve("icon.png")));
        assertTrue(Files.exists(destination.resolve("authored.png")));
    }

    @Test
    void sourcePixelEditsInvalidateCopiesAndUserEditsReleaseOwnership() throws Exception
    {
        Path source = root.resolve("source"), destination = root.resolve("destination");
        png(source.resolve("icon.png"), 0xffff0000);
        GeneratedTextureFiles.copy(source, destination);
        png(source.resolve("icon.png"), 0xff0000ff);
        Files.setLastModifiedTime(source.resolve("icon.png"), java.nio.file.attribute.FileTime.fromMillis(System.currentTimeMillis() + 2000));
        assertTrue(GeneratedTextureFiles.needsUpdate(source, destination, Map.of(), false));
        GeneratedTextureFiles.copy(source, destination);
        assertEquals(0xff0000ff, ImageIO.read(destination.resolve("icon.png").toFile()).getRGB(0, 0));
        png(destination.resolve("icon.png"), 0xff00ff00);
        Files.delete(source.resolve("icon.png"));
        GeneratedTextureFiles.copy(source, destination);
        assertEquals(0xff00ff00, ImageIO.read(destination.resolve("icon.png").toFile()).getRGB(0, 0));
    }

    @Test
    void ownershipSurvivesArchiveExtractionAndTimestampChanges() throws Exception
    {
        Path source = root.resolve("source"), destination = root.resolve("destination");
        png(source.resolve("icon.png"), 0xffff0000);
        GeneratedTextureFiles.copy(source, destination);
        Files.setLastModifiedTime(destination.resolve("icon.png"), java.nio.file.attribute.FileTime.fromMillis(0));
        png(source.resolve("icon.png"), 0xff0000ff);
        GeneratedTextureFiles.copy(source, destination);
        assertEquals(0xff0000ff, ImageIO.read(destination.resolve("icon.png").toFile()).getRGB(0, 0));
        assertFalse(Files.exists(destination.resolve("icon-1.png")));
    }

    @Test
    void customModelsSurviveAliasRemovalAndOldMigrationIsRecovered() throws Exception
    {
        ContentPack provider = new ContentPack("test", root);
        TestBlock type = new TestBlock(provider, "crate", "crate_2");
        Path model = provider.getAssetsPath().resolve("models/block/crate.json");
        String authored = "{\"elements\":[],\"customLabel\":\"Keep My Case\"}";
        write(model.resolveSibling("crate_2.json"), authored);
        AliasFileManager.writeToAliasMappingFile(ContentPackPaths.ID_ALIAS_FILE, provider, Map.of("crate", "crate_2"));
        ContentPackModels.generate(provider, List.of(type), List.of(type));
        assertEquals(authored, Files.readString(model));
        TestBlock original = new TestBlock(provider, "crate", "crate");
        ContentPackModels.generate(provider, List.of(original), List.of(original));
        assertEquals(authored, Files.readString(model));
    }

    @Test
    void englishFallbackRefreshesGeneratedNamesAndPreservesAuthoredTranslations() throws Exception
    {
        ContentPack provider = new ContentPack("test", root);
        TestBlock type = new TestBlock(provider, "crate", "crate");
        Path english = provider.getAssetsPath().resolve("lang/en_us.json");
        ContentPackLocalization.generate(provider, List.of(type));
        assertEquals("Test crate", json(english).get("block.flansmod.crate").getAsString());
        type.changeName("New name");
        ContentPackLocalization.generate(provider, List.of(type));
        assertEquals("New name", json(english).get("block.flansmod.crate").getAsString());
        write(english, "{\"block.flansmod.crate\":\"Authored name\"}");
        type.changeName("Another default");
        ContentPackLocalization.generate(provider, List.of(type));
        assertEquals("Authored name", json(english).get("block.flansmod.crate").getAsString());
    }

    @Test
    void firstJarGenerationProducesAWarmIdempotentZipLoad() throws Exception
    {
        Path packs = Files.createDirectories(root.resolve("flan"));
        Path jar = packs.resolve("test.jar");
        try (var zip = new java.util.zip.ZipOutputStream(Files.newOutputStream(jar)))
        {
            zip.putNextEntry(new java.util.zip.ZipEntry("gunboxes/crate.txt"));
            zip.write("ShortName crate\nName Test crate".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        ContentFileCache.configure(root.resolve("cache"));
        try
        {
            ContentPack provider = new ContentPack("test.jar", jar);
            TestBlock type = new TestBlock(provider, "crate", "crate");
            var aliases = new ContentPackAssets.TextureReferences(Map.of(), Map.of(), Map.of());
            var legacy = PackAssetIndex.legacy(provider);
            ContentProcessingCache cold = new ContentProcessingCache(provider, "definitions");
            assertFalse(cold.assetsCurrent());
            assertFalse(cold.dataCurrent());
            assertTrue(com.flansmodultimate.util.FileUtils.extractArchive(jar, provider.getExtractedPath()));
            ContentPackAssets.createMcMeta(provider);
            ContentPackAssets.createRecipeJsonFiles(provider, List.of(type));
            ContentPackAssets.generate(provider, List.of(type), List.of(type), List.of(type), aliases, ContentLoadingWorkers.sequential());
            assertTrue(ContentPackAssets.outputsPresent(provider, List.of(type), List.of(type), aliases));
            assertTrue(ContentPackAssets.dataOutputsPresent(provider, List.of(type)));
            AliasFileManager.writeToAliasMappingFile(ContentPackPaths.ID_ALIAS_FILE, provider, Map.of("crate", "crate"));
            assertTrue(com.flansmodultimate.util.FileUtils.repackArchive(provider));
            cold.remember(true, true, true, true);
            var fs = com.flansmodultimate.util.FileUtils.createFileSystem(provider);
            try
            {
                PackAssetIndex.rememberLegacy(provider, fs, legacy);
            }
            finally
            {
                com.flansmodultimate.util.FileUtils.closeFileSystem(fs, provider);
            }
            var modern = PackAssetIndex.modern(provider);
            assertFalse(Files.exists(jar));
            assertFalse(Files.exists(provider.getExtractedPath()));
            assertTrue(provider.isZipFile());

            // New provider simulates a second process, with a new extraction run ID.
            ContentPack next = new ContentPack("test.zip", provider.getPath());
            var before = ContentFileCache.snapshot(root);
            ContentProcessingCache warm = new ContentProcessingCache(next, "definitions");
            assertTrue(warm.assetsCurrent());
            assertTrue(warm.dataCurrent());
            assertFalse(ContentPackAssets.aliasesChanged(next, aliases));
            assertEquals(legacy, PackAssetIndex.legacy(next));
            assertEquals(modern, PackAssetIndex.modern(next));
            warm.remember(false, false, false, false);
            assertEquals(before, ContentFileCache.snapshot(root));
            assertFalse(Files.exists(next.getExtractedPath()));
        }
        finally
        {
            ContentFileCache.configure(null);
        }
    }

    @Test
    void concurrentPackReprocessingProducesTheSameArchivesAsSequential()
    {
        ContentFileCache.configure(root.resolve("cache"));
        try (ContentLoadingWorkers workers = ContentLoadingWorkers.create(4))
        {
            List<Integer> packs = java.util.stream.IntStream.range(0, 8).boxed().toList();
            List<Map<String, String>> sequential = ContentPackJobs.runAll(packs, pack -> reprocessPack("sequential", pack, workers), 1, finished -> {});
            List<Map<String, String>> concurrent = ContentPackJobs.runAll(packs, pack -> reprocessPack("concurrent", pack, workers), 4, finished -> {});

            assertEquals(sequential, concurrent);
            for (Map<String, String> pack : concurrent)
            {
                assertTrue(pack.containsKey("pack.mcmeta"));
                assertTrue(pack.containsKey("assets/flansmod/models/item/crate.json"));
                assertTrue(pack.containsKey("assets/flansmod/textures/item/crate.png"));
                assertTrue(pack.containsKey("guns/unrelated.txt"));
            }
            assertNotEquals(concurrent.get(0).get("assets/flansmod/textures/item/crate.png"), concurrent.get(1).get("assets/flansmod/textures/item/crate.png"));
        }
        finally
        {
            ContentFileCache.configure(null);
        }
    }

    /** Takes a pack through the stages of a client's reprocessing and returns the archive's files. */
    private Map<String, String> reprocessPack(String run, int pack, ContentLoadingWorkers workers)
    {
        try
        {
            Path source = root.resolve(run).resolve("source" + pack);
            png(source.resolve("assets/flansmod/textures/items/crate.png"), 0xff000000 | pack);
            write(source.resolve("guns/unrelated.txt"), "pack " + pack);
            Path archive = root.resolve(run).resolve("flan/pack" + pack + ".zip");
            Files.createDirectories(archive.getParent());
            try (var zip = new java.util.zip.ZipOutputStream(Files.newOutputStream(archive)); var files = Files.walk(source))
            {
                for (Path file : files.filter(Files::isRegularFile).sorted().toList())
                {
                    zip.putNextEntry(new java.util.zip.ZipEntry(source.relativize(file).toString().replace('\\', '/')));
                    zip.write(Files.readAllBytes(file));
                    zip.closeEntry();
                }
            }

            ContentPack provider = new ContentPack("pack" + pack + ".zip", archive);
            TestBlock type = new TestBlock(provider, "crate", "crate");
            var aliases = new ContentPackAssets.TextureReferences(Map.of(), Map.of(), Map.of());
            IncrementalContentArchive incremental = IncrementalContentArchive.open(archive);
            assertNotNull(incremental);
            assertTrue(incremental.extract(provider.getExtractedPath(), ContentPackAssets.generationInputs(true, true, false), workers));
            ContentPackAssets.createMcMeta(provider);
            ContentPackAssets.createRecipeJsonFiles(provider, List.of(type));
            ContentPackAssets.generate(provider, List.of(type), List.of(type), List.of(type), aliases, workers);
            assertTrue(ContentPackAssets.outputsPresent(provider, List.of(type), List.of(type), aliases));
            assertTrue(com.flansmodultimate.util.FileUtils.repackArchive(provider, target -> incremental.write(target, provider.getExtractedPath(), workers)));
            assertFalse(Files.exists(provider.getExtractedPath()));
            return archiveContents(archive);
        }
        catch (Exception e)
        {
            throw new IllegalStateException(e);
        }
    }

    private static Map<String, String> archiveContents(Path archive) throws Exception
    {
        Map<String, String> contents = new java.util.TreeMap<>();
        try (var zip = new java.util.zip.ZipFile(archive.toFile()))
        {
            for (var entry : java.util.Collections.list(zip.entries()))
            {
                // The manifest records file times, which differ between any two runs.
                if (!entry.isDirectory() && !entry.getName().endsWith(".flansmod_generated_textures.json"))
                    contents.put(entry.getName(), java.util.Base64.getEncoder().encodeToString(zip.getInputStream(entry).readAllBytes()));
            }
        }
        return contents;
    }

    private static void write(Path file, String text) throws Exception
    {
        Files.createDirectories(file.getParent());
        Files.writeString(file, text, StandardCharsets.UTF_8);
    }

    private static JsonObject json(Path file) throws Exception
    {
        return JsonParser.parseString(Files.readString(file)).getAsJsonObject();
    }

    private static void png(Path file, int color) throws Exception
    {
        Files.createDirectories(file.getParent());
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, color);
        assertTrue(ImageIO.write(image, "png", file.toFile()));
    }

    private static final class TestBlock extends BlockType
    {
        TestBlock(IContentProvider provider, String original, String registered)
        {
            contentPack = provider;
            originalShortName = original;
            uniqueShortName = registered;
            name = "Test crate";
            icon = "crate";
            type = EnumType.GUN_BOX;
            topTextureName = "top";
            bottomTextureName = "bottom";
            sideTextureName = "side";
        }

        @Override
        public String getShortName()
        {
            return uniqueShortName;
        }

        void changeTop(String texture)
        {
            topTextureName = texture;
        }

        void changeName(String value)
        {
            name = value;
        }
    }
}
