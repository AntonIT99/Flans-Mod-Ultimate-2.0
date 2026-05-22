package com.flansmodultimate;

import com.flansmodultimate.common.block.BlockFactory;
import com.flansmodultimate.common.item.ItemFactory;
import com.flansmodultimate.common.paintjob.Paintjob;
import com.flansmodultimate.common.recipe.RecipeJsonGenerator;
import com.flansmodultimate.common.types.BlockType;
import com.flansmodultimate.common.types.EnumType;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.common.types.PaintableType;
import com.flansmodultimate.common.types.TypeFile;
import com.flansmodultimate.config.CategoryManager;
import com.flansmodultimate.config.ContentLoadingConfig;
import com.flansmodultimate.util.AliasFileManager;
import com.flansmodultimate.util.DynamicReference;
import com.flansmodultimate.util.FileUtils;
import com.flansmodultimate.util.LogUtils;
import com.flansmodultimate.util.ResourceUtils;
import com.flansmodultimate.util.SoundJsonProcessor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.MalformedInputException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
    private static final Map<IContentProvider, Map<String, DynamicReference>> shortnameReferences = new HashMap<>();
    private static final Map<IContentProvider, Map<String, DynamicReference>> armorTextureReferences = new HashMap<>();
    private static final Map<IContentProvider, Map<String, DynamicReference>> guiTextureReferences = new HashMap<>();
    private static final Map<IContentProvider, Map<String, DynamicReference>> skinsTextureReferences = new HashMap<>();
    private static final Map<IContentProvider, Map<String, DynamicReference>> modelReferences = new HashMap<>();

    private static final String ID_ALIAS_FILE = "id_alias.json";
    private static final String ARMOR_TEXTURES_ALIAS_FILE = "armor_textures_alias.json";
    private static final String GUI_TEXTURES_ALIAS_FILE = "gui_textures_alias.json";
    private static final String SKINS_TEXTURES_ALIAS_FILE = "skins_textures_alias.json";

    private static final List<IContentProvider> contentPacks = new ArrayList<>();
    private static final Map<IContentProvider, ArrayList<TypeFile>> files = new HashMap<>();
    private static final Map<IContentProvider, ArrayList<InfoType>> configs = new HashMap<>();

    // Keep track of registered items and loaded textures and models
    /** &lt; shortname, config file string representation &gt; */
    private static final Map<String, String> registeredItems = new HashMap<>();
    /** &lt; folder name, &lt;lowercase name, texture file &gt;&gt; */
    private static final Map<String, Map<String, TextureFile>> textures = new HashMap<>();
    /** &lt; model class name, &lt; contentPack &gt;&gt; */
    private static final Map<String, IContentProvider> registeredModels = new HashMap<>();

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private record TextureFile(String name, IContentProvider contentPack) {}

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
        Path tempRoot = flanFolder != null ? flanFolder.getParent().resolve(".flantemp") : null;
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

            if (archiveExtracted || preLoadAssets || !provider.isArchive())
            {
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

            if (archiveExtracted || (preLoadAssets && provider.isArchive()))
            {
                cleanEmptyRecipeFiles(provider);
                FileUtils.repackArchive(provider);
            }
            else if (!provider.isArchive())
            {
                cleanEmptyRecipeFiles(provider);
            }

            long endTime = System.currentTimeMillis();
            String loadingTimeMs = String.format("%,d", endTime - startTime);
            FlansMod.log.info("Loaded content pack {} in {} ms.", provider.getName(), loadingTimeMs);
        }

        FileUtils.deleteDirectoryIfEmpty(flanFolder.getParent().resolve(".flantemp"));
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
            List<String> lines = readAllLinesUtf8OrLatin1(file);
            stripBomIfPresent(lines);
            return new TypeFile(file.getFileName().toString(), EnumType.getType(folderName).orElse(null), provider, lines);
        }
        catch (IOException e)
        {
            FlansMod.log.error("Failed to read '{}/{}' in content pack '{}'", folderName, file.getFileName(), provider.getName(), e);
            return null;
        }
    }

    private static List<String> readAllLinesUtf8OrLatin1(Path file) throws IOException {
        try
        {
            return Files.readAllLines(file, StandardCharsets.UTF_8);
        }
        catch (Exception e)
        {
            try
            {
                return Files.readAllLines(file, Charset.forName("GBK"));
            }
            catch (Exception e2)
            {
                try
                {
                    return Files.readAllLines(file, Charset.forName("GB2312"));
                }
                catch (Exception e3)
                {
                    return Files.readAllLines(file, StandardCharsets.ISO_8859_1);
                }
            }
        }
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
                            configs.get(contentPack).add(config);
                        }
                    }
                    else
                    {
                        configs.get(contentPack).add(config);
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
                LogUtils.logWithoutStacktrace(e);
            }
        }
        files.clear();
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

        return provider.isArchive() || ContentLoadingConfig.isForceRegenContentPacksAssetsAndIds()
            || provider.isJarFile()
            || shouldUpdateAliasMappingFile(ID_ALIAS_FILE, provider, DynamicReference.getAliasMapping(shortnameReferences.get(provider)))
            || shouldUpdateAliasMappingFile(ARMOR_TEXTURES_ALIAS_FILE, provider, DynamicReference.getAliasMapping(armorTextureReferences.get(provider)))
            || shouldUpdateAliasMappingFile(GUI_TEXTURES_ALIAS_FILE, provider, DynamicReference.getAliasMapping(guiTextureReferences.get(provider)))
            || shouldUpdateAliasMappingFile(SKINS_TEXTURES_ALIAS_FILE, provider, DynamicReference.getAliasMapping(skinsTextureReferences.get(provider)))
            || isMissingGeneratedAssets(provider);
    }

    private static boolean isMissingGeneratedAssets(IContentProvider provider)
    {
        FileSystem fs = FileUtils.createFileSystem(provider);
        boolean missingAssets = !Files.exists(provider.getAssetsPath(fs).resolve(FOLDER_BLOCKSTATES))
            || !Files.exists(provider.getAssetsPath(fs).resolve(FOLDER_MODELS).resolve(FOLDER_MODELS_ITEM))
            || !Files.exists(provider.getAssetsPath(fs).resolve(FOLDER_MODELS).resolve(FOLDER_MODELS_BLOCK))
            || (!Files.exists(provider.getAssetsPath(fs).resolve(FOLDER_TEXTURES).resolve(FOLDER_TEXTURES_ITEM)) && Files.exists(provider.getAssetsPath(fs).resolve(FOLDER_TEXTURES).resolve(FOLDER_TEXTURES_ITEMS)))
            || (!Files.exists(provider.getAssetsPath(fs).resolve(FOLDER_TEXTURES).resolve(FOLDER_TEXTURES_BLOCK)) && Files.exists(provider.getAssetsPath(fs).resolve(FOLDER_TEXTURES).resolve(FOLDER_TEXTURES_BLOCKS)))
            || (!Files.exists(provider.getAssetsPath(fs).resolve(FOLDER_TEXTURES).resolve(FOLDER_TEXTURES_ARMOR)) && Files.exists(provider.getAssetsPath(fs).resolve(FOLDER_TEXTURES_ARMOR)))
            || (!Files.exists(provider.getAssetsPath(fs).resolve(FOLDER_TEXTURES).resolve(FOLDER_TEXTURES_GUI)) && Files.exists(provider.getAssetsPath(fs).resolve(FOLDER_TEXTURES_GUI)))
            || (!Files.exists(provider.getAssetsPath(fs).resolve(FOLDER_TEXTURES).resolve(FOLDER_TEXTURES_SKINS)) && Files.exists(provider.getAssetsPath(fs).resolve(FOLDER_TEXTURES_SKINS)))
            || !Files.exists(provider.getAssetsPath(fs).resolve(FOLDER_LANG))
            || !Files.exists(provider.getAssetsPath(fs).resolve(FOLDER_LANG).resolve("en_us.json"));

        if (!missingAssets)
        {
            try
            {
                Path mcMetaPath = fs.getPath("pack.mcmeta");
                if (Files.exists(mcMetaPath))
                {
                    String content = Files.readString(mcMetaPath);
                    com.google.gson.JsonObject json = new Gson().fromJson(content, com.google.gson.JsonObject.class);
                    int packFormat = json.getAsJsonObject("pack").get("pack_format").getAsInt();
                    if (packFormat < 48)
                        missingAssets = true;
                }
            }
            catch (Exception ignored) {}
        }

        FileUtils.closeFileSystem(fs, provider);
        return missingAssets;
    }

    private static boolean shouldPreLoadData(IContentProvider provider)
    {
        if (provider.isArchive() || ContentLoadingConfig.isForceRegenContentPacksAssetsAndIds()
            || provider.isJarFile()
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
                if (blockModelsExist && config instanceof BlockType blockConfig)
                    generateBlockModelJson(blockConfig, jsonBlockModelsFolderPath);
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
                Path recipeFile = recipeFolderPath.resolve(recipeFileName);
                if (!Files.exists(recipeFile))
                    return true;
                try
                {
                    String content = Files.readString(recipeFile).trim();
                    if (content.isEmpty() || content.equals("{}"))
                        return true;
                }
                catch (IOException ignored) {}
            }
        }
        return false;
    }

    private static void cleanEmptyRecipeFiles(IContentProvider provider)
    {
        Path recipeFolder = provider.getDataPath().resolve(FOLDER_RECIPES);
        if (!Files.isDirectory(recipeFolder))
            return;

        try (java.util.stream.Stream<Path> files = Files.list(recipeFolder))
        {
            files.filter(p -> p.toString().endsWith(FileUtils.JSON_EXTENSION))
                .forEach(p -> {
                    try
                    {
                        String content = Files.readString(p).trim();
                        if (content.equals("{}"))
                        {
                            Files.delete(p);
                            FlansMod.log.debug("Deleted empty recipe file: {}", p);
                        }
                    }
                    catch (IOException ignored) {}
                });
        }
        catch (IOException ignored) {}
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

    private static void generateBlockModelJson(BlockType config, Path outputFolder)
    {
        ResourceUtils.ModelJson model = ResourceUtils.ModelJson.createBlockModel(config);
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
        FileUtils.deleteRecursively(destPath);
        copyPngFilesAndLowercaseFileNames(sourcePath, destPath);
        rotateItemImagesMinus90(destPath);
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
        copyPngFilesAndLowercaseFileNames(sourcePath, destPath);
        renameTextureFilesWithAliases(destPath, aliasMapping);
    }

    private static void renameTextureFilesWithAliases(Path folder, Map<String, DynamicReference> aliasMapping)
    {
        if (Files.exists(folder))
        {
            try (Stream<Path> stream = Files.list(folder))
            {
                stream.filter(file ->
                    {
                        String baseFileName = FilenameUtils.getBaseName(file.getFileName().toString());
                        if (folder.getFileName().toString().equals(FOLDER_TEXTURES_ARMOR))
                        {
                            baseFileName = getArmorTextureBaseName(baseFileName);
                        }
                        return file.toString().toLowerCase(Locale.ROOT).endsWith(FileUtils.PNG_EXTENSION) && aliasMapping.containsKey(baseFileName);
                    })
                    .forEach(file ->
                    {
                        String baseFileName = FilenameUtils.getBaseName(file.getFileName().toString());
                        if (folder.getFileName().toString().equals(FOLDER_TEXTURES_ARMOR))
                        {
                            baseFileName = getArmorTextureBaseName(baseFileName);
                        }
                        String newFileName = aliasMapping.get(baseFileName).get();
                        if (folder.getFileName().toString().equals(FOLDER_TEXTURES_ARMOR))
                        {
                            if (file.getFileName().toString().endsWith("_1.png"))
                            {
                                newFileName += "_1";
                            }
                            else if (file.getFileName().toString().endsWith("_2.png"))
                            {
                                newFileName += "_2";
                            }
                        }
                        Path destFile = file.getParent().resolve(newFileName + FileUtils.PNG_EXTENSION);
                        try
                        {
                            Files.move(file, destFile, StandardCopyOption.REPLACE_EXISTING);
                        }
                        catch (IOException e)
                        {
                            FlansMod.log.error("Could not create {}", file, e);
                        }
                    });
            }
            catch (IOException e)
            {
                FlansMod.log.error("Could not scan generated texture folder '{}' while applying texture aliases", folder, e);
            }
        }
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
        if (!Files.exists(sourcePath))
            return;
        if (!FileUtils.tryCreateDirectories(destPath))
            return;

        try (Stream<Path> paths = Files.walk(sourcePath))
        {
            paths.filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(FileUtils.PNG_EXTENSION))
                .forEach(src -> {
                    Path rel = sourcePath.relativize(src);
                    String sanitizedRel = FileUtils.sanitizePngRelPath(rel);
                    Path dst = destPath.resolve(sanitizedRel).normalize();
                    if (FileUtils.tryCreateDirectories(dst.getParent()))
                    {
                        try
                        {
                            Path target = FileUtils.skipIfSameElseEnsureUnique(src, dst);
                            if (target != null)
                                Files.copy(src, target, StandardCopyOption.REPLACE_EXISTING);
                        }
                        catch (IOException e)
                        {
                            FlansMod.log.error("Could not copy {} to {}", src, dst, e);
                        }
                    }
                });
        }
        catch (IOException e)
        {
            FlansMod.log.error("Could not walk source texture folder '{}' while copying PNG files to '{}'", sourcePath, destPath, e);
        }
    }

    private static void rotateItemImagesMinus90(Path folder)
    {
        if (!Files.exists(folder))
            return;
        try (Stream<Path> stream = Files.walk(folder))
        {
            stream.filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(FileUtils.PNG_EXTENSION))
                .forEach(path -> {
                    try
                    {
                        BufferedImage image = ImageIO.read(path.toFile());
                        if (image == null)
                            return;
                        int w = image.getWidth();
                        int h = image.getHeight();
                        BufferedImage rotated = new BufferedImage(h, w, BufferedImage.TYPE_INT_ARGB);
                        for (int y = 0; y < h; y++)
                        {
                            for (int x = 0; x < w; x++)
                            {
                                rotated.setRGB(h - 1 - y, x, image.getRGB(x, y));
                            }
                        }
                        ImageIO.write(rotated, "png", path.toFile());
                    }
                    catch (IOException e)
                    {
                        FlansMod.log.error("Could not rotate image {}", path, e);
                    }
                });
        }
        catch (IOException e)
        {
            FlansMod.log.error("Could not walk generated item texture folder '{}' while rotating images", folder, e);
        }
    }

    private static void createSounds(IContentProvider provider)
    {
        Path soundsDir = provider.getAssetsPath().resolve(FOLDER_SOUNDS);
        Path soundsJsonFile = provider.getAssetsPath().resolve("sounds.json");

        if (Files.isDirectory(soundsDir))
        {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(soundsDir, Files::isRegularFile))
            {
                processSoundFiles(stream, soundsDir);
            }
            catch (IOException e)
            {
                FlansMod.log.error("Could not process {}", soundsDir, e);
            }
        }

        if (Files.isRegularFile(soundsJsonFile))
            SoundJsonProcessor.process(soundsJsonFile, FlansMod.FLANSMOD_ID, soundsDir);
    }

    private static void processSoundFiles(DirectoryStream<Path> stream, Path soundsDir)
    {
        for (Path src : stream)
        {
            if (!FileUtils.isOgg(src) || !FileUtils.needsRename(src))
                continue;

            try
            {
                Path target = soundsDir.resolve(ResourceUtils.sanitize(src.getFileName().toString())).normalize();
                Path finalTarget = FileUtils.skipIfSameElseEnsureUnique(src, target);
                if (finalTarget != null)
                    FileUtils.moveWithCaseOnlyHopIfNeeded(src, finalTarget);
            }
            catch (IOException e)
            {
                FlansMod.log.error("Could not rename {}", src, e);
            }
        }
    }

    private static void createMcMeta(IContentProvider provider) {
        Path mcMetaFolder = provider.isArchive() ? provider.getExtractedPath() : provider.getPath();
        Path mcMetaFile = mcMetaFolder.resolve("pack.mcmeta");
        try
        {
            FileUtils.tryCreateDirectories(mcMetaFolder);
            int packFormat = 48;
            String content = String.format("""
                {
                    "pack": {
                        "pack_format": %d,
                        "description": "%s"
                    }
                }""", packFormat, FilenameUtils.getBaseName(provider.getName()));
            Files.writeString(mcMetaFile, content);
        }
        catch (IOException e)
        {
            FlansMod.log.error("Failed to create {}", mcMetaFile, e);
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

    public static void cleanupTextureCache()
    {
        int totalCleaned = 0;
        for (Map<String, TextureFile> folderTextures : textures.values())
        {
            totalCleaned += folderTextures.size();
            folderTextures.clear();
        }
        
        shortnameReferences.values().forEach(Map::clear);
        armorTextureReferences.values().forEach(Map::clear);
        guiTextureReferences.values().forEach(Map::clear);
        skinsTextureReferences.values().forEach(Map::clear);
        modelReferences.values().forEach(Map::clear);
        
        FlansMod.log.info("Cleaned up {} texture cache entries", totalCleaned);
    }

    public static Map<IContentProvider, Map<String, DynamicReference>> getShortnameReferences() { return shortnameReferences; }
    public static Map<IContentProvider, Map<String, DynamicReference>> getModelReferences() { return modelReferences; }
    public static Map<String, IContentProvider> getRegisteredModels() { return registeredModels; }
    public static Map<IContentProvider, Map<String, DynamicReference>> getArmorTextureReferences() { return armorTextureReferences; }
    public static Map<IContentProvider, Map<String, DynamicReference>> getSkinsTextureReferences() { return skinsTextureReferences; }
    public static Map<IContentProvider, Map<String, DynamicReference>> getGuiTextureReferences() { return guiTextureReferences; }
}
