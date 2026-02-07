package com.flansmodultimate;

import com.flansmodultimate.common.item.ItemFactory;
import com.flansmodultimate.common.paintjob.Paintjob;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ContentManager
{
    public static final String TEXTURES_ARMOR_FOLDER = "armor";
    public static final String TEXTURES_GUI_FOLDER = "gui";
    public static final String TEXTURES_SKINS_FOLDER = "skins";

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

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private record TextureFile(String name, IContentProvider contentPack) {}

    static
    {
        textures.put(TEXTURES_ARMOR_FOLDER, new HashMap<>());
        textures.put(TEXTURES_GUI_FOLDER, new HashMap<>());
        textures.put(TEXTURES_SKINS_FOLDER, new HashMap<>());
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

            if (shouldUnpackArchive(provider, preLoadAssets))
            {
                FileUtils.extractArchive(provider.getPath(), provider.getExtractedPath());
                archiveExtracted = true;
            }

            if (archiveExtracted || !provider.isArchive())
            {
                createMcMeta(provider);
                writeToAliasMappingFile(ID_ALIAS_FILE, provider,DynamicReference.getAliasMapping(shortnameReferences.get(provider)));
            }

            // preLoadAssets -> archive extracted
            if (preLoadAssets)
            {
                writeToAliasMappingFile(ARMOR_TEXTURES_ALIAS_FILE, provider, DynamicReference.getAliasMapping(armorTextureReferences.get(provider)));
                writeToAliasMappingFile(GUI_TEXTURES_ALIAS_FILE, provider, DynamicReference.getAliasMapping(guiTextureReferences.get(provider)));
                writeToAliasMappingFile(SKINS_TEXTURES_ALIAS_FILE, provider, DynamicReference.getAliasMapping(skinsTextureReferences.get(provider)));
                createItemJsonFiles(provider);
                createLocalization(provider);
                copyItemIcons(provider);
                copyTextures(provider, TEXTURES_ARMOR_FOLDER, armorTextureReferences.get(provider));
                copyTextures(provider, TEXTURES_GUI_FOLDER, guiTextureReferences.get(provider));
                copyTextures(provider, TEXTURES_SKINS_FOLDER, skinsTextureReferences.get(provider));
                createSounds(provider);
            }

            if (archiveExtracted)
            {
                FileUtils.repackArchive(provider);
            }

            long endTime = System.currentTimeMillis();
            String loadingTimeMs = String.format("%,d", endTime - startTime);
            FlansMod.log.info("Loaded content pack {} in {} ms.", provider.getName(), loadingTimeMs);
        }
    }

    private static void loadFlanFolder()
    {
        if (!Files.exists(defaultFlanPath) && Files.exists(fallbackFlanPath))
        {
            flanFolder = fallbackFlanPath;
        }
        else
        {
            try
            {
                Files.createDirectories(defaultFlanPath);
            }
            catch (Exception e)
            {
                FlansMod.log.error("Failed to create the flan directory at {}", defaultFlanPath, e);
                return;
            }
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

                if (Files.isDirectory(path) || path.toString().toLowerCase(Locale.ROOT).endsWith(".jar") || path.toString().toLowerCase(Locale.ROOT).endsWith(".zip"))
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
                .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".txt"))
                .map(txtFile -> readTypeFile(txtFile, folderName, provider))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(TypeFile::getType).thenComparing(TypeFile::getName))
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
        catch (MalformedInputException e)
        {
            // Legacy/Windows encodings often decode fine as ISO-8859-1
            return Files.readAllLines(file, StandardCharsets.ISO_8859_1);
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
        for (TypeFile typeFile : files.get(contentPack))
        {
            try
            {
                CategoryManager.applyCategoriesToFile(typeFile);
                InfoType config = typeFile.getType().getTypeClass().getConstructor().newInstance();
                config.load(typeFile);
                String shortName = config.getOriginalShortName();

                if (!shortName.isBlank())
                {
                    if (typeFile.getType().isItemType())
                    {
                        shortName = findNewValidShortName(shortName, contentPack, typeFile);
                        if (!shortName.isBlank())
                        {
                            registerItem(shortName, config, typeFile);
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
            Optional<String> otherFileAlias = Optional.empty();
            if (shortnameReferences.get(provider).containsKey(originalShortname))
                otherFileAlias = Optional.ofNullable(registeredItems.get(shortnameReferences.get(provider).get(originalShortname).get()));

            // Conflict is in the same Content Pack -> Ignore file
            if (contentPackName.equals(TypeFile.getContentPackName(otherFileOriginal)))
            {
                FlansMod.log.warn("Detected conflict for item id '{}' in same content pack: {} and {}. Ignoring {}", originalShortname, file, otherFileOriginal, fileName);
                return StringUtils.EMPTY;
            }
            else if (otherFileAlias.isPresent() && contentPackName.equals(TypeFile.getContentPackName(otherFileAlias.get())))
            {
                FlansMod.log.warn("Detected conflict for item id '{}' in same content pack: {} and {}. Ignoring {}", originalShortname, file, otherFileAlias.get(), fileName);
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

    private static void findDuplicateTextures(IContentProvider provider)
    {
        FileSystem fs = FileUtils.createFileSystem(provider);
        findDuplicateTexturesInFolder(TEXTURES_ARMOR_FOLDER, provider, fs, armorTextureReferences.get(provider));
        findDuplicateTexturesInFolder(TEXTURES_GUI_FOLDER, provider, fs, guiTextureReferences.get(provider));
        findDuplicateTexturesInFolder(TEXTURES_SKINS_FOLDER, provider, fs, skinsTextureReferences.get(provider));
        FileUtils.closeFileSystem(fs, provider);
    }

    private static void findDuplicateTexturesInFolder(String folderName, IContentProvider provider, FileSystem fs, Map<String, DynamicReference> aliasMapping)
    {
        Path textureFolderPath = provider.getAssetsPath(fs).resolve(folderName);

        if (Files.exists(textureFolderPath))
        {
            try (Stream<Path> stream = Files.list(textureFolderPath))
            {
                stream.filter(p -> p.toString().toLowerCase(Locale.ROOT).endsWith(".png"))
                        .forEach(p -> checkForDuplicateTextures(p, provider, folderName, aliasMapping));
            }
            catch (IOException e)
            {
                FlansMod.log.error("Could not read {}", textureFolderPath, e);
            }
        }
    }

    private static void checkForDuplicateTextures(Path texturePath, IContentProvider provider, String folderName, Map<String, DynamicReference> aliasMapping)
    {
        String fileName = FilenameUtils.getBaseName(texturePath.getFileName().toString());
        if (folderName.equals(TEXTURES_ARMOR_FOLDER))
            fileName = getArmorTextureBaseName(fileName);
        fileName = ResourceUtils.sanitize(fileName);
        String aliasName = fileName;

        if (isTextureNameAlreadyRegistered(fileName, folderName, provider))
        {
            TextureFile otherFile = textures.get(folderName).get(fileName);
            FileSystem fs = FileUtils.createFileSystem(otherFile.contentPack());
            Path otherPath = otherFile.contentPack().getAssetsPath(fs).resolve(folderName).resolve(otherFile.name());
            if (FileUtils.filesHaveDifferentBytes(texturePath, otherPath) && !FileUtils.isSameImage(texturePath, otherPath))
            {
                aliasName = findValidTextureName(fileName, folderName, provider, otherFile.contentPack(), aliasMapping);
            }
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

        boolean missingAssets = !Files.exists(provider.getAssetsPath(fs).resolve("models").resolve("item"))
            || (!Files.exists(provider.getAssetsPath(fs).resolve("textures").resolve("item")) && Files.exists(provider.getAssetsPath(fs).resolve("textures").resolve("items")))
            || (!Files.exists(provider.getAssetsPath(fs).resolve("textures").resolve(TEXTURES_ARMOR_FOLDER)) && Files.exists(provider.getAssetsPath(fs).resolve(TEXTURES_ARMOR_FOLDER)))
            || (!Files.exists(provider.getAssetsPath(fs).resolve("textures").resolve(TEXTURES_GUI_FOLDER)) && Files.exists(provider.getAssetsPath(fs).resolve(TEXTURES_GUI_FOLDER)))
            || (!Files.exists(provider.getAssetsPath(fs).resolve("textures").resolve(TEXTURES_SKINS_FOLDER)) && Files.exists(provider.getAssetsPath(fs).resolve(TEXTURES_SKINS_FOLDER)))
            || !Files.exists(provider.getAssetsPath(fs).resolve("lang"))
            || !Files.exists(provider.getAssetsPath(fs).resolve("lang").resolve("en_us.json"));

        FileUtils.closeFileSystem(fs, provider);
        return missingAssets;
    }

    private static boolean shouldUnpackArchive(IContentProvider provider, boolean preLoadAssets)
    {
        return provider.isArchive() && (preLoadAssets || shouldUpdateAliasMappingFile(ID_ALIAS_FILE, provider, DynamicReference.getAliasMapping(shortnameReferences.get(provider))));
    }

    private static void createItemJsonFiles(IContentProvider provider)
    {
        Path jsonModelsFolderPath = provider.getAssetsPath().resolve("models");
        Path jsonItemModelsFolderPath = jsonModelsFolderPath.resolve("item");
        Path jsonBlockstatesFolderPath = provider.getAssetsPath().resolve("blockstates");

        convertExistingJsonFiles(jsonBlockstatesFolderPath);
        convertExistingJsonFiles(jsonModelsFolderPath);

        try
        {
            Files.createDirectories(jsonItemModelsFolderPath);
        }
        catch (IOException e)
        {
            FlansMod.log.error("Could not create {}", jsonItemModelsFolderPath, e);
            return;
        }

        for (InfoType config : listItems(provider))
        {
            generateItemJson(config, jsonItemModelsFolderPath);
        }
    }

    private static void convertExistingJsonFiles(Path jsonFolderPath)
    {
        if (!Files.isDirectory(jsonFolderPath))
            return;

        try (Stream<Path> walk = Files.walk(jsonFolderPath))
        {
            walk.filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".json"))
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
            jsonFile = renameToLowercase(jsonFile);

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

    /** Rename file to lowercase (and replace spaces with underscores). */
    private static Path renameToLowercase(Path file) throws IOException
    {
        String name = file.getFileName().toString();
        String lower = ResourceUtils.sanitize(name);
        if (name.equals(lower)) return file;

        Path target = file.resolveSibling(lower);

        // If only the case differs, do a two-step move for Windows/macOS
        if (name.equalsIgnoreCase(lower))
        {
            Path tmp = file.resolveSibling(name + "." + UUID.randomUUID() + ".tmp");
            Files.move(file, tmp, StandardCopyOption.REPLACE_EXISTING);
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }
        else
        {
            Files.move(file, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return target;
    }



    private static List<InfoType> listItems(IContentProvider provider)
    {
        return configs.get(provider).stream()
            .filter(config -> config.getType().isItemType())
            .toList();
    }

    private static void generateItemJson(InfoType config, Path outputFolder)
    {
        ResourceUtils.ItemModel model = ResourceUtils.ItemModel.create(config);
        String jsonContent = gson.toJson(model);
        String shortName = config.getShortName();

        if (!shortName.equals(config.getOriginalShortName()))
        {
            Path oldFile = outputFolder.resolve(config.getOriginalShortName() + ".json");
            try
            {
                Files.deleteIfExists(oldFile);
            }
            catch (IOException e)
            {
                FlansMod.log.error("Could not delete {}", oldFile, e);
            }
        }

        Path outputFile = outputFolder.resolve(shortName + ".json");
        try
        {
            Files.write(outputFile, jsonContent.getBytes());
        }
        catch (IOException e)
        {
            FlansMod.log.error("Could not create {}", outputFile, e);
        }

        if (config instanceof PaintableType paintableType)
        {
            for (Paintjob p : paintableType.getPaintjobs().values())
            {
                if (!p.equals(paintableType.getDefaultPaintjob()))
                {
                    outputFile = outputFolder.resolve(p.getIconName() + ".json");
                    model = ResourceUtils.ItemModel.create(config, p);
                    jsonContent = gson.toJson(model);
                    try
                    {
                        Files.write(outputFile, jsonContent.getBytes());
                    }
                    catch (IOException e)
                    {
                        FlansMod.log.error("Could not create {}", outputFile, e);
                    }
                }
            }
        }
    }

    private static void copyItemIcons(IContentProvider provider)
    {
        Path sourcePath = provider.getAssetsPath().resolve("textures").resolve("items");
        Path destPath = provider.getAssetsPath().resolve("textures").resolve("item");
        copyPngFilesAndLowercaseFileNames(sourcePath, destPath);
    }

    private static void copyTextures(IContentProvider provider, String folderName, Map<String, DynamicReference> aliasMapping)
    {
        Path sourcePath = provider.getAssetsPath().resolve(folderName);
        Path destPath = provider.getAssetsPath().resolve("textures").resolve(folderName);
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
                        if (folder.getFileName().toString().equals(TEXTURES_ARMOR_FOLDER))
                        {
                            baseFileName = getArmorTextureBaseName(baseFileName);
                        }
                        return file.toString().toLowerCase(Locale.ROOT).endsWith(".png") && aliasMapping.containsKey(baseFileName);
                    })
                    .forEach(file ->
                    {
                        String baseFileName = FilenameUtils.getBaseName(file.getFileName().toString());
                        if (folder.getFileName().toString().equals(TEXTURES_ARMOR_FOLDER))
                        {
                            baseFileName = getArmorTextureBaseName(baseFileName);
                        }
                        String newFileName = aliasMapping.get(baseFileName).get();
                        if (folder.getFileName().toString().equals(TEXTURES_ARMOR_FOLDER))
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
                        Path destFile = file.getParent().resolve(newFileName + ".png");
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
                FlansMod.log.error("Could not read {}", folder, e);
            }
        }
    }

    private static void createLocalization(IContentProvider provider)
    {
        Path langDir = provider.getAssetsPath().resolve("lang");

        try
        {
            Files.createDirectories(langDir);
        }
        catch (IOException e)
        {
            FlansMod.log.error("Could not create directory for localization {}", langDir, e);
            return;
        }

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
                String keyToAdd = generateTranslationKey(shortName, config.getType().isBlockType());
                String keyToRemove = generateTranslationKey(config.getOriginalShortName(), config.getType().isBlockType());
                translations.putIfAbsent(keyToAdd, config.getName());
                translations.remove(keyToRemove);
            }
            else
            {
                translations.putIfAbsent(generateTranslationKey(shortName, config.getType().isBlockType()), config.getName());
            }
        }

        String jsonFileName = langFile.getFileName().toString().toLowerCase(Locale.ROOT).replace(".lang", ".json");
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
        if (legacyKey.startsWith("item.") && legacyKey.endsWith(".name")) {
            String id = legacyKey.substring(5, legacyKey.length() - 5).toLowerCase(Locale.ROOT);
            return "item." + FlansMod.FLANSMOD_ID + "." + id;
        }
        if ((legacyKey.startsWith("tile.") || legacyKey.startsWith("block.")) && legacyKey.endsWith(".name")) {
            String id = legacyKey.substring(legacyKey.indexOf('.') + 1, legacyKey.length() - 5).toLowerCase(Locale.ROOT);
            return "block." + FlansMod.FLANSMOD_ID + "." + id;
        }
        return legacyKey;
    }

    private static String generateTranslationKey(String itemId, boolean isBlock)
    {
        return (isBlock ? "block." : "item.") + FlansMod.FLANSMOD_ID + "." + itemId;
    }

    private static void copyPngFilesAndLowercaseFileNames(Path sourcePath, Path destPath)
    {
        if (!Files.exists(sourcePath))
            return;

        try
        {
            Files.createDirectories(destPath);
        }
        catch (IOException e)
        {
            FlansMod.log.error("Could not create {}", destPath, e);
            return;
        }

        try (Stream<Path> paths = Files.walk(sourcePath, 1))
        {
            paths.filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png"))
                .forEach(src -> {
                    // Sanitize each path segment (even though depth=1, this is safe if you later allow subfolders)
                    Path rel = sourcePath.relativize(src);
                    String sanitizedRel = FileUtils.sanitizePngRelPath(rel);

                    Path dst = destPath.resolve(sanitizedRel).normalize();
                    try
                    {
                        Files.createDirectories(dst.getParent());
                        // Avoid collisions after sanitizing (e.g., "A.png" & "a.png" → "a.png")
                        Path unique = FileUtils.ensureUnique(dst);
                        Files.copy(src, unique, StandardCopyOption.REPLACE_EXISTING);
                    }
                    catch (IOException e)
                    {
                        FlansMod.log.error("Could not create {}", dst, e);
                    }
                });
        }
        catch (IOException e)
        {
            FlansMod.log.error("Could not read {}", sourcePath, e);
        }
    }

    private static void createSounds(IContentProvider provider)
    {
        Path soundsDir = provider.getAssetsPath().resolve("sounds");
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
            if (!FileUtils.isOgg(src))
                continue;
            if (!FileUtils.needsRename(src))
                continue;

            try
            {
                Path target = soundsDir.resolve(ResourceUtils.sanitize(src.getFileName().toString())).normalize();
                Path finalTarget = FileUtils.ensureUnique(target);
                FileUtils.movePossiblyCaseOnly(src, finalTarget);
            }
            catch (IOException e)
            {
                FlansMod.log.error("Could not rename {}", src, e);
            }
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
