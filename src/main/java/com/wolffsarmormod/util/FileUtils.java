package com.wolffsarmormod.util;

import com.wolffsarmormod.ArmorMod;
import com.wolffsarmormod.IContentProvider;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileUtils
{
    private FileUtils() {}

    /** If a destination path already exists (or would alias on case-insensitive FS), append -1, -2, ... */
    public static Path ensureUnique(Path dst) throws IOException
    {
        if (!Files.exists(dst))
            return dst;

        String file = dst.getFileName().toString();
        String name = file;
        String ext = "";
        int dot = file.lastIndexOf('.');
        if (dot >= 0)
        {
            name = file.substring(0, dot);
            ext = file.substring(dot);
        }
        AtomicInteger i = new AtomicInteger(1);
        Path parent = dst.getParent();
        Path candidate;
        do {
            candidate = parent.resolve(name + "-" + i.getAndIncrement() + ext);
        }
        while (Files.exists(candidate));
        return candidate;
    }

    public static boolean filesHaveDifferentBytesContent(Path file1, Path file2)
    {
        if (!Files.exists(file1) || !Files.exists(file2))
        {
            return false;
        }
        try
        {
            return !Arrays.equals(Files.readAllBytes(file1), Files.readAllBytes(file2));
        }
        catch (IOException e)
        {
            ArmorMod.log.error("Could not compare files {} and {}", file1, file2, e);
            return false;
        }
    }

    public static boolean isSameImage(Path file1, Path file2)
    {
        BufferedImage img1 = null;
        BufferedImage img2 = null;

        try (InputStream in1 = Files.newInputStream(file1); InputStream in2 = Files.newInputStream(file2))
        {
            img1 = ImageIO.read(in1);
            img2 = ImageIO.read(in2);
        }
        catch (IOException e)
        {
            ArmorMod.log.error("Could not compare images {} and {}", file1, file2, e);
        }

        if (img1 == null || img2 == null)
        {
            return false;
        }

        if (img1.getWidth() != img2.getWidth() || img1.getHeight() != img2.getHeight())
        {
            return false;
        }

        for (int y = 0; y < img1.getHeight(); y++)
        {
            for (int x = 0; x < img1.getWidth(); x++)
            {
                if (img1.getRGB(x, y) != img2.getRGB(x, y))
                {
                    return false;
                }
            }
        }

        return true;
    }

    @Nullable
    public static FileSystem createFileSystem(IContentProvider provider)
    {
        if (provider.isArchive())
        {
            try
            {
                return FileSystems.newFileSystem(provider.getPath());
            }
            catch (IOException e)
            {
                ArmorMod.log.error("Failed to open {}", provider.getPath(), e);
            }
        }
        return null;
    }

    public static void closeFileSystem(@Nullable FileSystem fs, IContentProvider provider)
    {
        if (fs != null)
        {
            try
            {
                fs.close();
            }
            catch (IOException e)
            {
                ArmorMod.log.error("Failed to close {}", provider.getPath(), e);
            }
        }
    }


    public static DirectoryStream<Path> createDirectoryStream(IContentProvider provider) throws IOException
    {
        if (provider.isDirectory())
        {
            return Files.newDirectoryStream(provider.getPath());
        }
        else if (provider.isArchive())
        {
            FileSystem fs = FileSystems.newFileSystem(provider.getPath());
            return new AutoCloseableDirectoryStream(Files.newDirectoryStream(fs.getPath("/")), fs);
        }
        throw new IllegalArgumentException("Content Pack must be either a directory or a ZIP/JAR-archive");
    }

    public static void extractArchive(Path archivePath, Path outputDir)
    {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(archivePath)))
        {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null)
            {
                Path outPath = outputDir.resolve(entry.getName());

                if (entry.isDirectory())
                {
                    Files.createDirectories(outPath);
                }
                else
                {
                    Files.createDirectories(outPath.getParent());
                    try (OutputStream os = Files.newOutputStream(outPath))
                    {
                        zis.transferTo(os);
                    }
                }
            }
        }
        catch (IOException e)
        {
            ArmorMod.log.error("Failed to extract archive for content pack {}", archivePath, e);
        }
    }

    public static void repackArchive(IContentProvider provider)
    {
        try
        {
            Files.deleteIfExists(provider.getPath());
            Path archivePath = provider.isJarFile() ? provider.getPath().getParent().resolve(FilenameUtils.getBaseName(provider.getName()) + ".zip") : provider.getPath();
            Files.deleteIfExists(archivePath);

            URI uri = URI.create("jar:" + archivePath.toUri());
            try (FileSystem zipFs = FileSystems.newFileSystem(uri, Map.of("create", "true")))
            {
                try (Stream<Path> stream = Files.walk(provider.getExtractedPath()))
                {
                    stream.forEach(source ->
                    {
                        try
                        {
                            Path relativePath = provider.getExtractedPath().relativize(source);
                            Path zipEntry = zipFs.getPath("/").resolve(relativePath.toString());

                            if (Files.isDirectory(source))
                            {
                                Files.createDirectories(zipEntry);
                            }
                            else
                            {
                                Files.createDirectories(zipEntry.getParent());
                                Files.copy(source, zipEntry, StandardCopyOption.REPLACE_EXISTING);
                            }
                        }
                        catch (IOException e)
                        {
                            ArmorMod.log.error("Error repacking archive {}", provider.getExtractedPath(), e);
                        }
                    });
                }
            }

            if (provider.isJarFile())
                provider.update(FilenameUtils.getBaseName(provider.getName()) + ".zip", archivePath);

            deleteRecursively(provider.getExtractedPath());
        }
        catch (IOException e)
        {
            ArmorMod.log.error("Error repacking archive {}", provider.getExtractedPath(), e);
        }
    }

    public static void deleteRecursively(Path dir)
    {
        if (Files.notExists(dir)) return;

        try (Stream<Path> stream = Files.walk(dir))
        {
            stream.sorted(Comparator.reverseOrder())
            .forEach(path ->
            {
                try
                {
                    Files.delete(path);
                }
                catch (IOException e)
                {
                    ArmorMod.log.error("Failed to delete {}", path, e);
                }
            });
        }
        catch (IOException e)
        {
            ArmorMod.log.error("Failed to delete {}", dir, e);
        }
    }

    private record AutoCloseableDirectoryStream(DirectoryStream<Path> delegate, FileSystem fileSystem) implements DirectoryStream<Path>
    {
        @Override
        public void close() throws IOException
        {
            delegate.close();
            fileSystem.close();
        }

        @Override
        @NotNull
        public Iterator<Path> iterator()
        {
            return delegate.iterator();
        }
    }
}
