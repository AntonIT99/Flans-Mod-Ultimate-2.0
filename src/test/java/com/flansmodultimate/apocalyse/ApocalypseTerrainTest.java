package com.flansmodultimate.apocalyse;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import net.neoforged.fml.loading.LoadingModList;
import org.junit.jupiter.api.*;

import net.minecraft.SharedConstants;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.data.worldgen.NoiseData;
import net.minecraft.resources.*;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ApocalypseTerrainTest
{
    private static final Path DATA = Path.of("src/main/resources/data/flansmodapocalypse/worldgen");
    private static HolderLookup.Provider vanilla;

    @BeforeAll
    static void bootstrap()
    {
        // NeoForge loads optional feature flags from its discovery list during vanilla bootstrap.
        if (LoadingModList.get() == null)
            LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        vanilla = VanillaRegistries.createLookup();
    }

    @Test
    void terrainAndNoiseSettingsDecodeWithNativeCodecsAndKeepBasinsDry() throws IOException
    {
        var ops = RegistryOps.create(JsonOps.INSTANCE, vanilla);
        DensityFunction terrain = DensityFunction.DIRECT_CODEC.parse(ops, read("density_function/terrain.json")).getOrThrow(AssertionError::new);
        var terrainKey = ResourceKey.create(Registries.DENSITY_FUNCTION, ResourceLocation.fromNamespaceAndPath("flansmodapocalypse", "terrain"));
        HolderLookup.Provider patched = new RegistrySetBuilder().add(Registries.NOISE, NoiseData::bootstrap).add(Registries.DENSITY_FUNCTION, context ->
        {
            NoiseRouterData.bootstrap(context);
            context.register(terrainKey, terrain);
        }).build(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
        // Minecraft 1.21 block codecs reject unregistered ids instead of decoding air, and plain JUnit registers no mod blocks.
        JsonElement noise = JsonParser.parseString(Files.readString(DATA.resolve("noise_settings/apocalypse.json")).replace("flansmodapocalypse:blocksulphur", "minecraft:stone"));
        NoiseGeneratorSettings settings = NoiseGeneratorSettings.DIRECT_CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, patched), noise).getOrThrow(AssertionError::new);
        assertFalse(settings.isAquifersEnabled());
        assertEquals(0, settings.seaLevel());
    }

    @Test
    void climateBandsHaveFlatFloorsAndPlateausWithLowSulphurBasins() throws IOException
    {
        assertFloor(-0.6, 0.5, 0.5, 33);
        assertFloor(-0.1, 0.5, 0.5, 47);
        assertFloor(0.5, 0.5, 0.5, 64);
        assertFloor(0.5, -0.1, 0.5, 81);
        assertFloor(0.5, -0.6, 0.5, 106);
        assertFloor(0.5, -0.6, 0.0, 33);
    }

    @Test
    void sulphurPitsAreNotShadowedByTheCommonBiomeClimateRanges() throws IOException
    {
        JsonObject dimension = JsonParser.parseString(Files.readString(Path.of("src/main/resources/datapacks/apocalypse/data/flansmodapocalypse/dimension/apocalypse.json"))).getAsJsonObject();
        JsonElement entries = dimension.getAsJsonObject("generator").getAsJsonObject("biome_source").get("biomes");
        Climate.ParameterList<ResourceLocation> biomes = Climate.ParameterList.codec(ResourceLocation.CODEC.fieldOf("biome")).parse(JsonOps.INSTANCE, entries).getOrThrow(AssertionError::new);
        assertEquals("flansmodapocalypse:sulphur_pits", biomes.findValue(Climate.target(0F, 0F, 0.5F, -0.6F, 0F, 0F)).toString());
        assertEquals("flansmodapocalypse:high_plateau", biomes.findValue(Climate.target(0F, 0F, 0.5F, -0.6F, 0F, 0.5F)).toString());
        assertEquals("flansmodapocalypse:apocalypse", biomes.findValue(Climate.target(0F, 0F, 0.5F, 0.5F, 0F, 0.5F)).toString());
        assertEquals("flansmodapocalypse:deep_canyon", biomes.findValue(Climate.target(0F, 0F, -0.6F, 0.5F, 0F, 0.5F)).toString());
    }

    private static void assertFloor(double continents, double erosion, double ridges, int floor) throws IOException
    {
        JsonElement json = read("density_function/terrain.json");
        replaceCoordinates(json, Map.of("minecraft:overworld/continents", continents, "minecraft:overworld/erosion", erosion, "minecraft:overworld/ridges", ridges));
        DensityFunction terrain = DensityFunction.DIRECT_CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, vanilla), json).getOrThrow(AssertionError::new);
        assertTrue(terrain.compute(new DensityFunction.SinglePointContext(0, floor - 1, 0)) > 0);
        assertEquals(0D, terrain.compute(new DensityFunction.SinglePointContext(0, floor, 0)), 1.0E-5D);
        assertTrue(terrain.compute(new DensityFunction.SinglePointContext(0, floor + 1, 0)) < 0);
    }

    private static void replaceCoordinates(JsonElement json, Map<String, Double> values)
    {
        if (json.isJsonObject())
        {
            JsonObject object = json.getAsJsonObject();
            JsonElement coordinate = object.get("coordinate");
            if (coordinate != null && coordinate.isJsonPrimitive() && values.containsKey(coordinate.getAsString()))
                object.add("coordinate", new JsonPrimitive(values.get(coordinate.getAsString())));
            object.entrySet().forEach(entry -> replaceCoordinates(entry.getValue(), values));
        }
        else if (json.isJsonArray())
            json.getAsJsonArray().forEach(value -> replaceCoordinates(value, values));
    }

    private static JsonElement read(String path) throws IOException
    {
        return JsonParser.parseString(Files.readString(DATA.resolve(path)));
    }
}
