package com.flansmodultimate.common.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

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
        Path namespace = dataFolder.resolve("flansmod");
        Path oldFolder = Files.createDirectories(namespace.resolve("recipes"));
        Path crafting = oldFolder.resolve("example_shaped.json");
        Path smelting = oldFolder.resolve("example_smelting.json");
        Files.writeString(crafting, "{\"type\":\"minecraft:crafting_shaped\",\"result\":{\"item\":\"flansmod:example\",\"count\":3}}");
        Files.writeString(smelting, "{\"type\":\"minecraft:smelting\",\"result\":\"flansmod:example\"}");

        assertTrue(RecipeDataCompatibility.hasMissingCounterparts(dataFolder));
        RecipeDataCompatibility.fillMissingCounterparts(dataFolder);

        JsonObject craftingResult = JsonParser.parseString(Files.readString(namespace.resolve("recipe/example_shaped.json")))
            .getAsJsonObject().getAsJsonObject("result");
        assertEquals("flansmod:example", craftingResult.get("id").getAsString());
        assertEquals(3, craftingResult.get("count").getAsInt());
        assertFalse(craftingResult.has("item"));
        JsonObject smeltingResult = JsonParser.parseString(Files.readString(namespace.resolve("recipe/example_smelting.json")))
            .getAsJsonObject().getAsJsonObject("result");
        assertEquals("flansmod:example", smeltingResult.get("id").getAsString());
        assertFalse(RecipeDataCompatibility.hasMissingCounterparts(dataFolder));
        assertTrue(Files.readString(crafting).contains("\"item\""));
    }

}
