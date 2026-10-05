package com.flansmodultimate.content;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

/** External, disposable caches. Validation reads metadata, never unchanged resource contents. */
final class ContentFileCache
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().create();
    private static Path directory;
    private static boolean bypass;

    record Stamp(long size, String modified, String identity) {}

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

    static Map<String, Stamp> snapshot(Path root) throws IOException
    {
        Map<String, Stamp> result = new TreeMap<>();
        if (!Files.exists(root))
            return result;
        try (Stream<Path> files = Files.walk(root))
        {
            for (Path file : files.filter(Files::isRegularFile).sorted().toList())
                result.put(root.relativize(file).toString().replace('\\', '/'), stamp(file));
        }
        return result;
    }

    static Stamp stamp(Path file) throws IOException
    {
        BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);
        return new Stamp(attributes.size(), attributes.lastModifiedTime().toString(), String.valueOf(attributes.fileKey()));
    }

    static <T> T read(String key, Class<T> type)
    {
        if (bypass)
            return null;
        Path file = file(key);
        if (file == null || !Files.isRegularFile(file))
            return null;
        try
        {
            return GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), type);
        }
        catch (IOException | RuntimeException e)
        {
            LOGGER.debug("Ignoring invalid content cache '{}': {}", file, e.toString());
            return null;
        }
    }

    static void write(String key, Object value)
    {
        Path file = file(key);
        if (file != null)
            writeJson(file, GSON.toJsonTree(value));
    }

    static void writeJson(Path file, JsonElement value)
    {
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        try
        {
            Files.createDirectories(file.getParent());
            Files.writeString(temporary, GSON.toJson(value), StandardCharsets.UTF_8);
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException e)
        {
            LOGGER.warn("Could not write content cache '{}': {}", file, e.toString());
        }
    }

    static String hash(String text)
    {
        try
        {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
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
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int count;
            while ((count = stream.read(buffer)) != -1)
                digest.update(buffer, 0, count);
            return HexFormat.of().formatHex(digest.digest());
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new IllegalStateException(e);
        }
    }

    private static Path file(String key)
    {
        return directory == null ? null : directory.resolve(hash(key) + ".json");
    }
}
