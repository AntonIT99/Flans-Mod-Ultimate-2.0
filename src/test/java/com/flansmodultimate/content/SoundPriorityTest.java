package com.flansmodultimate.content;

import com.flansmodultimate.util.SoundLengthIndex;
import com.google.gson.Gson;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.flag.FeatureFlagSet;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class SoundPriorityTest
{
    @TempDir Path root;
    @AfterEach void reset() { ContentFileCache.configure(null); SoundLengthIndex.clear(); }

    @Test
    void selectsTheHighestEventDefinitionAndFileIndependently()
    {
        var high = assets("{\"shoot\":{\"sounds\":[\"flansmod:high\"]}}", Map.of("high", 20, "engine", 80));
        var low = assets("{\"shoot\":{\"sounds\":[\"flansmod:low\"]},\"loop\":{\"sounds\":[\"flansmod:engine\"]}}", Map.of("low", 40, "engine", 10));
        var plan = SoundPriorityPlan.create(List.of(high, low));
        assertEquals("flansmod:high", plan.events().getAsJsonObject("shoot").getAsJsonArray("sounds").get(0).getAsString());
        assertTrue(plan.events().getAsJsonObject("shoot").get("replace").getAsBoolean());
        assertEquals(20, plan.lengths().get("shoot"));
        // The event comes from the low pack, but its audio is overridden by the high pack.
        assertEquals(80, plan.lengths().get("loop"));
        assertEquals(0, plan.fileOwners().get("engine"));
        assertEquals(10, SoundPriorityPlan.create(List.of(low, high)).lengths().get("loop"));
        SoundLengthIndex.replace(plan.lengths());
        assertEquals(OptionalInt.of(80), SoundLengthIndex.getSoundLength("loop"));
    }

    @Test
    void resolvesNestedEventsAndVariantPitchAndPreservesUnknownOrCyclicTimers()
    {
        var source = assets("""
            {"base":{"sounds":["flansmod:short", "flansmod:long"]},
             "alias":{"sounds":[{"name":"flansmod:base","type":"event","pitch":2}]},
             "unknown":{"sounds":["flansmod:missing", "flansmod:long"]},
             "a":{"sounds":[{"name":"flansmod:b","type":"event"}]},
             "b":{"sounds":[{"name":"flansmod:a","type":"event"}]}}
            """, Map.of("short", 10, "long", 60));
        var plan = SoundPriorityPlan.create(List.of(source));
        assertEquals(60, plan.lengths().get("base"));
        assertEquals(30, plan.lengths().get("alias"));
        SoundLengthIndex.replace(plan.lengths());
        assertTrue(SoundLengthIndex.getSoundLength("unknown").isEmpty());
        assertTrue(SoundLengthIndex.getSoundLength("a").isEmpty());
    }

    @Test
    void anUnreadableHigherPriorityFileNeverFallsBackToAnotherPacksDuration()
    {
        var high = assets("{}", Map.of("engine", 0));
        var low = assets("{\"loop\":{\"sounds\":[\"flansmod:engine\"]}}", Map.of("engine", 40));
        var plan = SoundPriorityPlan.create(List.of(high, low));
        assertEquals(0, plan.fileOwners().get("engine"));
        SoundLengthIndex.replace(plan.lengths());
        assertTrue(SoundLengthIndex.getSoundLength("loop").isEmpty());
    }

    @Test
    void standaloneIdsSurviveJarConversionAndSourceOrderIgnoresMissingEntries()
    {
        ContentPack pack = new ContentPack("Pack.jar", root.resolve("Pack.jar"));
        assertEquals("pack:pack", SoundPriority.standaloneId(pack));
        pack.update("Pack.zip", root.resolve("Pack.zip"));
        assertEquals("pack:pack", SoundPriority.standaloneId(pack));
        var a = new SoundPriority.Source("pack:a", root.resolve("a"), null);
        var b = new SoundPriority.Source("mod:b", root.resolve("b"), null);
        Map<String, SoundPriority.Source> sources = new LinkedHashMap<>();
        sources.put(a.id(), a); sources.put(b.id(), b);
        assertEquals(List.of(b, a), SoundPriority.orderedSources(sources, List.of("removed", b.id(), b.id())));
    }

    @Test
    void warmSoundIndexDoesNotReadAudioOrSourceJsonAndChangedFilesInvalidateIt() throws Exception
    {
        ContentFileCache.configure(root.resolve("cache"));
        var source = source("pack:a", root.resolve("a"), 40);
        var cold = SoundAssetIndex.read(source);
        Path audio = source.root().resolve("assets/flansmod/sounds/engine.ogg");
        Path json = source.root().resolve("assets/flansmod/sounds.json");
        FileTime audioTime = Files.getLastModifiedTime(audio), jsonTime = Files.getLastModifiedTime(json);
        Files.write(audio, new byte[(int)Files.size(audio)]); Files.setLastModifiedTime(audio, audioTime);
        Files.writeString(json, " ".repeat((int)Files.size(json))); Files.setLastModifiedTime(json, jsonTime);
        var before = ContentFileCache.snapshot(root.resolve("cache"));
        assertEquals(cold, SoundAssetIndex.read(source));
        assertEquals(before, ContentFileCache.snapshot(root.resolve("cache")));
        writeAudio(audio, 80);
        Files.setLastModifiedTime(audio, FileTime.fromMillis(audioTime.toMillis() + 2000));
        Files.writeString(json, "{\"loop\":{\"sounds\":[\"flansmod:engine\"]}}");
        assertEquals(80, SoundAssetIndex.read(source).files().get("engine"));
    }

    @Test
    void archiveCacheAvoidsOpeningUnchangedArchives() throws Exception
    {
        ContentFileCache.configure(root.resolve("cache"));
        var directory = source("pack:a", root.resolve("a"), 40);
        Path archive = root.resolve("a.zip");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive)); var files = Files.walk(directory.root()))
        {
            for (Path file : files.filter(Files::isRegularFile).toList())
            {
                zip.putNextEntry(new ZipEntry(directory.root().relativize(file).toString().replace('\\', '/')));
                zip.write(Files.readAllBytes(file)); zip.closeEntry();
            }
        }
        var source = new SoundPriority.Source("pack:a", archive, archive);
        var cold = SoundAssetIndex.read(source);
        FileTime time = Files.getLastModifiedTime(archive);
        Files.write(archive, new byte[(int)Files.size(archive)]); Files.setLastModifiedTime(archive, time);
        assertEquals(cold, SoundAssetIndex.read(source));
    }

    @Test
    void overlayProvidesTheChosenAudioAndDefinitionsAndDoesNotExposeOtherResources() throws Exception
    {
        var high = source("pack:high", root.resolve("high"), 80);
        var low = source("pack:low", root.resolve("low"), 20);
        var plan = SoundPriorityPlan.create(List.of(SoundAssetIndex.read(high), SoundAssetIndex.read(low)));
        byte[] json = new Gson().toJson(plan.events()).getBytes(StandardCharsets.UTF_8);
        var packLocation = new PackLocationInfo("overlay", Component.literal("overlay"), PackSource.BUILT_IN, Optional.empty());
        try (var overlay = new SoundPriorityPackResources(packLocation, List.of(high, low), plan, json))
        {
            var location = ResourceLocation.fromNamespaceAndPath("flansmod", "sounds/engine.ogg");
            try (var stream = overlay.getResource(PackType.CLIENT_RESOURCES, location).get())
            {
                assertArrayEquals(Files.readAllBytes(high.root().resolve("assets/flansmod/sounds/engine.ogg")), stream.readAllBytes());
            }
            var found = new java.util.ArrayList<ResourceLocation>();
            overlay.listResources(PackType.CLIENT_RESOURCES, "flansmod", "sounds", (id, stream) -> found.add(id));
            assertEquals(List.of(location), found);
            assertNull(overlay.getResource(PackType.SERVER_DATA, location));
            assertNull(overlay.getResource(PackType.CLIENT_RESOURCES, ResourceLocation.fromNamespaceAndPath("flansmod", "textures/item/icon.png")));
            try (var stream = overlay.getResource(PackType.CLIENT_RESOURCES, ResourceLocation.fromNamespaceAndPath("flansmod", "sounds.json")).get())
            {
                assertArrayEquals(json, stream.readAllBytes());
            }
        }
    }

    @Test
    void overlayWinsNeoForgeRepositorySourceOrderingAndOpensSelectedAudio() throws Exception
    {
        var source = source("pack:override", root.resolve("override"), 80);
        var plan = SoundPriorityPlan.create(List.of(SoundAssetIndex.read(source)));
        byte[] json = new Gson().toJson(plan.events()).getBytes(StandardCharsets.UTF_8);
        Pack overlay = SoundPriority.createPack(List.of(source), plan, json);
        var location = new PackLocationInfo("packaged:assets", Component.literal("Packaged assets"), PackSource.BUILT_IN, Optional.empty());
        Pack assets = new Pack(location, new PathPackResources.PathResourcesSupplier(source.root()),
            new Pack.Metadata(location.title(), PackCompatibility.COMPATIBLE, FeatureFlagSet.of(), List.of(), false),
            new PackSelectionConfig(true, Pack.Position.TOP, true));
        // NeoForge keeps repository source order; the first discovered fixed TOP pack wins.
        PackRepository repository = new PackRepository(acceptor -> acceptor.accept(overlay), acceptor -> acceptor.accept(assets));
        repository.reload();
        assertEquals(List.of(assets.getId(), overlay.getId()), List.copyOf(repository.getSelectedIds()));
        repository.setSelected(List.of());
        assertEquals(List.of(assets.getId(), overlay.getId()), List.copyOf(repository.getSelectedIds()));
        assertTrue(overlay.isRequired());
        assertTrue(overlay.isFixedPosition());
        try (var resources = overlay.open())
        {
            var audio = ResourceLocation.fromNamespaceAndPath("flansmod", "sounds/engine.ogg");
            try (var stream = resources.getResource(PackType.CLIENT_RESOURCES, audio).get())
            {
                assertArrayEquals(Files.readAllBytes(source.root().resolve("assets/flansmod/sounds/engine.ogg")), stream.readAllBytes());
            }
            assertNull(resources.getResource(PackType.SERVER_DATA, audio));
        }
    }

    @Test
    void commonSoundPreparationResolvesLegacyAssetsWithoutClientInitialization() throws Exception
    {
        ContentPack pack = new ContentPack("legacy", root.resolve("legacy"));
        Path assets = pack.getAssetsPath();
        Path legacy = Files.createDirectories(assets.resolve("sound")).resolve("Engine Loop.ogg");
        writeAudio(legacy, 40);
        Files.writeString(assets.resolve("sounds.json"), "{\"loop\":{\"sounds\":[\"Engine Loop\"]}}");
        ContentPackSounds.generate(pack);
        assertTrue(Files.exists(legacy));
        var indexed = SoundAssetIndex.read(new SoundPriority.Source("pack:legacy", pack.getPath(), null));
        var plan = SoundPriorityPlan.create(List.of(indexed));
        assertEquals(40, plan.lengths().get("loop"));
        assertTrue(indexed.files().containsKey("engine_loop"));
    }

    private static SoundPriorityPlan.Assets assets(String json, Map<String, Integer> files)
    {
        return new SoundPriorityPlan.Assets(JsonParser.parseString(json).getAsJsonObject(), files);
    }

    private static SoundPriority.Source source(String id, Path root, int ticks) throws Exception
    {
        Path assets = root.resolve("assets/flansmod");
        Files.createDirectories(assets.resolve("sounds"));
        writeAudio(assets.resolve("sounds/engine.ogg"), ticks);
        Files.writeString(assets.resolve("sounds.json"), "{\"loop\":{\"sounds\":[\"flansmod:engine\"]}}");
        return new SoundPriority.Source(id, root, null);
    }

    private static void writeAudio(Path file, int ticks) throws Exception
    {
        ByteBuffer data = ByteBuffer.allocate(87).order(ByteOrder.LITTLE_ENDIAN);
        data.put("OggS".getBytes(StandardCharsets.US_ASCII));
        data.position(26); data.put((byte)1).put((byte)30);
        data.put((byte)1).put("vorbis".getBytes(StandardCharsets.US_ASCII));
        data.position(40); data.putInt(44100);
        data.position(58); data.put("OggS".getBytes(StandardCharsets.US_ASCII));
        data.position(64); data.putLong(ticks * 44100L / 20);
        data.position(84); data.put((byte)1).put((byte)1).put((byte)0);
        Files.write(file, data.array());
    }
}
