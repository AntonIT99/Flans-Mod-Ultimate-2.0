package com.flansmodultimate.content;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.paintjob.Paintjob;
import com.flansmodultimate.common.types.BlockType;
import com.flansmodultimate.common.types.GloveType;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.common.types.ItemHolderType;
import com.flansmodultimate.common.types.PaintableType;
import com.flansmodultimate.util.FileUtils;
import com.flansmodultimate.util.FlansLog;
import com.flansmodultimate.util.ResourceUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static com.flansmodultimate.content.ContentPackPaths.*;

/** Generates item/block JSON and preserves supplied custom model geometry and variants. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class ContentPackModels
{
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    static void generate(IContentProvider provider, List<InfoType> items, List<InfoType> blocks)
    {
        Path jsonBlockstatesFolderPath = provider.getAssetsPath().resolve(FOLDER_BLOCKSTATES);
        Path jsonModelsFolderPath = provider.getAssetsPath().resolve(FOLDER_MODELS);
        Path jsonItemModelsFolderPath = jsonModelsFolderPath.resolve(FOLDER_MODELS_ITEM);
        Path jsonBlockModelsFolderPath = jsonModelsFolderPath.resolve(FOLDER_MODELS_BLOCK);

        // Recover packs processed by the previous destructive migration before writing a new ID map.
        try (AliasFileManager aliases = new AliasFileManager(ID_ALIAS_FILE, provider))
        {
            aliases.readFile().ifPresent(previous ->
            {
                for (InfoType config : items)
                    restoreOriginal(jsonItemModelsFolderPath, config, previous);
                for (InfoType config : blocks)
                {
                    restoreOriginal(jsonBlockModelsFolderPath, config, previous);
                    restoreOriginal(jsonBlockstatesFolderPath, config, previous);
                }
            });
        }

        convertExistingJsonFiles(jsonBlockstatesFolderPath);
        convertExistingJsonFiles(jsonModelsFolderPath);

        boolean blockstatesExist = FileUtils.tryCreateDirectories(jsonBlockstatesFolderPath);
        boolean blockModelsExist = FileUtils.tryCreateDirectories(jsonBlockModelsFolderPath);
        boolean itemModelsExist = FileUtils.tryCreateDirectories(jsonItemModelsFolderPath);

        if (itemModelsExist)
        {
            for (InfoType config : items)
            {
                generateItemModelJson(config, jsonItemModelsFolderPath);
            }
        }

        if (blockstatesExist || blockModelsExist)
        {
            for (InfoType config : blocks)
            {
                if (blockstatesExist)
                    generateBlockstateJson(config, jsonBlockstatesFolderPath);
                if (blockModelsExist)
                    generateBlockModelJson(config, jsonBlockModelsFolderPath);
            }
        }
    }

    private static void convertExistingJsonFiles(Path jsonFolderPath)
    {
        if (!Files.isDirectory(jsonFolderPath))
            return;

        try (Stream<Path> walk = Files.walk(jsonFolderPath))
        {
            walk.filter(p -> Files.isRegularFile(p) && p.toString().endsWith(FileUtils.JSON_EXTENSION)).forEach(ContentPackModels::processJsonItemFile);
        }
        catch (IOException e)
        {
            FlansLog.log.error("Could not open {}", jsonFolderPath, e);
        }
    }

    private static void processJsonItemFile(Path jsonFile)
    {
        try
        {
            // 1) Rename the file itself to lowercase (safe even on case-insensitive FS)
            FileUtils.renameToLowercase(jsonFile);

            // Resource references are normalized in the view; custom JSON strings stay authored.
        }
        catch (IOException e)
        {
            FlansLog.log.error("Failed to process file: {}", jsonFile, e);
        }
    }

    private static void generateItemModelJson(InfoType config, Path outputFolder)
    {
        ResourceUtils.ModelJson model = ResourceUtils.ModelJson.createItemModel(config);
        String shortName = config.getOriginalShortName();

        Path outputFile = outputFolder.resolve(shortName + FileUtils.JSON_EXTENSION);
        writeGeneratedItemModelJson(outputFile, model, config);

        if (config instanceof PaintableType paintableType)
        {
            for (Paintjob p : paintableType.getPaintjobs().values())
            {
                if (!p.equals(paintableType.getDefaultPaintjob()))
                {
                    outputFile = outputFolder.resolve(p.getIcon() + FileUtils.JSON_EXTENSION);
                    String icon = hasItemIcon(config.getContentPack(), p.getIcon()) ? p.getIcon() : config.getIcon();
                    model = ResourceUtils.ModelJson.createItemModel(config, icon);
                    writeGeneratedItemModelJson(outputFile, model, config);
                }
            }
        }
    }

    private static boolean hasItemIcon(IContentProvider provider, String icon)
    {
        String fileName = ResourceUtils.sanitize(icon) + FileUtils.PNG_EXTENSION;
        Path textures = provider.getAssetsPath().resolve(FOLDER_TEXTURES);
        return Files.isRegularFile(textures.resolve(FOLDER_TEXTURES_ITEMS).resolve(fileName)) || Files.isRegularFile(textures.resolve(FOLDER_TEXTURES_ITEM).resolve(fileName));
    }

    private static void writeGeneratedItemModelJson(Path outputFile, ResourceUtils.ModelJson model, InfoType config)
    {
        if (shouldPreserveExistingItemModel(outputFile, config))
            return;

        FileUtils.writeString(outputFile, canonicalIds(gson.toJson(model), config));
    }

    private static boolean shouldPreserveExistingItemModel(Path modelFile, InfoType config)
    {
        if (!Files.isRegularFile(modelFile))
            return false;

        try
        {
            JsonObject model = JsonParser.parseString(Files.readString(modelFile, StandardCharsets.UTF_8)).getAsJsonObject();
            return !isGeneratedSimpleItemModel(model) && !isOutdatedGeneratedGloveModel(model, config);
        }
        catch (IOException | IllegalStateException | JsonSyntaxException e)
        {
            return false;
        }
    }

    private static boolean isGeneratedSimpleItemModel(JsonObject model)
    {
        if (model.has("elements") || model.has("display") || model.has("loader"))
            return false;
        if (!model.has("parent") || !model.get("parent").isJsonPrimitive())
            return false;

        String parent = model.get("parent").getAsString();
        return parent.equals("minecraft:item/generated") || parent.equals("minecraft:item/handheld") || parent.startsWith(FlansMod.FLANSMOD_ID + ":block/");
    }

    /**
     * Earlier versions replaced the model shipped by the content pack with a hardcoded glove shape whose UVs were
     * mapped onto the flat item icon, which rendered as garbage. Such a model is recognizable by its two texture
     * slots both pointing at the item icon, and is regenerated instead of preserved.
     */
    private static boolean isOutdatedGeneratedGloveModel(JsonObject model, InfoType config)
    {
        if (!(config instanceof GloveType) || model.has("parent") || !model.has("elements"))
            return false;
        if (!model.has("textures") || !model.get("textures").isJsonObject())
            return false;

        JsonObject textures = model.getAsJsonObject("textures");
        if (textures.size() != 2 || !textures.has("0") || !textures.has("particle"))
            return false;

        String icon = FlansMod.FLANSMOD_ID + ":" + FOLDER_TEXTURES_ITEM + "/" + ResourceUtils.sanitize(config.getIcon());
        return icon.equals(textures.get("0").getAsString()) && icon.equals(textures.get("particle").getAsString());
    }

    private static void generateBlockModelJson(InfoType config, Path outputFolder)
    {
        ResourceUtils.ModelJson model;
        if (config instanceof BlockType blockConfig)
            model = ResourceUtils.ModelJson.createBlockModel(blockConfig);
        else if (config instanceof ItemHolderType itemHolderType)
            model = ResourceUtils.ModelJson.createItemHolderBlockModel(itemHolderType);
        else
            return;

        String jsonContent = canonicalIds(gson.toJson(model), config);
        String shortName = config.getOriginalShortName();

        Path outputFile = outputFolder.resolve(shortName + FileUtils.JSON_EXTENSION);
        if (shouldPreserveExistingBlockModel(outputFile))
            return;
        FileUtils.writeString(outputFile, jsonContent);
    }

    private static void generateBlockstateJson(InfoType config, Path outputFolder)
    {
        ResourceUtils.BlockStateJson model = ResourceUtils.BlockStateJson.create(config);
        String jsonContent = canonicalIds(gson.toJson(model), config);
        String shortName = config.getOriginalShortName();

        Path outputFile = outputFolder.resolve(shortName + FileUtils.JSON_EXTENSION);
        if (shouldPreserveExistingBlockstate(outputFile, config, jsonContent))
            return;
        FileUtils.writeString(outputFile, jsonContent);
    }

    private static String canonicalIds(String json, InfoType config)
    {
        return json.replace(FlansMod.FLANSMOD_ID + ":block/" + config.getShortName() + "\"", FlansMod.FLANSMOD_ID + ":block/" + config.getOriginalShortName() + "\"");
    }

    private static void restoreOriginal(Path folder, InfoType config, java.util.Map<String, String> previous)
    {
        String alias = previous.get(config.getOriginalShortName());
        if (alias == null || alias.equals(config.getOriginalShortName()))
            return;
        Path original = folder.resolve(config.getOriginalShortName() + FileUtils.JSON_EXTENSION);
        Path old = folder.resolve(alias + FileUtils.JSON_EXTENSION);
        try
        {
            if (!Files.exists(original) && Files.isRegularFile(old))
                Files.copy(old, original);
        }
        catch (IOException e)
        {
            FlansLog.log.error("Could not restore authored asset '{}' from '{}'", original, old, e);
        }
    }

    private static boolean shouldPreserveExistingBlockModel(Path file)
    {
        if (!Files.isRegularFile(file))
            return false;
        try
        {
            JsonObject model = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            return !ResourceUtils.isGeneratedBlockModel(model);
        }
        catch (IOException | IllegalStateException | JsonSyntaxException e)
        {
            return false;
        }
    }

    private static boolean shouldPreserveExistingBlockstate(Path file, InfoType config, String generatedJson)
    {
        if (!Files.isRegularFile(file))
            return false;
        try
        {
            JsonObject existing = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            String originalJson = generatedJson.replace(FlansMod.FLANSMOD_ID + ":block/" + config.getShortName(), FlansMod.FLANSMOD_ID + ":block/" + config.getOriginalShortName());
            return !existing.equals(JsonParser.parseString(generatedJson)) && !existing.equals(JsonParser.parseString(originalJson));
        }
        catch (IOException | IllegalStateException | JsonSyntaxException e)
        {
            return false;
        }
    }
}
