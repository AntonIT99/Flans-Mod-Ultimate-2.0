package com.flansmodultimate.common.recipe;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.util.FileUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RecipeJsonGenerator
{
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String SHAPED_SUFFIX = "_shaped";
    private static final String SHAPELESS_SUFFIX = "_shapeless";
    private static final String SMELTING_SUFFIX = "_smelting";

    public static Set<String> getRecipeFileNames(InfoType config)
    {
        Set<String> fileNames = new HashSet<>();
        if (config.hasCraftingRecipe())
            fileNames.add(getCraftingFileName(config));
        if (config.hasSmeltingRecipe())
            fileNames.add(getSmeltingFileName(config));
        return fileNames;
    }

    public static void writeRecipes(InfoType config, Path outputFolder)
    {
        if (!config.getType().isHasItem() || (!config.hasCraftingRecipe() && !config.hasSmeltingRecipe()))
            return;

        if (!FileUtils.tryCreateDirectories(outputFolder))
            return;

        deleteGeneratedRecipes(config, outputFolder);

        if (config.hasCraftingRecipe())
        {
            Optional<JsonObject> recipe = config.isShapeless() ? createShapelessRecipe(config) : createShapedRecipe(config);
            recipe.ifPresent(json -> writeRecipe(outputFolder.resolve(getCraftingFileName(config)), json));
        }

        if (config.hasSmeltingRecipe())
            createSmeltingRecipe(config).ifPresent(json -> writeRecipe(outputFolder.resolve(getSmeltingFileName(config)), json));
    }

    private static Optional<JsonObject> createShapedRecipe(InfoType config)
    {
        Optional<List<String>> pattern = getTrimmedPattern(config);
        if (pattern.isEmpty())
        {
            FlansMod.log.warn("Invalid recipe grid in {}", config);
            return Optional.empty();
        }

        Map<Character, String> recipeKeys = parseShapedRecipeKeys(config);
        Set<Character> usedKeys = getUsedRecipeKeys(pattern.get());
        JsonObject keyJson = new JsonObject();

        for (char c : usedKeys)
        {
            String itemToken = recipeKeys.get(c);
            if (StringUtils.isBlank(itemToken))
            {
                FlansMod.log.warn("Failed to find '{}' in recipe for {}", c, config);
                return Optional.empty();
            }

            Optional<JsonObject> ingredient = createIngredient(itemToken, config);
            if (ingredient.isEmpty())
                return Optional.empty();

            keyJson.add(String.valueOf(c), ingredient.get());
        }

        JsonObject root = new JsonObject();
        root.addProperty("type", "minecraft:crafting_shaped");

        JsonArray patternJson = new JsonArray();
        pattern.get().forEach(patternJson::add);
        root.add("pattern", patternJson);
        root.add("key", keyJson);
        root.add("result", createResult(config, true));
        return Optional.of(root);
    }

    private static Optional<JsonObject> createShapelessRecipe(InfoType config)
    {
        JsonArray ingredients = new JsonArray();
        for (String itemToken : config.getRecipeTokens())
        {
            Optional<JsonObject> ingredient = createIngredient(itemToken, config);
            if (ingredient.isEmpty())
                return Optional.empty();
            ingredients.add(ingredient.get());
        }

        JsonObject root = new JsonObject();
        root.addProperty("type", "minecraft:crafting_shapeless");
        root.add("ingredients", ingredients);
        root.add("result", createResult(config, true));
        return Optional.of(root);
    }

    private static Optional<JsonObject> createSmeltingRecipe(InfoType config)
    {
        Optional<JsonObject> ingredient = createIngredient(config.getSmeltableFrom(), config);
        if (ingredient.isEmpty())
            return Optional.empty();

        JsonObject root = new JsonObject();
        root.addProperty("type", "minecraft:smelting");
        root.add("ingredient", ingredient.get());
        root.addProperty("result", FlansMod.FLANSMOD_ID + ":" + config.getShortName());
        root.addProperty("experience", 0.0F);
        root.addProperty("cookingtime", 200);
        return Optional.of(root);
    }

    private static Optional<JsonObject> createIngredient(String itemToken, InfoType config)
    {
        Optional<ResourceLocation> itemId = RecipeResolver.resolveItemId(itemToken, config.getContentPack());
        if (itemId.isEmpty())
        {
            FlansMod.log.warn("Could not find item '{}' for recipe in {}", itemToken, config);
            return Optional.empty();
        }

        JsonObject ingredient = new JsonObject();
        ingredient.addProperty("item", itemId.get().toString());
        return Optional.of(ingredient);
    }

    private static JsonObject createResult(InfoType config, boolean includeCount)
    {
        JsonObject result = new JsonObject();
        result.addProperty("item", FlansMod.FLANSMOD_ID + ":" + config.getShortName());
        if (includeCount && config.getRecipeOutput() != 1)
            result.addProperty("count", config.getRecipeOutput());
        return result;
    }

    private static Map<Character, String> parseShapedRecipeKeys(InfoType config)
    {
        List<String> tokens = config.getRecipeTokens();
        if (tokens.size() % 2 != 0)
            FlansMod.log.warn("Ignoring trailing recipe token '{}' in {}", tokens.get(tokens.size() - 1), config);

        Map<Character, String> keys = new HashMap<>();
        for (int i = 0; i + 1 < tokens.size(); i += 2)
        {
            if (StringUtils.isNotEmpty(tokens.get(i)))
                keys.put(tokens.get(i).charAt(0), tokens.get(i + 1));
        }
        return keys;
    }

    private static Optional<List<String>> getTrimmedPattern(InfoType config)
    {
        char[][] grid = config.getRecipeGrid();
        int minRow = 3;
        int minColumn = 3;
        int maxRow = -1;
        int maxColumn = -1;

        for (int row = 0; row < 3; row++)
        {
            for (int column = 0; column < 3; column++)
            {
                if (grid[row][column] != ' ')
                {
                    minRow = Math.min(minRow, row);
                    minColumn = Math.min(minColumn, column);
                    maxRow = Math.max(maxRow, row);
                    maxColumn = Math.max(maxColumn, column);
                }
            }
        }

        if (maxRow < minRow || maxColumn < minColumn)
            return Optional.empty();

        List<String> pattern = new ArrayList<>();
        for (int row = minRow; row <= maxRow; row++)
        {
            StringBuilder builder = new StringBuilder();
            for (int column = minColumn; column <= maxColumn; column++)
                builder.append(grid[row][column]);
            pattern.add(builder.toString());
        }
        return Optional.of(pattern);
    }

    private static Set<Character> getUsedRecipeKeys(List<String> pattern)
    {
        Set<Character> usedKeys = new HashSet<>();
        for (String row : pattern)
        {
            for (int i = 0; i < row.length(); i++)
            {
                char c = row.charAt(i);
                if (c != ' ')
                    usedKeys.add(c);
            }
        }
        return usedKeys;
    }

    private static void deleteGeneratedRecipes(InfoType config, Path outputFolder)
    {
        deleteRecipe(outputFolder.resolve(config.getShortName() + SHAPED_SUFFIX + ".json"));
        deleteRecipe(outputFolder.resolve(config.getShortName() + SHAPELESS_SUFFIX + ".json"));
        deleteRecipe(outputFolder.resolve(config.getShortName() + SMELTING_SUFFIX + ".json"));

        if (!config.getShortName().equals(config.getOriginalShortName()))
        {
            deleteRecipe(outputFolder.resolve(config.getOriginalShortName() + SHAPED_SUFFIX + ".json"));
            deleteRecipe(outputFolder.resolve(config.getOriginalShortName() + SHAPELESS_SUFFIX + ".json"));
            deleteRecipe(outputFolder.resolve(config.getOriginalShortName() + SMELTING_SUFFIX + ".json"));
        }
    }

    private static void deleteRecipe(Path recipeFile)
    {
        FileUtils.deleteIfExists(recipeFile);
    }

    private static void writeRecipe(Path recipeFile, JsonObject json)
    {
        try
        {
            Files.writeString(recipeFile, GSON.toJson(json), StandardCharsets.UTF_8);
        }
        catch (IOException e)
        {
            FlansMod.log.error("Could not create {}", recipeFile, e);
        }
    }

    private static String getCraftingFileName(InfoType config)
    {
        return config.getShortName() + (config.isShapeless() ? SHAPELESS_SUFFIX : SHAPED_SUFFIX) + ".json";
    }

    private static String getSmeltingFileName(InfoType config)
    {
        return config.getShortName() + SMELTING_SUFFIX + ".json";
    }
}
