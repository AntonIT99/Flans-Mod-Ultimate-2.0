package com.flansmodultimate.packs;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Mod(FlanPacksMod.MOD_ID)
public class FlanPacksMod
{
    public static final String MOD_ID = "flansmodultimate_packs";
    public static final Logger log = LogUtils.getLogger();

    private static final String MAIN_MOD_ID = "flansmodultimate";
    private static final String FLAN_DIR_NAME = "flan";
    private static final String FALLBACK_FLAN_DIR_NAME = "Flan";
    private static final String CONTENT_LOADING_CONFIG_FILE_NAME = MAIN_MOD_ID + "-content-loading.toml";
    private static final String CONTENT_PACKS_RELATIVE_PATH_KEY = "contentPacksRelativePath";
    private static final String MARKER_PREFIX = ".extracted_" + MOD_ID + "_";
    private static final String MARKER_SUFFIX = ".marker";
    private static final DateTimeFormatter BACKUP_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

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
        Path jarPath = ModList.get().getModFileById(MOD_ID).getFile().getFilePath();
        Path flanOutputDir = resolveFlanOutputDir();

        String version = getModVersion().orElse("unknown");
        String safeVersion = makeFilenameSafe(version);
        Path markerPath = flanOutputDir.resolve(MARKER_PREFIX + safeVersion + MARKER_SUFFIX);

        if (Files.exists(markerPath))
        {
            log.info("Packs already extracted for version {} (marker present). Skipping.", version);
            return;
        }

        prepareFlanOutputDir(flanOutputDir, safeVersion);
        log.info("Extracting packs to '{}' for {} version {}...", flanOutputDir, MOD_ID, version);

        try (JarFile jarFile = new JarFile(jarPath.toFile()))
        {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements())
            {
                JarEntry entry = entries.nextElement();
                if (!shouldProcessEntry(entry))
                    continue;

                extractEntry(jarFile, entry, flanOutputDir);
            }
        }

        Files.writeString(markerPath, "extracted=" + version + "\n", StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
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

    private void prepareFlanOutputDir(Path flanOutputDir, String currentSafeVersion) throws IOException
    {
        if (Files.notExists(flanOutputDir))
        {
            ensureDirectoryExists(flanOutputDir);
            return;
        }

        if (!Files.isDirectory(flanOutputDir))
            throw new IOException("Flan output path exists but is not a directory: " + flanOutputDir);

        if (isDirectoryEmpty(flanOutputDir))
            return;

        MarkerScan markerScan = scanExtractionMarkers(flanOutputDir, currentSafeVersion);
        if (markerScan.differentVersion().isPresent())
        {
            backupFlanFolder(flanOutputDir, "flan_backup_" + markerScan.differentVersion().get() + "_" + currentTimestamp());
        }
        else if (!markerScan.anyMarker())
        {
            backupFlanFolder(flanOutputDir, "flan_backup_" + currentTimestamp());
        }

        ensureDirectoryExists(flanOutputDir);
    }

    private static boolean isDirectoryEmpty(Path directory) throws IOException
    {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory))
        {
            return !stream.iterator().hasNext();
        }
    }

    private static MarkerScan scanExtractionMarkers(Path directory, String currentSafeVersion) throws IOException
    {
        boolean anyMarker = false;
        List<String> differentVersions = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory))
        {
            for (Path path : stream)
            {
                if (!Files.isRegularFile(path))
                    continue;

                String fileName = path.getFileName().toString();
                if (!fileName.startsWith(MARKER_PREFIX) || !fileName.endsWith(MARKER_SUFFIX))
                    continue;

                anyMarker = true;
                String markerVersion = fileName.substring(MARKER_PREFIX.length(), fileName.length() - MARKER_SUFFIX.length());
                if (!markerVersion.equals(currentSafeVersion))
                    differentVersions.add(markerVersion.isBlank() ? "unknown" : markerVersion);
            }
        }

        Collections.sort(differentVersions);
        return new MarkerScan(anyMarker, differentVersions.stream().findFirst());
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

    private static Optional<String> getModVersion()
    {
        return ModList.get()
            .getModContainerById(MOD_ID)
            .map(c -> c.getModInfo().getVersion().toString());
    }

    private static String makeFilenameSafe(String s)
    {
        return s.replaceAll("[^A-Za-z0-9._-]", "_");
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

    private record MarkerScan(boolean anyMarker, Optional<String> differentVersion) {}
}
