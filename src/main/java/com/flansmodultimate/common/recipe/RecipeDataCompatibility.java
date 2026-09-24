package com.flansmodultimate.common.recipe;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.common.types.PartType;
import com.flansmodultimate.common.types.ShootableType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/** Keeps the two Minecraft recipe data layouts available in ordinary content packs. */
public final class RecipeDataCompatibility
{
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public enum Format
    {
        LEGACY("recipes"), MODERN("recipe");

        private final String folder;

        Format(String folder)
        {
            this.folder = folder;
        }

        public Path resolve(Path namespaceDataPath)
        {
            return namespaceDataPath.resolve(folder);
        }
    }

    private RecipeDataCompatibility()
    {
    }

    /** Inspect every namespace, not just the one used by Flan's generated recipes. */
    public static boolean hasMissingCounterparts(Path dataRoot)
    {
        for (Path namespace : namespaces(dataRoot))
        {
            if (hasMissing(namespace, Format.LEGACY, Format.MODERN)
                || hasMissing(namespace, Format.MODERN, Format.LEGACY))
                return true;
        }
        return false;
    }

    public static void fillMissingCounterparts(Path dataRoot)
    {
        for (Path namespace : namespaces(dataRoot))
        {
            fillMissing(namespace, Format.LEGACY, Format.MODERN);
            fillMissing(namespace, Format.MODERN, Format.LEGACY);
        }
    }

    private static List<Path> namespaces(Path dataRoot)
    {
        if (!Files.isDirectory(dataRoot))
            return List.of();
        try (Stream<Path> paths = Files.list(dataRoot))
        {
            return paths.filter(Files::isDirectory).sorted().toList();
        }
        catch (IOException e)
        {
            FlansMod.log.warn("Could not inspect content-pack data in {}", dataRoot, e);
            return List.of();
        }
    }

    private static List<Path> recipeFiles(Path folder)
    {
        if (!Files.isDirectory(folder))
            return List.of();
        try (Stream<Path> paths = Files.walk(folder))
        {
            return paths.filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(".json"))
                .sorted().toList();
        }
        catch (IOException e)
        {
            FlansMod.log.warn("Could not inspect recipes in {}", folder, e);
            return List.of();
        }
    }

    private static boolean hasMissing(Path namespace, Format sourceFormat, Format targetFormat)
    {
        Path sourceFolder = sourceFormat.resolve(namespace);
        Path targetFolder = targetFormat.resolve(namespace);
        for (Path source : recipeFiles(sourceFolder))
        {
            if (!Files.exists(targetFolder.resolve(sourceFolder.relativize(source)))
                && readConverted(source, targetFormat).isPresent())
                return true;
        }
        return false;
    }

    private static void fillMissing(Path namespace, Format sourceFormat, Format targetFormat)
    {
        Path sourceFolder = sourceFormat.resolve(namespace);
        Path targetFolder = targetFormat.resolve(namespace);
        for (Path source : recipeFiles(sourceFolder))
        {
            Path target = targetFolder.resolve(sourceFolder.relativize(source));
            if (Files.exists(target))
                continue;
            Optional<JsonObject> converted = readConverted(source, targetFormat);
            if (converted.isEmpty())
                continue;
            try
            {
                Files.createDirectories(target.getParent());
                Files.writeString(target, GSON.toJson(converted.get()), StandardCharsets.UTF_8);
            }
            catch (IOException e)
            {
                FlansMod.log.warn("Could not create recipe counterpart {}", target, e);
            }
        }
    }

    private static Optional<JsonObject> readConverted(Path source, Format format)
    {
        try
        {
            JsonElement parsed = JsonParser.parseString(Files.readString(source, StandardCharsets.UTF_8));
            return parsed.isJsonObject() ? convert(parsed.getAsJsonObject(), format) : Optional.empty();
        }
        catch (RuntimeException | IOException e)
        {
            FlansMod.log.warn("Could not parse recipe {}", source, e);
            return Optional.empty();
        }
    }

    /** Converts the vanilla recipe result fields that changed in Minecraft 1.21. */
    public static Optional<JsonObject> convert(JsonObject source, Format format)
    {
        if (!source.has("type") || !source.get("type").isJsonPrimitive()
            || !source.getAsJsonPrimitive("type").isString())
            return Optional.empty();
        String type = source.get("type").getAsString();
        JsonObject result = source.deepCopy();
        JsonElement output = result.get("result");
        if (type.equals("minecraft:crafting_shaped") || type.equals("minecraft:crafting_shapeless"))
        {
            if (output == null || !output.isJsonObject())
                return Optional.empty();
            JsonObject item = output.getAsJsonObject();
            String wanted = format == Format.MODERN ? "id" : "item";
            String previous = format == Format.MODERN ? "item" : "id";
            if (item.has(wanted) && item.has(previous) && !item.get(wanted).equals(item.get(previous)))
                return Optional.empty();
            if (!item.has(wanted) && item.has(previous))
                item.add(wanted, item.remove(previous));
            item.remove(previous);
            return item.has(wanted) && item.get(wanted).isJsonPrimitive()
                && item.getAsJsonPrimitive(wanted).isString() ? Optional.of(result) : Optional.empty();
        }
        if (type.equals("minecraft:smelting") || type.equals("minecraft:blasting")
            || type.equals("minecraft:smoking") || type.equals("minecraft:campfire_cooking")
            || type.equals("minecraft:stonecutting"))
        {
            if (format == Format.MODERN && output != null && output.isJsonPrimitive()
                && output.getAsJsonPrimitive().isString())
            {
                JsonObject item = new JsonObject();
                item.add("id", output.deepCopy());
                result.add("result", item);
                return Optional.of(result);
            }
            if (format == Format.LEGACY && output != null && output.isJsonObject())
            {
                JsonObject item = output.getAsJsonObject();
                if (item.size() != 1 || !item.has("id") || !item.get("id").isJsonPrimitive()
                    || !item.getAsJsonPrimitive("id").isString())
                    return Optional.empty();
                result.add("result", item.get("id").deepCopy());
                return Optional.of(result);
            }
            if (format == Format.MODERN)
                return output != null && output.isJsonObject() && output.getAsJsonObject().has("id")
                    ? Optional.of(result) : Optional.empty();
            return output != null && output.isJsonPrimitive() && output.getAsJsonPrimitive().isString()
                ? Optional.of(result) : Optional.empty();
        }
        return Optional.empty();
    }

    /** Generated .txt recipes carry the requested 1.20.1 count and the legal 1.21.1 count. */
    public static JsonObject formatGenerated(JsonObject recipe, InfoType config, Format format)
    {
        JsonObject result = convert(recipe, format).orElseGet(recipe::deepCopy);
        String type = result.has("type") ? result.get("type").getAsString() : "";
        if ((type.equals("minecraft:crafting_shaped") || type.equals("minecraft:crafting_shapeless"))
            && result.has("result") && result.get("result").isJsonObject())
        {
            int count = config.getRecipeOutput();
            if (format == Format.MODERN)
                count = Math.min(Math.max(1, count), maxRecipeStackSize(config));
            JsonObject output = result.getAsJsonObject("result");
            if (count == 1)
                output.remove("count");
            else
                output.addProperty("count", count);
        }
        return result;
    }

    public static int maxRecipeStackSize(InfoType config)
    {
        if (config instanceof ShootableType shootable)
            return Math.max(1, shootable.getMaxStackSize());
        if (config instanceof PartType part)
            return part.getCategory() == PartType.Category.FUEL && part.getFuel() > 0
                ? 1 : Math.max(1, part.getStackSize());
        return config.getType().isHasBlock() ? 64 : 1;
    }
}
