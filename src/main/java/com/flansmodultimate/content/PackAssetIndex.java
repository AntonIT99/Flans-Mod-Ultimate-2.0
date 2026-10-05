package com.flansmodultimate.content;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.util.FileUtils;
import com.flansmodultimate.util.ResourceUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

/** Persisted texture signatures and parsed model JSON, reused when file metadata is unchanged. */
final class PackAssetIndex
{
    private static final Logger LOGGER = FlansMod.Logging.LOGGER;
    private static final int VERSION = 1;
    private record Data(int version, Map<String, ContentFileCache.Stamp> files,
                        Map<String, Map<String, Map<String, String>>> legacy, ModernAssetAliases.Assets modern) {}
    private final String key;
    private final Map<String, ContentFileCache.Stamp> files;
    private Map<String, Map<String, Map<String, String>>> legacy;
    private ModernAssetAliases.Assets modern;

    private PackAssetIndex(IContentProvider provider, FileSystem fs) throws IOException
    {
        this(provider.getAssetsPath(fs), provider.isArchive() ? provider.getPath() : null);
    }

    private PackAssetIndex(Path assets, Path archive) throws IOException
    {
        key = key(assets.toString(), archive == null ? assets.toAbsolutePath().normalize() : archive);
        files = archive == null ? ContentFileCache.snapshot(assets) : Map.of("archive", ContentFileCache.stamp(archive));
        Data previous = readData(key);
        if (previous != null && files.equals(previous.files()))
        {
            legacy = previous.legacy();
            modern = previous.modern();
        }
    }

    /** An unchanged archive needs no ZIP directory scan just to obtain its cached asset tree. */
    private static Data cachedArchive(IContentProvider provider) throws IOException
    {
        if (!provider.isArchive())
            return null;
        String assets = provider instanceof PackagedContentProvider packaged
            ? packaged.getArchiveAssetsRoot() : "/assets/flansmod";
        Data previous = readData(key(assets, provider.getPath()));
        return previous != null
            && Map.of("archive", ContentFileCache.stamp(provider.getPath())).equals(previous.files()) ? previous : null;
    }

    private static Data readData(String key)
    {
        Data data = ContentFileCache.read(key, Data.class);
        if (data == null || data.version() != VERSION || data.files() == null)
            return null;
        if (data.legacy() != null && (!data.legacy().keySet().equals(Set.of("armor", "gui", "skins"))
            || data.legacy().values().stream().anyMatch(groups -> groups == null
                || groups.values().stream().anyMatch(layers -> layers == null || layers.containsValue(null)))))
            return null;
        ModernAssetAliases.Assets modern = data.modern();
        if (modern != null && (modern.textures() == null || modern.models() == null
            || modern.blockstates() == null || modern.metadata() == null
            || modern.textures().containsValue(null) || modern.models().containsValue(null)
            || modern.blockstates().containsValue(null)))
            return null;
        return data;
    }

    private static String key(String assets, Path source)
    {
        return "assets:" + source.toAbsolutePath().normalize() + "!" + assets;
    }

    static Map<String, Map<String, Map<String, String>>> legacy(IContentProvider provider) throws IOException
    {
        Data cached = cachedArchive(provider);
        if (cached != null && cached.legacy() != null)
            return cached.legacy();
        FileSystem fs = FileUtils.createFileSystem(provider);
        try
        {
            return legacy(provider, fs);
        }
        finally
        {
            FileUtils.closeFileSystem(fs, provider);
        }
    }

    static ModernAssetAliases.Assets modern(IContentProvider provider) throws IOException
    {
        Data cached = cachedArchive(provider);
        if (cached != null && cached.modern() != null)
            return cached.modern();
        FileSystem fs = FileUtils.createFileSystem(provider);
        try
        {
            return modern(provider, fs);
        }
        finally
        {
            FileUtils.closeFileSystem(fs, provider);
        }
    }

    static Map<String, Map<String, Map<String, String>>> legacy(IContentProvider provider, FileSystem fs) throws IOException
    {
        PackAssetIndex index = new PackAssetIndex(provider, fs);
        if (index.legacy == null)
        {
            index.legacy = new LinkedHashMap<>();
            Path root = provider.getTextureSourcePath(fs);
            for (String folder : new String[] {"armor", "gui", "skins"})
                index.legacy.put(folder, readLegacy(root.resolve(folder), folder.equals("armor")));
            index.save();
        }
        return index.legacy;
    }

    static ModernAssetAliases.Assets modern(IContentProvider provider, FileSystem fs) throws IOException
    {
        PackAssetIndex index = new PackAssetIndex(provider, fs);
        if (index.modern == null)
        {
            index.modern = ModernAssetAliases.read(provider.getAssetsPath(fs));
            index.save();
        }
        return index.modern;
    }

    static ModernAssetAliases.Assets modern(Path assets, Path archive) throws IOException
    {
        PackAssetIndex index = new PackAssetIndex(assets, archive);
        if (index.modern == null)
        {
            index.modern = ModernAssetAliases.read(assets);
            index.save();
        }
        return index.modern;
    }

    static void rememberLegacy(IContentProvider provider, FileSystem fs, Map<String, Map<String, Map<String, String>>> legacy) throws IOException
    {
        PackAssetIndex index = new PackAssetIndex(provider, fs);
        index.legacy = legacy;
        index.save();
    }

    private void save()
    {
        ContentFileCache.write(key, new Data(VERSION, files, legacy, modern));
    }

    private static Map<String, Map<String, String>> readLegacy(Path directory, boolean armor) throws IOException
    {
        Map<String, Map<String, String>> groups = new TreeMap<>();
        if (!Files.isDirectory(directory))
            return groups;
        try (Stream<Path> paths = Files.list(directory))
        {
            for (Path file : paths.filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png"))
                .filter(path -> !FileUtils.isMacOsMetadataPath(path.getFileName().toString())).sorted().toList())
            {
                String base = FilenameUtils.getBaseName(file.getFileName().toString());
                String name = ResourceUtils.sanitize(armor ? LegacyTextureAliases.getArmorTextureBaseName(base) : base);
                String layer = armor ? base.substring(LegacyTextureAliases.getArmorTextureBaseName(base).length()) : "";
                groups.computeIfAbsent(name, ignored -> new TreeMap<>()).put(layer, pixelSignature(file));
            }
        }
        return groups;
    }

    private static String pixelSignature(Path file) throws IOException
    {
        try (InputStream input = Files.newInputStream(file))
        {
            BufferedImage image = ImageIO.read(input);
            if (image == null)
            {
                LOGGER.warn("Invalid PNG texture '{}'", file);
                return "invalid:" + file.getFileName();
            }
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(ByteBuffer.allocate(8).putInt(image.getWidth()).putInt(image.getHeight()).array());
            ByteBuffer row = ByteBuffer.allocate(image.getWidth() * Integer.BYTES);
            for (int y = 0; y < image.getHeight(); y++)
            {
                row.clear();
                for (int x = 0; x < image.getWidth(); x++)
                    row.putInt(image.getRGB(x, y));
                digest.update(row.array());
            }
            return HexFormat.of().formatHex(digest.digest());
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new IllegalStateException(e);
        }
    }
}
