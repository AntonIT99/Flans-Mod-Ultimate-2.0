package com.flansmodultimate.common.recipe;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.util.FileUtils;
import com.flansmodultimate.util.FlansLog;
import com.google.gson.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RecipeJsonGenerator
{
    private static final String KEY_RESULT = "result";
    private static final String KEY_COUNT = "count";
    private static final String KEY_INGREDIENT = "ingredient";
    private static final String KEY_INGREDIENTS = "ingredients";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String SHAPED_SUFFIX = "_shaped";
    private static final String SHAPELESS_SUFFIX = "_shapeless";
    private static final String SMELTING_SUFFIX = "_smelting";

    public static Set<String> getRecipeFileNames(InfoType config)
    {
        Set<String> fileNames = new HashSet<>();
        if (producesCraftingRecipe(config))
            fileNames.add(getCraftingFileName(config));
        if (config.hasSmeltingRecipe())
            fileNames.add(getSmeltingFileName(config));
        return fileNames;
    }

    /** A malformed shaped recipe has no output, so it must never count as missing or stale generation. */
    private static boolean producesCraftingRecipe(InfoType config)
    {
        if (!config.hasCraftingRecipe())
            return false;
        return config.isShapeless() || getTrimmedPattern(config).flatMap(pattern -> removeUndefinedRecipeKeys(pattern, parseShapedRecipeKeys(config), new HashSet<>())).isPresent();
    }

    /** Both generated layouts must exist, and modern crafting results must fit the output stack. */
    public static boolean needsRegeneration(List<InfoType> items, Path dataFolder)
    {
        for (InfoType config : items)
        {
            if (hasMissingOutputs(config, dataFolder) || hasInvalidModernResult(config, dataFolder))
                return true;
        }
        return false;
    }

    private static boolean hasMissingOutputs(InfoType config, Path dataFolder)
    {
        for (String fileName : getRecipeFileNames(config))
        {
            for (RecipeDataCompatibility.Format format : RecipeDataCompatibility.Format.values())
                if (!Files.isRegularFile(format.resolve(dataFolder).resolve(fileName)))
                    return true;
            for (Path folder : advancementFolders(dataFolder))
                if (!Files.isRegularFile(folder.resolve(fileName)))
                    return true;
        }
        return false;
    }

    private static boolean hasInvalidModernResult(InfoType config, Path dataFolder)
    {
        if (!producesCraftingRecipe(config))
            return false;
        Path modernRecipe = RecipeDataCompatibility.Format.MODERN.resolve(dataFolder).resolve(getCraftingFileName(config));
        try
        {
            JsonObject recipe = JsonParser.parseString(Files.readString(modernRecipe, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject result = recipe.getAsJsonObject(KEY_RESULT);
            return result == null || (result.has(KEY_COUNT) && (result.get(KEY_COUNT).getAsInt() < 1 || result.get(KEY_COUNT).getAsInt() > RecipeDataCompatibility.maxRecipeStackSize(config)));
        }
        catch (RuntimeException | IOException exception)
        {
            FlansLog.log.warn("Could not inspect generated recipe {}; regenerating it", modernRecipe, exception);
            return true;
        }
    }

    private static List<Path> advancementFolders(Path dataFolder)
    {
        return Arrays.stream(RecipeDataCompatibility.Format.values()).map(format -> advancementFolder(dataFolder, format)).toList();
    }

    /** 1.20.1 reads unlocks from {@code advancements}, 1.21 from {@code advancement}. */
    private static Path advancementFolder(Path dataFolder, RecipeDataCompatibility.Format format)
    {
        return dataFolder.resolve(format == RecipeDataCompatibility.Format.LEGACY ? "advancements" : "advancement").resolve("recipes");
    }

    public static void writeRecipes(InfoType config, Path dataFolder)
    {
        if (!config.getType().isHasItem() || (!config.hasCraftingRecipe() && !config.hasSmeltingRecipe()))
            return;

        Path legacyFolder = RecipeDataCompatibility.Format.LEGACY.resolve(dataFolder);
        Path modernFolder = RecipeDataCompatibility.Format.MODERN.resolve(dataFolder);
        if (!FileUtils.tryCreateDirectories(legacyFolder) || !FileUtils.tryCreateDirectories(modernFolder))
            return;

        deleteGeneratedRecipes(config, legacyFolder);
        deleteGeneratedRecipes(config, modernFolder);
        for (Path folder : advancementFolders(dataFolder))
            deleteGeneratedRecipes(config, folder);

        if (config.hasCraftingRecipe())
        {
            Optional<JsonObject> recipe = config.isShapeless() ? Optional.of(createShapelessRecipe(config)) : createShapedRecipe(config);
            recipe.ifPresent(json -> writeBothVersions(config, dataFolder, getCraftingFileName(config), json));
        }

        if (config.hasSmeltingRecipe())
        {
            JsonObject recipe = createSmeltingRecipe(config);
            writeBothVersions(config, dataFolder, getSmeltingFileName(config), recipe);
        }
    }

    private static void writeBothVersions(InfoType config, Path dataFolder, String fileName, JsonObject recipe)
    {
        for (RecipeDataCompatibility.Format format : RecipeDataCompatibility.Format.values())
            writeRecipe(format.resolve(dataFolder).resolve(fileName), RecipeDataCompatibility.formatGenerated(recipe, config, format));
        List<JsonObject> ingredients = new ArrayList<>();
        if (recipe.has("key"))
            recipe.getAsJsonObject("key").entrySet().forEach(entry -> ingredients.add(entry.getValue().getAsJsonObject()));
        if (recipe.has(KEY_INGREDIENTS))
            recipe.getAsJsonArray(KEY_INGREDIENTS).forEach(ingredient -> ingredients.add(ingredient.getAsJsonObject()));
        if (recipe.has(KEY_INGREDIENT))
            ingredients.add(recipe.getAsJsonObject(KEY_INGREDIENT));
        String recipeId = FlansMod.FLANSMOD_ID + ":" + fileName.substring(0, fileName.length() - FileUtils.JSON_EXTENSION.length());
        for (RecipeDataCompatibility.Format format : RecipeDataCompatibility.Format.values())
        {
            Path output = advancementFolder(dataFolder, format);
            if (FileUtils.tryCreateDirectories(output))
                writeRecipe(output.resolve(fileName), RecipeBookAdvancement.create(recipeId, ingredients, format));
        }
    }

    private static Optional<JsonObject> createShapedRecipe(InfoType config)
    {
        Optional<List<String>> pattern = getTrimmedPattern(config);
        if (pattern.isEmpty())
        {
            FlansLog.log.warn("Invalid recipe grid in {}", config);
            return Optional.empty();
        }

        List<String> tokens = config.getRecipeTokens();
        if (tokens.size() % 2 != 0)
        {
            String trailingToken = tokens.get(tokens.size() - 1);
            FlansLog.log.warn("Ignoring trailing recipe token '{}' in {}", trailingToken, config);
        }

        Map<Character, String> recipeKeys = parseShapedRecipeKeys(config);
        Set<Character> undefinedKeys = new LinkedHashSet<>();
        pattern = removeUndefinedRecipeKeys(pattern.get(), recipeKeys, undefinedKeys);
        undefinedKeys.forEach(key -> FlansLog.log.warn("Failed to find '{}' in recipe for {}", key, config));
        if (pattern.isEmpty())
        {
            FlansLog.log.warn("Invalid recipe grid in {}", config);
            return Optional.empty();
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
        root.add(KEY_RESULT, createResult(config));

        return Optional.of(root);
    }

    private static Optional<List<String>> removeUndefinedRecipeKeys(List<String> pattern, Map<Character, String> recipeKeys, Set<Character> undefinedKeys)
    {
        List<String> sanitizedPattern = new ArrayList<>(pattern.size());

        for (String row : pattern)
        {
            StringBuilder builder = new StringBuilder(row.length());
            for (int i = 0; i < row.length(); i++)
            {
                char c = row.charAt(i);
                if (c != ' ' && StringUtils.isBlank(recipeKeys.get(c)))
                {
                    undefinedKeys.add(c);
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
        root.add(KEY_INGREDIENTS, ingredients);
        root.add(KEY_RESULT, createResult(config));

        return root;
    }

    private static JsonObject createSmeltingRecipe(InfoType config)
    {
        JsonObject root = new JsonObject();

        root.addProperty("type", "minecraft:smelting");
        root.add(KEY_INGREDIENT, createIngredient(config.getSmeltableFrom(), config));
        root.addProperty(KEY_RESULT, FlansMod.FLANSMOD_ID + ":" + config.getShortName());
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
        result.addProperty("item", FlansMod.FLANSMOD_ID + ":" + config.getShortName());
        if (config.getRecipeOutput() != 1)
            result.addProperty(KEY_COUNT, config.getRecipeOutput());
        return result;
    }

    private static Map<Character, String> parseShapedRecipeKeys(InfoType config)
    {
        List<String> tokens = config.getRecipeTokens();
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
            FlansLog.log.error("Could not create {}", recipeFile, e);
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
