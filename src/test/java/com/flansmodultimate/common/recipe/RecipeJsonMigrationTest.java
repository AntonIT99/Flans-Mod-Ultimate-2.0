package com.flansmodultimate.common.recipe;

import com.flansmodultimate.ContentPack;
import com.flansmodultimate.common.types.EnumType;
import com.flansmodultimate.common.types.ToolType;
import com.flansmodultimate.common.types.TypeFile;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeJsonMigrationTest
{
    @TempDir
    Path dataFolder;

    @Test
    void migratesOldCraftingAndSmeltingOutputsWithoutChangingSource() throws Exception
    {
        Path oldFolder = Files.createDirectories(dataFolder.resolve("recipes"));
        Path crafting = oldFolder.resolve("example_shaped.json");
        Path smelting = oldFolder.resolve("example_smelting.json");
        Files.writeString(crafting, "{\"type\":\"minecraft:crafting_shaped\",\"result\":{\"item\":\"flansmod:example\",\"count\":3}}");
        Files.writeString(smelting, "{\"type\":\"minecraft:smelting\",\"result\":\"flansmod:example\"}");

        assertTrue(RecipeJsonGenerator.hasUnmigratedLegacyRecipes(dataFolder));
        RecipeJsonGenerator.migrateLegacyRecipes(dataFolder);

        JsonObject craftingResult = JsonParser.parseString(Files.readString(dataFolder.resolve("recipe/example_shaped.json")))
            .getAsJsonObject().getAsJsonObject("result");
        assertEquals("flansmod:example", craftingResult.get("id").getAsString());
        assertEquals(3, craftingResult.get("count").getAsInt());
        assertFalse(craftingResult.has("item"));
        JsonObject smeltingResult = JsonParser.parseString(Files.readString(dataFolder.resolve("recipe/example_smelting.json")))
            .getAsJsonObject().getAsJsonObject("result");
        assertEquals("flansmod:example", smeltingResult.get("id").getAsString());
        assertFalse(RecipeJsonGenerator.hasUnmigratedLegacyRecipes(dataFolder));
        assertTrue(Files.readString(crafting).contains("\"item\""));
    }

    @Test
    void detectsPreprocessedRecipeWhoseOutputExceedsToolStackLimit() throws Exception
    {
        ToolType tool = new ToolType()
        {
            @Override
            public String getShortName()
            {
                return "hardtack";
            }
        };
        tool.load(new TypeFile("hardtack", EnumType.TOOL, new ContentPack("test", dataFolder), List.of(
            "ShortName hardtack", "RecipeOutput 16", "ShapelessRecipe minecraft:wheat")));
        Path recipeFolder = Files.createDirectories(dataFolder.resolve("recipe"));
        Path recipeFile = recipeFolder.resolve("hardtack_shapeless.json");
        Files.writeString(recipeFile, "{\"type\":\"minecraft:crafting_shapeless\",\"result\":{\"id\":\"flansmod:hardtack\",\"count\":16}}");

        assertEquals(1, RecipeJsonGenerator.maxRecipeStackSize(tool));
        assertTrue(RecipeJsonGenerator.hasOversizedGeneratedRecipeOutputs(List.of(tool), dataFolder));
        Files.writeString(recipeFile, "{\"type\":\"minecraft:crafting_shapeless\",\"result\":{\"id\":\"flansmod:hardtack\"}}");
        assertFalse(RecipeJsonGenerator.hasOversizedGeneratedRecipeOutputs(List.of(tool), dataFolder));
    }
}
