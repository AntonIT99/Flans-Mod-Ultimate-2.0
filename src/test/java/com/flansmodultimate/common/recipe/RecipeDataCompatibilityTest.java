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

class RecipeDataCompatibilityTest
{
    @TempDir
    Path dataRoot;

    @Test
    void fillsMissingCounterpartsInBothDirectionsAcrossNamespaces() throws Exception
    {
        Path oldRecipe = dataRoot.resolve("flansmod/recipes/nested/crafting.json");
        Path newRecipe = dataRoot.resolve("other/recipe/smelting.json");
        Files.createDirectories(oldRecipe.getParent());
        Files.createDirectories(newRecipe.getParent());
        Files.writeString(oldRecipe, "{\"type\":\"minecraft:crafting_shaped\",\"result\":{\"item\":\"flansmod:test\",\"count\":8}}");
        Files.writeString(newRecipe, "{\"type\":\"minecraft:smelting\",\"result\":{\"id\":\"minecraft:iron_ingot\"}}");
        Path existing = dataRoot.resolve("flansmod/recipe/keep.json");
        Files.writeString(dataRoot.resolve("flansmod/recipes/keep.json"), "{\"type\":\"minecraft:crafting_shapeless\",\"result\":{\"item\":\"flansmod:old\"}}");
        Files.createDirectories(existing.getParent());
        Files.writeString(existing, "{\"type\":\"minecraft:crafting_shapeless\",\"result\":{\"id\":\"flansmod:new\"}}");

        assertTrue(RecipeDataCompatibility.hasMissingCounterparts(dataRoot));
        RecipeDataCompatibility.fillMissingCounterparts(dataRoot);

        JsonObject modern = JsonParser.parseString(Files.readString(dataRoot.resolve("flansmod/recipe/nested/crafting.json")))
            .getAsJsonObject().getAsJsonObject("result");
        assertEquals("flansmod:test", modern.get("id").getAsString());
        assertEquals(8, modern.get("count").getAsInt());
        assertFalse(modern.has("item"));
        JsonObject legacy = JsonParser.parseString(Files.readString(dataRoot.resolve("other/recipes/smelting.json")))
            .getAsJsonObject();
        assertEquals("minecraft:iron_ingot", legacy.get("result").getAsString());
        assertTrue(Files.readString(existing).contains("flansmod:new"));
        assertTrue(Files.readString(oldRecipe).contains("\"item\""));
        assertFalse(RecipeDataCompatibility.hasMissingCounterparts(dataRoot));
    }

    @Test
    void generatedRecipeKeepsLegacyCountAndCapsModernCount() throws Exception
    {
        ToolType tool = new ToolType();
        tool.load(new TypeFile("example", EnumType.TOOL, new ContentPack("test", dataRoot), List.of(
            "ShortName example", "RecipeOutput 16", "ShapelessRecipe minecraft:wheat")));
        JsonObject recipe = JsonParser.parseString("{\"type\":\"minecraft:crafting_shapeless\",\"result\":{\"item\":\"flansmod:example\",\"count\":16}}")
            .getAsJsonObject();

        JsonObject legacy = RecipeDataCompatibility.formatGenerated(recipe, tool, RecipeDataCompatibility.Format.LEGACY);
        JsonObject modern = RecipeDataCompatibility.formatGenerated(recipe, tool, RecipeDataCompatibility.Format.MODERN);
        assertEquals(16, legacy.getAsJsonObject("result").get("count").getAsInt());
        assertEquals("flansmod:example", modern.getAsJsonObject("result").get("id").getAsString());
        assertFalse(modern.getAsJsonObject("result").has("count"));

    }
}
