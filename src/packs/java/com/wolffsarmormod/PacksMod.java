package com.wolffsarmormod;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Mod(PacksMod.MOD_ID)
public class PacksMod
{
    public static final String MOD_ID = "wolffsarmorpacks";

    public static final Logger log = LogUtils.getLogger();

    public PacksMod()
    {
        if (FMLEnvironment.production)
        {
            extractFlanFolderIfNeeded();
        }
    }

    private void extractFlanFolderIfNeeded() {
        try
        {
            Path jarPath = ModList.get().getModFileById(MOD_ID).getFile().getFilePath();
            Path flanOutputDir = FMLPaths.GAMEDIR.get().resolve("flan");
            ensureDirectoryExists(flanOutputDir);

            try (JarFile jarFile = new JarFile(jarPath.toFile()))
            {
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements())
                {
                    JarEntry entry = entries.nextElement();
                    if (shouldProcessEntry(entry))
                    {
                        extractEntryIfNeeded(jarFile, entry, flanOutputDir);
                    }
                }
            }
        }
        catch (Exception e)
        {
            log.error("Failed to extract content packs to flan folder", e);
        }
    }

    private boolean shouldProcessEntry(JarEntry entry)
    {
        return entry.getName().startsWith("flan/") && !entry.isDirectory();
    }

    private void extractEntryIfNeeded(JarFile jarFile, JarEntry entry, Path flanOutputDir) throws IOException
    {
        Path relativePath = Paths.get(entry.getName().substring("flan/".length()));
        Path outputPath = flanOutputDir.resolve(relativePath);
        ensureDirectoryExists(outputPath.getParent());

        try (InputStream jarInput = jarFile.getInputStream(entry))
        {
            boolean shouldExtract = true;

            if (Files.exists(outputPath) && Files.size(outputPath) == entry.getSize())
            {
                byte[] jarHash = hashStream(jarInput);
                byte[] existingHash;
                try (InputStream existingInput = Files.newInputStream(outputPath))
                {
                    existingHash = hashStream(existingInput);
                }

                if (MessageDigest.isEqual(jarHash, existingHash))
                {
                    shouldExtract = false;
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
    }

    private void ensureDirectoryExists(Path path) throws IOException {
        if (!Files.exists(path))
        {
            Files.createDirectories(path);
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    private static byte[] hashStream(InputStream input) throws IOException
    {
        MessageDigest digest;
        try
        {
            digest = MessageDigest.getInstance("SHA-1");
        }
        catch (Exception e)
        {
            throw new IOException("Unable to get SHA-1 MessageDigest", e);
        }

        try (DigestInputStream dis = new DigestInputStream(input, digest))
        {
            byte[] buffer = new byte[4096];
            while (dis.read(buffer) != -1) {}
        }
        return digest.digest();
    }
}