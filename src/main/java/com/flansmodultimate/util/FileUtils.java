package com.flansmodultimate.util;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.IContentProvider;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileUtils
{
    private FileUtils() {}

    /** If a destination path already exists (or would alias on case-insensitive FS), append -1, -2, ... */
    public static Path ensureUnique(Path dst)
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
        int i = 1;
        Path candidate;
        do
        {
            candidate = dst.getParent().resolve(name + "-" + i++ + ext);
        }
        while (Files.exists(candidate));
        return candidate;
    }

    /** Returns true if the current filename differs from the sanitized target name. */
    public static boolean needsRename(Path p)
    {
        String current = p.getFileName().toString();
        String target  = sanitizedOggName(current);
        return !current.equals(target);
    }

    /** Safe move that handles case-only renames on case-insensitive filesystems. */
    public static void movePossiblyCaseOnly(Path src, Path dst) throws IOException
    {
        String srcName = src.getFileName().toString();
        String dstName = dst.getFileName().toString();

        // Case-only rename? Use a temp hop.
        if (srcName.equalsIgnoreCase(dstName) && !srcName.equals(dstName))
        {
            Path tmp = src.resolveSibling(srcName + "." + UUID.randomUUID() + ".tmp");
            Files.move(src, tmp, StandardCopyOption.REPLACE_EXISTING);
            Files.move(tmp, dst, StandardCopyOption.REPLACE_EXISTING);
        }
        else
        {
            Files.move(src, dst, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static boolean isOgg(Path p)
    {
        String n = p.getFileName().toString();
        int dot = n.lastIndexOf('.');
        return dot >= 0 && n.substring(dot).equalsIgnoreCase(".ogg");
    }

    /** Build sanitized target filename for an .ogg (lowercase, safe chars, force .ogg). */
    private static String sanitizedOggName(String currentName)
    {
        int dot = currentName.lastIndexOf('.');
        String base = dot >= 0 ? currentName.substring(0, dot) : currentName;
        String sanitizedBase = ResourceUtils.sanitize(base); // must lowercase + map illegal chars to '_'
        return sanitizedBase + ".ogg";
    }

    /** Sanitize a relative path: lowercase, replace spaces with '_', remove illegal chars, force .png extension. */
    public static String sanitizePngRelPath(Path rel)
    {
        StringBuilder out = new StringBuilder();
        for (Path part : rel)
        {
            String name = part.getFileName().toString();
            int dot = name.lastIndexOf('.');
            if (dot >= 0)
            {
                name = name.substring(0, dot);
            }
            // sanitize basename and enforce .png extension
            String finalName = ResourceUtils.sanitize(name) + ".png";

            if (!out.isEmpty())
                out.append('/');
            out.append(finalName);
        }
        // collapse any accidental double slashes, trim leading slash
        String s = out.toString().replaceAll("/{2,}", "/");
        if (s.startsWith("/")) s = s.substring(1);
        return s;
    }

    public static boolean filesHaveDifferentBytes(Path file1, Path file2)
    {
        if (!Files.exists(file1) || !Files.exists(file2))
            return false;

        try
        {
            return differentBytes(Files.readAllBytes(file1), Files.readAllBytes(file2));
        }
        catch (IOException e)
        {
            FlansMod.log.error("Could not compare files {} and {}", file1, file2, e);
            return false;
        }
    }

    public static boolean differentBytes(byte[] bytes1, byte[] bytes2)
    {
        return !Arrays.equals(bytes1, bytes2);
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
            FlansMod.log.error("Could not compare images {} and {}", file1, file2, e);
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
                FlansMod.log.error("Failed to open {}", provider.getPath(), e);
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
                FlansMod.log.error("Failed to close {}", provider.getPath(), e);
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
        Path extractingMarker = outputDir.resolve("EXTRACTING");
        Path readyMarker = outputDir.resolve("READY");

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(archivePath)))
        {
            Files.createDirectories(outputDir);
            Files.deleteIfExists(readyMarker);
            Files.writeString(extractingMarker, "extracting " + Instant.now(), StandardCharsets.UTF_8);

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null)
            {
                Path outPath = outputDir.resolve(entry.getName()).normalize();

                // Zip Slip protection
                if (!outPath.startsWith(outputDir))
                    throw new IOException("Blocked zip entry (zip slip): " + entry.getName());

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

            // mark success
            Files.deleteIfExists(extractingMarker);
            Files.writeString(readyMarker, "ready " + Instant.now(), StandardCharsets.UTF_8);
        }
        catch (IOException e)
        {
            FlansMod.log.error("Failed to extract archive for content pack {}", archivePath, e);
        }
    }

    public static void repackArchive(IContentProvider provider)
    {
        Path target = provider.isJarFile()
            ? provider.getPath().getParent().resolve(FilenameUtils.getBaseName(provider.getName()) + ".zip")
            : provider.getPath();

        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        Path bak = target.resolveSibling(target.getFileName() + ".bak");

        try
        {
            // Ensure tmp from previous crash doesn't mess us up
            Files.deleteIfExists(tmp);

            // 1) Write new archive to tmp (NOT to target)
            URI uri = URI.create("jar:" + tmp.toUri());
            try (FileSystem zipFs = FileSystems.newFileSystem(uri, Map.of("create", "true")))
            {
                try (Stream<Path> stream = Files.walk(provider.getExtractedPath()))
                {
                    stream.forEach(source ->
                    {
                        try
                        {
                            Path rel = provider.getExtractedPath().relativize(source);
                            if (rel.toString().isEmpty())
                                return;

                            Path zipEntry = zipFs.getPath("/").resolve(rel.toString());

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
                            // Re-throw so we fail the repack and do NOT replace the original archive
                            throw new RuntimeException(e);
                        }
                    });
                }
            }
            catch (RuntimeException re)
            {
                // unwrap and rethrow as IOException for consistent error handling
                Throwable cause = re.getCause();
                if (cause instanceof IOException ioe)
                    throw ioe;
                throw re;
            }

            // 2) Swap: target -> bak, tmp -> target (atomic when supported)
            atomicReplace(tmp, target, bak);

            // 3) If we created a .zip from a .jar, update provider
            if (provider.isJarFile())
                provider.update(FilenameUtils.getBaseName(provider.getName()) + ".zip", target);

            // 4) Cleanup extracted dir (only after successful commit)
            deleteRecursively(provider.getExtractedPath());

            // 5) remove backup after success
            Files.deleteIfExists(bak);
        }
        catch (IOException e)
        {
            FlansMod.log.error("Error repacking archive {}", provider.getExtractedPath(), e);
            // Best-effort cleanup of tmp (leave bak intact for recovery)
            try
            {
                Files.deleteIfExists(tmp);
            }
            catch (IOException ignored)
            {
                // Ignored
            }
        }
    }

    private static void atomicReplace(Path tmp, Path target, Path backup) throws IOException
    {
        // Move target to backup (best effort)
        if (Files.exists(target))
        {
            try
            {
                Files.move(target, backup, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            }
            catch (AtomicMoveNotSupportedException e)
            {
                Files.move(target, backup, StandardCopyOption.REPLACE_EXISTING);
            }
        }

        // Move tmp into place
        try
        {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        }
        catch (AtomicMoveNotSupportedException e)
        {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
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
                    FlansMod.log.error("Failed to delete {}", path, e);
                }
            });
        }
        catch (IOException e)
        {
            FlansMod.log.error("Failed to delete {}", dir, e);
        }
    }

    public static void cleanupFlanTempOnStartup(Iterable<IContentProvider> providers)
    {
        // tune these to taste
        Duration extractingMaxAge = Duration.ofMinutes(15);
        Duration readyMaxAge = Duration.ofHours(24);

        Set<Path> roots = new HashSet<>();
        for (IContentProvider p : providers)
        {
            if (p != null && p.isArchive())
            {
                try
                {
                    roots.add(p.getTempRoot());
                }
                catch (Exception ignored)
                {
                    // Ignored
                }
            }
        }

        for (Path root : roots)
        {
            cleanupTempRoot(root, extractingMaxAge, readyMaxAge);
        }
    }

    private static void cleanupTempRoot(Path tempRoot, Duration extractingMaxAge, Duration readyMaxAge)
    {
        if (tempRoot == null || Files.notExists(tempRoot))
            return;

        try (Stream<Path> entries = Files.list(tempRoot))
        {
            Instant now = Instant.now();

            entries.filter(Files::isDirectory).forEach(dir ->
            {
                Path extracting = dir.resolve("EXTRACTING");
                Path ready = dir.resolve("READY");

                try
                {
                    if (Files.exists(extracting))
                    {
                        java.time.Instant t = Files.getLastModifiedTime(extracting).toInstant();
                        if (t.plus(extractingMaxAge).isBefore(now))
                        {
                            FlansMod.log.warn("Cleaning stale temp extraction dir {}", dir);
                            deleteRecursively(dir);
                        }
                        return;
                    }

                    if (Files.exists(ready))
                    {
                        Instant t = Files.getLastModifiedTime(ready).toInstant();
                        if (t.plus(readyMaxAge).isBefore(now))
                        {
                            deleteRecursively(dir);
                        }
                    }
                    else
                    {
                        // No marker? Treat as suspicious; delete if old enough (use dir mtime)
                        Instant t = Files.getLastModifiedTime(dir).toInstant();
                        if (t.plus(readyMaxAge).isBefore(now))
                        {
                            deleteRecursively(dir);
                        }
                    }
                }
                catch (Exception e)
                {
                    FlansMod.log.debug("Temp cleanup skipped for {}: {}", dir, e.toString());
                }
            });
        }
        catch (IOException e)
        {
            FlansMod.log.debug("Failed to list temp root {}: {}", tempRoot, e.toString());
        }
    }

    public static void prepareFreshExtractionDir(Path outputDir)
    {
        try
        {
            if (Files.exists(outputDir))
            {
                deleteRecursively(outputDir);
            }
            Files.createDirectories(outputDir);
        }
        catch (IOException e)
        {
            FlansMod.log.error("Failed to prepare extraction dir {}", outputDir, e);
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
