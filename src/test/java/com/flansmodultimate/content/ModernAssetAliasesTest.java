package com.flansmodultimate.content;

import com.flansmodultimate.util.ResourceUtils;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.PackSource;

import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ModernAssetAliasesTest
{
    @Test
    void identicalLaterConflictsShareOneAlias() throws Exception
    {
        Path a = temp.resolve("a"), b = temp.resolve("b"), c = temp.resolve("c");
        write(a, "textures/item/shared.png", "first");
        write(b, "textures/item/shared.png", "second");
        write(c, "textures/item/shared.png", "second");
        List<ModernAssetAliases.View> views = ModernAssetAliases.plan(List.of(
            new ModernAssetAliases.Input(a, false), new ModernAssetAliases.Input(b, false), new ModernAssetAliases.Input(c, false)));
        assertEquals(views.get(1).sources(), views.get(2).sources());
        assertTrue(views.get(2).sources().containsKey("textures/item/shared_2.png"));
        assertFalse(views.get(2).sources().containsKey("textures/item/shared_3.png"));
    }
    @TempDir Path temp;

    @Test
    void propagatesTextureModelAndBlockstateAliasesWithoutChangingFiles() throws Exception
    {
        Path first = pack("first");
        Path second = pack("second");
        String model = """
            {"parent":"flansmod:block/base","textures":{"side":"flansmod:block/metal","particle":"#side",
            "foreign":"other:block/metal","default":"block/metal"},"elements":[],
            "overrides":[{"predicate":{"flansmod:paintjob":1},"model":"flansmod:item/paint"}]}
            """;
        for (Path pack : List.of(first, second))
        {
            write(pack, "models/block/base.json", "{\"textures\":{\"all\":\"flansmod:block/metal\"}}");
            write(pack, "models/block/metal.json", model);
            write(pack, "models/item/paint.json", "{\"textures\":{\"layer0\":\"flansmod:item/icon\"}}");
        }
        write(first, "textures/block/metal.png", "first pixels");
        write(second, "textures/block/metal.png", "second pixels");
        write(first, "textures/item/icon.png", "first icon");
        write(second, "textures/item/icon.png", "second icon");
        write(second, "textures/block/metal.png.mcmeta", "{\"animation\":{}}");
        // Reserve an existing suffix before assigning aliases.
        write(second, "textures/block/metal_2.png", "another texture");
        write(second, "blockstates/unique.json", """
            {"variants":{"":{"model":"flansmod:block/metal"},"a=true":[{"model":"flansmod:block/base","weight":2}]},
            "multipart":[{"when":{"a":"true"},"apply":[{"model":"flansmod:block/metal","y":90}]}]}
            """ );

        List<ModernAssetAliases.Input> inputs = List.of(new ModernAssetAliases.Input(first, true), new ModernAssetAliases.Input(second, false));
        ModernAssetAliases.View view = ModernAssetAliases.plan(inputs).get(1);
        assertEquals("textures/block/metal.png", view.sources().get("textures/block/metal_3.png"));
        assertEquals("textures/block/metal.png.mcmeta", view.sources().get("textures/block/metal_3.png.mcmeta"));
        JsonObject rewritten = json(view, "models/block/metal_2.json");
        assertEquals("flansmod:block/base_2", rewritten.get("parent").getAsString());
        assertEquals("flansmod:block/metal_3", rewritten.getAsJsonObject("textures").get("side").getAsString());
        assertEquals("#side", rewritten.getAsJsonObject("textures").get("particle").getAsString());
        assertEquals("other:block/metal", rewritten.getAsJsonObject("textures").get("foreign").getAsString());
        assertEquals("block/metal", rewritten.getAsJsonObject("textures").get("default").getAsString());
        assertEquals("flansmod:item/paint_2", rewritten.getAsJsonArray("overrides").get(0).getAsJsonObject().get("model").getAsString());
        assertTrue(rewritten.has("elements"));
        String state = new String(view.json().get("blockstates/unique.json"), StandardCharsets.UTF_8);
        assertTrue(state.contains("flansmod:block/metal_2"));
        assertTrue(state.contains("flansmod:block/base_2"));
        assertTrue(state.contains("\"weight\": 2"));
        assertTrue(state.contains("\"y\": 90"));
        assertEquals(model, Files.readString(second.resolve("models/block/metal.json")));
        ModernAssetAliases.View repeated = ModernAssetAliases.plan(inputs).get(1);
        assertEquals(view.sources(), repeated.sources());
        assertEquals(view.hidden(), repeated.hidden());
        for (String path : view.json().keySet())
            assertArrayEquals(view.json().get(path), repeated.json().get(path));
    }

    @Test
    void keepsIdenticalTexturesButSeparatesDifferentAnimationAndSubdirectories() throws Exception
    {
        Path first = pack("first");
        Path second = pack("second");
        for (Path pack : List.of(first, second))
        {
            write(pack, "textures/item/shared.png", "pixels");
            write(pack, "textures/block/shared.png", "different folder");
            write(pack, "textures/item/sub/shared.png", "nested");
        }
        write(second, "textures/item/sub/shared.png.mcmeta", "{\"animation\":{}}");
        ModernAssetAliases.View view = ModernAssetAliases.plan(List.of(
            new ModernAssetAliases.Input(first, true), new ModernAssetAliases.Input(second, false))).get(1);
        assertFalse(view.hidden().contains("textures/item/shared.png"));
        assertFalse(view.hidden().contains("textures/block/shared.png"));
        assertEquals("textures/item/sub/shared.png", view.sources().get("textures/item/sub/shared_2.png"));
    }

    @Test
    void entryModelsTakePriorityOverEarlierPaintjobModelsAndCyclesCanBeRewritten() throws Exception
    {
        Path first = pack("first");
        Path second = pack("second");
        write(first, "models/item/tool.json", "{\"parent\":\"flansmod:item/paint\"}");
        write(first, "models/item/paint.json", "{\"parent\":\"flansmod:item/tool\"}");
        write(second, "models/item/paint.json", "{\"parent\":\"minecraft:item/generated\"}");
        List<ModernAssetAliases.View> views = ModernAssetAliases.plan(List.of(
            new ModernAssetAliases.Input(first, false, Set.of("item/tool")),
            new ModernAssetAliases.Input(second, false, Set.of("item/paint"))));
        assertEquals("flansmod:item/paint_2", json(views.get(0), "models/item/tool.json").get("parent").getAsString());
        assertTrue(views.get(0).sources().containsKey("models/item/paint_2.json"));
        assertTrue(views.get(1).hidden().isEmpty());
    }

    @Test
    void redirectsReferencesToEntryModelsWhoseShortnamesWereAliased() throws Exception
    {
        Path pack = pack("aliased");
        write(pack, "models/block/box_2.json", "{\"elements\":[],\"textures\":{\"particle\":\"flansmod:block/metal\"}}");
        write(pack, "models/item/box_2.json", "{\"parent\":\"flansmod:block/box\"}");
        write(pack, "blockstates/box_2.json", "{\"multipart\":[{\"apply\":{\"model\":\"flansmod:block/box\"}}]}");
        ModernAssetAliases.View view = ModernAssetAliases.plan(List.of(new ModernAssetAliases.Input(pack, false,
            Set.of("block/box_2", "item/box_2"), Map.of("block/box", "block/box_2")))).get(0);
        assertEquals("flansmod:block/box_2", json(view, "models/item/box_2.json").get("parent").getAsString());
        assertEquals("flansmod:block/box_2", json(view, "blockstates/box_2.json").getAsJsonArray("multipart")
            .get(0).getAsJsonObject().getAsJsonObject("apply").get("model").getAsString());
    }

    @Test
    void customBlockGeometryAndModelPropertiesArePreservedDuringRegeneration()
    {
        JsonObject generated = JsonParser.parseString("""
            {"parent":"minecraft:block/cube_bottom_top","textures":{"top":"a","bottom":"b","side":"c"}}
            """).getAsJsonObject();
        assertTrue(ResourceUtils.isGeneratedBlockModel(generated));
        JsonObject geometry = generated.deepCopy();
        geometry.add("elements", JsonParser.parseString("[]"));
        assertFalse(ResourceUtils.isGeneratedBlockModel(geometry));
        JsonObject loader = generated.deepCopy();
        loader.addProperty("loader", "custom:loader");
        assertFalse(ResourceUtils.isGeneratedBlockModel(loader));
        JsonObject customParent = generated.deepCopy();
        customParent.addProperty("parent", "flansmod:block/custom");
        assertFalse(ResourceUtils.isGeneratedBlockModel(customParent));
    }

    @Test
    void readsArchiveAssetsAndRebuildsAliasesAfterSourceChanges() throws Exception
    {
        Path first = pack("first");
        write(first, "textures/item/icon.png", "first pixels");
        Path archive = temp.resolve("pack.zip");
        try (var fs = FileSystems.newFileSystem(archive, Map.of("create", "true")))
        {
            Path archived = fs.getPath("/assets/flansmod");
            write(archived, "textures/item/icon.png", "second pixels");
            write(archived, "models/item/test.json", "{\"textures\":{\"layer0\":\"flansmod:item/icon\"}}");
            List<ModernAssetAliases.Input> inputs = List.of(new ModernAssetAliases.Input(first, true),
                new ModernAssetAliases.Input(archived, false));
            ModernAssetAliases.View view = ModernAssetAliases.plan(inputs).get(1);
            assertEquals("flansmod:item/icon_2", json(view, "models/item/test.json").getAsJsonObject("textures")
                .get("layer0").getAsString());
            write(archived, "textures/item/icon.png", "first pixels");
            ModernAssetAliases.View reloaded = ModernAssetAliases.plan(inputs).get(1);
            assertTrue(reloaded.sources().isEmpty());
            assertTrue(reloaded.json().isEmpty());
        }
    }

    @Test
    void resourceLookupAndEnumerationExposeTheSameViewAndIgnoreLegacySources() throws Exception
    {
        Path first = pack("first");
        Path second = pack("second");
        write(first, "textures/item/icon.png", "a");
        write(second, "textures/item/icon.png", "b");
        write(second, "textures/items/icon.png", "legacy pixels");
        write(second, "textures/blocks/block.png", "legacy block");
        write(second, "models/item/unique.json", "{\"textures\":{\"layer0\":\"flansmod:item/icon\"}}");
        write(second.getParent().getParent(), "data/flansmod/textures/item/icon.png", "server data");
        ModernAssetAliases.View view = ModernAssetAliases.plan(List.of(
            new ModernAssetAliases.Input(first, true), new ModernAssetAliases.Input(second, false))).get(1);
        try (FilteringPackResources resources = new FilteringPackResources(
            new PathPackResources(new PackLocationInfo("test", Component.literal("test"), PackSource.DEFAULT, Optional.empty()),
                second.getParent().getParent()), PackType.CLIENT_RESOURCES, view))
        {
            assertNull(resources.getResource(PackType.CLIENT_RESOURCES, id("textures/item/icon.png")));
            assertNotNull(resources.getResource(PackType.CLIENT_RESOURCES, id("textures/item/icon_2.png")));
            Map<String, String> listed = new HashMap<>();
            for (String folder : List.of("textures", "models"))
                resources.listResources(PackType.CLIENT_RESOURCES, "flansmod", folder, (location, supplier) -> {
                    try (var stream = supplier.get())
                    {
                        listed.put(location.getPath(), new String(stream.readAllBytes(), StandardCharsets.UTF_8));
                    }
                    catch (Exception e) { throw new AssertionError(e); }
                });
            assertEquals("b", listed.get("textures/item/icon_2.png"));
            assertFalse(listed.containsKey("textures/item/icon.png"));
            assertTrue(listed.get("models/item/unique.json").contains("flansmod:item/icon_2"));
            assertFalse(listed.containsKey("textures/items/icon.png"));
            assertEquals("legacy pixels", Files.readString(second.resolve("textures/items/icon.png")));
            assertEquals("legacy block", Files.readString(second.resolve("textures/blocks/block.png")));
            try (var stream = resources.getResource(PackType.SERVER_DATA, id("textures/item/icon.png")).get())
            {
                assertEquals("server data", new String(stream.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
    }

    private Path pack(String name) throws Exception
    {
        return Files.createDirectories(temp.resolve(name).resolve("assets/flansmod"));
    }

    private static void write(Path root, String name, String content) throws Exception
    {
        Path file = root.resolve(name);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    private static JsonObject json(ModernAssetAliases.View view, String path)
    {
        return JsonParser.parseString(new String(view.json().get(path), StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static ResourceLocation id(String path)
    {
        return ResourceLocation.fromNamespaceAndPath("flansmod", path);
    }
}
