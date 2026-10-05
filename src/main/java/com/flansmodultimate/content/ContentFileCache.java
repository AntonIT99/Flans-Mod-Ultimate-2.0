package com.flansmodultimate.content;

import com.flansmodultimate.util.FlansLog;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HexFormat;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * External, disposable caches. Validation reads metadata, never unchanged resource contents.
 * <p>
 * An entry stores a fingerprint of the file stamps it was built from rather than the stamps
 * themselves, and the path of the pack, archive or mod it describes, so that entries of sources
 * that are gone can be removed.
 */
final class ContentFileCache
{
    private static final Gson GSON = new GsonBuilder().create();
    private static final String PRUNE_MARKER = "last-prune";
    private static final Duration PRUNE_INTERVAL = Duration.ofDays(7);
    /** Older temporary files belong to no running write: a crash left them. */
    private static final Duration ABANDONED_TEMPORARY_AGE = Duration.ofDays(1);

    private static volatile Path directory;
    private static volatile boolean bypass;
    /** Snapshots taken while the packs load, by absolute root; null at any other time. */
    private static volatile Map<Path, Map<String, Stamp>> runSnapshots;

    record Stamp(long size, String modified, String identity) {}

    /** One subfolder per kind of entry, so the cache folder says what each file is for. */
    enum Kind
    {
        /** Whether a standalone pack's generated assets and data are up to date. */
        GENERATION("generation"),
        /** Texture names, pixel signatures and modern asset digests used for conflict detection. */
        ASSET_INDEX("asset-index"),
        /** Sound events and measured .ogg lengths of each sound source. */
        SOUND_INDEX("sound-index");

        private final String folder;

        Kind(String folder)
        {
            this.folder = folder;
        }
    }

    private ContentFileCache() {}

    static void configure(Path path)
    {
        configure(path, false);
    }

    static void configure(Path path, boolean forceRegeneration)
    {
        directory = path;
        bypass = forceRegeneration;
    }

    static Map<String, Stamp> snapshot(IContentProvider provider, Path directoryRoot) throws IOException
    {
        if (provider.isArchive())
            return Map.of("archive", stamp(provider.getPath()));
        return snapshot(directoryRoot);
    }

    /**
     * Starts remembering the directory snapshots taken while the content packs load. A folder pack
     * is then walked once, on a worker, and its asset and sound folders are cut out of that walk.
     * Whatever writes to a pack must {@link #forget} it.
     */
    static void beginRun()
    {
        runSnapshots = new ConcurrentHashMap<>();
    }

    /**
     * Stops remembering snapshots. Nothing writes to the packs between the end of loading and the
     * first resource discovery, which may still use them before releasing them.
     */
    static void endRun(boolean keepForResourceDiscovery)
    {
        if (!keepForResourceDiscovery)
            runSnapshots = null;
    }

    /** Drops the snapshots kept for the first resource discovery; later ones see the packs as they are. */
    static void releaseRunSnapshots()
    {
        runSnapshots = null;
    }

    /** Walks a standalone folder pack ahead of its turn, so that its later snapshots come from memory. */
    static void prefetch(IContentProvider provider)
    {
        if (runSnapshots == null || provider.isArchive() || provider.isPreprocessed())
            return;
        try
        {
            snapshot(provider.getPath());
        }
        catch (IOException | RuntimeException e)
        {
            FlansLog.log.debug("Could not stamp the files of '{}' ahead of loading: {}", provider.getName(), e.toString());
        }
    }

    /** Forgets every remembered snapshot that includes or lies within the path, after writing to it. */
    static void forget(Path path)
    {
        Map<Path, Map<String, Stamp>> snapshots = runSnapshots;
        if (snapshots == null)
            return;
        Path changed = path.toAbsolutePath().normalize();
        snapshots.keySet().removeIf(root -> root.startsWith(changed) || changed.startsWith(root));
    }

    /**
     * Stamps every regular file below the root, sorted by relative path. While the packs load, a
     * snapshot of the root or of a folder containing it is reused.
     */
    static Map<String, Stamp> snapshot(Path root) throws IOException
    {
        Map<Path, Map<String, Stamp>> snapshots = runSnapshots;
        if (snapshots == null)
            return walk(root);

        Path normalized = root.toAbsolutePath().normalize();
        for (Path known = normalized; known != null; known = known.getParent())
        {
            Map<String, Stamp> remembered = snapshots.get(known);
            if (remembered == null)
                continue;
            if (known.equals(normalized))
                return remembered;
            Map<String, Stamp> within = within(remembered, relative(known, normalized));
            // Nothing found may also mean the folder is spelt in another case than on disk.
            if (!within.isEmpty() || !Files.isDirectory(normalized))
                return within;
            break;
        }
        Map<String, Stamp> walked = walk(root);
        snapshots.put(normalized, walked);
        return walked;
    }

    /** A new walk of the root, after files below it have been written; it replaces what was remembered. */
    static Map<String, Stamp> freshSnapshot(Path root) throws IOException
    {
        forget(root);
        return snapshot(root);
    }

    private static Map<String, Stamp> within(Map<String, Stamp> snapshot, String folder)
    {
        String prefix = folder + "/";
        SortedMap<String, Stamp> result = new TreeMap<>();
        // Sorted keys: everything below the folder follows the prefix without a gap.
        for (Map.Entry<String, Stamp> entry : ((SortedMap<String, Stamp>) snapshot).tailMap(prefix).entrySet())
        {
            if (!entry.getKey().startsWith(prefix))
                break;
            result.put(entry.getKey().substring(prefix.length()), entry.getValue());
        }
        return Collections.unmodifiableSortedMap(result);
    }

    /**
     * The walk supplies each file's attributes, which on Windows come with the directory listing, so
     * most files cost no extra file system call.
     */
    private static Map<String, Stamp> walk(Path root) throws IOException
    {
        SortedMap<String, Stamp> result = new TreeMap<>();
        if (!Files.exists(root))
            return Collections.unmodifiableSortedMap(result);
        Files.walkFileTree(root, new SimpleFileVisitor<>()
        {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException
            {
                // Links are stamped through to their target, as the file they stand for.
                if (attributes.isSymbolicLink())
                {
                    if (Files.isRegularFile(file))
                        result.put(relative(root, file), stamp(file));
                }
                else if (attributes.isRegularFile())
                    result.put(relative(root, file), stamp(attributes));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exception) throws IOException
            {
                // A file deleted during the walk is simply absent from the snapshot.
                if (Files.notExists(file))
                    return FileVisitResult.CONTINUE;
                throw exception;
            }
        });
        return Collections.unmodifiableSortedMap(result);
    }

    /** What an entry stores of a snapshot: equal snapshots, and only those, have equal fingerprints. */
    static String fingerprint(Map<String, Stamp> files)
    {
        MessageDigest digest = newDigest();
        for (Map.Entry<String, Stamp> entry : new TreeMap<>(files).entrySet())
        {
            Stamp stamp = entry.getValue();
            digest.update((entry.getKey() + '\0' + stamp.size() + '\0' + stamp.modified() + '\0' + stamp.identity() + '\n')
                .getBytes(StandardCharsets.UTF_8));
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    /**
     * The source an entry describes, as {@link #pruneIfDue} checks for it. A path inside a loader's
     * virtual file system names no file of its own and is recorded blank, which is never pruned.
     */
    static String source(Path path)
    {
        if (path.getFileSystem() != FileSystems.getDefault())
            return "";
        return path.toAbsolutePath().normalize().toString();
    }

    private static String relative(Path root, Path file)
    {
        return root.relativize(file).toString().replace('\\', '/');
    }

    static Stamp stamp(Path file) throws IOException
    {
        return stamp(Files.readAttributes(file, BasicFileAttributes.class));
    }

    private static Stamp stamp(BasicFileAttributes attributes)
    {
        return new Stamp(attributes.size(), attributes.lastModifiedTime().toString(), String.valueOf(attributes.fileKey()));
    }

    static <T> T read(Kind kind, String key, Class<T> type)
    {
        if (bypass)
            return null;
        Path file = file(kind, key);
        if (file == null || !Files.isRegularFile(file))
            return null;
        try
        {
            return GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), type);
        }
        catch (IOException | RuntimeException e)
        {
            FlansLog.log.debug("Ignoring invalid content cache '{}': {}", file, e.toString());
            return null;
        }
    }

    static void write(Kind kind, String key, Object value)
    {
        Path file = file(kind, key);
        if (file != null)
            writeJson(file, GSON.toJsonTree(value));
    }

    static void writeJson(Path file, JsonElement value)
    {
        Path temporary = null;
        try
        {
            Files.createDirectories(file.getParent());
            // A temporary file of its own, so writers on different threads never share one.
            temporary = Files.createTempFile(file.getParent(), file.getFileName().toString(), ".tmp");
            Files.writeString(temporary, GSON.toJson(value), StandardCharsets.UTF_8);
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException e)
        {
            FlansLog.log.warn("Could not write content cache '{}': {}", file, e.toString());
            if (temporary != null)
            {
                try
                {
                    Files.deleteIfExists(temporary);
                }
                catch (IOException ignored)
                {
                    // The next write replaces it.
                }
            }
        }
    }

    static String hash(String text)
    {
        MessageDigest digest = newDigest();
        digest.update(text.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest.digest());
    }

    /** The digest {@link #hash(String)} uses, for input too large to concatenate first. */
    static MessageDigest newDigest()
    {
        try
        {
            return MessageDigest.getInstance("SHA-256");
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new IllegalStateException(e);
        }
    }

    static String digest(Path file) throws IOException
    {
        try (java.io.InputStream stream = Files.newInputStream(file))
        {
            MessageDigest digest = newDigest();
            byte[] buffer = new byte[8192];
            int count;
            while ((count = stream.read(buffer)) != -1)
                digest.update(buffer, 0, count);
            return HexFormat.of().formatHex(digest.digest());
        }
    }

    /**
     * Removes, at most once a week, the entries of packs, archives and mods that no longer exist,
     * such as an older version of a renamed pack, and temporary files a crash left behind.
     * An entry of a pack that is only away for a while is rebuilt when the pack returns.
     */
    static void pruneIfDue()
    {
        Path root = directory;
        if (root == null || bypass || !Files.isDirectory(root))
            return;
        Path marker = root.resolve(PRUNE_MARKER);
        Instant now = Instant.now();
        try
        {
            if (Files.isRegularFile(marker)
                && Files.getLastModifiedTime(marker).toInstant().isAfter(now.minus(PRUNE_INTERVAL)))
                return;
        }
        catch (IOException e)
        {
            return;
        }

        int removed = 0;
        for (Kind kind : Kind.values())
        {
            Path folder = root.resolve(kind.folder);
            if (!Files.isDirectory(folder))
                continue;
            try (DirectoryStream<Path> files = Files.newDirectoryStream(folder))
            {
                for (Path file : files)
                {
                    String name = file.getFileName().toString();
                    boolean obsolete = name.endsWith(".tmp")
                        ? Files.getLastModifiedTime(file).toInstant().isBefore(now.minus(ABANDONED_TEMPORARY_AGE))
                        : name.endsWith(".json") && !sourceExists(readSource(file));
                    if (obsolete && Files.deleteIfExists(file))
                        removed++;
                }
            }
            catch (IOException | RuntimeException e)
            {
                FlansLog.log.debug("Could not prune content cache '{}': {}", folder, e.toString());
            }
        }

        try
        {
            if (!Files.exists(marker))
                Files.createFile(marker);
            Files.setLastModifiedTime(marker, FileTime.from(now));
        }
        catch (IOException e)
        {
            FlansLog.log.debug("Could not mark content cache pruning: {}", e.toString());
        }
        if (removed > 0)
            FlansLog.log.info("Removed {} content cache file(s) of content packs that are no longer installed.", removed);
    }

    /** The source an entry names, read without parsing the rest of it; null for an entry without one. */
    @Nullable
    static String readSource(Path file)
    {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8);
             JsonReader json = new JsonReader(reader))
        {
            json.beginObject();
            while (json.hasNext())
            {
                if (json.nextName().equals("source") && json.peek() == JsonToken.STRING)
                    return json.nextString();
                json.skipValue();
            }
        }
        catch (IOException | RuntimeException e)
        {
            // An unreadable entry is pruned like an orphaned one.
        }
        return null;
    }

    /** Missing: an entry of an older layout. Blank: a source with no file of its own, which is kept. */
    static boolean sourceExists(@Nullable String source)
    {
        if (source == null)
            return false;
        if (source.isBlank())
            return true;
        try
        {
            Path path = Path.of(source);
            if (Files.exists(path))
                return true;
            // A JAR pack converted to ZIP keeps its generation entry.
            String extension = FilenameUtils.getExtension(path.getFileName().toString());
            String base = FilenameUtils.removeExtension(path.getFileName().toString());
            return (extension.equalsIgnoreCase("jar") && Files.exists(path.resolveSibling(base + ".zip")))
                || (extension.equalsIgnoreCase("zip") && Files.exists(path.resolveSibling(base + ".jar")));
        }
        catch (InvalidPathException e)
        {
            return false;
        }
    }

    private static Path file(Kind kind, String key)
    {
        Path root = directory;
        return root == null ? null : root.resolve(kind.folder).resolve(hash(key) + ".json");
    }
}
