package com.flansmodultimate.content;

import com.flansmodultimate.util.*;
import org.apache.commons.io.FilenameUtils;

import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.nio.file.FileSystem;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Stream;

import static com.flansmodultimate.content.ContentPackPaths.*;

/** Persisted texture signatures and parsed model JSON, reused when file metadata is unchanged. */
final class PackAssetIndex
{
    // 2: name-only entries; older loaders must not compare their empty signatures.
    // 3: a fingerprint of the file stamps and the source path instead of every stamp.
    private static final int VERSION = 3;
    private static final List<String> LEGACY_FOLDERS = List.of(FOLDER_TEXTURES_ARMOR, FOLDER_TEXTURES_GUI, FOLDER_TEXTURES_SKINS);
    private record Data(String source, int version, String files, Map<String, Map<String, Map<String, String>>> legacy, ModernAssetAliases.Assets modern, Map<String, String> pixels)
    {}
    private final String key;
    private final String source;
    private final String files;
    private Map<String, Map<String, Map<String, String>>> legacy;
    private ModernAssetAliases.Assets modern;
    /**
     * Pixel signatures by digest of the PNG bytes. They outlive changed file stamps, because a build
     * that copies resources anew rewrites every stamp, and hashing bytes is far cheaper than decoding.
     */
    private Map<String, String> pixels = Map.of();

    private PackAssetIndex(IContentProvider provider, FileSystem fs) throws IOException
    {
        this(provider.getAssetsPath(fs), provider.isArchive() ? provider.getPath() : null, provider instanceof PackagedContentProvider ? provider.getPath() : provider.getAssetsPath(fs));
    }

    private PackAssetIndex(Path assets, Path archive, Path directorySource) throws IOException
    {
        // Loader union paths print only their path inside the module. Different modules can
        // all print "assets/flansmod", so persist the physical source identity as well.
        key = key(assets.toString(), archive == null ? directorySource : archive);
        source = ContentFileCache.source(archive == null ? directorySource : archive);
        files = ContentFileCache.fingerprint(archive == null ? ContentFileCache.snapshot(assets) : Map.of("archive", ContentFileCache.stamp(archive)));
        Data previous = readData(key);
        if (previous != null && previous.pixels() != null)
            pixels = previous.pixels();
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
        String assets = provider instanceof PackagedContentProvider packaged ? packaged.getArchiveAssetsRoot() : "/assets/flansmod";
        Data previous = readData(key(assets, provider.getPath()));
        return previous != null && ContentFileCache.fingerprint(Map.of("archive", ContentFileCache.stamp(provider.getPath()))).equals(previous.files()) ? previous : null;
    }

    private static Data readData(String key)
    {
        Data data = ContentFileCache.read(ContentFileCache.Kind.ASSET_INDEX, key, Data.class);
        if (data == null || data.version() != VERSION || data.files() == null)
            return null;
        if (data.legacy() != null && (!data.legacy().keySet().equals(Set.copyOf(LEGACY_FOLDERS))
            || data.legacy().values().stream().anyMatch(groups -> groups == null || groups.values().stream().anyMatch(layers -> layers == null || layers.containsValue(null)))))
            return null;
        ModernAssetAliases.Assets modern = data.modern();
        if (modern != null && (modern.textures() == null || modern.models() == null || modern.blockstates() == null || modern.metadata() == null || modern.textures().containsValue(null)
            || modern.models().containsValue(null) || modern.blockstates().containsValue(null)))
            return null;
        if (data.pixels() != null && data.pixels().containsValue(null))
            return null;
        return data;
    }

    private static String key(String assets, Path source)
    {
        return "assets:" + source.toAbsolutePath().normalize() + "!" + assets;
    }

    static Map<String, Map<String, Map<String, String>>> legacy(IContentProvider provider) throws IOException
    {
        return legacy(provider, (Map<String, Set<String>>) null);
    }

    /** Empty signatures reserve filenames without decoding textures that cannot collide. */
    static Map<String, Map<String, Map<String, String>>> legacyNames(IContentProvider provider) throws IOException
    {
        return legacy(provider, Map.of());
    }

    static Map<String, Map<String, Map<String, String>>> legacy(IContentProvider provider, Map<String, Set<String>> signatureNames) throws IOException
    {
        Data cached = cachedArchive(provider);
        if (cached != null && !needsSignatures(cached.legacy(), signatureNames))
            return cached.legacy();
        FileSystem fs = FileUtils.createFileSystem(provider);
        try
        {
            return legacy(provider, fs, signatureNames);
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
        return legacy(provider, fs, null);
    }

    static boolean needsSignatures(Map<String, Map<String, Map<String, String>>> legacy, Map<String, Set<String>> signatureNames)
    {
        return legacy == null || legacy.entrySet().stream().anyMatch(folder -> folder.getValue().entrySet().stream()
            .anyMatch(group -> (signatureNames == null || signatureNames.getOrDefault(folder.getKey(), Set.of()).contains(group.getKey())) && group.getValue().containsValue("")));
    }

    private static Map<String, Map<String, Map<String, String>>> legacy(IContentProvider provider, FileSystem fs, Map<String, Set<String>> signatureNames) throws IOException
    {
        PackAssetIndex index = new PackAssetIndex(provider, fs);
        if (needsSignatures(index.legacy, signatureNames))
        {
            FlansLog.log.debug("{}: Rebuilding legacy texture index", provider.getName());
            index.legacy = new LinkedHashMap<>();
            Map<String, String> signed = new TreeMap<>();
            Path root = provider.getTextureSourcePath(fs);
            for (String folder : LEGACY_FOLDERS)
                index.legacy.put(folder,
                    index.readLegacy(root.resolve(folder), folder.equals(FOLDER_TEXTURES_ARMOR), signatureNames == null ? null : signatureNames.getOrDefault(folder, Set.of()), signed));
            // A name-only scan signs nothing and keeps the signatures for the next collision.
            if (!signed.isEmpty())
                index.pixels = signed;
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
        return modern(assets, archive, assets.toAbsolutePath().normalize());
    }

    static ModernAssetAliases.Assets modern(Path assets, Path archive, Path directorySource) throws IOException
    {
        PackAssetIndex index = new PackAssetIndex(assets, archive, directorySource);
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
        ContentFileCache.write(ContentFileCache.Kind.ASSET_INDEX, key, new Data(source, VERSION, files, legacy, modern, pixels));
    }

    private Map<String, Map<String, String>> readLegacy(Path directory, boolean armor, Set<String> signatureNames, Map<String, String> signed) throws IOException
    {
        Map<String, Map<String, String>> groups = new TreeMap<>();
        if (!Files.isDirectory(directory))
            return groups;
        try (Stream<Path> paths = Files.list(directory))
        {
            for (Path file : paths.filter(Files::isRegularFile).filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png"))
                .filter(path -> !FileUtils.isMacOsMetadataPath(path.getFileName().toString())).sorted().toList())
            {
                String base = FilenameUtils.getBaseName(file.getFileName().toString());
                String name = ResourceUtils.sanitize(armor ? LegacyTextureAliases.getArmorTextureBaseName(base) : base);
                String layer = armor ? base.substring(LegacyTextureAliases.getArmorTextureBaseName(base).length()) : "";
                groups.computeIfAbsent(name, ignored -> new TreeMap<>()).put(layer, signatureNames == null || signatureNames.contains(name) ? pixelSignature(file, signed) : "");
            }
        }
        return groups;
    }

    private String pixelSignature(Path file, Map<String, String> signed) throws IOException
    {
        byte[] bytes = Files.readAllBytes(file);
        String content = HexFormat.of().formatHex(ContentFileCache.newDigest().digest(bytes));
        String signature = pixels.get(content);
        if (signature == null)
        {
            BufferedImage image = decode(bytes);
            if (image == null)
            {
                FlansLog.log.warn("Invalid PNG texture '{}'", file);
                return "invalid:" + file.getFileName();
            }
            signature = pixelSignature(image);
        }
        signed.put(content, signature);
        return signature;
    }

    private static BufferedImage decode(byte[] bytes) throws IOException
    {
        try (InputStream input = new ByteArrayInputStream(bytes))
        {
            // Decoded in memory: ImageIO would otherwise buffer the stream in a temporary file.
            return ImageIO.read(new MemoryCacheImageInputStream(input));
        }
    }

    private static String pixelSignature(BufferedImage image)
    {
        MessageDigest digest = ContentFileCache.newDigest();
        digest.update(ByteBuffer.allocate(8).putInt(image.getWidth()).putInt(image.getHeight()).array());
        ByteBuffer row = ByteBuffer.allocate(image.getWidth() * Integer.BYTES);
        int[] rgb = new int[image.getWidth()];
        for (int y = 0; y < image.getHeight(); y++)
        {
            row.clear();
            image.getRGB(0, y, image.getWidth(), 1, rgb, 0, image.getWidth());
            for (int pixel : rgb)
                row.putInt(pixel);
            digest.update(row.array());
        }
        return HexFormat.of().formatHex(digest.digest());
    }
}
