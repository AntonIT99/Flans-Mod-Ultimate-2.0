package com.flansmodultimate.common.recipe;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.common.types.PartType;
import com.flansmodultimate.common.types.ShootableType;
import com.flansmodultimate.util.FileUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import java.util.stream.Stream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RecipeJsonGenerator
{
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String SHAPED_SUFFIX = "_shaped";
    private static final String SHAPELESS_SUFFIX = "_shapeless";
    private static final String SMELTING_SUFFIX = "_smelting";

    /** Copies 1.20.1 recipe data into the 1.21.1 directory without changing the old pack files. */
    public static void migrateLegacyRecipes(Path dataFolder)
    {
        Path oldFolder = dataFolder.resolve("recipes");
        if (!Files.isDirectory(oldFolder))
            return;

        try (Stream<Path> paths = Files.walk(oldFolder))
        {
            for (Path oldFile : paths.filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".json")).toList())
            {
                Path newFile = dataFolder.resolve("recipe").resolve(oldFolder.relativize(oldFile));
                if (Files.exists(newFile))
                    continue;

                try
                {
                    JsonElement parsed = JsonParser.parseString(Files.readString(oldFile, StandardCharsets.UTF_8));
                    if (!parsed.isJsonObject())
                        continue;
                    JsonObject recipe = parsed.getAsJsonObject();
                    String type = recipe.has("type") ? recipe.get("type").getAsString() : "";
                    JsonElement result = recipe.get("result");
                    if ((type.equals("minecraft:smelting") || type.equals("minecraft:blasting")
                        || type.equals("minecraft:smoking") || type.equals("minecraft:campfire_cooking"))
                        && result != null && result.isJsonPrimitive() && result.getAsJsonPrimitive().isString())
                    {
                        JsonObject migrated = new JsonObject();
                        migrated.add("id", result.deepCopy());
                        recipe.add("result", migrated);
                    }
                    else if ((type.equals("minecraft:crafting_shaped") || type.equals("minecraft:crafting_shapeless"))
                        && result != null && result.isJsonObject())
                    {
                        JsonObject output = result.getAsJsonObject();
                        if (output.has("item") && !output.has("id"))
                        {
                            output.add("id", output.remove("item"));
                        }
                    }

                    Files.createDirectories(newFile.getParent());
                    Files.writeString(newFile, GSON.toJson(recipe), StandardCharsets.UTF_8);
                }
                catch (RuntimeException | IOException e)
                {
                    FlansMod.log.warn("Could not migrate legacy recipe {}", oldFile, e);
                }
            }
        }
        catch (IOException e)
        {
            FlansMod.log.warn("Could not inspect legacy recipes in {}", oldFolder, e);
        }
    }

    public static boolean hasUnmigratedLegacyRecipes(Path dataFolder)
    {
        Path oldFolder = dataFolder.resolve("recipes");
        if (!Files.isDirectory(oldFolder))
            return false;
        try (Stream<Path> paths = Files.walk(oldFolder))
        {
            return paths.filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".json"))
                .anyMatch(path -> !Files.exists(dataFolder.resolve("recipe").resolve(oldFolder.relativize(path))));
        }
        catch (IOException e)
        {
            FlansMod.log.warn("Could not inspect legacy recipes in {}", oldFolder, e);
            return false;
        }
    }

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
            JsonObject recipe = config.isShapeless() ? createShapelessRecipe(config) : createShapedRecipe(config);
            writeRecipe(outputFolder.resolve(getCraftingFileName(config)), recipe);
        }

        if (config.hasSmeltingRecipe())
        {
            JsonObject recipe = createSmeltingRecipe(config);
            writeRecipe(outputFolder.resolve(getSmeltingFileName(config)), recipe);
        }
    }

    private static JsonObject createShapedRecipe(InfoType config)
    {
        Optional<List<String>> pattern = getTrimmedPattern(config);
        if (pattern.isEmpty())
        {
            FlansMod.log.warn("Invalid recipe grid in {}", config);
            return new JsonObject();
        }

        Map<Character, String> recipeKeys = parseShapedRecipeKeys(config);
        pattern = removeUndefinedRecipeKeys(pattern.get(), recipeKeys, config);
        if (pattern.isEmpty())
        {
            FlansMod.log.warn("Invalid recipe grid in {}", config);
            return new JsonObject();
        }

        Set<Character> usedKeys = getUsedRecipeKeys(pattern.get());
        JsonObject keyJson = new JsonObject();

        for (char c : usedKeys)
        {
            String itemToken = recipeKeys.get(c);
            keyJson.add(String.valueOf(c), createIngredient(itemToken, config));
        }

        JsonObject root = new JsonObject();
        root.addProperty("type", "minecraft:crafting_shaped");

        JsonArray patternJson = new JsonArray();
        pattern.get().forEach(patternJson::add);

        root.add("pattern", patternJson);
        root.add("key", keyJson);
        root.add("result", createResult(config));

        return root;
    }

    private static Optional<List<String>> removeUndefinedRecipeKeys(List<String> pattern, Map<Character, String> recipeKeys, InfoType config)
    {
        List<String> sanitizedPattern = new ArrayList<>(pattern.size());
        Set<Character> warnedKeys = new HashSet<>();

        for (String row : pattern)
        {
            StringBuilder builder = new StringBuilder(row.length());
            for (int i = 0; i < row.length(); i++)
            {
                char c = row.charAt(i);
                if (c != ' ' && StringUtils.isBlank(recipeKeys.get(c)))
                {
                    if (warnedKeys.add(c))
                        FlansMod.log.warn("Failed to find '{}' in recipe for {}", c, config);
                    builder.append(' ');
                }
                else
                    builder.append(c);
            }
            sanitizedPattern.add(builder.toString());
        }

        return trimPattern(sanitizedPattern);
    }

    private static JsonObject createShapelessRecipe(InfoType config)
    {
        JsonArray ingredients = new JsonArray();

        for (String itemToken : config.getRecipeTokens())
            ingredients.add(createIngredient(itemToken, config));

        JsonObject root = new JsonObject();
        root.addProperty("type", "minecraft:crafting_shapeless");
        root.add("ingredients", ingredients);
        root.add("result", createResult(config));

        return root;
    }

    private static JsonObject createSmeltingRecipe(InfoType config)
    {
        JsonObject root = new JsonObject();

        root.addProperty("type", "minecraft:smelting");
        root.add("ingredient", createIngredient(config.getSmeltableFrom(), config));
        JsonObject result = new JsonObject();
        result.addProperty("id", FlansMod.FLANSMOD_ID + ":" + config.getShortName());
        root.add("result", result);
        root.addProperty("experience", 0.0F);
        root.addProperty("cookingtime", 200);

        return root;
    }

    private static JsonObject createIngredient(String itemToken, InfoType config)
    {
        Optional<ResourceLocation> itemId = RecipeResolver.resolveItemId(itemToken, config.getContentPack());
        if (itemId.isEmpty())
        {
            ResourceLocation fallbackItemId = RecipeResolver.createFallbackItemId(itemToken);
            itemId = Optional.of(fallbackItemId);
        }

        JsonObject ingredient = new JsonObject();
        ingredient.addProperty("item", itemId.get().toString());
        return ingredient;
    }

    private static JsonObject createResult(InfoType config)
    {
        JsonObject result = new JsonObject();
        result.addProperty("id", FlansMod.FLANSMOD_ID + ":" + config.getShortName());
        int count = Math.max(1, config.getRecipeOutput());
        int maxStackSize = Integer.MAX_VALUE;
        if (config instanceof ShootableType shootable)
            maxStackSize = Math.max(1, shootable.getMaxStackSize());
        else if (config instanceof PartType part)
            maxStackSize = part.getCategory() == PartType.Category.FUEL && part.getFuel() > 0
                ? 1 : part.getStackSize();

        if (count > maxStackSize)
        {
            FlansMod.log.warn("Recipe for {} requests {} items, but the output stacks to at most {}; limiting the result",
                config, count, maxStackSize);
            count = maxStackSize;
        }
        if (count != 1)
            result.addProperty("count", count);
        return result;
    }

    private static Map<Character, String> parseShapedRecipeKeys(InfoType config)
    {
        List<String> tokens = config.getRecipeTokens();
        if (tokens.size() % 2 != 0) {
            String token = tokens.get(tokens.size() - 1);
            FlansMod.log.warn("Ignoring trailing recipe token '{}' in {}", token, config);
        }


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
        List<String> recipeRows = new ArrayList<>(3);
        char[][] grid = config.getRecipeGrid();
        for (int row = 0; row < 3; row++)
            recipeRows.add(new String(grid[row]));
        return trimPattern(recipeRows);
    }

    private static Optional<List<String>> trimPattern(List<String> recipeRows)
    {
        int minRow = recipeRows.size();
        int minColumn = recipeRows.stream().mapToInt(String::length).max().orElse(0);
        int maxRow = -1;
        int maxColumn = -1;

        for (int row = 0; row < recipeRows.size(); row++)
        {
            String recipeRow = recipeRows.get(row);
            for (int column = 0; column < recipeRow.length(); column++)
            {
                if (recipeRow.charAt(column) != ' ')
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
            String recipeRow = recipeRows.get(row);
            StringBuilder builder = new StringBuilder();
            for (int column = minColumn; column <= maxColumn; column++)
                builder.append(column < recipeRow.length() ? recipeRow.charAt(column) : ' ');
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
        deleteRecipe(outputFolder.resolve(config.getShortName() + SHAPED_SUFFIX + FileUtils.JSON_EXTENSION));
        deleteRecipe(outputFolder.resolve(config.getShortName() + SHAPELESS_SUFFIX + FileUtils.JSON_EXTENSION));
        deleteRecipe(outputFolder.resolve(config.getShortName() + SMELTING_SUFFIX + FileUtils.JSON_EXTENSION));

        if (!config.getShortName().equals(config.getOriginalShortName()))
        {
            deleteRecipe(outputFolder.resolve(config.getOriginalShortName() + SHAPED_SUFFIX + FileUtils.JSON_EXTENSION));
            deleteRecipe(outputFolder.resolve(config.getOriginalShortName() + SHAPELESS_SUFFIX + FileUtils.JSON_EXTENSION));
            deleteRecipe(outputFolder.resolve(config.getOriginalShortName() + SMELTING_SUFFIX + FileUtils.JSON_EXTENSION));
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
        return config.getShortName() + (config.isShapeless() ? SHAPELESS_SUFFIX : SHAPED_SUFFIX) + FileUtils.JSON_EXTENSION;
    }

    private static String getSmeltingFileName(InfoType config)
    {
        return config.getShortName() + SMELTING_SUFFIX + FileUtils.JSON_EXTENSION;
    }
}
