package com.flansmodultimate.util;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.IContentProvider;
import lombok.NoArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class FileUtils
{
    public static final String CLASS_EXTENSION = ".class";
    public static final String JAR_EXTENSION = ".jar";
    public static final String JSON_EXTENSION = ".json";
    public static final String LANG_EXTENSION = ".lang";
    public static final String OGG_EXTENSION = ".ogg";
    public static final String PNG_EXTENSION = ".png";
    public static final String TXT_EXTENSION = ".txt";
    public static final String ZIP_EXTENSION = ".zip";

    private static final long IMAGE_COMPARE_TIMEOUT_SECONDS = 5L;
    private static final long MAX_IMAGE_PIXELS_FOR_COMPARE = 67_108_864L;
    private static final int MAX_IMAGE_COMPARE_PIXELS_PER_CHUNK = 1_048_576;
    private static final AtomicInteger IMAGE_COMPARE_THREAD_ID = new AtomicInteger();
    private static final Object FILE_LOCK_MONITOR = new Object();
    private static final ExecutorService IMAGE_COMPARE_EXECUTOR = Executors.newCachedThreadPool(r ->
    {
        Thread thread = new Thread(r, "flansmod-image-compare-" + IMAGE_COMPARE_THREAD_ID.incrementAndGet());
        thread.setDaemon(true);
        return thread;
    });

    /**
     * Writes UTF-8 text to a file and logs failures instead of throwing them.
     *
     * @param outputFile target file to write
     * @param content text content to write
     */
    public static void writeString(Path outputFile, String content)
    {
        try
        {
            Files.writeString(outputFile, content, StandardCharsets.UTF_8);
        }
        catch (IOException e)
        {
            FlansMod.log.error("Could not create {}", outputFile, e);
        }
    }

    /**
     * Deletes a file if it exists and logs failures instead of throwing them.
     *
     * @param file file to delete
     */
    public static void deleteIfExists(Path file)
    {
        try
        {
            Files.deleteIfExists(file);
        }
        catch (IOException e)
        {
            FlansMod.log.error("Could not delete {}", file, e);
        }
    }

    /**
     * Finds an unused sibling path by appending a numeric suffix when the destination exists.
     *
     * @param dst desired destination path
     * @return {@code dst} when it does not exist, otherwise a sibling with a {@code -1}, {@code -2},
     * etc. suffix before the extension
     */
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

    /**
     * Checks whether an OGG file name needs sanitizing.
     *
     * @param p path whose file name should be checked
     * @return {@code true} when the current file name differs from its sanitized OGG target name
     */
    public static boolean needsRename(Path p)
    {
        String current = p.getFileName().toString();
        String target  = sanitizedOggName(current);
        return !current.equals(target);
    }

    /**
     * Moves a file while safely handling case-only renames on case-insensitive filesystems.
     *
     * @param src source file to move
     * @param dst destination file
     * @throws IOException when either move operation fails
     */
    public static void moveWithCaseOnlyHopIfNeeded(Path src, Path dst) throws IOException
    {
        String srcName = src.getFileName().toString();
        String dstName = dst.getFileName().toString();

        boolean sameDir = src.getParent() != null && src.getParent().equals(dst.getParent());
        boolean caseOnly = sameDir && srcName.equalsIgnoreCase(dstName) && !srcName.equals(dstName);

        if (caseOnly)
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

    /**
     * Renames a file to its sanitized lowercase resource name.
     *
     * @param file file to rename
     * @return the renamed path, or the original path when no rename was needed
     * @throws IOException when the rename fails
     */
    public static Path renameToLowercase(Path file) throws IOException
    {
        String name = file.getFileName().toString();
        String lower = ResourceUtils.sanitize(name);
        if (name.equals(lower))
            return file;

        Path target = file.resolveSibling(lower);
        moveWithCaseOnlyHopIfNeeded(file, target);
        return target;
    }

    /**
     * Checks whether a path has an {@code .ogg} file extension.
     *
     * @param p path to inspect
     * @return {@code true} when the file name ends with {@code .ogg}, case-insensitively
     */
    public static boolean isOgg(Path p)
    {
        String n = p.getFileName().toString();
        int dot = n.lastIndexOf('.');
        return dot >= 0 && n.substring(dot).equalsIgnoreCase(OGG_EXTENSION);
    }

    /**
     * Builds the sanitized target filename for an OGG file.
     *
     * @param currentName current file name
     * @return sanitized lowercase base name with a forced {@code .ogg} extension
     */
    private static String sanitizedOggName(String currentName)
    {
        int dot = currentName.lastIndexOf('.');
        String base = dot >= 0 ? currentName.substring(0, dot) : currentName;
        String sanitizedBase = ResourceUtils.sanitize(base); // must lowercase + map illegal chars to '_'
        return sanitizedBase + OGG_EXTENSION;
    }

    /**
     * Sanitizes a relative PNG path for resource output.
     * <p>
     * Each path segment is sanitized independently, converted to lowercase by
     * {@link ResourceUtils#sanitize(String)}, and forced to use the {@code .png} extension.
     *
     * @param rel relative source path
     * @return sanitized relative path using forward slashes
     */
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

    /**
     * If desired exists and has the same content as src -> return null (skip).
     * If desired exists and differs -> return ensureUnique(desired).
     * If desired doesn't exist -> return desired.
     */
    public static Path skipIfSameElseEnsureUnique(Path src, Path desired)
    {
        if (!Files.exists(desired))
            return desired;

        // desired exists: if content is the same, do nothing
        if (!isDifferentFileContent(src, desired, false))
            return null;

        // desired exists but different content -> pick a unique name
        return ensureUnique(desired);
    }

    /**
     * Compares two files by content.
     * <p>
     * Non-image files use cheap size and byte checks. Image files still use byte equality as the
     * fast path, but when bytes differ they are decoded and compared by normalized ARGB pixels so
     * differently encoded copies of the same image are treated as equivalent. If either file is
     * missing, {@code assumeDifferentWhenMissing} is returned. If comparison fails for any other
     * reason, this method returns {@code true} so callers refresh or keep files separate instead of
     * silently treating unknown content as identical.
     *
     * @param file1 first file to compare
     * @param file2 second file to compare
     * @param assumeDifferentWhenMissing value to return when either file does not exist
     * @return {@code true} when the files should be treated as different
     */
    public static boolean isDifferentFileContent(Path file1, Path file2, boolean assumeDifferentWhenMissing)
    {
        if (!Files.exists(file1) || !Files.exists(file2))
            return assumeDifferentWhenMissing;

        boolean bothImages = isImageFile(file1) && isImageFile(file2);

        try
        {
            if (Files.isSameFile(file1, file2))
                return false;

            // For non-images, size mismatch => different (fast fail)
            if (!bothImages && Files.size(file1) != Files.size(file2))
                return true;

            // Fast path: identical bytes => same content
            if (Files.mismatch(file1, file2) == -1)
                return false;

            // Bytes differ: if both are images, treat as same if pixel-identical
            if (bothImages)
                return !isSameImage(file1, file2);

            return true;
        }
        catch (IOException e)
        {
            FlansMod.log.error("Could not compare files {} and {}", file1, file2, e);
            return true;
        }
    }

    /**
     * Compares two files by raw bytes only.
     * <p>
     * This is the preferred comparison for generated files that are written with
     * {@link Files#copy(Path, Path, java.nio.file.CopyOption...)} or other byte-preserving writes.
     * It avoids image decoding and returns {@code true} on read errors so callers refresh or keep
     * files separate instead of treating unknown content as identical.
     *
     * @param file1 first file to compare
     * @param file2 second file to compare
     * @param assumeDifferentWhenMissing value to return when either file does not exist
     * @return {@code true} when the files should be treated as byte-different
     */
    public static boolean isDifferentFileBytes(Path file1, Path file2, boolean assumeDifferentWhenMissing)
    {
        if (!Files.exists(file1) || !Files.exists(file2))
            return assumeDifferentWhenMissing;

        try
        {
            if (Files.isSameFile(file1, file2))
                return false;

            return Files.size(file1) != Files.size(file2) || Files.mismatch(file1, file2) != -1;
        }
        catch (IOException e)
        {
            FlansMod.log.error("Could not compare file bytes {} and {}", file1, file2, e);
            return true;
        }
    }

    /**
     * Compares a file with in-memory bytes using a streaming byte-by-byte comparison.
     * <p>
     * If the file is missing, {@code assumeDifferentWhenMissing} is returned. If the file cannot be
     * read, this method returns {@code true} so callers rewrite or regenerate the file rather than
     * assuming the existing content is correct.
     *
     * @param file file to compare
     * @param data expected file content
     * @param assumeDifferentWhenMissing value to return when the file does not exist
     * @return {@code true} when the file content differs from {@code data}
     */
    public static boolean isDifferentFileContent(Path file, byte[] data, boolean assumeDifferentWhenMissing)
    {
        if (!Files.exists(file))
            return assumeDifferentWhenMissing;

        try (InputStream in = Files.newInputStream(file))
        {
            int off = 0;
            byte[] buf = new byte[8192];

            while (true)
            {
                int r = in.read(buf);
                if (r < 0)
                    break;

                // file has more data than expected
                if (off + r > data.length)
                    return true;

                for (int i = 0; i < r; i++)
                {
                    if (buf[i] != data[off + i])
                        return true;
                }
                off += r;
            }

            // file ended early (or exactly)
            return off != data.length;
        }
        catch (IOException e)
        {
            FlansMod.log.error("Could not compare file {} with in-memory data", file, e);
            return true; // safest: treat as different so we refresh it
        }
    }

    /**
     * Checks whether a path has a file extension that this utility treats as an image candidate.
     * Actual image decoding is still validated later; unsupported or corrupt image files are not
     * considered identical by pixel comparison.
     *
     * @param p path to inspect
     * @return {@code true} when the file name has a known image extension
     */
    private static boolean isImageFile(Path p)
    {
        String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
        return n.endsWith(PNG_EXTENSION) || n.endsWith(".jpg") || n.endsWith(".jpeg")
            || n.endsWith(".gif") || n.endsWith(".bmp") || n.endsWith(".webp");
    }

    /**
     * Compares two image files by decoded normalized ARGB pixels.
     * <p>
     * Decoding is performed on a daemon worker with a timeout because some malformed images or image
     * readers can block indefinitely. Timeout, interruption, unsupported formats, oversized images,
     * decode errors, or any other failure return {@code false}. Pixel comparison is chunked by rows
     * to avoid allocating one large temporary array per image.
     *
     * @param file1 first image file
     * @param file2 second image file
     * @return {@code true} only when both images decode successfully and all pixels match
     */
    private static boolean isSameImage(Path file1, Path file2)
    {
        Future<Boolean> result = IMAGE_COMPARE_EXECUTOR.submit(() -> hasSameImagePixels(file1, file2));

        try
        {
            return result.get(IMAGE_COMPARE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
        catch (TimeoutException e)
        {
            result.cancel(true);
            FlansMod.log.warn("Timed out comparing image pixels between {} and {}", file1, file2);
            return false;
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            result.cancel(true);
            FlansMod.log.warn("Interrupted while comparing image pixels between {} and {}", file1, file2);
            return false;
        }
        catch (ExecutionException e)
        {
            FlansMod.log.warn("Could not compare image pixels between {} and {}: {}", file1, file2, String.valueOf(e.getCause()));
            return false;
        }
    }

    /**
     * Decodes both images and compares dimensions plus normalized ARGB pixels.
     *
     * @param file1 first image file
     * @param file2 second image file
     * @return {@code true} when both images decode and have identical dimensions and pixels
     * @throws IOException when an image reader fails while inspecting or decoding either file
     */
    private static boolean hasSameImagePixels(Path file1, Path file2) throws IOException
    {
        BufferedImage img1 = readImage(file1);
        BufferedImage img2 = readImage(file2);

        if (img1 == null || img2 == null)
            return false;

        int w = img1.getWidth();
        int h = img1.getHeight();
        if (w != img2.getWidth() || h != img2.getHeight())
            return false;

        int rowsPerChunk = Math.max(1, Math.min(h, MAX_IMAGE_COMPARE_PIXELS_PER_CHUNK / w));
        int[] pixels1 = new int[w * rowsPerChunk];
        int[] pixels2 = new int[w * rowsPerChunk];

        for (int y = 0; y < h; y += rowsPerChunk)
        {
            int rows = Math.min(rowsPerChunk, h - y);
            int pixels = w * rows;

            img1.getRGB(0, y, w, rows, pixels1, 0, w);
            img2.getRGB(0, y, w, rows, pixels2, 0, w);

            for (int i = 0; i < pixels; i++)
            {
                if (pixels1[i] != pixels2[i])
                    return false;
            }
        }

        return true;
    }

    /**
     * Reads the first image frame from a file through an explicit {@link ImageReader}.
     * <p>
     * The image dimensions are checked before full decode so clearly invalid or unreasonably large
     * files fail before allocating the decoded image buffer.
     *
     * @param file image file to decode
     * @return the decoded image, or {@code null} when no reader is available or dimensions are invalid
     * @throws IOException when the selected image reader fails while reading metadata or pixels
     */
    @Nullable
    private static BufferedImage readImage(Path file) throws IOException
    {
        try (InputStream fileInput = Files.newInputStream(file);
             ImageInputStream input = ImageIO.createImageInputStream(fileInput))
        {
            if (input == null)
                return null;

            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext())
                return null;

            ImageReader reader = readers.next();
            try
            {
                reader.setInput(input, true, true);

                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > MAX_IMAGE_PIXELS_FOR_COMPARE)
                    return null;

                return reader.read(0);
            }
            finally
            {
                reader.dispose();
            }
        }
    }

    /**
     * Creates a directory and any missing parent directories.
     * <p>
     * Failures are logged and reported as {@code false} instead of being thrown.
     *
     * @param path directory path to create
     * @return {@code true} when the directory exists or was created successfully
     */
    public static boolean tryCreateDirectories(Path path)
    {
        if (path == null)
        {
            FlansMod.log.error("Could not create directory: path is null");
            return false;
        }

        try
        {
            Files.createDirectories(path);
            return true;
        }
        catch (IOException | SecurityException e)
        {
            FlansMod.log.error("Could not create directory {}", path.toAbsolutePath(), e);
            return false;
        }
    }

    /**
     * Runs an operation while holding an exclusive OS file lock.
     * <p>
     * The lock serializes shared filesystem mutations across separate game instances that use the
     * same directory. A local JVM monitor is also used because Java throws
     * {@link OverlappingFileLockException} instead of waiting when the same JVM already owns an
     * overlapping lock. If the lock cannot be created or acquired, the operation is not run.
     *
     * @param lockFile file used as the lock target
     * @param operationName human-readable operation name for log messages
     * @param operation operation to execute while the lock is held
     * @return {@code true} if the operation ran, {@code false} if the lock could not be acquired
     */
    public static boolean runWithFileLock(Path lockFile, String operationName, Runnable operation)
    {
        if (lockFile == null)
        {
            FlansMod.log.error("Cannot run locked operation '{}': lock file is null", operationName);
            return false;
        }

        Path normalizedLockFile = lockFile.toAbsolutePath().normalize();
        Path lockParent = normalizedLockFile.getParent();
        if (lockParent != null && !tryCreateDirectories(lockParent))
            return false;

        synchronized (FILE_LOCK_MONITOR)
        {
            try (FileChannel channel = FileChannel.open(normalizedLockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE))
            {
                FlansMod.log.debug("Waiting for {} lock at {}", operationName, normalizedLockFile);
                try (FileLock ignored = channel.lock())
                {
                    FlansMod.log.debug("Acquired {} lock at {}", operationName, normalizedLockFile);
                    operation.run();
                    return true;
                }
            }
            catch (IOException | OverlappingFileLockException e)
            {
                FlansMod.log.error("Could not acquire {} lock at {}", operationName, normalizedLockFile, e);
                return false;
            }
        }
    }

    /**
     * Opens a filesystem view for an archive content provider.
     *
     * @param provider content provider to open
     * @return a filesystem for archive providers, or {@code null} for non-archives or open failures
     */
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

    /**
     * Closes a filesystem opened for a content provider and logs close failures.
     *
     * @param fs filesystem to close, or {@code null}
     * @param provider provider used only for contextual logging
     */
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


    /**
     * Creates a directory stream over the root of a content provider.
     * <p>
     * Directory providers are streamed directly. Archive providers are opened as a filesystem and the
     * returned stream closes that filesystem when the stream itself is closed.
     *
     * @param provider content provider to read
     * @return directory stream for the provider root
     * @throws IOException when the provider directory or archive cannot be opened
     * @throws IllegalArgumentException when the provider is neither a directory nor an archive
     */
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

    /**
     * Extracts a ZIP or JAR archive into a prepared output directory.
     * <p>
     * Entry names are sanitized and normalized before writing, and Zip Slip attempts are rejected.
     * Failures are logged and reported as {@code false}.
     *
     * @param archivePath archive file to extract
     * @param outputDir destination directory
     * @return {@code true} when extraction completed successfully
     */
    public static boolean extractArchive(Path archivePath, Path outputDir)
    {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(archivePath)))
        {
            Files.createDirectories(outputDir);

            Path normalizedOutputDir = outputDir.toAbsolutePath().normalize();

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null)
            {
                String rawName = entry.getName();
                String safeName = sanitizeArchiveEntryName(rawName);

                if (safeName.isBlank())
                {
                    FlansMod.log.warn("Skipping archive entry with empty sanitized name: '{}'", rawName);
                    continue;
                }

                if (wasMeaningfullySanitized(rawName, safeName, entry.isDirectory()))
                    FlansMod.log.warn("Sanitized invalid archive entry name: '{}' -> '{}'", rawName, safeName);

                Path outPath = normalizedOutputDir.resolve(safeName).normalize();

                // Zip Slip protection
                if (!outPath.startsWith(normalizedOutputDir))
                    throw new IOException("Blocked zip entry (zip slip): " + rawName);

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

            return true;
        }
        catch (IOException e)
        {
            FlansMod.log.error("Failed to extract archive for content pack {}", archivePath, e);
            return false;
        }
    }

    /**
     * Rewrites an archive provider from its extracted directory and swaps it back into place.
     * <p>
     * JAR content packs are converted to ZIP files. The original archive is moved to a backup before
     * replacement, the extracted directory is deleted after a successful swap, and temporary output
     * files are cleaned up on failure when possible.
     *
     * @param provider archive provider whose extracted directory should be repacked
     */
    public static void repackArchive(IContentProvider provider)
    {
        Path target = provider.isJarFile()
            ? provider.getPath().getParent().resolve(FilenameUtils.getBaseName(provider.getName()) + ZIP_EXTENSION)
            : provider.getPath();

        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        Path bak = target.resolveSibling(target.getFileName() + ".bak");

        try
        {
            // Ensure tmp from previous crash doesn't mess us up
            Files.deleteIfExists(tmp);

            // 1) Write new archive to tmp (NOT to target)
            URI uri = URI.create("jar:" + tmp.toUri());
            try (FileSystem zipFs = FileSystems.newFileSystem(uri, Map.of("create", "true")); Stream<Path> stream = Files.walk(provider.getExtractedPath()))
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
                provider.update(FilenameUtils.getBaseName(provider.getName()) + ZIP_EXTENSION, target);

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

    /**
     * Replaces a target file with a temporary file while preserving a backup of the old target.
     * <p>
     * Atomic moves are attempted first and regular replacement moves are used when the filesystem
     * does not support atomic moves.
     *
     * @param tmp replacement file that should become the target
     * @param target file to replace
     * @param backup backup location for the previous target
     * @throws IOException when any required move fails
     */
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

    /**
     * Moves a file, using an atomic move when supported by the filesystem.
     *
     * @param src source path to move
     * @param dest destination path
     * @throws IOException when the move fails
     */
    public static void safeMove(Path src, Path dest) throws IOException
    {
        try
        {
            Files.move(src, dest, StandardCopyOption.ATOMIC_MOVE);
        }
        catch (AtomicMoveNotSupportedException e)
        {
            Files.move(src, dest);
        }
    }

    /**
     * Deletes a directory tree or single file recursively.
     * <p>
     * Individual delete failures are logged and traversal failures are logged after traversal stops.
     *
     * @param dir path to delete
     */
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

    /**
     * Deletes a directory only when it exists and contains no entries.
     * <p>
     * This helper is best-effort; deletion failures are intentionally ignored.
     *
     * @param dir directory to remove when empty
     */
    public static void deleteDirectoryIfEmpty(@Nullable Path dir)
    {
        if (dir == null)
            return;

        try {
            if (!Files.exists(dir) || !Files.isDirectory(dir))
                return;

            try (Stream<Path> s = Files.list(dir))
            {
                if (s.findAny().isPresent())
                {
                    return; // not empty
                }
            }

            Files.delete(dir);
        }
        catch (IOException ignored)
        {
            // Ignored
        }
    }

    /**
     * Clears all direct entries inside the shared Flan temporary directory at startup.
     * <p>
     * Directories are deleted recursively and files are deleted directly. The temp root itself is
     * left in place unless later removed by {@link #deleteDirectoryIfEmpty(Path)}.
     *
     * @param tempRoot temporary root directory, usually {@code .flantemp}
     */
    public static void cleanupFlanTempOnStartup(@Nullable Path tempRoot)
    {
        if (tempRoot == null)
            return;

        if (Files.notExists(tempRoot))
            return;

        if (!Files.isDirectory(tempRoot))
        {
            FlansMod.log.warn("Skipping .flantemp startup cleanup because {} is not a directory", tempRoot);
            return;
        }

        try (Stream<Path> entries = Files.list(tempRoot))
        {
            entries.forEach(path ->
            {
                try
                {
                    if (Files.isDirectory(path))
                        deleteRecursively(path);
                    else
                        Files.deleteIfExists(path);
                }
                catch (IOException e)
                {
                    FlansMod.log.warn("Failed to clean .flantemp entry {} on startup: {}", path, e.toString());
                }
            });
        }
        catch (IOException e)
        {
            FlansMod.log.debug("Failed to list temp root {}: {}", tempRoot, e.toString());
        }
    }

    /**
     * Ensures an archive extraction directory is empty and ready for a fresh extraction.
     *
     * @param outputDir extraction directory to recreate
     */
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

    /**
     * Directory stream wrapper that also owns the filesystem opened for an archive.
     *
     * @param delegate directory stream returned by the archive filesystem
     * @param fileSystem archive filesystem that must be closed with the stream
     */
    private record AutoCloseableDirectoryStream(DirectoryStream<Path> delegate, FileSystem fileSystem) implements DirectoryStream<Path>
    {
        /**
         * Closes the wrapped directory stream and then closes the archive filesystem that owns it.
         *
         * @throws IOException when closing the stream or filesystem fails
         */
        @Override
        public void close() throws IOException
        {
            delegate.close();
            fileSystem.close();
        }

        /**
         * Returns the iterator from the wrapped directory stream.
         *
         * @return iterator over paths in the wrapped stream
         */
        @Override
        @NotNull
        public Iterator<Path> iterator()
        {
            return delegate.iterator();
        }
    }

    /**
     * Sanitizes an archive entry name for safe extraction.
     * <p>
     * Backslashes are normalized to slashes, invalid filename characters are replaced, control
     * characters are removed, and empty path segments are discarded.
     *
     * @param name raw archive entry name
     * @return sanitized relative entry path
     */
    private static String sanitizeArchiveEntryName(String name)
    {
        name = name.replace('\\', '/');
        name = name.replaceAll("[:*?\"<>|\u00D7]", "_");
        name = name.replaceAll("\\p{Cntrl}", "");

        name = Arrays.stream(name.split("/"))
                .filter(s -> !s.isEmpty())
                .map(String::trim)
                .collect(Collectors.joining("/"));

        return name;
    }

    /**
     * Checks whether archive entry sanitization changed anything significant.
     * <p>
     * Directory trailing slashes and redundant empty path segments are ignored so normal ZIP
     * directory entries do not produce warnings.
     *
     * @param rawName original archive entry name
     * @param safeName sanitized archive entry name
     * @param isDirectory whether the entry is a directory
     * @return {@code true} when sanitization materially changed the entry name
     */
    private static boolean wasMeaningfullySanitized(String rawName, String safeName, boolean isDirectory)
    {
        String comparableRawName = rawName.replace('\\', '/');

        if (isDirectory && comparableRawName.endsWith("/"))
            comparableRawName = comparableRawName.substring(0, comparableRawName.length() - 1);

        comparableRawName = Arrays.stream(comparableRawName.split("/"))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.joining("/"));

        return !comparableRawName.equals(safeName);
    }
}
