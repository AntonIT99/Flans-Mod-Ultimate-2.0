package com.flansmodultimate.packs;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Forge companion mod that ships bundled Flan content packs and extracts them into the configured flan folder.
 * <p>
 * Extraction only runs in production. The target folder is resolved from the main mod's content loading config,
 * falling back to {@code flan} and then to an existing legacy {@code Flan} folder. The game directory itself is
 * rejected as an output target.
 * <p>
 * Startup coordination with the main mod is done through a stable JSON state file named
 * {@value #EXTRACTION_STATE_FILE_NAME} in the game directory. The file name is deliberately independent from the
 * packs mod version and bundled packs version so {@code FlansMod} can wait for the extractor to finish without
 * needing to know which pack version was resolved. The state file stores a protocol version, an extraction status,
 * the currently resolved bundled packs version, the last successfully extracted packs version, the resolved flan
 * output path, an update timestamp, and an optional failure message.
 * <p>
 * On startup this mod writes {@link ExtractionStatus#RESOLVING} before inspecting or extracting packs, then writes
 * {@link ExtractionStatus#COMPLETE} once the currently resolved packs version is present. If extraction fails it
 * attempts to write {@link ExtractionStatus#FAILED}. State writes use a temporary file followed by an atomic move
 * where supported, so the main mod does not observe a partially written state file.
 * <p>
 * {@link #EXTRACTION_STATE_PROTOCOL_VERSION} describes the schema/coordination protocol. Increment it when the
 * state file contract changes. {@link #BUNDLED_PACKS_VERSION_RESOURCE} stores the bundled pack contents version.
 * Extraction only runs when this resolved version is higher than the last extracted packs version, so increment the
 * resource value when the extracted pack contents should be refreshed independently of the mod jar version.
 */
@Mod(FlanPacksMod.MOD_ID)
public class FlanPacksMod
{
    public static final String MOD_ID = "flansmodultimate_packs";
    public static final String MAIN_MOD_ID = "flansmodultimate";

    public static final Logger log = LogUtils.getLogger();
    public static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /** Content Extraction **/
    private static final int EXTRACTION_STATE_PROTOCOL_VERSION = 1;
    private static final int NO_EXTRACTED_PACKS_VERSION = 0;
    private static final int FALLBACK_BUNDLED_PACKS_VERSION = 1;
    private static final String BUNDLED_PACKS_VERSION_RESOURCE = "/flansmodultimate_packs/bundled_packs_version.txt";
    private static final String EXTRACTION_STATE_FILE_NAME = ".flansmod_packs_extraction_state.json";
    private static final DateTimeFormatter BACKUP_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    /** Flan Directory */
    private static final String FLAN_DIR_NAME = "flan";
    private static final String FALLBACK_FLAN_DIR_NAME = "Flan";
    private static final String CONTENT_LOADING_CONFIG_FILE_NAME = MAIN_MOD_ID + "-content-loading.toml";
    private static final String CONTENT_PACKS_RELATIVE_PATH_KEY = "contentPacksRelativePath";

    public FlanPacksMod(FMLJavaModLoadingContext context)
    {
        if (!FMLEnvironment.production)
            return;

        try
        {
            extractFlanFolderIfNeeded();
        }
        catch (Exception e)
        {
            log.error("Failed to extract content packs to flan folder", e);
        }
    }

    private void extractFlanFolderIfNeeded() throws IOException
    {
        Path flanOutputDir = resolveFlanOutputDir();
        Path statePath = resolveExtractionStatePath();
        int resolvedPacksVersion = readBundledPacksVersion();
        ExtractionState previousState = readExtractionState(statePath).orElse(null);
        int lastExtractedPacksVersion = Optional.ofNullable(previousState)
            .map(ExtractionState::effectiveLastExtractedPacksVersion)
            .orElse(NO_EXTRACTED_PACKS_VERSION);

        writeResolvingState(statePath, flanOutputDir, resolvedPacksVersion, lastExtractedPacksVersion);
        try
        {
            if (isAlreadyExtracted(previousState, flanOutputDir, resolvedPacksVersion))
            {
                log.info("Packs already extracted at version {}, current bundled packs version is {}. Skipping.", lastExtractedPacksVersion, resolvedPacksVersion);
                writeCompleteState(statePath, flanOutputDir, resolvedPacksVersion, lastExtractedPacksVersion);
                return;
            }

            extractBundledPacks(flanOutputDir, statePath, previousState, resolvedPacksVersion, lastExtractedPacksVersion);
            writeCompleteState(statePath, flanOutputDir, resolvedPacksVersion, resolvedPacksVersion);
        }
        catch (IOException | RuntimeException e)
        {
            writeFailedState(statePath, flanOutputDir, resolvedPacksVersion, lastExtractedPacksVersion, e);
            throw e;
        }
    }

    private static boolean isAlreadyExtracted(@Nullable ExtractionState previousState, Path flanOutputDir, int resolvedPacksVersion)
    {
        return Optional.ofNullable(previousState).map(state -> state.isCompleteForAtLeast(resolvedPacksVersion, flanOutputDir)).orElse(false);
    }

    private void extractBundledPacks(Path flanOutputDir, Path statePath, @Nullable ExtractionState previousState, int resolvedPacksVersion, int lastExtractedPacksVersion) throws IOException
    {
        prepareFlanOutputDir(flanOutputDir, previousState, resolvedPacksVersion);
        writeResolvingState(statePath, flanOutputDir, resolvedPacksVersion, lastExtractedPacksVersion);

        Path jarPath = ModList.get().getModFileById(MOD_ID).getFile().getFilePath();
        log.info("Extracting packs to '{}' for {} bundled packs version {}...", flanOutputDir, MOD_ID, resolvedPacksVersion);
        copyBundledPacks(jarPath, flanOutputDir);
    }

    private void copyBundledPacks(Path jarPath, Path flanOutputDir) throws IOException
    {
        try (JarFile jarFile = new JarFile(jarPath.toFile()))
        {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements())
            {
                JarEntry entry = entries.nextElement();
                if (shouldProcessEntry(entry))
                    extractEntry(jarFile, entry, flanOutputDir);
            }
        }
    }

    private static Path resolveFlanOutputDir()
    {
        Path gameDir = FMLPaths.GAMEDIR.get().toAbsolutePath().normalize();
        Path defaultFlanPath = gameDir.resolve(readContentPacksRelativePath()).toAbsolutePath().normalize();
        Path fallbackFlanPath = gameDir.resolve(FALLBACK_FLAN_DIR_NAME).toAbsolutePath().normalize();
        Path resolvedPath = !Files.exists(defaultFlanPath) && Files.exists(fallbackFlanPath)
            ? fallbackFlanPath
            : defaultFlanPath;

        if (resolvedPath.equals(gameDir))
            throw new IllegalStateException("Refusing to use the game directory itself as the flan output directory. Check " + CONTENT_LOADING_CONFIG_FILE_NAME + ".");

        return resolvedPath;
    }

    private static Path resolveExtractionStatePath()
    {
        return FMLPaths.GAMEDIR.get().toAbsolutePath().normalize().resolve(EXTRACTION_STATE_FILE_NAME);
    }

    private static String readContentPacksRelativePath()
    {
        Path file = FMLPaths.CONFIGDIR.get().resolve(CONTENT_LOADING_CONFIG_FILE_NAME);
        if (!Files.isRegularFile(file))
            return FLAN_DIR_NAME;

        try (CommentedFileConfig config = CommentedFileConfig.of(file, TomlFormat.instance()))
        {
            config.load();
            Object value = config.get(CONTENT_PACKS_RELATIVE_PATH_KEY);
            if (value instanceof String str)
                return str;

            if (value != null)
                log.warn("Ignoring invalid {} in {}: {}. Expected string.", CONTENT_PACKS_RELATIVE_PATH_KEY, CONTENT_LOADING_CONFIG_FILE_NAME, value);
        }
        catch (Exception e)
        {
            log.warn("Could not read {}. Falling back to '{}'.", CONTENT_LOADING_CONFIG_FILE_NAME, FLAN_DIR_NAME, e);
        }

        return FLAN_DIR_NAME;
    }

    private static int readBundledPacksVersion()
    {
        try (InputStream input = FlanPacksMod.class.getResourceAsStream(BUNDLED_PACKS_VERSION_RESOURCE))
        {
            if (input == null)
            {
                log.error("Missing bundled packs version resource '{}'. Falling back to {}.", BUNDLED_PACKS_VERSION_RESOURCE, FALLBACK_BUNDLED_PACKS_VERSION);
                return FALLBACK_BUNDLED_PACKS_VERSION;
            }

            String value = new String(input.readAllBytes(), StandardCharsets.UTF_8).trim();
            try
            {
                int version = Integer.parseInt(value);
                if (version <= NO_EXTRACTED_PACKS_VERSION)
                {
                    log.error("Bundled packs version in '{}' must be greater than {} but was '{}'. Falling back to {}.", BUNDLED_PACKS_VERSION_RESOURCE, NO_EXTRACTED_PACKS_VERSION, value, FALLBACK_BUNDLED_PACKS_VERSION);
                    return FALLBACK_BUNDLED_PACKS_VERSION;
                }

                return version;
            }
            catch (NumberFormatException e)
            {
                log.error("Invalid bundled packs version in '{}': '{}'. Falling back to {}.", BUNDLED_PACKS_VERSION_RESOURCE, value, FALLBACK_BUNDLED_PACKS_VERSION, e);
                return FALLBACK_BUNDLED_PACKS_VERSION;
            }
        }
        catch (IOException e)
        {
            log.error("Could not read bundled packs version resource '{}'. Falling back to {}.", BUNDLED_PACKS_VERSION_RESOURCE, FALLBACK_BUNDLED_PACKS_VERSION, e);
            return FALLBACK_BUNDLED_PACKS_VERSION;
        }
    }

    private void prepareFlanOutputDir(Path flanOutputDir, @Nullable ExtractionState previousState, int resolvedPacksVersion) throws IOException
    {
        if (Files.notExists(flanOutputDir))
        {
            ensureDirectoryExists(flanOutputDir);
            return;
        }

        if (!Files.isDirectory(flanOutputDir))
            throw new IOException("Flan output path exists but is not a directory: " + flanOutputDir);

        if (isDirectoryEmptyIgnoringExtractionState(flanOutputDir))
            return;

        if (previousState != null && previousState.isCompleteForAtLeast(resolvedPacksVersion, flanOutputDir))
            return;

        backupFlanFolder(flanOutputDir, "flan_backup_" + currentTimestamp());

        ensureDirectoryExists(flanOutputDir);
    }

    private static boolean isDirectoryEmptyIgnoringExtractionState(Path directory) throws IOException
    {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory))
        {
            for (Path path : stream)
            {
                if (isExtractionStateFile(path))
                    continue;

                return false;
            }
        }

        return true;
    }

    private static void backupFlanFolder(Path flanOutputDir, String backupName) throws IOException
    {
        Path parent = flanOutputDir.getParent();
        if (parent == null)
            throw new IOException("Cannot create flan folder backup because the output directory has no parent: " + flanOutputDir);

        Path backupPath = ensureUniqueSibling(parent.resolve(backupName));
        moveDirectory(flanOutputDir, backupPath);
        log.info("Moved existing flan folder '{}' to backup '{}'.", flanOutputDir, backupPath);
    }

    private static Path ensureUniqueSibling(Path path)
    {
        if (!Files.exists(path))
            return path;

        int suffix = 1;
        Path candidate;
        do
        {
            candidate = path.resolveSibling(path.getFileName() + "_" + suffix++);
        }
        while (Files.exists(candidate));
        return candidate;
    }

    private static void moveDirectory(Path source, Path destination) throws IOException
    {
        try
        {
            Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE);
        }
        catch (AtomicMoveNotSupportedException e)
        {
            Files.move(source, destination);
        }
    }

    private static String currentTimestamp()
    {
        return LocalDateTime.now().format(BACKUP_TIMESTAMP_FORMAT);
    }

    private void ensureDirectoryExists(@Nullable Path path) throws IOException
    {
        if (path != null)
            Files.createDirectories(path);
    }

    private static Optional<ExtractionState> readExtractionState(Path statePath)
    {
        if (!Files.isRegularFile(statePath))
            return Optional.empty();

        try
        {
            return Optional.ofNullable(gson.fromJson(Files.readString(statePath, StandardCharsets.UTF_8), ExtractionState.class));
        }
        catch (Exception e)
        {
            log.warn("Could not read packs extraction state '{}': {}", statePath, e.toString());
            return Optional.empty();
        }
    }

    private void writeExtractionState(Path statePath, Path flanOutputDir, ExtractionStatus state, int resolvedPacksVersion, int lastExtractedPacksVersion, @Nullable String message) throws IOException
    {
        ensureDirectoryExists(statePath.getParent());

        ExtractionState extractionState = new ExtractionState(
            EXTRACTION_STATE_PROTOCOL_VERSION,
            state,
            resolvedPacksVersion,
            lastExtractedPacksVersion,
            normalizePathForState(flanOutputDir),
            LocalDateTime.now().toString(),
            message
        );
        Path tempPath = statePath.resolveSibling(statePath.getFileName() + ".tmp");
        Files.writeString(tempPath, gson.toJson(extractionState), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        try
        {
            Files.move(tempPath, statePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        }
        catch (AtomicMoveNotSupportedException e)
        {
            Files.move(tempPath, statePath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void writeResolvingState(Path statePath, Path flanOutputDir, int resolvedPacksVersion, int lastExtractedPacksVersion) throws IOException
    {
        writeExtractionState(statePath, flanOutputDir, ExtractionStatus.RESOLVING, resolvedPacksVersion, lastExtractedPacksVersion, null);
    }

    private void writeCompleteState(Path statePath, Path flanOutputDir, int resolvedPacksVersion, int lastExtractedPacksVersion) throws IOException
    {
        writeExtractionState(statePath, flanOutputDir, ExtractionStatus.COMPLETE, resolvedPacksVersion, lastExtractedPacksVersion, null);
    }

    private void writeFailedState(Path statePath, Path flanOutputDir, int resolvedPacksVersion, int lastExtractedPacksVersion, Exception extractionException)
    {
        try
        {
            writeExtractionState(statePath, flanOutputDir, ExtractionStatus.FAILED, resolvedPacksVersion, lastExtractedPacksVersion, extractionException.toString());
        }
        catch (IOException stateWriteException)
        {
            extractionException.addSuppressed(stateWriteException);
        }
    }

    private static String normalizePathForState(Path path)
    {
        return path.toAbsolutePath().normalize().toString();
    }

    private static boolean isExtractionStateFile(Path path)
    {
        String fileName = path.getFileName().toString();
        return fileName.equals(EXTRACTION_STATE_FILE_NAME) || fileName.equals(EXTRACTION_STATE_FILE_NAME + ".tmp");
    }

    private boolean shouldProcessEntry(JarEntry entry)
    {
        return entry.getName().startsWith("flan/") && !entry.isDirectory();
    }

    private void extractEntry(JarFile jarFile, JarEntry entry, Path flanOutputDir) throws IOException
    {
        Path relativePath = Paths.get(entry.getName().substring("flan/".length())).normalize();
        Path outputPath = flanOutputDir.resolve(relativePath).normalize();

        if (!outputPath.startsWith(flanOutputDir))
        {
            log.warn("Skipping suspicious entry path: {}", entry.getName());
            return;
        }

        ensureDirectoryExists(outputPath.getParent());

        try (InputStream freshInput = jarFile.getInputStream(entry))
        {
            Files.copy(freshInput, outputPath, StandardCopyOption.REPLACE_EXISTING);
        }

        long written = Files.size(outputPath);
        log.info("Extracted: {} -> {} ({} bytes)", entry.getName(), outputPath, written);
    }

    private record ExtractionState(int protocolVersion, ExtractionStatus state, int resolvedPacksVersion, int lastExtractedPacksVersion, @Nullable String flanOutputPath, String updatedAt, @Nullable String message)
    {
        private int effectiveLastExtractedPacksVersion()
        {
            if (lastExtractedPacksVersion > NO_EXTRACTED_PACKS_VERSION)
                return lastExtractedPacksVersion;
            if (state == ExtractionStatus.COMPLETE)
                return resolvedPacksVersion;
            return NO_EXTRACTED_PACKS_VERSION;
        }

        private boolean isCompleteForAtLeast(int expectedPacksVersion, Path expectedFlanOutputDir)
        {
            return protocolVersion == EXTRACTION_STATE_PROTOCOL_VERSION
                && state == ExtractionStatus.COMPLETE
                && effectiveLastExtractedPacksVersion() >= expectedPacksVersion
                && normalizePathForState(expectedFlanOutputDir).equals(flanOutputPath);
        }
    }

    private enum ExtractionStatus
    {
        @SerializedName("resolving")
        RESOLVING,
        @SerializedName("complete")
        COMPLETE,
        @SerializedName("failed")
        FAILED
    }
}
