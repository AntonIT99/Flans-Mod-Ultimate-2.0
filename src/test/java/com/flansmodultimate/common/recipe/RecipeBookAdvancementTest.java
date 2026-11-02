package com.flansmodultimate.common.recipe;

import com.flansmodultimate.common.types.*;
import com.flansmodultimate.content.ContentPack;
import com.google.gson.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.minecraft.resources.ResourceLocation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mockStatic;

class RecipeBookAdvancementTest
{
    @TempDir
    Path generatedData;

    @Test
    void anyIngredientOrExistingRecipeUnlocksTheHiddenAdvancement()
    {
        JsonObject item = JsonParser.parseString("{\"item\":\"minecraft:iron_ingot\"}").getAsJsonObject();
        JsonObject tag = JsonParser.parseString("{\"tag\":\"minecraft:planks\"}").getAsJsonObject();
        JsonObject advancement = RecipeBookAdvancement.create("flansmod:test", List.of(item, tag), RecipeDataCompatibility.Format.LEGACY);

        assertEquals("minecraft:recipes/root", advancement.get("parent").getAsString());
        assertFalse(advancement.has("display"));
        assertEquals("flansmod:test", advancement.getAsJsonObject("rewards").getAsJsonArray("recipes").get(0).getAsString());
        JsonObject criteria = advancement.getAsJsonObject("criteria");
        assertEquals("flansmod:test", criteria.getAsJsonObject("has_the_recipe").getAsJsonObject("conditions").get("recipe").getAsString());
        JsonObject itemPredicate = criteria.getAsJsonObject("has_ingredient_0").getAsJsonObject("conditions").getAsJsonArray("items").get(0).getAsJsonObject();
        assertEquals("minecraft:iron_ingot", itemPredicate.getAsJsonArray("items").get(0).getAsString());
        JsonObject tagPredicate = criteria.getAsJsonObject("has_ingredient_1").getAsJsonObject("conditions").getAsJsonArray("items").get(0).getAsJsonObject();
        assertEquals("minecraft:planks", tagPredicate.get("tag").getAsString());
        JsonArray requirements = advancement.getAsJsonArray("requirements");
        assertEquals(1, requirements.size(), "Criteria must be ORed, not require every ingredient");
        assertEquals(criteria.size(), requirements.get(0).getAsJsonArray().size());
    }

    @Test
    void modernUnlocksNameTagsInTheItemsPredicate()
    {
        JsonObject item = JsonParser.parseString("{\"item\":\"minecraft:iron_ingot\"}").getAsJsonObject();
        JsonObject tag = JsonParser.parseString("{\"tag\":\"minecraft:planks\"}").getAsJsonObject();
        JsonObject criteria = RecipeBookAdvancement.create("flansmod:test", List.of(item, tag), RecipeDataCompatibility.Format.MODERN).getAsJsonObject("criteria");
        JsonObject itemPredicate = criteria.getAsJsonObject("has_ingredient_0").getAsJsonObject("conditions").getAsJsonArray("items").get(0).getAsJsonObject();
        assertEquals("minecraft:iron_ingot", itemPredicate.getAsJsonArray("items").get(0).getAsString());
        JsonObject tagPredicate = criteria.getAsJsonObject("has_ingredient_1").getAsJsonObject("conditions").getAsJsonArray("items").get(0).getAsJsonObject();
        assertEquals("#minecraft:planks", tagPredicate.get("items").getAsString());
        assertFalse(tagPredicate.has("tag"), "1.21 ignores the tag field, which would match every item");
    }

    @Test
    void everyBuiltInRecipeHasAnUnlockWithMatchingConditions() throws Exception
    {
        for (String namespace : List.of("flansmod", "flansmodapocalypse"))
        {
            Path data = Path.of("src/main/resources/data", namespace);
            try (var recipes = Files.list(data.resolve("recipe")))
            {
                for (Path recipeFile : recipes.toList())
                {
                    JsonObject recipe = JsonParser.parseString(Files.readString(recipeFile)).getAsJsonObject();
                    Path unlockFile = data.resolve("advancement/recipes").resolve(recipeFile.getFileName());
                    JsonObject unlock = JsonParser.parseString(Files.readString(unlockFile)).getAsJsonObject();
                    String id = namespace + ":" + recipeFile.getFileName().toString().replace(".json", "");
                    assertEquals(id, unlock.getAsJsonObject("rewards").getAsJsonArray("recipes").get(0).getAsString());
                    assertEquals(recipe.get("neoforge:conditions"), unlock.get("neoforge:conditions"));
                    assertFalse(unlock.has("conditions"), "NeoForge 1.21 only reads neoforge:conditions");
                }
            }
        }
    }

    @Test
    void generatedUnlocksFollowRecipeFormatChangesAndInvalidateIncompleteCaches() throws Exception
    {
        try (var resolver = mockStatic(RecipeResolver.class))
        {
            resolver.when(() -> RecipeResolver.resolveItemId(anyString(), any())).thenReturn(Optional.of(ResourceLocation.fromNamespaceAndPath("minecraft", "iron_ingot")));
            ToolType shaped = tool(List.of("Recipe I minecraft:iron_ingot", "III", " I ", "III", "SmeltableFrom minecraft:iron_ingot"));
            RecipeJsonGenerator.writeRecipes(shaped, generatedData);
            for (String folder : List.of("advancements", "advancement"))
            {
                assertTrue(Files.isRegularFile(generatedData.resolve(folder + "/recipes/example_shaped.json")));
                assertTrue(Files.isRegularFile(generatedData.resolve(folder + "/recipes/example_smelting.json")));
            }
            assertFalse(RecipeJsonGenerator.needsRegeneration(List.of(shaped), generatedData));

            ToolType shapeless = tool(List.of("ShapelessRecipe minecraft:iron_ingot"));
            RecipeJsonGenerator.writeRecipes(shapeless, generatedData);
            for (String folder : List.of("advancements", "advancement"))
            {
                assertFalse(Files.exists(generatedData.resolve(folder + "/recipes/example_shaped.json")));
                assertFalse(Files.exists(generatedData.resolve(folder + "/recipes/example_smelting.json")));
                assertTrue(Files.isRegularFile(generatedData.resolve(folder + "/recipes/example_shapeless.json")));
            }
            assertFalse(RecipeJsonGenerator.needsRegeneration(List.of(shapeless), generatedData));
            Files.delete(generatedData.resolve("advancements/recipes/example_shapeless.json"));
            assertTrue(RecipeJsonGenerator.needsRegeneration(List.of(shapeless), generatedData));
        }
    }

    private ToolType tool(List<String> lines)
    {
        ToolType type = new ToolType()
        {
            @Override
            public String getShortName()
            {
                return "example";
            }
        };
        type.load(new TypeFile("example", EnumType.TOOL, new ContentPack("test", generatedData), lines));
        return type;
    }
}
