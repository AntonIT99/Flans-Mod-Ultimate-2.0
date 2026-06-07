package com.flansmodultimate;

import com.flansmodultimate.common.block.BlockFactory;
import com.flansmodultimate.common.item.ItemFactory;
import com.flansmodultimate.common.paintjob.Paintjob;
import com.flansmodultimate.common.recipe.RecipeJsonGenerator;
import com.flansmodultimate.common.types.ArmorBoxType;
import com.flansmodultimate.common.types.BlockType;
import com.flansmodultimate.common.types.EnumType;
import com.flansmodultimate.common.types.GunBoxType;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.common.types.ItemHolderType;
import com.flansmodultimate.common.types.PaintableType;
import com.flansmodultimate.common.types.PartType;
import com.flansmodultimate.common.types.ToolType;
import com.flansmodultimate.common.types.TypeFile;
import com.flansmodultimate.config.CategoryManager;
import com.flansmodultimate.config.ContentLoadingConfig;
import com.flansmodultimate.util.AliasFileManager;
import com.flansmodultimate.util.DynamicReference;
import com.flansmodultimate.util.FileUtils;
import com.flansmodultimate.util.JavaModelCompiler;
import com.flansmodultimate.util.LogUtils;
import com.flansmodultimate.util.ResourceUtils;
import com.flansmodultimate.util.SoundJsonProcessor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ContentManager
{
    public static final String FOLDER_BLOCKSTATES = "blockstates";
    public static final String FOLDER_MODELS = "models";
    public static final String FOLDER_MODELS_BLOCK = "block";
    public static final String FOLDER_MODELS_ITEM = "item";
    public static final String FOLDER_LANG = "lang";
    public static final String FOLDER_TEXTURES = "textures";
    public static final String FOLDER_TEXTURES_ARMOR = "armor";
    public static final String FOLDER_TEXTURES_GUI = "gui";
    public static final String FOLDER_TEXTURES_SKINS = "skins";
    public static final String FOLDER_TEXTURES_BLOCK = "block";
    public static final String FOLDER_TEXTURES_BLOCKS = "blocks";
    public static final String FOLDER_TEXTURES_ITEM = "item";
    public static final String FOLDER_TEXTURES_ITEMS = "items";
    public static final String FOLDER_SOUND = "sound";
    public static final String FOLDER_SOUNDS = "sounds";
    public static final String FOLDER_RECIPES = "recipes";

    private static final String TRANSLATION_KEY_PREFIX_ITEM = "item.";
    private static final String TRANSLATION_KEY_PREFIX_BLOCK = "block.";
    private static final String TRANSLATION_KEY_PREFIX_TYPE = "tile.";
    private static final String TRANSLATION_KEY_SUFFIX_NAME = ".name";

    @Getter
    private static Path flanFolder;
    private static final Path defaultFlanPath = FMLPaths.GAMEDIR.get().resolve(ContentLoadingConfig.getContentPacksRelativePath());
    private static final Path fallbackFlanPath = FMLPaths.GAMEDIR.get().resolve("Flan");

    // Mappings which allow to use aliases for duplicate short names and texture names (also contain unmodified references)
    // The idea behind dynamic references is to allow references to shortnames and textures to change
    // even after configs are registered (as long as item classes have not been instantiated yet)
    @Getter
    private static final Map<IContentProvider, Map<String, DynamicReference>> shortnameReferences = new HashMap<>();
    @Getter
    private static final Map<IContentProvider, Map<String, DynamicReference>> armorTextureReferences = new HashMap<>();
    @Getter
    private static final Map<IContentProvider, Map<String, DynamicReference>> guiTextureReferences = new HashMap<>();
    @Getter
    private static final Map<IContentProvider, Map<String, DynamicReference>> skinsTextureReferences = new HashMap<>();
    @Getter
    private static final Map<IContentProvider, Map<String, DynamicReference>> modelReferences = new HashMap<>();

    private static final String ID_ALIAS_FILE = "id_alias.json";
    private static final String ARMOR_TEXTURES_ALIAS_FILE = "armor_textures_alias.json";
    private static final String GUI_TEXTURES_ALIAS_FILE = "gui_textures_alias.json";
    private static final String SKINS_TEXTURES_ALIAS_FILE = "skins_textures_alias.json";
    private static final String GENERATED_TEXTURES_MANIFEST_FILE = ".flansmod_generated_textures.json";
    private static final String CONTENT_STARTUP_LOCK_FILE = ".flansmod-content.lock";

    private static final List<IContentProvider> contentPacks = new ArrayList<>();
    private static final Map<IContentProvider, ArrayList<TypeFile>> files = new HashMap<>();
    private static final Map<IContentProvider, ArrayList<InfoType>> configs = new HashMap<>();

    // Keep track of registered items and loaded textures and models
    /** &lt; shortname, config file string representation &gt; */
    private static final Map<String, String> registeredItems = new HashMap<>();
    /** &lt; folder name, &lt;lowercase name, texture file &gt;&gt; */
    private static final Map<String, Map<String, TextureFile>> textures = new HashMap<>();
    /** &lt; model class name, &lt; contentPack &gt;&gt; */
    @Getter
    private static final Map<String, IContentProvider> registeredModels = new HashMap<>();
    private static final Map<ResourceLocation, Set<TextureOrigin>> modelTextureOrigins = new HashMap<>();

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    // Latin-1 must stay last because it can decode every byte sequence.
    private static final List<Charset> TYPE_FILE_CHARSETS = List.of(
        StandardCharsets.UTF_8,
        Charset.forName("GB18030"),
        StandardCharsets.ISO_8859_1
    );

    private record TextureFile(String name, IContentProvider contentPack) {}
    private record FileContentSignature(long size, String sha256) {}
    private record TextureOrigin(String contentPackName, String typeFolderName, String fileName)
    {
        @Override
        @NotNull
        public String toString()
        {
            return typeFolderName + "/" + fileName + " [" + contentPackName + "]";
        }
    }
    private record MissingModelTexture(ResourceLocation textureId, TextureOrigin origin) {}

    static
    {
        textures.put(FOLDER_TEXTURES_ARMOR, new HashMap<>());
        textures.put(FOLDER_TEXTURES_GUI, new HashMap<>());
        textures.put(FOLDER_TEXTURES_SKINS, new HashMap<>());
    }

    /**
     * Scan the Forge mods folder for misplaced Flan content packs and move them to flanFolder.
     * - If destination already exists: do NOT move, log a warning.
     * - Any IOExceptions are handled internally (won't crash startup).
     */
    public static void searchForContentPacksInModsFolder()
    {
        final Path modsFolder = FMLPaths.MODSDIR.get();

        if (canSearchModsFolderForContentPacks(modsFolder))
        {
            try
            {
                moveMisplacedContentPacks(modsFolder);
            }
            catch (IOException e)
            {
                FlansMod.log.warn("Error while scanning mods folder '{}' for misplaced content packs: {}", modsFolder.toAbsolutePath(), e.toString());
            }
            catch (Exception e)
            {
                FlansMod.log.warn("Unexpected error while scanning mods folder '{}' for misplaced content packs: {}", modsFolder.toAbsolutePath(), e.toString());
            }
        }
    }

    private static boolean canSearchModsFolderForContentPacks(Path modsFolder)
    {
        return flanFolder != null && FileUtils.tryCreateDirectories(flanFolder) && Files.isDirectory(modsFolder);
    }

    private static void moveMisplacedContentPacks(Path modsFolder) throws IOException
    {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(modsFolder, ContentManager::isJarOrZipFile))
        {
            for (Path candidate : stream)
                moveContentPackIfNeeded(candidate);
        }
    }

    private static boolean isJarOrZipFile(Path entry)
    {
        if (!Files.isRegularFile(entry))
            return false;

        String name = entry.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(FileUtils.JAR_EXTENSION) || name.endsWith(FileUtils.ZIP_EXTENSION);
    }

    private static void moveContentPackIfNeeded(Path candidate)
    {
        if (!isReadableContentPack(candidate))
            return;

        Path destination = flanFolder.resolve(candidate.getFileName());
        if (Files.exists(destination))
        {
            FlansMod.log.warn("Found Flan content pack '{}' in mods folder, but '{}' already exists in '{}'. Not moving to avoid overwriting.", candidate.getFileName(), destination.getFileName(), flanFolder.toAbsolutePath());
            return;
        }

        moveContentPack(candidate, destination);
    }

    private static boolean isReadableContentPack(Path candidate)
    {
        try
        {
            return looksLikeContentPack(candidate);
        }
        catch (IOException e)
        {
            return false;
        }
    }

    private static void moveContentPack(Path candidate, Path destination)
    {
        try
        {
            FileUtils.safeMove(candidate, destination);
            FlansMod.log.info("Moved misplaced Flan content pack '{}' to '{}'.", candidate.getFileName(), flanFolder.toAbsolutePath());
        }
        catch (IOException e)
        {
            FlansMod.log.warn("Failed to move '{}' to '{}': {}", candidate.getFileName(), destination, e.toString());
        }
    }

    /**
     * Identification rules:
     *  - Must contain at least one entry under assets/flansmod/
     *  - Must NOT contain META-INF/mods.toml
     */
    private static boolean looksLikeContentPack(Path jarOrZip) throws IOException
    {
        try (ZipFile zip = new ZipFile(jarOrZip.toFile()))
        {
            // Fast exact lookup: if mods.toml exists -> it's a Forge mod -> not a pack
            if (zip.getEntry("META-INF/mods.toml") != null)
                return false;

            // Prefix scan with early exit
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements())
            {
                ZipEntry e = entries.nextElement();
                String name = e.getName();
                if (name.startsWith("assets/flansmod/"))
                    return true;
            }
            return false;
        }
    }

    public static void findContentInFlanFolder()
    {
        loadFlanFolder();
        if (flanFolder == null)
            return;

        try
        {
            contentPacks.addAll(loadFoldersAndJarZipFiles(flanFolder)
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new ContentPack(entry.getKey(), entry.getValue()))
                .toList());
        }
        catch (IOException e)
        {
            FlansMod.log.error("Failed to load content packs from flan folder.", e);
        }
    }

    public static void readContentPacks()
    {
        if (flanFolder == null)
            return;

        Path gameDir = flanFolder.getParent();
        if (gameDir == null)
        {
            FlansMod.log.error("Cannot load content packs because flan folder '{}' has no parent directory.", flanFolder);
            return;
        }

        FileUtils.runWithFileLock(gameDir.resolve(CONTENT_STARTUP_LOCK_FILE), "Flan content startup", ContentManager::readContentPacksLocked);
    }

    private static void readContentPacksLocked()
    {
        Path tempRoot = flanFolder.getParent().resolve(".flantemp");
        FileUtils.cleanupFlanTempOnStartup(tempRoot);

        for (IContentProvider provider : contentPacks)
        {
            long startTime = System.currentTimeMillis();

            files.putIfAbsent(provider, new ArrayList<>());
            configs.putIfAbsent(provider, new ArrayList<>());

            shortnameReferences.putIfAbsent(provider, new HashMap<>());
            armorTextureReferences.putIfAbsent(provider, new HashMap<>());
            guiTextureReferences.putIfAbsent(provider, new HashMap<>());
            skinsTextureReferences.putIfAbsent(provider, new HashMap<>());
            modelReferences.putIfAbsent(provider, new HashMap<>());

            if (!provider.isArchive())
                compileJavaModelsIfNeeded(provider);

            loadTypes(provider);

            if (FMLEnvironment.dist == Dist.CLIENT)
            {
                findDuplicateTextures(provider);
            }

            boolean archiveExtracted = false;
            boolean preLoadAssets = shouldPreLoadAssets(provider);
            boolean preLoadData = shouldPreLoadData(provider);

            if (shouldUnpackArchive(provider, preLoadAssets, preLoadData))
            {
                FlansMod.log.info("Reprocessing {}...", provider.getName());
                FileUtils.prepareFreshExtractionDir(provider.getExtractedPath());
                archiveExtracted = FileUtils.extractArchive(provider.getPath(), provider.getExtractedPath());
            }

            if (archiveExtracted || !provider.isArchive())
            {
                if (archiveExtracted)
                    compileJavaModelsIfNeeded(provider);

                createMcMeta(provider);
                writeToAliasMappingFile(ID_ALIAS_FILE, provider,DynamicReference.getAliasMapping(shortnameReferences.get(provider)));

                if (preLoadData)
                    createRecipeJsonFiles(provider);

                if (preLoadAssets)
                {
                    writeToAliasMappingFile(ARMOR_TEXTURES_ALIAS_FILE, provider, DynamicReference.getAliasMapping(armorTextureReferences.get(provider)));
                    writeToAliasMappingFile(GUI_TEXTURES_ALIAS_FILE, provider, DynamicReference.getAliasMapping(guiTextureReferences.get(provider)));
                    writeToAliasMappingFile(SKINS_TEXTURES_ALIAS_FILE, provider, DynamicReference.getAliasMapping(skinsTextureReferences.get(provider)));
                    createItemAndBlockJsonFiles(provider);
                    createLocalization(provider);
                    copyItemIcons(provider);
                    copyBlockTextures(provider);
                    copyTextures(provider, FOLDER_TEXTURES_ARMOR, armorTextureReferences.get(provider));
                    copyTextures(provider, FOLDER_TEXTURES_GUI, guiTextureReferences.get(provider));
                    copyTextures(provider, FOLDER_TEXTURES_SKINS, skinsTextureReferences.get(provider));
                    createSounds(provider);
                }
            }

            if (archiveExtracted)
            {
                FileUtils.repackArchive(provider);
            }

            long endTime = System.currentTimeMillis();
            String loadingTimeMs = String.format("%,d", endTime - startTime);
            FlansMod.log.info("Loaded content pack {} in {} ms.", provider.getName(), loadingTimeMs);
        }

        resolveDeferredContentReferences();

        FileUtils.deleteDirectoryIfEmpty(tempRoot);
    }

    private static void loadTypes(IContentProvider provider)
    {
        if (FlansMod.log.isDebugEnabled())
        {
            long start = System.currentTimeMillis();
            readFiles(provider);
            registerConfigs(provider);
            long end = System.currentTimeMillis();
            FlansMod.log.debug("{}: Types loaded in {} ms", provider.getName(), String.format("%,d", end - start));
        }
        else
        {
            readFiles(provider);
            registerConfigs(provider);
        }
    }

    private static void resolveDeferredContentReferences()
    {
        for (ArrayList<InfoType> providerConfigs : configs.values())
        {
            for (InfoType config : providerConfigs)
            {
                if (config instanceof GunBoxType gunBoxType)
                    gunBoxType.resolveDeferredReferences();
                else if (config instanceof ArmorBoxType armorBoxType)
                    armorBoxType.resolveDeferredReferences();
            }
        }
    }

    public static void validateContentReferences()
    {
        int gunBoxes = 0;
        int armorBoxes = 0;
        int parts = 0;
        int tools = 0;

        FlansMod.log.info("Validating content references...");
        for (ArrayList<InfoType> providerConfigs : configs.values())
        {
            for (InfoType config : providerConfigs)
            {
                if (config instanceof GunBoxType gunBoxType)
                {
                    gunBoxType.validateRecipeIngredients();
                    gunBoxes++;
                }
                else if (config instanceof ArmorBoxType armorBoxType)
                {
                    armorBoxType.validateRecipeIngredients();
                    armorBoxes++;
                }
                else if (config instanceof PartType partType)
                {
                    partType.validateRecipeIngredients();
                    parts++;
                }
                else if (config instanceof ToolType toolType)
                {
                    toolType.validateRecipeIngredients();
                    tools++;
                }
            }
        }
        FlansMod.log.info("Validated content references for {} GunBoxes, {} ArmorBoxes, {} parts, and {} tools.", gunBoxes, armorBoxes, parts, tools);
    }

    private static void loadFlanFolder()
    {
        if (!Files.exists(defaultFlanPath) && Files.exists(fallbackFlanPath))
        {
            flanFolder = fallbackFlanPath;
        }
        else
        {
            if (!FileUtils.tryCreateDirectories(defaultFlanPath))
                return;

            flanFolder = defaultFlanPath;
        }
    }

    private static Map<String, Path> loadFoldersAndJarZipFiles(Path rootPath) throws IOException
    {
        Set<String> processedNames = new HashSet<>();

        try (Stream<Path> stream = Files.walk(rootPath, 1))
        {
            return stream.filter(path -> {
                if (path.equals(rootPath))
                    return false;

                if (Files.isDirectory(path) || path.toString().toLowerCase(Locale.ROOT).endsWith(FileUtils.JAR_EXTENSION) || path.toString().toLowerCase(Locale.ROOT).endsWith(FileUtils.ZIP_EXTENSION))
                {
                    String name = FilenameUtils.getBaseName(path.getFileName().toString());
                    if (!processedNames.contains(name))
                    {
                        FlansMod.log.info("Content pack found in flan folder: '{}'", path.getFileName());
                        processedNames.add(name);
                        return true;
                    }
                    else
                    {
                        FlansMod.log.info("Skipping loading content pack from flan folder as it is duplicated: '{}'", path.getFileName());
                        return false;
                    }
                }
                return false;
            }).collect(Collectors.toMap(path -> path.getFileName().toString(), path -> path));
        }
    }

    private static void readFiles(IContentProvider provider)
    {
        try (DirectoryStream<Path> dirStream = FileUtils.createDirectoryStream(provider))
        {
            dirStream.forEach(path ->
            {
                if (Files.isDirectory(path))
                {
                    readTypeFolder(path, provider);
                }
                else if (Files.isRegularFile(path))
                {
                    if (path.getFileName().toString().equals(ID_ALIAS_FILE))
                    {
                        readAliasMappingFile(path.getFileName().toString(), provider, shortnameReferences);
                    }
                    if (FMLEnvironment.dist == Dist.CLIENT)
                    {
                        if (path.getFileName().toString().equals(ARMOR_TEXTURES_ALIAS_FILE))
                        {
                            readAliasMappingFile(path.getFileName().toString(), provider, armorTextureReferences);
                        }
                        if (path.getFileName().toString().equals(GUI_TEXTURES_ALIAS_FILE))
                        {
                            readAliasMappingFile(path.getFileName().toString(), provider, guiTextureReferences);
                        }
                        if (path.getFileName().toString().equals(SKINS_TEXTURES_ALIAS_FILE))
                        {
                            readAliasMappingFile(path.getFileName().toString(), provider, skinsTextureReferences);
                        }
                    }
                }
            });
        }
        catch (IOException e)
        {
            FlansMod.log.error("Failed to load types in content pack '{}'", provider.getName(), e);
        }
    }

    private static void readAliasMappingFile(String fileName, IContentProvider provider, Map<IContentProvider, Map<String, DynamicReference>> references)
    {
        try (AliasFileManager fileManager = new AliasFileManager(fileName, provider))
        {
            fileManager.readFile().ifPresent(map ->
                    map.forEach((originalShortname, aliasShortname) -> DynamicReference.storeOrUpdate(originalShortname, aliasShortname, references.get(provider))));
        }
    }

    private static void readTypeFolder(Path folder, IContentProvider provider)
    {
        String folderName = folder.getFileName().toString();
        if (!EnumType.getFoldersList().contains(folderName))
            return;

        try (Stream<Path> walk = Files.walk(folder))
        {
            files.get(provider).addAll(walk
                .filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(FileUtils.TXT_EXTENSION))
                .map(txtFile -> readTypeFile(txtFile, folderName, provider))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt((TypeFile typeFile) -> typeFile.getType().getLoadOrder()).thenComparing(TypeFile::getName))
                .toList()
            );
        }
        catch (IOException e)
        {
            FlansMod.log.error("Failed to read '{}' folder in content pack '{}'", folderName, provider.getName(), e);
        }
    }

    @Nullable
    private static TypeFile readTypeFile(Path file, String folderName, IContentProvider provider)
    {
        try
        {
            List<String> lines = readTypeFileLines(file);
            stripBomIfPresent(lines);
            return new TypeFile(file.getFileName().toString(), EnumType.getType(folderName).orElse(null), provider, lines);
        }
        catch (IOException e)
        {
            FlansMod.log.error("Failed to read '{}/{}' in content pack '{}'", folderName, file.getFileName(), provider.getName(), e);
            return null;
        }
    }

    private static List<String> readTypeFileLines(Path file) throws IOException
    {
        CharacterCodingException firstDecodeFailure = null;

        for (Charset charset : TYPE_FILE_CHARSETS)
        {
            try
            {
                return Files.readAllLines(file, charset);
            }
            catch (CharacterCodingException e)
            {
                if (firstDecodeFailure == null)
                    firstDecodeFailure = e;
                else
                    firstDecodeFailure.addSuppressed(e);
            }
        }

        throw firstDecodeFailure != null ? firstDecodeFailure : new IOException("No charset configured for " + file);
    }

    private static void stripBomIfPresent(List<String> lines)
    {
        if (!lines.isEmpty() && !lines.get(0).isEmpty() && lines.get(0).charAt(0) == '\uFEFF')
        {
            lines.set(0, lines.get(0).substring(1));
        }
    }

    private static void registerConfigs(IContentProvider contentPack)
    {
        List<TypeFile> typeFiles = files.get(contentPack).stream()
            .sorted(Comparator.comparingInt((TypeFile typeFile) -> typeFile.getType().getLoadOrder()).thenComparing(TypeFile::getName))
            .toList();

        for (TypeFile typeFile : typeFiles)
        {
            try
            {
                CategoryManager.applyCategoriesToFile(typeFile);
                EnumType type = typeFile.getType();
                InfoType config = type.getTypeClass().getConstructor().newInstance();
                config.load(typeFile);
                String shortName = config.getOriginalShortName();

                if (!shortName.isBlank())
                {
                    if (type.isHasItem())
                    {
                        shortName = findNewValidShortName(shortName, contentPack, typeFile);
                        if (!shortName.isBlank())
                        {
                            registerItem(shortName, config, typeFile);
                            if (type.isHasBlock())
                                registerBlock(shortName, config);
                            config.onItemRegistration(shortName);
                            addConfig(contentPack, config);
                        }
                    }
                    else
                    {
                        addConfig(contentPack, config);
                    }
                }
                else
                {
                    FlansMod.log.error("ShortName not set: {}", typeFile);
                }
            }
            catch (Exception e)
            {
                FlansMod.log.error("Failed to add {}", typeFile);
                LogUtils.logErrorWithoutStacktrace(e);
            }
        }
        files.clear();
    }

    private static void addConfig(IContentProvider contentPack, InfoType config)
    {
        configs.get(contentPack).add(config);
        if (FMLEnvironment.dist == Dist.CLIENT)
            registerModelTextureOrigins(config);
    }

    private static void registerModelTextureOrigins(InfoType config)
    {
        TextureOrigin origin = new TextureOrigin(config.getContentPack().getName(), config.getType().getConfigFolderName(), config.getFileName());

        if (config instanceof BlockType blockConfig)
        {
            registerModelTextureOrigin(FOLDER_TEXTURES_BLOCK, blockConfig.getTopTextureName(), origin);
            registerModelTextureOrigin(FOLDER_TEXTURES_BLOCK, blockConfig.getBottomTextureName(), origin);
            registerModelTextureOrigin(FOLDER_TEXTURES_BLOCK, blockConfig.getSideTextureName(), origin);
            return;
        }

        if (config.getType().isHasItem())
            registerModelTextureOrigin(FOLDER_TEXTURES_ITEM, config.getIcon(), origin);

        if (config instanceof PaintableType paintableType)
        {
            paintableType.getPaintjobs().values().stream()
                .filter(paintjob -> !paintjob.equals(paintableType.getDefaultPaintjob()))
                .forEach(paintjob -> registerModelTextureOrigin(FOLDER_TEXTURES_ITEM, paintjob.getIcon(), origin));
        }
    }

    private static void registerModelTextureOrigin(String textureFolder, @Nullable String textureName, TextureOrigin origin)
    {
        String texturePath = textureFolder + "/" + ResourceUtils.sanitize(textureName);
        try
        {
            ResourceLocation textureId = ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, texturePath);
            modelTextureOrigins.computeIfAbsent(textureId, key -> new HashSet<>()).add(origin);
        }
        catch (Exception e)
        {
            FlansMod.log.warn("Could not register expected model texture '{}': {}", texturePath, origin);
        }
    }

    public static void logMissingModelTextures(ResourceManager resourceManager)
    {
        List<MissingModelTexture> missingTextures = new ArrayList<>();
        for (Map.Entry<ResourceLocation, Set<TextureOrigin>> entry : modelTextureOrigins.entrySet())
        {
            if (resourceManager.getResource(toTextureResource(entry.getKey())).isEmpty())
            {
                for (TextureOrigin origin : entry.getValue())
                    missingTextures.add(new MissingModelTexture(entry.getKey(), origin));
            }
        }

        missingTextures.stream()
            .sorted(Comparator.comparing((MissingModelTexture missing) -> missing.origin().contentPackName())
                .thenComparing(missing -> missing.origin().typeFolderName())
                .thenComparing(missing -> missing.origin().fileName())
                .thenComparing(missing -> missing.textureId().toString()))
            .forEach(missing -> FlansMod.log.warn("Missing texture {}: {}", missing.textureId(), missing.origin()));
    }

    private static ResourceLocation toTextureResource(ResourceLocation textureId)
    {
        return ResourceLocation.fromNamespaceAndPath(textureId.getNamespace(), FOLDER_TEXTURES + "/" + textureId.getPath() + FileUtils.PNG_EXTENSION);
    }

    private static String findNewValidShortName(String originalShortname, IContentProvider provider, TypeFile file)
    {
        String shortname = originalShortname;
        // Item shortname already registered and this content pack already has an alias shortname for this item
        if (registeredItems.containsKey(originalShortname) && shortnameReferences.get(provider).containsKey(originalShortname))
        {
            shortname = shortnameReferences.get(provider).get(originalShortname).get();
        }

        String newShortname = shortname;
        for (int i = 2; registeredItems.containsKey(newShortname); i++)
            newShortname = originalShortname + "_" + i;

        if (!shortname.equals(newShortname))
        {
            // This file
            String contentPackName = provider.getName();
            String fileName = file.getName();
            // otherFileOriginal -> the file that registered the original shortname
            // otherFileAlias -> in case another file of the same pack already registered the existing alias
            String otherFileOriginal = registeredItems.get(originalShortname);
            Optional<String> otherFileAlias = Optional.ofNullable(shortnameReferences.get(provider).get(originalShortname))
                .map(DynamicReference::get)
                .map(registeredItems::get);

            // Conflict is in the same Content Pack -> Ignore file
            Optional<String> conflictingFileInSamePack = Optional.of(otherFileOriginal)
                .filter(conflictingFile -> contentPackName.equals(TypeFile.getContentPackName(conflictingFile)))
                .or(() -> otherFileAlias.filter(conflictingFile -> contentPackName.equals(TypeFile.getContentPackName(conflictingFile))));
            if (conflictingFileInSamePack.isPresent())
            {
                FlansMod.log.warn("Detected conflict for item id '{}' in same content pack: {} and {}. Ignoring {}", originalShortname, file, conflictingFileInSamePack.get(), fileName);
                return StringUtils.EMPTY;
            }

            FlansMod.log.warn("Detected conflict for item id '{}': {} and {}. Creating id alias '{}' in [{}]", originalShortname, file, otherFileOriginal, newShortname, contentPackName);
            shortname = newShortname;
        }

        DynamicReference.storeOrUpdate(originalShortname, shortname, shortnameReferences.get(provider));
        return shortname;
    }

    private static void registerItem(String shortName, InfoType config, TypeFile typeFile)
    {
        registeredItems.put(shortName, typeFile.toString());
        FlansMod.registerItem(shortName, config.getType(), () -> ItemFactory.createItem(config));
    }

    private static void registerBlock(String shortName, InfoType config)
    {
        FlansMod.registerBlock(shortName, config.getType(), () -> BlockFactory.createBlock(config));
    }

    private static void findDuplicateTextures(IContentProvider provider)
    {
        FileSystem fs = FileUtils.createFileSystem(provider);
        findDuplicateTexturesInFolder(FOLDER_TEXTURES_ARMOR, provider, fs, armorTextureReferences.get(provider));
        findDuplicateTexturesInFolder(FOLDER_TEXTURES_GUI, provider, fs, guiTextureReferences.get(provider));
        findDuplicateTexturesInFolder(FOLDER_TEXTURES_SKINS, provider, fs, skinsTextureReferences.get(provider));
        FileUtils.closeFileSystem(fs, provider);
    }

    private static void findDuplicateTexturesInFolder(String folderName, IContentProvider provider, FileSystem fs, Map<String, DynamicReference> aliasMapping)
    {
        Path textureFolderPath = provider.getAssetsPath(fs).resolve(folderName);

        if (Files.exists(textureFolderPath))
        {
            try (Stream<Path> stream = Files.list(textureFolderPath))
            {
                stream.filter(p -> p.toString().toLowerCase(Locale.ROOT).endsWith(FileUtils.PNG_EXTENSION))
                        .forEach(p -> checkForDuplicateTextures(p, provider, folderName, aliasMapping));
            }
            catch (IOException e)
            {
                FlansMod.log.error("Could not scan '{}' textures for duplicate detection in content pack '{}'", folderName, provider.getName(), e);
            }
        }
    }

    private static void checkForDuplicateTextures(Path texturePath, IContentProvider provider, String folderName, Map<String, DynamicReference> aliasMapping)
    {
        String fileName = FilenameUtils.getBaseName(texturePath.getFileName().toString());
        if (folderName.equals(FOLDER_TEXTURES_ARMOR))
            fileName = getArmorTextureBaseName(fileName);
        fileName = ResourceUtils.sanitize(fileName);
        String aliasName = fileName;

        if (isTextureNameAlreadyRegistered(fileName, folderName, provider))
        {
            TextureFile otherFile = textures.get(folderName).get(fileName);
            FileSystem fs = FileUtils.createFileSystem(otherFile.contentPack());

            Path otherPath = otherFile.contentPack().getAssetsPath(fs).resolve(folderName).resolve(otherFile.name());
            if (FileUtils.isDifferentFileContent(texturePath, otherPath, false))
                aliasName = findValidTextureName(fileName, folderName, provider, otherFile.contentPack(), aliasMapping);

            FileUtils.closeFileSystem(fs, otherFile.contentPack());
        }

        DynamicReference.storeOrUpdate(fileName, aliasName, aliasMapping);
        textures.get(folderName).put(aliasName, new TextureFile(texturePath.getFileName().toString(), provider));
    }

    private static String getArmorTextureBaseName(String fileBaseName)
    {
        if (fileBaseName.endsWith("_1") || fileBaseName.endsWith("_2")) {
            return fileBaseName.substring(0, fileBaseName.length() - 2);
        }
        return fileBaseName;
    }

    private static String findValidTextureName(String originalName, String folderName, IContentProvider thisContentPack, IContentProvider otherContentPack, Map<String, DynamicReference> aliasMapping)
    {
        String name = originalName;

        if (isTextureNameAlreadyRegistered(name, folderName, thisContentPack) && aliasMapping.containsKey(name))
        {
            name = aliasMapping.get(name).get();
        }

        String newName = name;
        for (int i = 2; isTextureNameAlreadyRegistered(newName, folderName, thisContentPack); i++)
            newName = originalName + "_" + i;

        if (!name.equals(newName))
        {
            name = newName;
            FlansMod.log.warn("Duplicate texture detected: '{}/{}' in [{}] and [{}]. Creating texture alias '{}' in [{}]", folderName, originalName, thisContentPack.getName(), otherContentPack.getName(), name, thisContentPack.getName());
        }

        return name;
    }

    private static boolean shouldUpdateAliasMappingFile(String fileName, IContentProvider provider, @Nullable Map<String, String> aliasMapping)
    {
        if (aliasMapping == null)
            aliasMapping = Collections.emptyMap();

        try (AliasFileManager fileManager = new AliasFileManager(fileName, provider))
        {
            Optional<Map<String, String>> mapping = fileManager.readFile();
            return mapping.isEmpty() || !mapping.get().equals(aliasMapping);
        }
    }

    private static void writeToAliasMappingFile(String fileName, IContentProvider provider, @Nullable Map<String, String> aliasMapping)
    {
        if (aliasMapping == null)
            aliasMapping = Collections.emptyMap();

        try (AliasFileManager fileManager = new AliasFileManager(fileName, provider))
        {
            fileManager.writeToFile(aliasMapping);
        }
    }

    private static boolean shouldPreLoadAssets(IContentProvider provider)
    {
        if (FMLEnvironment.dist != Dist.CLIENT)
            return false;

        if (ContentLoadingConfig.isForceRegenContentPacksAssetsAndIds())
            return true;

        if (provider.isJarFile() // JAR File means it's the first time we've loaded the pack
            || shouldUpdateAliasMappingFile(ID_ALIAS_FILE, provider, DynamicReference.getAliasMapping(shortnameReferences.get(provider)))
            || shouldUpdateAliasMappingFile(ARMOR_TEXTURES_ALIAS_FILE, provider, DynamicReference.getAliasMapping(armorTextureReferences.get(provider)))
            || shouldUpdateAliasMappingFile(GUI_TEXTURES_ALIAS_FILE, provider, DynamicReference.getAliasMapping(guiTextureReferences.get(provider)))
            || shouldUpdateAliasMappingFile(SKINS_TEXTURES_ALIAS_FILE, provider, DynamicReference.getAliasMapping(skinsTextureReferences.get(provider))))
            return true;

        FileSystem fs = FileUtils.createFileSystem(provider);

        boolean missingAssets = !Files.exists(provider.getAssetsPath(fs).resolve(FOLDER_BLOCKSTATES))
            || !Files.exists(provider.getAssetsPath(fs).resolve(FOLDER_MODELS).resolve(FOLDER_MODELS_ITEM))
            || !Files.exists(provider.getAssetsPath(fs).resolve(FOLDER_MODELS).resolve(FOLDER_MODELS_BLOCK))
            || shouldUpdateGeneratedTextureFiles(provider, fs)
            || (!Files.exists(provider.getAssetsPath(fs).resolve(FOLDER_TEXTURES).resolve(FOLDER_TEXTURES_ITEM)) && Files.exists(provider.getAssetsPath(fs).resolve(FOLDER_TEXTURES).resolve(FOLDER_TEXTURES_ITEMS)))
            || (!Files.exists(provider.getAssetsPath(fs).resolve(FOLDER_TEXTURES).resolve(FOLDER_TEXTURES_BLOCK)) && Files.exists(provider.getAssetsPath(fs).resolve(FOLDER_TEXTURES).resolve(FOLDER_TEXTURES_BLOCKS)))
            || (!Files.exists(provider.getAssetsPath(fs).resolve(FOLDER_TEXTURES).resolve(FOLDER_TEXTURES_ARMOR)) && Files.exists(provider.getAssetsPath(fs).resolve(FOLDER_TEXTURES_ARMOR)))
            || (!Files.exists(provider.getAssetsPath(fs).resolve(FOLDER_TEXTURES).resolve(FOLDER_TEXTURES_GUI)) && Files.exists(provider.getAssetsPath(fs).resolve(FOLDER_TEXTURES_GUI)))
            || (!Files.exists(provider.getAssetsPath(fs).resolve(FOLDER_TEXTURES).resolve(FOLDER_TEXTURES_SKINS)) && Files.exists(provider.getAssetsPath(fs).resolve(FOLDER_TEXTURES_SKINS)))
            || !Files.exists(provider.getAssetsPath(fs).resolve(FOLDER_LANG))
            || !Files.exists(provider.getAssetsPath(fs).resolve(FOLDER_LANG).resolve("en_us.json"));

        FileUtils.closeFileSystem(fs, provider);
        return missingAssets;
    }

    private static boolean shouldUpdateGeneratedTextureFiles(IContentProvider provider, FileSystem fs)
    {
        Path assetsPath = provider.getAssetsPath(fs);
        return shouldUpdateGeneratedTextureFiles(assetsPath.resolve(FOLDER_TEXTURES).resolve(FOLDER_TEXTURES_ITEMS), assetsPath.resolve(FOLDER_TEXTURES).resolve(FOLDER_TEXTURES_ITEM), Collections.emptyMap(), false)
            || shouldUpdateGeneratedTextureFiles(assetsPath.resolve(FOLDER_TEXTURES).resolve(FOLDER_TEXTURES_BLOCKS), assetsPath.resolve(FOLDER_TEXTURES).resolve(FOLDER_TEXTURES_BLOCK), Collections.emptyMap(), false)
            || shouldUpdateGeneratedTextureFiles(assetsPath.resolve(FOLDER_TEXTURES_ARMOR), assetsPath.resolve(FOLDER_TEXTURES).resolve(FOLDER_TEXTURES_ARMOR), armorTextureReferences.get(provider), true)
            || shouldUpdateGeneratedTextureFiles(assetsPath.resolve(FOLDER_TEXTURES_GUI), assetsPath.resolve(FOLDER_TEXTURES).resolve(FOLDER_TEXTURES_GUI), guiTextureReferences.get(provider), false)
            || shouldUpdateGeneratedTextureFiles(assetsPath.resolve(FOLDER_TEXTURES_SKINS), assetsPath.resolve(FOLDER_TEXTURES).resolve(FOLDER_TEXTURES_SKINS), skinsTextureReferences.get(provider), false);
    }

    private static boolean shouldUpdateGeneratedTextureFiles(Path sourcePath, Path destPath, Map<String, DynamicReference> aliasMapping, boolean armorTextureFolder)
    {
        Set<String> previousGeneratedTextures = readGeneratedTexturesManifest(destPath);
        if (!Files.exists(sourcePath))
            return !previousGeneratedTextures.isEmpty();

        List<Path> sourceFiles = listSourcePngFiles(sourcePath);
        if (sourceFiles == null)
            return false;

        Set<String> textureNamePlan = createTextureNamePlan(sourcePath, destPath, sourceFiles, aliasMapping, armorTextureFolder, previousGeneratedTextures);
        if (!previousGeneratedTextures.equals(textureNamePlan))
            return true;

        for (String fileName : textureNamePlan)
        {
            Path destFile = destPath.resolve(fileName);
            if (!Files.exists(destFile))
                return true;
        }

        return false;
    }

    private static boolean shouldPreLoadData(IContentProvider provider)
    {
        if (ContentLoadingConfig.isForceRegenContentPacksAssetsAndIds())
            return true;

        if (provider.isJarFile()
            || shouldUpdateAliasMappingFile(ID_ALIAS_FILE, provider, DynamicReference.getAliasMapping(shortnameReferences.get(provider))))
            return true;

        FileSystem fs = FileUtils.createFileSystem(provider);
        boolean missingData = isMissingGeneratedRecipeFiles(provider, fs);
        FileUtils.closeFileSystem(fs, provider);
        return missingData;
    }

    private static boolean shouldUnpackArchive(IContentProvider provider, boolean preLoadAssets, boolean preLoadData)
    {
        return provider.isArchive() && (preLoadAssets || preLoadData || shouldUpdateAliasMappingFile(ID_ALIAS_FILE, provider, DynamicReference.getAliasMapping(shortnameReferences.get(provider))));
    }

    private static void compileJavaModelsIfNeeded(IContentProvider provider)
    {
        if (FMLEnvironment.dist != Dist.CLIENT)
            return;

        try
        {
            boolean hasOutdatedJavaModels = JavaModelCompiler.hasOutdatedJavaModels(provider);
            if (!hasOutdatedJavaModels)
                return;

            if (!JavaModelCompiler.isCompilerAvailable())
            {
                FlansMod.log.warn("Found Java model sources in content pack '{}', but no Java compiler is available. Run Minecraft with a JDK to compile pack model sources automatically.", provider.getName());
                return;
            }

            JavaModelCompiler.compileJavaModels(provider);
        }
        catch (LinkageError e)
        {
            FlansMod.log.warn("Java model source compilation is unavailable for content pack '{}': {}", provider.getName(), e.toString());
        }
    }

    private static void createItemAndBlockJsonFiles(IContentProvider provider)
    {
        Path jsonBlockstatesFolderPath = provider.getAssetsPath().resolve(FOLDER_BLOCKSTATES);
        Path jsonModelsFolderPath = provider.getAssetsPath().resolve(FOLDER_MODELS);
        Path jsonItemModelsFolderPath = jsonModelsFolderPath.resolve(FOLDER_MODELS_ITEM);
        Path jsonBlockModelsFolderPath = jsonModelsFolderPath.resolve(FOLDER_MODELS_BLOCK);

        convertExistingJsonFiles(jsonBlockstatesFolderPath);
        convertExistingJsonFiles(jsonModelsFolderPath);

        boolean blockstatesExist = FileUtils.tryCreateDirectories(jsonBlockstatesFolderPath);
        boolean blockModelsExist = FileUtils.tryCreateDirectories(jsonBlockModelsFolderPath);
        boolean itemModelsExist = FileUtils.tryCreateDirectories(jsonItemModelsFolderPath);

        if (itemModelsExist)
        {
            for (InfoType config : listItems(provider))
            {
                generateItemModelJson(config, jsonItemModelsFolderPath);
            }
        }

        if (blockstatesExist || blockModelsExist)
        {
            for (InfoType config : listBlocks(provider))
            {
                if (blockstatesExist)
                    generateBlockstateJson(config, jsonBlockstatesFolderPath);
                if (blockModelsExist)
                    generateBlockModelJson(config, jsonBlockModelsFolderPath);
            }
        }
    }

    private static void createRecipeJsonFiles(IContentProvider provider)
    {
        Path recipeFolderPath = provider.getDataPath().resolve(FOLDER_RECIPES);
        for (InfoType config : listItems(provider))
        {
            RecipeJsonGenerator.writeRecipes(config, recipeFolderPath);
        }
    }

    private static boolean isMissingGeneratedRecipeFiles(IContentProvider provider, FileSystem fs)
    {
        Path recipeFolderPath = provider.getDataPath(fs).resolve(FOLDER_RECIPES);
        for (InfoType config : listItems(provider))
        {
            for (String recipeFileName : RecipeJsonGenerator.getRecipeFileNames(config))
            {
                if (!Files.exists(recipeFolderPath.resolve(recipeFileName)))
                    return true;
            }
        }
        return false;
    }

    private static void convertExistingJsonFiles(Path jsonFolderPath)
    {
        if (!Files.isDirectory(jsonFolderPath))
            return;

        try (Stream<Path> walk = Files.walk(jsonFolderPath))
        {
            walk.filter(p -> Files.isRegularFile(p) && p.toString().endsWith(FileUtils.JSON_EXTENSION))
                .forEach(ContentManager::processJsonItemFile);
        }
        catch (IOException e)
        {
            FlansMod.log.error("Could not open {}", jsonFolderPath, e);
        }
    }

    private static void processJsonItemFile(Path jsonFile)
    {
        try
        {
            // 1) Rename the file itself to lowercase (safe even on case-insensitive FS)
            jsonFile = FileUtils.renameToLowercase(jsonFile);

            // 2) Lowercase the content (OK for blockstates/models only)
            String content = Files.readString(jsonFile, StandardCharsets.UTF_8);
            String modified = content
                    .replace("flansmod:items/", "flansmod:item/")
                    .toLowerCase(Locale.ROOT);
            if (!modified.equals(content))
            {
                Files.writeString(jsonFile, modified, StandardCharsets.UTF_8);
            }
        }
        catch (IOException e)
        {
            FlansMod.log.error("Failed to process file: {}", jsonFile, e);
        }
    }

    @Unmodifiable
    private static List<InfoType> listItems(IContentProvider provider)
    {
        return configs.get(provider).stream()
            .filter(config -> config.getType().isHasItem())
            .toList();
    }

    @Unmodifiable
    private static List<InfoType> listBlocks(IContentProvider provider)
    {
        return configs.get(provider).stream()
            .filter(config -> config.getType().isHasBlock())
            .toList();
    }

    private static void generateItemModelJson(InfoType config, Path outputFolder)
    {
        ResourceUtils.ModelJson model = ResourceUtils.ModelJson.createItemModel(config);
        String jsonContent = gson.toJson(model);
        String shortName = config.getShortName();

        if (!shortName.equals(config.getOriginalShortName()))
        {
            Path oldFile = outputFolder.resolve(config.getOriginalShortName() + FileUtils.JSON_EXTENSION);
            FileUtils.deleteIfExists(oldFile);
        }

        Path outputFile = outputFolder.resolve(shortName + FileUtils.JSON_EXTENSION);
        FileUtils.writeString(outputFile, jsonContent);

        if (config instanceof PaintableType paintableType)
        {
            for (Paintjob p : paintableType.getPaintjobs().values())
            {
                if (!p.equals(paintableType.getDefaultPaintjob()))
                {
                    outputFile = outputFolder.resolve(p.getIcon() + FileUtils.JSON_EXTENSION);
                    model = ResourceUtils.ModelJson.createItemModel(config, p);
                    jsonContent = gson.toJson(model);
                    FileUtils.writeString(outputFile, jsonContent);
                }
            }
        }
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

        String jsonContent = gson.toJson(model);
        String shortName = config.getShortName();

        if (!shortName.equals(config.getOriginalShortName()))
        {
            Path oldFile = outputFolder.resolve(config.getOriginalShortName() + FileUtils.JSON_EXTENSION);
            FileUtils.deleteIfExists(oldFile);
        }

        Path outputFile = outputFolder.resolve(shortName + FileUtils.JSON_EXTENSION);
        FileUtils.writeString(outputFile, jsonContent);
    }

    private static void generateBlockstateJson(InfoType config, Path outputFolder)
    {
        ResourceUtils.BlockStateJson model = ResourceUtils.BlockStateJson.create(config);
        String jsonContent = gson.toJson(model);
        String shortName = config.getShortName();

        if (!shortName.equals(config.getOriginalShortName()))
        {
            Path oldFile = outputFolder.resolve(config.getOriginalShortName() + FileUtils.JSON_EXTENSION);
            FileUtils.deleteIfExists(oldFile);
        }

        Path outputFile = outputFolder.resolve(shortName + FileUtils.JSON_EXTENSION);
        FileUtils.writeString(outputFile, jsonContent);
    }

    private static void copyItemIcons(IContentProvider provider)
    {
        Path sourcePath = provider.getAssetsPath().resolve(FOLDER_TEXTURES).resolve(FOLDER_TEXTURES_ITEMS);
        Path destPath = provider.getAssetsPath().resolve(FOLDER_TEXTURES).resolve(FOLDER_TEXTURES_ITEM);
        copyPngFilesAndLowercaseFileNames(sourcePath, destPath);
    }

    private static void copyBlockTextures(IContentProvider provider)
    {
        Path sourcePath = provider.getAssetsPath().resolve(FOLDER_TEXTURES).resolve(FOLDER_TEXTURES_BLOCKS);
        Path destPath = provider.getAssetsPath().resolve(FOLDER_TEXTURES).resolve(FOLDER_TEXTURES_BLOCK);
        copyPngFilesAndLowercaseFileNames(sourcePath, destPath);
    }

    private static void copyTextures(IContentProvider provider, String folderName, Map<String, DynamicReference> aliasMapping)
    {
        Path sourcePath = provider.getAssetsPath().resolve(folderName);
        Path destPath = provider.getAssetsPath().resolve(FOLDER_TEXTURES).resolve(folderName);
        copyPngFilesAndLowercaseFileNames(sourcePath, destPath, aliasMapping, folderName.equals(FOLDER_TEXTURES_ARMOR));
    }

    private static void createLocalization(IContentProvider provider)
    {
        Path langDir = provider.getAssetsPath().resolve(FOLDER_LANG);

        if (!FileUtils.tryCreateDirectories(langDir))
            return;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(langDir, "*.lang"))
        {
            for (Path langFile : stream)
            {
                generateLocalizationFile(provider, langFile);
            }
        }
        catch (IOException e)
        {
            FlansMod.log.error("Failed to read localization files in {}", langDir, e);
        }
    }

    private static void generateLocalizationFile(IContentProvider provider, Path langFile)
    {
        Map<String, String> translations = readLangFile(langFile);
        for (InfoType config : configs.get(provider))
        {
            String shortName = config.getShortName();
            if (!shortName.equals(config.getOriginalShortName()))
            {
                String keyToAdd = generateTranslationKey(shortName, config.getType().isHasBlock());
                String keyToRemove = generateTranslationKey(config.getOriginalShortName(), config.getType().isHasBlock());
                translations.putIfAbsent(keyToAdd, config.getName());
                translations.remove(keyToRemove);
            }
            else
            {
                translations.putIfAbsent(generateTranslationKey(shortName, config.getType().isHasBlock()), config.getName());
            }
        }

        String jsonFileName = langFile.getFileName().toString().toLowerCase(Locale.ROOT).replace(FileUtils.LANG_EXTENSION, FileUtils.JSON_EXTENSION);
        Path jsonPath = langFile.getParent().resolve(jsonFileName);

        try (Writer writer = Files.newBufferedWriter(jsonPath, StandardCharsets.UTF_8))
        {
            gson.toJson(translations, writer);
        }
        catch (IOException e)
        {
            FlansMod.log.error("Failed to write to localization file {}", jsonPath, e);
        }
    }

    private static Map<String, String> readLangFile(Path langFile)
    {
        Map<String, String> translations = new LinkedHashMap<>();
        try
        {
            for (String line : readLinesUtf8OrUtf16(langFile))
            {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || line.indexOf('=') < 0)
                    continue;

                String key = line.substring(0, line.indexOf('=')).trim();
                String value = line.substring(line.indexOf('=') + 1).trim();

                // Convert key to new format
                key = convertTranslationKey(key);

                // Unescape properties-style characters
                value = value.replace("\\n", "\n").replace("\\\"", "\"");

                translations.put(key, value);
            }
        }
        catch (Exception e)
        {
            FlansMod.log.error("Failed to read localization file {}", langFile, e);
        }
        return translations;
    }

    private static List<String> readLinesUtf8OrUtf16(Path file) throws IOException {
        List<String> lines;
        try
        {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            stripBomIfPresent(lines);
        }
        catch (MalformedInputException ex)
        {
            // UTF-8 failed: try UTF-16
            lines = Files.readAllLines(file, StandardCharsets.UTF_16);
            stripBomIfPresent(lines);
        }
        return lines;
    }

    private static String convertTranslationKey(String legacyKey)
    {
        if (legacyKey.startsWith(TRANSLATION_KEY_PREFIX_ITEM) && legacyKey.endsWith(TRANSLATION_KEY_SUFFIX_NAME)) {
            String id = legacyKey.substring(5, legacyKey.length() - 5).toLowerCase(Locale.ROOT);
            return TRANSLATION_KEY_PREFIX_ITEM + FlansMod.FLANSMOD_ID + "." + id;
        }
        if ((legacyKey.startsWith(TRANSLATION_KEY_PREFIX_TYPE) || legacyKey.startsWith(TRANSLATION_KEY_PREFIX_BLOCK)) && legacyKey.endsWith(TRANSLATION_KEY_SUFFIX_NAME)) {
            String id = legacyKey.substring(legacyKey.indexOf('.') + 1, legacyKey.length() - 5).toLowerCase(Locale.ROOT);
            return TRANSLATION_KEY_PREFIX_BLOCK + FlansMod.FLANSMOD_ID + "." + id;
        }
        return legacyKey;
    }

    private static String generateTranslationKey(String itemId, boolean isBlock)
    {
        return (isBlock ? TRANSLATION_KEY_PREFIX_BLOCK : TRANSLATION_KEY_PREFIX_ITEM) + FlansMod.FLANSMOD_ID + "." + itemId;
    }

    private static void copyPngFilesAndLowercaseFileNames(Path sourcePath, Path destPath)
    {
        copyPngFilesAndLowercaseFileNames(sourcePath, destPath, Collections.emptyMap(), false);
    }

    private static void copyPngFilesAndLowercaseFileNames(Path sourcePath, Path destPath, Map<String, DynamicReference> aliasMapping, boolean armorTextureFolder)
    {
        if (!Files.exists(sourcePath))
        {
            cleanupGeneratedTexturesWhenSourceMissing(destPath);
            return;
        }
        if (!FileUtils.tryCreateDirectories(destPath))
            return;

        List<Path> sourceFiles = listSourcePngFiles(sourcePath);
        if (sourceFiles == null)
            return;

        Set<String> previousGeneratedTextures = readGeneratedTexturesManifest(destPath);
        Map<String, Path> textureCopyPlan = createTextureCopyPlan(sourcePath, destPath, sourceFiles, aliasMapping, armorTextureFolder, previousGeneratedTextures);

        cleanupStaleGeneratedTextures(destPath, previousGeneratedTextures, textureCopyPlan.keySet());
        copyGeneratedTextures(destPath, textureCopyPlan);
        cleanupUntrackedDuplicateGeneratedTextures(destPath, textureCopyPlan);
        writeGeneratedTexturesManifest(destPath, textureCopyPlan.keySet());
    }

    @Nullable
    private static List<Path> listSourcePngFiles(Path sourcePath)
    {
        try (Stream<Path> paths = Files.walk(sourcePath, 1))
        {
            return paths.filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(FileUtils.PNG_EXTENSION))
                .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)))
                .toList();
        }
        catch (IOException e)
        {
            FlansMod.log.error("Could not walk source texture folder '{}' while planning generated PNG files", sourcePath, e);
            return null;
        }
    }

    private static Map<String, Path> createTextureCopyPlan(Path sourcePath, Path destPath, List<Path> sourceFiles, Map<String, DynamicReference> aliasMapping, boolean armorTextureFolder, Set<String> previousGeneratedTextures)
    {
        Map<String, Path> textureCopyPlan = new LinkedHashMap<>();
        boolean overwriteExistingConflicts = !aliasMapping.isEmpty();
        for (Path sourceFile : sourceFiles)
        {
            String desiredFileName = getGeneratedTextureFileName(sourcePath, sourceFile, aliasMapping, armorTextureFolder);
            getAvailableGeneratedTextureFileName(sourceFile, desiredFileName, destPath, previousGeneratedTextures, textureCopyPlan, overwriteExistingConflicts)
                .ifPresent(fileName -> textureCopyPlan.put(fileName, sourceFile));
        }
        return textureCopyPlan;
    }

    private static Set<String> createTextureNamePlan(Path sourcePath, Path destPath, List<Path> sourceFiles, Map<String, DynamicReference> aliasMapping, boolean armorTextureFolder, Set<String> previousGeneratedTextures)
    {
        Set<String> textureNamePlan = new LinkedHashSet<>();
        boolean overwriteExistingConflicts = !aliasMapping.isEmpty();
        for (Path sourceFile : sourceFiles)
        {
            String desiredFileName = getGeneratedTextureFileName(sourcePath, sourceFile, aliasMapping, armorTextureFolder);
            getAvailableGeneratedTextureFileNameByName(desiredFileName, destPath, previousGeneratedTextures, textureNamePlan, overwriteExistingConflicts)
                .ifPresent(textureNamePlan::add);
        }
        return textureNamePlan;
    }

    private static String getGeneratedTextureFileName(Path sourcePath, Path sourceFile, Map<String, DynamicReference> aliasMapping, boolean armorTextureFolder)
    {
        if (aliasMapping.isEmpty())
            return FileUtils.sanitizePngRelPath(sourcePath.relativize(sourceFile));

        String sourceFileName = sourceFile.getFileName().toString();
        String baseFileName = FilenameUtils.getBaseName(sourceFileName);
        String suffix = StringUtils.EMPTY;

        if (armorTextureFolder)
        {
            String lowerFileName = sourceFileName.toLowerCase(Locale.ROOT);
            if (lowerFileName.endsWith("_1" + FileUtils.PNG_EXTENSION))
                suffix = "_1";
            else if (lowerFileName.endsWith("_2" + FileUtils.PNG_EXTENSION))
                suffix = "_2";
            baseFileName = getArmorTextureBaseName(baseFileName);
        }

        String sanitizedBaseFileName = ResourceUtils.sanitize(baseFileName);
        DynamicReference ref = aliasMapping.get(sanitizedBaseFileName);
        String generatedBaseFileName = ref != null ? ref.get() : sanitizedBaseFileName;
        return ResourceUtils.sanitize(generatedBaseFileName) + suffix + FileUtils.PNG_EXTENSION;
    }

    private static Optional<String> getAvailableGeneratedTextureFileName(Path sourceFile, String desiredFileName, Path destPath, Set<String> previousGeneratedTextures, Map<String, Path> textureCopyPlan, boolean overwriteExistingConflicts)
    {
        String candidateFileName = desiredFileName;
        for (int suffix = 1; ; suffix++)
        {
            Path plannedSource = textureCopyPlan.get(candidateFileName);
            if (plannedSource != null)
            {
                if (!FileUtils.isDifferentFileBytes(sourceFile, plannedSource, false))
                    return Optional.empty();
            }
            else if (!isReservedTextureFileConflict(sourceFile, destPath.resolve(candidateFileName), candidateFileName, previousGeneratedTextures, overwriteExistingConflicts))
            {
                return Optional.of(candidateFileName);
            }

            candidateFileName = addGeneratedTextureSuffix(desiredFileName, suffix);
        }
    }

    private static Optional<String> getAvailableGeneratedTextureFileNameByName(String desiredFileName, Path destPath, Set<String> previousGeneratedTextures, Set<String> textureNamePlan, boolean overwriteExistingConflicts)
    {
        if (textureNamePlan.contains(desiredFileName))
            return findPreviouslyGeneratedDuplicateTextureName(desiredFileName, previousGeneratedTextures, textureNamePlan);

        String candidateFileName = desiredFileName;
        for (int suffix = 1; ; suffix++)
        {
            if (!textureNamePlan.contains(candidateFileName)
                && !isReservedTextureFileNameConflict(destPath.resolve(candidateFileName), candidateFileName, previousGeneratedTextures, overwriteExistingConflicts))
                return Optional.of(candidateFileName);

            candidateFileName = addGeneratedTextureSuffix(desiredFileName, suffix);
        }
    }

    private static Optional<String> findPreviouslyGeneratedDuplicateTextureName(String desiredFileName, Set<String> previousGeneratedTextures, Set<String> textureNamePlan)
    {
        String desiredBaseName = FilenameUtils.getBaseName(desiredFileName);
        return previousGeneratedTextures.stream()
            .filter(fileName -> !textureNamePlan.contains(fileName))
            .filter(fileName -> getGeneratedTextureSuffix(fileName, desiredBaseName) != Integer.MAX_VALUE)
            .min(Comparator.comparingInt(fileName -> getGeneratedTextureSuffix(fileName, desiredBaseName)));
    }

    private static int getGeneratedTextureSuffix(String fileName, String desiredBaseName)
    {
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(FileUtils.PNG_EXTENSION))
            return Integer.MAX_VALUE;

        String candidateBaseName = FilenameUtils.getBaseName(fileName);
        String suffixPrefix = desiredBaseName + "-";
        if (!candidateBaseName.startsWith(suffixPrefix))
            return Integer.MAX_VALUE;

        try
        {
            int suffix = Integer.parseInt(candidateBaseName.substring(suffixPrefix.length()));
            return suffix > 0 ? suffix : Integer.MAX_VALUE;
        }
        catch (NumberFormatException e)
        {
            return Integer.MAX_VALUE;
        }
    }

    private static boolean isReservedTextureFileConflict(Path sourceFile, Path candidateFile, String candidateFileName, Set<String> previousGeneratedTextures, boolean overwriteExistingConflicts)
    {
        return !overwriteExistingConflicts
            && Files.exists(candidateFile)
            && !previousGeneratedTextures.contains(candidateFileName)
            && FileUtils.isDifferentFileBytes(sourceFile, candidateFile, false);
    }

    private static boolean isReservedTextureFileNameConflict(Path candidateFile, String candidateFileName, Set<String> previousGeneratedTextures, boolean overwriteExistingConflicts)
    {
        return !overwriteExistingConflicts
            && Files.exists(candidateFile)
            && !previousGeneratedTextures.contains(candidateFileName);
    }

    private static String addGeneratedTextureSuffix(String fileName, int suffix)
    {
        return FilenameUtils.getBaseName(fileName) + "-" + suffix + FileUtils.PNG_EXTENSION;
    }

    private static void cleanupGeneratedTexturesWhenSourceMissing(Path destPath)
    {
        Set<String> previousGeneratedTextures = readGeneratedTexturesManifest(destPath);
        cleanupStaleGeneratedTextures(destPath, previousGeneratedTextures, Collections.emptySet());
        writeGeneratedTexturesManifest(destPath, Collections.emptySet());
    }

    private static void cleanupStaleGeneratedTextures(Path destPath, Set<String> previousGeneratedTextures, Set<String> currentGeneratedTextures)
    {
        for (String fileName : previousGeneratedTextures)
        {
            if (!currentGeneratedTextures.contains(fileName))
                FileUtils.deleteIfExists(destPath.resolve(fileName));
        }
    }

    private static void copyGeneratedTextures(Path destPath, Map<String, Path> textureCopyPlan)
    {
        for (Map.Entry<String, Path> entry : textureCopyPlan.entrySet())
        {
            Path sourceFile = entry.getValue();
            Path destFile = destPath.resolve(entry.getKey()).normalize();
            if (FileUtils.tryCreateDirectories(destFile.getParent()))
            {
                try
                {
                    if (!Files.exists(destFile) || FileUtils.isDifferentFileBytes(sourceFile, destFile, false))
                        Files.copy(sourceFile, destFile, StandardCopyOption.REPLACE_EXISTING);
                }
                catch (IOException e)
                {
                    FlansMod.log.error("Could not copy {} to {}", sourceFile, destFile, e);
                }
            }
        }
    }

    private static void cleanupUntrackedDuplicateGeneratedTextures(Path destPath, Map<String, Path> textureCopyPlan)
    {
        findUntrackedDuplicateGeneratedTextures(destPath, textureCopyPlan).forEach(FileUtils::deleteIfExists);
    }

    private static List<Path> findUntrackedDuplicateGeneratedTextures(Path destPath, Map<String, Path> textureCopyPlan)
    {
        if (textureCopyPlan.isEmpty() || !Files.isDirectory(destPath))
            return Collections.emptyList();

        Map<Long, Set<String>> generatedSourceHashesBySize = createFileContentHashesBySize(textureCopyPlan.values());
        if (generatedSourceHashesBySize.isEmpty())
            return Collections.emptyList();

        try (Stream<Path> files = Files.list(destPath))
        {
            return files.filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(FileUtils.PNG_EXTENSION))
                .filter(path -> !textureCopyPlan.containsKey(path.getFileName().toString()))
                .filter(path -> hasMatchingFileContentSignature(path, generatedSourceHashesBySize))
                .toList();
        }
        catch (IOException e)
        {
            FlansMod.log.error("Could not scan generated texture folder '{}' while cleaning stale generated texture copies", destPath, e);
            return Collections.emptyList();
        }
    }

    private static Map<Long, Set<String>> createFileContentHashesBySize(Iterable<Path> files)
    {
        Map<Long, Set<String>> hashesBySize = new HashMap<>();
        files.forEach(file -> readFileContentSignature(file)
            .ifPresent(signature -> hashesBySize.computeIfAbsent(signature.size(), ignored -> new HashSet<>()).add(signature.sha256())));
        return hashesBySize;
    }

    private static boolean hasMatchingFileContentSignature(Path file, Map<Long, Set<String>> hashesBySize)
    {
        try
        {
            Set<String> sourceHashes = hashesBySize.get(Files.size(file));
            if (sourceHashes == null)
                return false;

            return readFileSha256(file)
                .map(sourceHashes::contains)
                .orElse(false);
        }
        catch (IOException e)
        {
            FlansMod.log.warn("Could not inspect generated texture candidate '{}': {}", file, e.toString());
            return false;
        }
    }

    private static Optional<FileContentSignature> readFileContentSignature(Path file)
    {
        try
        {
            long size = Files.size(file);
            return readFileSha256(file).map(sha256 -> new FileContentSignature(size, sha256));
        }
        catch (IOException e)
        {
            FlansMod.log.warn("Could not hash generated texture candidate '{}': {}", file, e.toString());
            return Optional.empty();
        }
    }

    private static Optional<String> readFileSha256(Path file)
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            try (InputStream input = Files.newInputStream(file))
            {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) >= 0)
                    digest.update(buffer, 0, read);
            }

            return Optional.of(HexFormat.of().formatHex(digest.digest()));
        }
        catch (IOException e)
        {
            FlansMod.log.warn("Could not hash generated texture candidate '{}': {}", file, e.toString());
            return Optional.empty();
        }
        catch (NoSuchAlgorithmException e)
        {
            FlansMod.log.error("SHA-256 is not available for generated texture cleanup.", e);
            return Optional.empty();
        }
    }

    private static Set<String> readGeneratedTexturesManifest(Path destPath)
    {
        Path manifestFile = destPath.resolve(GENERATED_TEXTURES_MANIFEST_FILE);
        Set<String> generatedTextures = new HashSet<>();
        if (!Files.isRegularFile(manifestFile))
            return generatedTextures;

        try
        {
            String[] fileNames = gson.fromJson(Files.readString(manifestFile, StandardCharsets.UTF_8), String[].class);
            if (fileNames != null)
                Collections.addAll(generatedTextures, fileNames);
        }
        catch (Exception e)
        {
            FlansMod.log.warn("Could not read generated texture manifest '{}': {}", manifestFile, e.toString());
        }
        return generatedTextures;
    }

    private static void writeGeneratedTexturesManifest(Path destPath, Set<String> generatedTextures)
    {
        Path manifestFile = destPath.resolve(GENERATED_TEXTURES_MANIFEST_FILE);
        if (generatedTextures.isEmpty())
        {
            FileUtils.deleteIfExists(manifestFile);
            return;
        }

        List<String> sortedGeneratedTextures = generatedTextures.stream().sorted().toList();
        FileUtils.writeString(manifestFile, gson.toJson(sortedGeneratedTextures));
    }

    private static void createSounds(IContentProvider provider)
    {
        Path assetsDir = provider.getAssetsPath();
        Path soundDir = assetsDir.resolve(FOLDER_SOUND);
        Path soundsDir = assetsDir.resolve(FOLDER_SOUNDS);
        copyLegacySoundFolder(soundDir, soundsDir);
        normalizeSoundsFolder(soundsDir);

        Path soundsJsonFile = provider.getAssetsPath().resolve("sounds.json");
        if (Files.isRegularFile(soundsJsonFile))
            SoundJsonProcessor.process(soundsJsonFile, FlansMod.FLANSMOD_ID, soundsDir);
    }

    private static void copyLegacySoundFolder(Path soundDir, Path soundsDir)
    {
        if (!Files.isDirectory(soundDir))
            return;
        if (!FileUtils.tryCreateDirectories(soundsDir))
            return;

        List<Path> sourceFiles;
        try (Stream<Path> stream = Files.walk(soundDir))
        {
            sourceFiles = stream.filter(Files::isRegularFile)
                .filter(FileUtils::isOgg)
                .sorted(Comparator.comparing(path -> soundDir.relativize(path).toString().toLowerCase(Locale.ROOT)))
                .toList();
        }
        catch (IOException e)
        {
            FlansMod.log.error("Could not scan legacy sound folder {}", soundDir, e);
            return;
        }

        Set<String> existingSounds = collectExistingSoundNames(soundsDir);
        for (Path source : sourceFiles)
            copyLegacySound(source, soundDir, soundsDir, existingSounds);
    }

    private static Set<String> collectExistingSoundNames(Path soundsDir)
    {
        if (!Files.isDirectory(soundsDir))
            return new HashSet<>();

        try (Stream<Path> stream = Files.walk(soundsDir))
        {
            return stream.filter(Files::isRegularFile)
                .filter(FileUtils::isOgg)
                .map(path -> soundNameKey(normalizeSoundRelativePath(soundsDir.relativize(path))))
                .collect(Collectors.toCollection(HashSet::new));
        }
        catch (IOException e)
        {
            FlansMod.log.error("Could not scan sounds folder {}", soundsDir, e);
            return new HashSet<>();
        }
    }

    private static void copyLegacySound(Path source, Path soundDir, Path soundsDir, Set<String> existingSounds)
    {
        Path relativePath = normalizeSoundRelativePath(soundDir.relativize(source));
        String soundNameKey = soundNameKey(relativePath);
        if (existingSounds.contains(soundNameKey))
            return;

        Path target = soundsDir.resolve(relativePath).normalize();
        if (!target.startsWith(soundsDir))
            return;

        try
        {
            Files.createDirectories(target.getParent());
            if (Files.notExists(target))
            {
                Files.copy(source, target);
                existingSounds.add(soundNameKey);
            }
        }
        catch (IOException e)
        {
            FlansMod.log.error("Could not copy legacy sound {} to {}", source, target, e);
        }
    }

    private static void normalizeSoundsFolder(Path soundsDir)
    {
        if (!Files.isDirectory(soundsDir))
            return;

        List<Path> soundFiles;
        try (Stream<Path> stream = Files.walk(soundsDir))
        {
            soundFiles = stream.filter(Files::isRegularFile)
                .sorted(Comparator.comparing((Path path) -> soundsDir.relativize(path).getNameCount()).reversed())
                .toList();
        }
        catch (IOException e)
        {
            FlansMod.log.error("Could not scan sounds folder {}", soundsDir, e);
            return;
        }

        for (Path soundFile : soundFiles)
            normalizeSoundFile(soundsDir, soundFile);
        deleteEmptySubdirectories(soundsDir);
        normalizeSoundDirectories(soundsDir);
        deleteEmptySubdirectories(soundsDir);
    }

    private static void normalizeSoundFile(Path soundsDir, Path source)
    {
        Path relativePath = soundsDir.relativize(source);
        Path normalizedRelativePath = normalizeSoundRelativePath(relativePath);
        if (relativePath.toString().equals(normalizedRelativePath.toString()))
            return;

        Path target = soundsDir.resolve(normalizedRelativePath).normalize();
        if (!target.startsWith(soundsDir))
            return;

        try
        {
            Files.createDirectories(target.getParent());
            Path finalTarget = resolveSoundNormalizeTarget(source, target);
            if (finalTarget == null)
            {
                Files.delete(source);
                return;
            }
            FileUtils.moveWithCaseOnlyHopIfNeeded(source, finalTarget);
        }
        catch (IOException e)
        {
            FlansMod.log.error("Could not normalize sound file {}", source, e);
        }
    }

    @Nullable
    private static Path resolveSoundNormalizeTarget(Path source, Path target) throws IOException
    {
        if (Files.exists(target))
        {
            if (Files.isSameFile(source, target))
                return target;
            if (!FileUtils.isDifferentFileContent(source, target, false))
                return null;
            return FileUtils.ensureUnique(target);
        }
        return target;
    }

    private static Path normalizeSoundRelativePath(Path relativePath)
    {
        Path normalizedPath = null;
        int count = relativePath.getNameCount();
        for (int i = 0; i < count; i++)
        {
            String segment = relativePath.getName(i).toString();
            String normalizedSegment = (i == count - 1) ? normalizeSoundFileName(segment) : ResourceUtils.sanitize(segment);
            normalizedPath = normalizedPath == null ? Path.of(normalizedSegment) : normalizedPath.resolve(normalizedSegment);
        }
        return normalizedPath == null ? Path.of(StringUtils.EMPTY) : normalizedPath;
    }

    private static String normalizeSoundFileName(String fileName)
    {
        int dot = fileName.lastIndexOf('.');
        if (dot >= 0 && fileName.substring(dot).equalsIgnoreCase(FileUtils.OGG_EXTENSION))
            return ResourceUtils.sanitize(fileName.substring(0, dot)) + FileUtils.OGG_EXTENSION;
        return ResourceUtils.sanitize(fileName);
    }

    private static String soundNameKey(Path relativePath)
    {
        return relativePath.toString().replace('\\', '/').toLowerCase(Locale.ROOT);
    }

    private static void normalizeSoundDirectories(Path root)
    {
        List<Path> soundDirectories;
        try (Stream<Path> stream = Files.walk(root))
        {
            soundDirectories = stream.filter(Files::isDirectory)
                .filter(path -> !path.equals(root))
                .sorted(Comparator.comparing((Path path) -> root.relativize(path).getNameCount()).reversed())
                .toList();
        }
        catch (IOException e)
        {
            FlansMod.log.error("Could not scan sound directories under {}", root, e);
            return;
        }

        for (Path directory : soundDirectories)
            normalizeSoundDirectory(directory);
    }

    private static void normalizeSoundDirectory(Path source)
    {
        if (!Files.isDirectory(source) || source.getParent() == null)
            return;

        String normalizedName = ResourceUtils.sanitize(source.getFileName().toString());
        if (StringUtils.isBlank(normalizedName))
            return;

        Path target = source.resolveSibling(normalizedName).normalize();
        if (source.getFileName().toString().equals(normalizedName))
            return;

        try
        {
            if (Files.exists(target))
            {
                if (Files.isSameFile(source, target))
                    FileUtils.moveWithCaseOnlyHopIfNeeded(source, target);
                else
                    FileUtils.deleteDirectoryIfEmpty(source);
            }
            else
            {
                FileUtils.moveWithCaseOnlyHopIfNeeded(source, target);
            }
        }
        catch (IOException e)
        {
            FlansMod.log.error("Could not normalize sound directory {}", source, e);
        }
    }

    private static void deleteEmptySubdirectories(Path root)
    {
        try (Stream<Path> stream = Files.walk(root))
        {
            stream.filter(Files::isDirectory)
                .filter(path -> !path.equals(root))
                .sorted(Comparator.reverseOrder())
                .forEach(FileUtils::deleteDirectoryIfEmpty);
        }
        catch (IOException e)
        {
            FlansMod.log.error("Could not clean empty sound directories under {}", root, e);
        }
    }

    private static void createMcMeta(IContentProvider provider) {
        Path mcMetaFile = (provider.isArchive() ? provider.getExtractedPath() : provider.getPath()).resolve("pack.mcmeta");
        if (Files.notExists(mcMetaFile))
        {
            try
            {
                Files.createFile(mcMetaFile);
                String content = String.format("""
                    {
                        "pack": {
                            "pack_format": 15,
                            "description": "%s"
                        }
                    }""", FilenameUtils.getBaseName(provider.getName()));
                Files.writeString(mcMetaFile, content);
            }
            catch (IOException e)
            {
                FlansMod.log.error("Failed to create {}", mcMetaFile, e);
            }
        }
    }

    private static boolean isTextureNameAlreadyRegistered(String name, String folderName, IContentProvider provider)
    {
        return textures.get(folderName).containsKey(name) && !textures.get(folderName).get(name).contentPack().equals(provider);
    }

    public static String getShortnameAliasInContentPack(String shortname, @Nullable IContentProvider provider)
    {
        if (provider != null) {
            DynamicReference ref = shortnameReferences.get(provider).get(shortname);
            if (ref != null) {
                return ref.get();
            }
        }
        return shortname;
    }
}
