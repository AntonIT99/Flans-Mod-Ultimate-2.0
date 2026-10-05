package com.flansmodultimate.content;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.block.BlockFactory;
import com.flansmodultimate.common.driveables.ModelMuzzleMeasurement;
import com.flansmodultimate.common.item.ItemFactory;
import com.flansmodultimate.common.sync.ContentFingerprint;
import com.flansmodultimate.common.types.ArmorBoxType;
import com.flansmodultimate.common.types.BlockType;
import com.flansmodultimate.common.types.DriveableType;
import com.flansmodultimate.common.types.EnumType;
import com.flansmodultimate.common.types.GunBoxType;
import com.flansmodultimate.common.types.IAmmoGroupUser;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.common.types.PartType;
import com.flansmodultimate.common.types.ShootableType;
import com.flansmodultimate.common.types.ToolType;
import com.flansmodultimate.common.types.TypeFile;
import com.flansmodultimate.config.CategoryManager;
import com.flansmodultimate.config.ContentLoadingConfig;
import com.flansmodultimate.platform.PlatformEnvironment;
import com.flansmodultimate.platform.PlatformPaths;
import com.flansmodultimate.util.DynamicReference;
import com.flansmodultimate.util.FileUtils;
import com.flansmodultimate.util.FlansLog;
import com.flansmodultimate.util.JavaModelCompiler;
import com.flansmodultimate.util.LogUtils;
import com.flansmodultimate.util.ModCachePaths;
import com.flansmodultimate.util.ResourceUtils;
import com.flansmodultimate.util.SoundLengthIndex;
import com.flansmodultimate.util.TextDecoding;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import static com.flansmodultimate.content.ContentPackPaths.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ContentManager
{
    @Getter
    private static Path flanFolder;
    private static final Path defaultFlanPath = PlatformPaths.gameDir().resolve(ContentLoadingConfig.getContentPacksRelativePath());
    private static final Path fallbackFlanPath = PlatformPaths.gameDir().resolve("Flan");

    private static final LegacyTextureAliases legacyTextures = new LegacyTextureAliases();

    public static Map<IContentProvider, Map<String, DynamicReference>> getArmorTextureReferences()
    {
        return legacyTextures.getArmorTextureReferences();
    }

    public static Map<IContentProvider, Map<String, DynamicReference>> getGuiTextureReferences()
    {
        return legacyTextures.getGuiTextureReferences();
    }

    public static Map<IContentProvider, Map<String, DynamicReference>> getSkinsTextureReferences()
    {
        return legacyTextures.getSkinsTextureReferences();
    }

    private static ContentPackAssets.TextureReferences textureReferences(IContentProvider provider)
    {
        return new ContentPackAssets.TextureReferences(getArmorTextureReferences().get(provider),
            getGuiTextureReferences().get(provider), getSkinsTextureReferences().get(provider));
    }

    // Mappings which allow to use aliases for duplicate short names and texture names (also contain unmodified references)
    // The idea behind dynamic references is to allow references to shortnames and textures to change
    // even after configs are registered (as long as item classes have not been instantiated yet)
    @Getter
    private static final Map<IContentProvider, Map<String, DynamicReference>> shortnameReferences = new HashMap<>();

    private static final String CONTENT_STARTUP_LOCK_FILE = ".flansmod-content.lock";

    private static final List<IContentProvider> contentPacks = new ArrayList<>();
    private static final Map<IContentProvider, ArrayList<TypeFile>> files = new HashMap<>();
    private static final Map<IContentProvider, ArrayList<InfoType>> configs = new HashMap<>();
    private static final Map<IContentProvider, String> generationInputs = new HashMap<>();
    private static Set<Path> excludedFlanArchives = Set.of();

    // Keep track of registered items and loaded textures
    /** &lt; shortname, config file string representation &gt; */
    private static final Map<String, String> registeredItems = new HashMap<>();
    private static final ConcurrentMap<EnumType, Constructor<? extends InfoType>> typeConstructors = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, Set<TextureOrigin>> modelTextureOrigins = new HashMap<>();

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

    /** Reconciles misplaced standalone content archives and packaged-content mod JARs. */
    public static void reconcileContentPackLocations()
    {
        loadFlanFolder();
        if (flanFolder == null)
            return;

        Path gameDir = PlatformPaths.gameDir().toAbsolutePath().normalize();
        Path normalizedFlanFolder = flanFolder.toAbsolutePath().normalize();
        FileUtils.runWithFileLock(gameDir.resolve(CONTENT_STARTUP_LOCK_FILE), "Flan cache migration",
            () -> ModCachePaths.migrate(gameDir));
        boolean isGameDirectory = normalizedFlanFolder.equals(gameDir);
        try
        {
            isGameDirectory = isGameDirectory || Files.isSameFile(normalizedFlanFolder, gameDir);
        }
        catch (IOException ignored)
        {
            // Ignored
        }
        if (isGameDirectory)
        {
            FlansLog.log.warn("Content pack relocation is disabled because the configured flan folder is the game directory: '{}'.", gameDir);
            return;
        }

        ContentPackRelocator.RelocationResult result = ContentPackRelocator.reconcile(
            PlatformPaths.modsDir(), normalizedFlanFolder,
            ModCachePaths.root(gameDir).resolve(ContentPackRelocator.CACHE_FILE_NAME)
        );
        excludedFlanArchives = result.excludedFromContentLoading();
        result.warnings().forEach(FlansLog.log::warn);
        if (result.movedContentPacks() > 0)
            FlansLog.log.info("Moved {} misplaced standalone Flan content pack(s) from mods to '{}'.",
                result.movedContentPacks(), normalizedFlanFolder);
        if (result.restartRequired())
            FlansLog.log.warn("Moved {} Flan pack mod bundle(s) to '{}'. Restart the game to activate them.",
                result.movedBundles(), PlatformPaths.modsDir().toAbsolutePath());
        FlansLog.log.debug("Verified Flan archive locations in {} ms; inspected {} new or changed archive(s).",
            result.elapsedMillis(), result.inspectedArchives());
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
            FlansLog.log.error("Failed to load content packs from flan folder.", e);
        }
    }

    /**
     * Adds immutable packaged providers ahead of user folder packs. First registration keeps the
     * original item/texture name, so this ordering makes later user conflicts receive aliases.
     */
    public static void addPackagedContentPacks(List<? extends IContentProvider> providers)
    {
        for (IContentProvider provider : providers)
        {
            if (!provider.isPreprocessed())
                throw new IllegalArgumentException("Packaged provider must be preprocessed: " + provider);
            if (!contentPacks.contains(provider))
                contentPacks.add(provider);
        }
    }

    /** Packaged providers first, then the user folder packs in alphabetical order. */
    public static List<IContentProvider> getContentPacks()
    {
        return Collections.unmodifiableList(contentPacks);
    }

    public static void readContentPacks()
    {
        if (flanFolder == null)
            return;

        Path gameDir = flanFolder.getParent();
        if (gameDir == null)
        {
            FlansLog.log.error("Cannot load content packs because flan folder '{}' has no parent directory.", flanFolder);
            return;
        }

        FileUtils.runWithFileLock(gameDir.resolve(CONTENT_STARTUP_LOCK_FILE), "Flan content startup", ContentManager::readContentPacksLocked);
    }

    public static Path resolveFlanFolderPath()
    {
        if (!Files.exists(defaultFlanPath) && Files.exists(fallbackFlanPath))
            return fallbackFlanPath;

        return defaultFlanPath;
    }

    private static void readContentPacksLocked()
    {
        Path tempRoot = flanFolder.getParent().resolve(".flansmod-temp");
        FileUtils.cleanupFlanTempOnStartup(tempRoot);
        ContentFileCache.configure(ModCachePaths.content(PlatformPaths.gameDir()),
            ContentLoadingConfig.isForceRegenContentPacksAssetsAndIds());
        PartType.clearDefaultEngines();
        if (PlatformEnvironment.isClient())
            legacyTextures.prepare(contentPacks);

        for (IContentProvider provider : contentPacks)
        {
            long startTime = System.currentTimeMillis();

            files.putIfAbsent(provider, new ArrayList<>());
            configs.putIfAbsent(provider, new ArrayList<>());

            shortnameReferences.putIfAbsent(provider, new HashMap<>());
            legacyTextures.initialize(provider);

            if (!provider.isArchive())
                compileJavaModelsIfNeeded(provider);

            boolean preprocessed = provider.isPreprocessed();
            boolean preLoadAssets = false;
            boolean preLoadData = false;
            boolean unpackArchive = false;
            ContentProcessingCache processingCache = null;
            long postTypeStart;
            long textureIndexNanos = 0L;
            long idAliasNanos = 0L;
            long assetCheckNanos = 0L;
            long dataCheckNanos = 0L;
            long unpackCheckNanos = 0L;

            try (FileUtils.ArchiveFileSystemCache ignored = FileUtils.cacheArchiveFileSystems())
            {
                loadTypes(provider);
                postTypeStart = System.nanoTime();

                if (PlatformEnvironment.isClient() && provider.shouldIndexAssetsForConflicts())
                {
                    long phaseStart = System.nanoTime();
                    legacyTextures.findDuplicates(provider);
                    textureIndexNanos = System.nanoTime() - phaseStart;
                }

                if (!preprocessed)
                {
                    processingCache = new ContentProcessingCache(provider, generationInputs.get(provider));
                    long phaseStart = System.nanoTime();
                    boolean idAliasNeedsUpdate = AliasFileManager.shouldUpdateAliasMappingFile(ID_ALIAS_FILE, provider, DynamicReference.getAliasMapping(shortnameReferences.get(provider)));
                    idAliasNanos = System.nanoTime() - phaseStart;

                    phaseStart = System.nanoTime();
                    preLoadAssets = PlatformEnvironment.isClient() &&
                        (ContentLoadingConfig.isForceRegenContentPacksAssetsAndIds() || idAliasNeedsUpdate
                            || !processingCache.assetsCurrent() || ContentPackAssets.aliasesChanged(provider, textureReferences(provider)));
                    assetCheckNanos = System.nanoTime() - phaseStart;

                    phaseStart = System.nanoTime();
                    preLoadData = ContentLoadingConfig.isForceRegenContentPacksAssetsAndIds() || idAliasNeedsUpdate
                        || !processingCache.dataCurrent();
                    dataCheckNanos = System.nanoTime() - phaseStart;

                    phaseStart = System.nanoTime();
                    unpackArchive = shouldUnpackArchive(provider, preLoadAssets, preLoadData, idAliasNeedsUpdate);
                    unpackCheckNanos = System.nanoTime() - phaseStart;
                }
            }

            long postTypeNanos = System.nanoTime() - postTypeStart;
            long archiveCloseNanos = postTypeNanos - textureIndexNanos - idAliasNanos - assetCheckNanos - dataCheckNanos - unpackCheckNanos;

            if (FlansLog.log.isDebugEnabled())
            {
                FlansLog.log.debug("{}: Post-type checks completed in {} ms (textures: {} ms, id aliases: {} ms, assets: {} ms, data: {} ms, unpack: {} ms, archive close: {} ms)",
                    provider.getName(),
                    formatMilliseconds(postTypeNanos),
                    formatMilliseconds(textureIndexNanos),
                    formatMilliseconds(idAliasNanos),
                    formatMilliseconds(assetCheckNanos),
                    formatMilliseconds(dataCheckNanos),
                    formatMilliseconds(unpackCheckNanos),
                    formatMilliseconds(Math.max(0L, archiveCloseNanos)));
            }

            if (preprocessed)
            {
                long endTime = System.currentTimeMillis();
                FlansLog.log.info("Loaded preprocessed content pack {} in {} ms.", provider.getName(), endTime - startTime);
                continue;
            }

            boolean archiveExtracted = false;
            boolean assetsComplete = false;
            boolean dataComplete = false;

            if (unpackArchive)
            {
                FlansLog.log.info("Reprocessing {}...", provider.getName());
                FileUtils.prepareFreshExtractionDir(provider.getExtractedPath());
                archiveExtracted = FileUtils.extractArchive(provider.getPath(), provider.getExtractedPath());
            }

            if (archiveExtracted || !provider.isArchive())
            {
                if (archiveExtracted)
                    compileJavaModelsIfNeeded(provider);

                ContentPackAssets.createMcMeta(provider);

                if (preLoadData)
                    ContentPackAssets.createRecipeJsonFiles(provider, listItems(provider));

                if (preLoadAssets)
                    ContentPackAssets.generate(provider, configs.get(provider), listItems(provider), listBlocks(provider), textureReferences(provider));
                else if (preLoadData && !PlatformEnvironment.isClient())
                    ContentPackSounds.generate(provider);

                assetsComplete = preLoadAssets && ContentPackAssets.outputsPresent(provider, listItems(provider), listBlocks(provider), textureReferences(provider));
                dataComplete = preLoadData && ContentPackAssets.dataOutputsPresent(provider, listItems(provider));

                AliasFileManager.writeToAliasMappingFile(ID_ALIAS_FILE, provider, DynamicReference.getAliasMapping(shortnameReferences.get(provider)));
            }

            boolean repackSucceeded = true;
            if (archiveExtracted)
            {
                repackSucceeded = FileUtils.repackArchive(provider);
            }

            if (repackSucceeded && (!provider.isArchive() || !unpackArchive || archiveExtracted))
            {
                processingCache.remember(assetsComplete, dataComplete, preLoadAssets, preLoadData);
                if (PlatformEnvironment.isClient() && (preLoadAssets || preLoadData))
                    legacyTextures.rememberAfterProcessing(provider);
            }

            long endTime = System.currentTimeMillis();
            FlansLog.log.info("Loaded content pack {} in {} ms.", provider.getName(), endTime - startTime);
        }

        applyMeasuredSoundLengths();
        resolveDeferredContentReferences();
        applyMeasuredMuzzles();

        FileUtils.deleteDirectoryIfEmpty(tempRoot);
    }

    private static void loadTypes(IContentProvider provider)
    {
        long readStart = System.nanoTime();
        long readEnd;
        long registerEnd;
        try (FileUtils.ArchiveFileSystemCache ignored = FileUtils.cacheArchiveFileSystems())
        {
            readFiles(provider);
            readEnd = System.nanoTime();
            registerConfigs(provider);
            registerEnd = System.nanoTime();
        }

        if (FlansLog.log.isDebugEnabled())
        {
            FlansLog.log.debug("{}: Types loaded in {} ms (read: {} ms, register: {} ms)",
                provider.getName(),
                formatMilliseconds(registerEnd - readStart),
                formatMilliseconds(readEnd - readStart),
                formatMilliseconds(registerEnd - readEnd));
        }
    }

    private static String formatMilliseconds(long nanoseconds)
    {
        return String.format(Locale.ROOT, "%.3f", nanoseconds / 1_000_000.0);
    }

    /**
     * Replaces the sound timers configured in the content packs with the real length of the sound
     * files they play. This runs once every pack has been read, because a type may well play a sound
     * that another pack provides.
     */
    private static void applyMeasuredSoundLengths()
    {
        long startTime = System.currentTimeMillis();
        boolean overrideConfiguredLengths = ContentLoadingConfig.isOverrideConfiguredSoundLengths();

        // Priority resolves event aliases against the actual selected .ogg, on servers and clients alike.
        SoundLengthIndex.clear();
        SoundPriority.initialize(contentPacks);
        if (!overrideConfiguredLengths)
            SoundLengthIndex.clear();

        int resolved = 0;
        for (ArrayList<InfoType> providerConfigs : configs.values())
        {
            for (InfoType config : providerConfigs)
                resolved += config.resolveSoundLengths();
        }

        if (!overrideConfiguredLengths)
        {
            FlansLog.log.info("Keeping the sound lengths configured in the content packs because overrideConfiguredSoundLengths is disabled.");
            return;
        }

        FlansLog.log.info("Replaced {} configured sound length(s) with the measured length of the sound file in {} ms. "
            + "Enable debug logging to see them, or set overrideConfiguredSoundLengths to false to keep the configured values.",
            resolved, System.currentTimeMillis() - startTime);
    }

    /**
     * Measures the muzzles of deployable-gun, driveable and AA-gun models, on a dedicated server as on a client.
     * This runs once every pack has been read, because a model may come from another pack and a seat gun from yet another.
     */
    private static void applyMeasuredMuzzles()
    {
        List<InfoType> types = new ArrayList<>();
        for (IContentProvider provider : contentPacks)
            types.addAll(configs.getOrDefault(provider, new ArrayList<>()));

        boolean overrideConfiguredShootPoints = ContentLoadingConfig.isOverrideConfiguredShootPoints();
        ModelMuzzleMeasurement.measure(types, contentPacks, overrideConfiguredShootPoints);
        if (!overrideConfiguredShootPoints)
            FlansLog.log.info("Keeping the shoot points and AA gun barrels configured in the flan folder packs because overrideConfiguredShootPoints is disabled.");
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
        int driveables = 0;
        int parts = 0;
        int tools = 0;

        FlansLog.log.info("Validating content references...");
        for (ArrayList<InfoType> providerConfigs : configs.values())
        {
            for (InfoType config : providerConfigs)
            {
                if (config instanceof IAmmoGroupUser ammoGroupUser)
                    ShootableType.validateAmmoGroups(config, ammoGroupUser.getAmmoGroups());

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
                else if (config instanceof DriveableType driveableType)
                {
                    driveableType.validateRecipeIngredients();
                    driveables++;
                }
                else if (config instanceof ToolType toolType)
                {
                    toolType.validateRecipeIngredients();
                    tools++;
                }
            }
        }
        FlansLog.log.info("Validated content references for {} GunBoxes, {} ArmorBoxes, {} driveables, {} parts, and {} tools.", gunBoxes, armorBoxes, driveables, parts, tools);
    }

    private static void loadFlanFolder()
    {
        Path resolvedFlanPath = resolveFlanFolderPath();
        if (!FileUtils.tryCreateDirectories(resolvedFlanPath))
            return;

        flanFolder = resolvedFlanPath;
    }

    private static Map<String, Path> loadFoldersAndJarZipFiles(Path rootPath) throws IOException
    {
        return ContentPackDiscovery.select(rootPath, excludedFlanArchives);
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
                    if (PlatformEnvironment.isClient())
                    {
                        if (path.getFileName().toString().equals(ARMOR_TEXTURES_ALIAS_FILE))
                        {
                            readAliasMappingFile(path.getFileName().toString(), provider, legacyTextures.getArmorTextureReferences());
                        }
                        if (path.getFileName().toString().equals(GUI_TEXTURES_ALIAS_FILE))
                        {
                            readAliasMappingFile(path.getFileName().toString(), provider, legacyTextures.getGuiTextureReferences());
                        }
                        if (path.getFileName().toString().equals(SKINS_TEXTURES_ALIAS_FILE))
                        {
                            readAliasMappingFile(path.getFileName().toString(), provider, legacyTextures.getSkinsTextureReferences());
                        }
                    }
                }
            });
        }
        catch (IOException e)
        {
            FlansLog.log.error("Failed to load types in content pack '{}'", provider.getName(), e);
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
                .toList()
            );
        }
        catch (IOException e)
        {
            FlansLog.log.error("Failed to read '{}' folder in content pack '{}'", folderName, provider.getName(), e);
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
            FlansLog.log.error("Failed to read '{}/{}' in content pack '{}'", folderName, file.getFileName(), provider.getName(), e);
            return null;
        }
    }

    private static List<String> readTypeFileLines(Path file) throws IOException
    {
        return TextDecoding.readLines(file);
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
        StringBuilder generationInput = new StringBuilder();

        for (TypeFile typeFile : typeFiles)
        {
            try
            {
                // Before the categories are applied: what is fingerprinted is the pack's own text,
                // and the categories are the mod's, identical on both sides.
                ContentFingerprint.record(typeFile);
                CategoryManager.applyCategoriesToFile(typeFile);
                generationInput.append(typeFile.getGenerationInput()).append('\n');
                EnumType type = typeFile.getType();
                Constructor<? extends InfoType> constructor = typeConstructors.get(type);
                if (constructor == null)
                {
                    Constructor<? extends InfoType> discovered = type.getTypeClass().getConstructor();
                    Constructor<? extends InfoType> previous = typeConstructors.putIfAbsent(type, discovered);
                    constructor = previous != null ? previous : discovered;
                }
                InfoType config = constructor.newInstance();
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
                    FlansLog.log.error("ShortName not set: {}", typeFile);
                }
            }
            catch (Exception e)
            {
                FlansLog.log.error("Failed to add {}", typeFile);
                LogUtils.logErrorWithoutStacktrace(e);
            }
        }
        files.clear();
        generationInput.append(new java.util.TreeMap<>(DynamicReference.getAliasMapping(shortnameReferences.get(contentPack))));
        generationInputs.put(contentPack, ContentFileCache.hash(generationInput.toString()));
    }

    private static void addConfig(IContentProvider contentPack, InfoType config)
    {
        configs.get(contentPack).add(config);
        if (PlatformEnvironment.isClient())
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

        // Optional paintjob icons may legitimately be absent in legacy packs;
        // their generated item models fall back to the base item icon.
    }

    private static void registerModelTextureOrigin(String textureFolder, @Nullable String textureName, TextureOrigin origin)
    {
        String texturePath = String.format("%s/%s", textureFolder, ResourceUtils.sanitize(textureName));
        try
        {
            ResourceLocation textureId = ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, texturePath);
            modelTextureOrigins.computeIfAbsent(textureId, key -> new HashSet<>()).add(origin);
        }
        catch (Exception e)
        {
            FlansLog.log.warn("Could not register expected model texture '{}': {}", texturePath, origin);
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
            .forEach(missing -> FlansLog.log.warn("Missing texture {}: {}", missing.textureId(), missing.origin()));
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
                FlansLog.log.warn("Detected conflict for item id '{}' in same content pack: {} and {}. Ignoring {}", originalShortname, file, conflictingFileInSamePack.get(), fileName);
                return StringUtils.EMPTY;
            }

            FlansLog.log.warn("Detected conflict for item id '{}': {} and {}. Creating id alias '{}' in [{}]", originalShortname, file, otherFileOriginal, newShortname, contentPackName);
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

    private static boolean shouldUnpackArchive(IContentProvider provider, boolean preLoadAssets, boolean preLoadData,
                                               boolean idAliasNeedsUpdate)
    {
        return provider.isArchive() && (preLoadAssets || preLoadData || idAliasNeedsUpdate);
    }

    private static void compileJavaModelsIfNeeded(IContentProvider provider)
    {
        if (!PlatformEnvironment.isClient())
            return;

        try
        {
            boolean hasOutdatedJavaModels = JavaModelCompiler.hasOutdatedJavaModels(provider);
            if (!hasOutdatedJavaModels)
                return;

            if (!JavaModelCompiler.isCompilerAvailable())
            {
                FlansLog.log.warn("Found Java model sources in content pack '{}', but no Java compiler is available. Run Minecraft with a JDK to compile pack model sources automatically.", provider.getName());
                return;
            }

            JavaModelCompiler.compileJavaModels(provider);
        }
        catch (LinkageError e)
        {
            FlansLog.log.warn("Java model source compilation is unavailable for content pack '{}': {}", provider.getName(), e.toString());
        }
    }

    @Unmodifiable
    static List<InfoType> listItems(IContentProvider provider)
    {
        return configs.get(provider).stream()
            .filter(config -> config.getType().isHasItem())
            .toList();
    }

    @Unmodifiable
    static List<InfoType> listBlocks(IContentProvider provider)
    {
        return configs.get(provider).stream()
            .filter(config -> config.getType().isHasBlock())
            .toList();
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
