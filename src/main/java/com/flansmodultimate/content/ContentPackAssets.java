package com.flansmodultimate.content;

import com.flansmodultimate.common.recipe.RecipeDataCompatibility;
import com.flansmodultimate.common.recipe.RecipeJsonGenerator;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.util.DynamicReference;
import com.flansmodultimate.util.FileUtils;
import com.flansmodultimate.util.FlansLog;
import com.flansmodultimate.util.SoundLengthIndex;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.flansmodultimate.content.ContentPackPaths.*;

/** Coordinates regeneration decisions and delegates each asset format to its own generator. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class ContentPackAssets
{
    private static final String GENERATED_ASSETS_VERSION_FILE = ".flans_generated_assets_version";
    // 3: regenerate lang JSON baked with names mis-decoded as GB18030.
    private static final String GENERATED_ASSETS_VERSION = "3";
    // Preserve portable metadata for 1.20.1 (format 15) and 1.21.1 (format 34).
    private static final int LEGACY_RESOURCE_PACK_FORMAT = 15;
    private static final int CURRENT_RESOURCE_PACK_FORMAT = 34;

    record TextureReferences(Map<String, DynamicReference> armor, Map<String, DynamicReference> gui, Map<String, DynamicReference> skins) {}

    static boolean aliasesChanged(IContentProvider provider, TextureReferences aliases)
    {
        return AliasFileManager.shouldUpdateAliasMappingFile(ARMOR_TEXTURES_ALIAS_FILE, provider, DynamicReference.getAliasMapping(aliases.armor()))
            || AliasFileManager.shouldUpdateAliasMappingFile(GUI_TEXTURES_ALIAS_FILE, provider, DynamicReference.getAliasMapping(aliases.gui()))
            || AliasFileManager.shouldUpdateAliasMappingFile(SKINS_TEXTURES_ALIAS_FILE, provider, DynamicReference.getAliasMapping(aliases.skins()));
    }

    /** Only called after regeneration, so a failed write never establishes a warm cache. */
    static boolean outputsPresent(IContentProvider provider, List<InfoType> items, List<InfoType> blocks, TextureReferences aliases)
    {
        Path assets = provider.getAssetsPath();
        if (!Files.isRegularFile(assets.resolve(FOLDER_LANG).resolve("en_us.json"))
            || shouldUpdateGeneratedTextureFiles(provider, null, aliases)
            || SoundLengthIndex.isOutdated(assets.resolve(FOLDER_SOUNDS), assets.resolve(SoundLengthIndex.FILE_NAME)))
            return false;
        for (InfoType type : items)
            if (!Files.isRegularFile(assets.resolve("models/item/" + type.getOriginalShortName() + ".json")))
                return false;
        for (InfoType type : blocks)
            if (!Files.isRegularFile(assets.resolve("models/block/" + type.getOriginalShortName() + ".json"))
                || !Files.isRegularFile(assets.resolve("blockstates/" + type.getOriginalShortName() + ".json")))
                return false;
        return true;
    }

    static boolean dataOutputsPresent(IContentProvider provider, List<InfoType> items)
    {
        Path data = provider.getDataPath();
        return !RecipeJsonGenerator.needsRegeneration(items, data)
            && !RecipeDataCompatibility.hasMissingCounterparts(data.getParent());
    }

    static void generate(IContentProvider provider, List<InfoType> types, List<InfoType> items, List<InfoType> blocks, TextureReferences aliases)
    {
        AliasFileManager.writeToAliasMappingFile(ARMOR_TEXTURES_ALIAS_FILE, provider, DynamicReference.getAliasMapping(aliases.armor()));
        AliasFileManager.writeToAliasMappingFile(GUI_TEXTURES_ALIAS_FILE, provider, DynamicReference.getAliasMapping(aliases.gui()));
        AliasFileManager.writeToAliasMappingFile(SKINS_TEXTURES_ALIAS_FILE, provider, DynamicReference.getAliasMapping(aliases.skins()));
        ContentPackModels.generate(provider, items, blocks);
        ContentPackLocalization.generate(provider, types);
        copyItemIcons(provider);
        copyBlockTextures(provider);
        copyTextures(provider, FOLDER_TEXTURES_ARMOR, aliases.armor());
        copyTextures(provider, FOLDER_TEXTURES_GUI, aliases.gui());
        copyTextures(provider, FOLDER_TEXTURES_SKINS, aliases.skins());
        ContentPackSounds.generate(provider);
        writeGeneratedAssetsVersion(provider);
    }

    private static void writeGeneratedAssetsVersion(IContentProvider provider)
    {
        Path root = provider.isArchive() ? provider.getExtractedPath() : provider.getPath();
        FileUtils.writeString(root.resolve(GENERATED_ASSETS_VERSION_FILE), GENERATED_ASSETS_VERSION);
    }

    static void createRecipeJsonFiles(IContentProvider provider, List<InfoType> items)
    {
        RecipeDataCompatibility.fillMissingCounterparts(provider.getDataPath().getParent());
        for (InfoType config : items)
        {
            RecipeJsonGenerator.writeRecipes(config, provider.getDataPath());
        }
    }

    private static void copyItemIcons(IContentProvider provider)
    {
        Path sourcePath = provider.getAssetsPath().resolve(FOLDER_TEXTURES).resolve(FOLDER_TEXTURES_ITEMS);
        Path destPath = provider.getAssetsPath().resolve(FOLDER_TEXTURES).resolve(FOLDER_TEXTURES_ITEM);
        GeneratedTextureFiles.copy(sourcePath, destPath);
    }

    private static void copyBlockTextures(IContentProvider provider)
    {
        Path sourcePath = provider.getAssetsPath().resolve(FOLDER_TEXTURES).resolve(FOLDER_TEXTURES_BLOCKS);
        Path destPath = provider.getAssetsPath().resolve(FOLDER_TEXTURES).resolve(FOLDER_TEXTURES_BLOCK);
        GeneratedTextureFiles.copy(sourcePath, destPath);
    }

    private static void copyTextures(IContentProvider provider, String folderName, Map<String, DynamicReference> aliasMapping)
    {
        Path sourcePath = provider.getAssetsPath().resolve(folderName);
        Path destPath = provider.getAssetsPath().resolve(FOLDER_TEXTURES).resolve(folderName);
        GeneratedTextureFiles.copy(sourcePath, destPath, aliasMapping, folderName.equals(FOLDER_TEXTURES_ARMOR));
    }

    static void createMcMeta(IContentProvider provider) {
        Path mcMetaFile = (provider.isArchive() ? provider.getExtractedPath() : provider.getPath()).resolve("pack.mcmeta");
        if (Files.notExists(mcMetaFile))
        {
            try
            {
                Files.createFile(mcMetaFile);
                String content = String.format("""
                    {
                        "pack": {
                            "pack_format": %d,
                            "supported_formats": [%d, %d],
                            "description": "%s"
                        }
                    }""", LEGACY_RESOURCE_PACK_FORMAT, LEGACY_RESOURCE_PACK_FORMAT, CURRENT_RESOURCE_PACK_FORMAT,
                    FilenameUtils.getBaseName(provider.getName()));
                Files.writeString(mcMetaFile, content);
            }
            catch (IOException e)
            {
                FlansLog.log.error("Failed to create {}", mcMetaFile, e);
            }
        }
    }

    private static boolean shouldUpdateGeneratedTextureFiles(IContentProvider provider, FileSystem fs, TextureReferences aliases)
    {
        Path assetsPath = provider.getAssetsPath(fs);
        return GeneratedTextureFiles.needsUpdate(assetsPath.resolve(FOLDER_TEXTURES).resolve(FOLDER_TEXTURES_ITEMS), assetsPath.resolve(FOLDER_TEXTURES).resolve(FOLDER_TEXTURES_ITEM), Collections.emptyMap(), false)
            || GeneratedTextureFiles.needsUpdate(assetsPath.resolve(FOLDER_TEXTURES).resolve(FOLDER_TEXTURES_BLOCKS), assetsPath.resolve(FOLDER_TEXTURES).resolve(FOLDER_TEXTURES_BLOCK), Collections.emptyMap(), false)
            || GeneratedTextureFiles.needsUpdate(assetsPath.resolve(FOLDER_TEXTURES_ARMOR), assetsPath.resolve(FOLDER_TEXTURES).resolve(FOLDER_TEXTURES_ARMOR), aliases.armor(), true)
            || GeneratedTextureFiles.needsUpdate(assetsPath.resolve(FOLDER_TEXTURES_GUI), assetsPath.resolve(FOLDER_TEXTURES).resolve(FOLDER_TEXTURES_GUI), aliases.gui(), false)
            || GeneratedTextureFiles.needsUpdate(assetsPath.resolve(FOLDER_TEXTURES_SKINS), assetsPath.resolve(FOLDER_TEXTURES).resolve(FOLDER_TEXTURES_SKINS), aliases.skins(), false);
    }
}
