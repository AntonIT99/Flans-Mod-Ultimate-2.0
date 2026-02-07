package com.flansmodultimate.packs;

import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Enumeration;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

@Mod(PacksMod.MOD_ID)
public class PacksMod
{
    public static final String MOD_ID = "flansmodultimate_packs";
    public static final Logger log = LogUtils.getLogger();

    private static final String FLAN_DIR_NAME = "flan";
    private static final String MARKER_PREFIX = ".extracted_" + MOD_ID + "_";

    public PacksMod(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();
        modEventBus.addListener(this::onConstruct);
    }

    private void onConstruct(final FMLConstructModEvent event)
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
        Path flanOutputDir = FMLPaths.GAMEDIR.get().resolve(FLAN_DIR_NAME);
        ensureDirectoryExists(flanOutputDir);

        String version = getModVersion().orElse("unknown");
        String safeVersion = makeFilenameSafe(version);
        Path markerPath = flanOutputDir.resolve(MARKER_PREFIX + safeVersion + ".marker");

        if (Files.exists(markerPath))
        {
            log.info("Packs already extracted for version {} (marker present). Skipping.", version);
            return;
        }
        log.info("Extracting packs to '{}' for {} version {}...", flanOutputDir, MOD_ID, version);

        try (JarFile jarFile = new JarFile(jarPath.toFile()))
        {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements())
            {
                JarEntry entry = entries.nextElement();
                if (!shouldProcessEntry(entry))
                    continue;

                extractEntryIfNeeded(jarFile, entry, flanOutputDir);
            }
        }

        Files.writeString(markerPath, "extracted=" + version + "\n", StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private boolean shouldProcessEntry(JarEntry entry)
    {
        return entry.getName().startsWith("flan/") && !entry.isDirectory();
    }

    private void extractEntryIfNeeded(JarFile jarFile, JarEntry entry, Path flanOutputDir) throws IOException
    {
        Path relativePath = Paths.get(entry.getName().substring("flan/".length())).normalize();
        Path outputPath = flanOutputDir.resolve(relativePath).normalize();

        if (!outputPath.startsWith(flanOutputDir))
        {
            log.warn("Skipping suspicious entry path: {}", entry.getName());
            return;
        }

        ensureDirectoryExists(outputPath.getParent());

        boolean shouldExtract = true;

        if (Files.exists(outputPath))
        {
            long entrySize = entry.getSize();
            long entryCrc = entry.getCrc();

            // Only do comparison if jar provides both size and crc
            if (entrySize >= 0 && entryCrc >= 0)
            {
                long fileSize = Files.size(outputPath);

                if (fileSize == entrySize)
                {
                    long fileCrc = crc32OfFile(outputPath);

                    if (fileCrc == entryCrc)
                        shouldExtract = false;
                }
            }
        }

        if (shouldExtract)
        {
            try (InputStream freshInput = jarFile.getInputStream(entry))
            {
                Files.copy(freshInput, outputPath, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    private static long crc32OfFile(Path path) throws IOException
    {
        CRC32 crc = new CRC32();
        try (InputStream in = Files.newInputStream(path); CheckedInputStream checked = new CheckedInputStream(in, crc))
        {
            byte[] buffer = new byte[8192];
            while (checked.read(buffer) != -1)
            {
                // reading updates CRC32
            }
        }
        return crc.getValue();
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
}