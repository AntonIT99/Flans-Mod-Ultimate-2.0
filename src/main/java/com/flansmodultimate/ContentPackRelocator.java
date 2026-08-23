package com.flansmodultimate;

import com.flansmodultimate.util.FileUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Reconciles standalone Flan content archives and packaged-content mod JARs before content loading.
 * Archive contents are inspected only when their path, size, or modification time changed.
 */
final class ContentPackRelocator
{
    static final String DESCRIPTOR_PATH = "META-INF/flansmodultimate-content.json";
    static final String CACHE_FILE_NAME = ".flansmod-pack-locations-cache.json";

    private static final int CACHE_VERSION = 1;
    private static final int DESCRIPTOR_VERSION = 1;
    private static final int MAX_DESCRIPTOR_BYTES = 64 * 1024;
    private static final long SLOW_SCAN_WARNING_MILLIS = 2_000L;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Set<String> DEFINITION_FOLDERS = Set.of(
        "aaguns", "armorfiles", "armorboxes", "attachments", "bullets", "classes", "gloves",
        "grenades", "guns", "boxes", "itemholders", "parts", "tools", "mechaitems", "vehicles",
        "planes", "mechas", "teams", "rewardboxes", "loadouts"
    );
    private static final Set<String> LEGACY_SUPPORT_FOLDERS = Set.of(
        "armor", "assets", "com", "gui", "models", "skins", "sound", "sounds"
    );

    private ContentPackRelocator()
    {
    }

    static RelocationResult reconcile(Path modsFolder, Path flanFolder, Path cachePath)
    {
        long start = System.nanoTime();
        List<String> warnings = new ArrayList<>();
        int movedContentPacks = 0;
        int movedBundles = 0;
        int inspectedArchives = 0;
        Set<Path> excludedFromContentLoading = new HashSet<>();

        Path normalizedMods = normalize(modsFolder);
        Path normalizedFlan = normalize(flanFolder);
        if (!areSafeDistinctFolders(normalizedMods, normalizedFlan))
        {
            warnings.add("Cannot relocate Flan content because the mods and flan folders overlap: '"
                + normalizedMods + "' and '" + normalizedFlan + "'.");
            return finish(start, movedContentPacks, movedBundles, inspectedArchives,
                excludedFromContentLoading, warnings);
        }
        if (!Files.isDirectory(normalizedMods) || !FileUtils.tryCreateDirectories(normalizedFlan))
        {
            warnings.add("Cannot relocate Flan content because a required folder is unavailable: mods='"
                + normalizedMods + "', flan='" + normalizedFlan + "'.");
            return finish(start, movedContentPacks, movedBundles, inspectedArchives,
                excludedFromContentLoading, warnings);
        }

        try
        {
            normalizedMods = normalizedMods.toRealPath();
            normalizedFlan = normalizedFlan.toRealPath();
        }
        catch (IOException e)
        {
            warnings.add("Cannot resolve the mods or flan folder safely: " + e.getMessage());
            return finish(start, movedContentPacks, movedBundles, inspectedArchives,
                excludedFromContentLoading, warnings);
        }
        if (!areSafeDistinctFolders(normalizedMods, normalizedFlan))
        {
            warnings.add("Cannot relocate Flan content because the resolved mods and flan folders overlap: '"
                + normalizedMods + "' and '" + normalizedFlan + "'.");
            return finish(start, movedContentPacks, movedBundles, inspectedArchives,
                excludedFromContentLoading, warnings);
        }

        ClassificationCache cache = loadCache(cachePath);
        Map<String, CacheEntry> refreshedEntries = new HashMap<>();

        try (DirectoryStream<Path> candidates = Files.newDirectoryStream(normalizedMods,
            ContentPackRelocator::isArchiveFile))
        {
            for (Path candidate : candidates)
            {
                CachedClassification classification = classifyCached(candidate, cache, refreshedEntries);
                ArchiveKind kind = classification.kind;
                inspectedArchives += classification.inspected ? 1 : 0;
                if (kind == ArchiveKind.CONTENT_PACK
                    && move(candidate, normalizedFlan.resolve(candidate.getFileName()), warnings))
                {
                    movedContentPacks++;
                }
            }
        }
        catch (IOException e)
        {
            warnings.add("Could not scan mods folder '" + normalizedMods + "': " + e.getMessage());
        }

        try (DirectoryStream<Path> candidates = Files.newDirectoryStream(normalizedFlan,
            ContentPackRelocator::isArchiveFile))
        {
            for (Path candidate : candidates)
            {
                CachedClassification classification = classifyCached(candidate, cache, refreshedEntries);
                ArchiveKind kind = classification.kind;
                inspectedArchives += classification.inspected ? 1 : 0;
                if (kind != ArchiveKind.PACK_MOD_BUNDLE)
                    continue;

                if (!candidate.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(FileUtils.JAR_EXTENSION))
                {
                    warnings.add("Pack mod bundle '" + candidate.getFileName()
                        + "' is not a JAR. It was left in the flan folder and will not be loaded as standalone content because the mod loader cannot reliably load ZIP bundles.");
                    excludedFromContentLoading.add(normalize(candidate));
                    continue;
                }

                if (move(candidate, normalizedMods.resolve(candidate.getFileName()), warnings))
                    movedBundles++;
                else
                    excludedFromContentLoading.add(normalize(candidate));
            }
        }
        catch (IOException e)
        {
            warnings.add("Could not scan flan folder '" + normalizedFlan + "': " + e.getMessage());
        }

        refreshedEntries.entrySet().removeIf(entry -> !Files.isRegularFile(Path.of(entry.getKey())));
        saveCache(cachePath, new ClassificationCache(CACHE_VERSION, refreshedEntries), warnings);
        return finish(start, movedContentPacks, movedBundles, inspectedArchives,
            excludedFromContentLoading, warnings);
    }

    static ArchiveKind classify(Path archive)
    {
        try (ZipFile zip = new ZipFile(archive.toFile()))
        {
            boolean loaderMod = zip.getEntry("META-INF/mods.toml") != null
                || zip.getEntry("META-INF/neoforge.mods.toml") != null;
            ArchiveKind descriptorKind = readDescriptorKind(zip);
            if (descriptorKind != ArchiveKind.UNKNOWN)
            {
                if (descriptorKind == ArchiveKind.PACK_MOD_BUNDLE)
                    return loaderMod ? ArchiveKind.PACK_MOD_BUNDLE : ArchiveKind.UNKNOWN;
                return loaderMod ? ArchiveKind.UNKNOWN : ArchiveKind.CONTENT_PACK;
            }

            if (loaderMod)
            {
                if (isKnownPackBundle(zip))
                    return ArchiveKind.PACK_MOD_BUNDLE;
                return ArchiveKind.UNKNOWN;
            }

            return looksLikeStandaloneContentPack(zip) ? ArchiveKind.CONTENT_PACK : ArchiveKind.UNKNOWN;
        }
        catch (Exception e)
        {
            return ArchiveKind.UNKNOWN;
        }
    }

    private static ArchiveKind readDescriptorKind(ZipFile zip) throws IOException
    {
        ZipEntry descriptor = zip.getEntry(DESCRIPTOR_PATH);
        if (descriptor == null || descriptor.isDirectory() || descriptor.getSize() > MAX_DESCRIPTOR_BYTES)
            return ArchiveKind.UNKNOWN;

        try (InputStream input = zip.getInputStream(descriptor))
        {
            byte[] bytes = input.readNBytes(MAX_DESCRIPTOR_BYTES + 1);
            if (bytes.length > MAX_DESCRIPTOR_BYTES)
                return ArchiveKind.UNKNOWN;

            JsonObject root = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
            if (!root.has("formatVersion") || root.get("formatVersion").getAsInt() != DESCRIPTOR_VERSION
                || !root.has("kind"))
                return ArchiveKind.UNKNOWN;

            return switch (root.get("kind").getAsString().toLowerCase(Locale.ROOT))
            {
                case "content_pack" -> ArchiveKind.CONTENT_PACK;
                case "pack_mod_bundle" -> ArchiveKind.PACK_MOD_BUNDLE;
                default -> ArchiveKind.UNKNOWN;
            };
        }
    }

    private static boolean isKnownPackBundle(ZipFile zip)
    {
        return zip.getEntry("flansmodultimate_packs/bundled_packs_version.txt") != null
            || zip.getEntry("flans_content/pack_names.json") != null;
    }

    private static boolean looksLikeStandaloneContentPack(ZipFile zip)
    {
        boolean hasFlanAssets = false;
        boolean hasSupportingFolder = false;
        Set<String> definitionFolders = new HashSet<>();
        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements())
        {
            String name = entries.nextElement().getName().replace('\\', '/').toLowerCase(Locale.ROOT);
            while (name.startsWith("/"))
                name = name.substring(1);

            if (name.startsWith("assets/flansmod/"))
                hasFlanAssets = true;

            String[] parts = name.split("/", 3);
            if (parts.length > 1 && LEGACY_SUPPORT_FOLDERS.contains(parts[0]))
                hasSupportingFolder = true;

            String definitionFolder = null;
            if (parts.length > 1 && DEFINITION_FOLDERS.contains(parts[0]))
                definitionFolder = parts[0];
            else if (parts.length > 2 && parts[0].equals("definitions") && DEFINITION_FOLDERS.contains(parts[1]))
                definitionFolder = parts[1];

            if (definitionFolder != null && name.endsWith(".txt"))
                definitionFolders.add(definitionFolder);
        }
        return !definitionFolders.isEmpty()
            && (hasFlanAssets || hasSupportingFolder || definitionFolders.size() > 1);
    }

    private static CachedClassification classifyCached(Path candidate, ClassificationCache cache,
                                                        Map<String, CacheEntry> refreshedEntries)
    {
        String key = normalize(candidate).toString();
        try
        {
            BasicFileAttributes attributes = Files.readAttributes(candidate, BasicFileAttributes.class);
            CacheEntry cached = cache.version == CACHE_VERSION ? cache.entries.get(key) : null;
            ArchiveKind kind;
            boolean inspected;
            if (cached != null && cached.size == attributes.size()
                && cached.lastModifiedMillis == attributes.lastModifiedTime().toMillis())
            {
                kind = cached.kind;
                inspected = false;
            }
            else
            {
                kind = classify(candidate);
                inspected = true;
            }
            refreshedEntries.put(key, new CacheEntry(attributes.size(), attributes.lastModifiedTime().toMillis(), kind));
            return new CachedClassification(kind, inspected);
        }
        catch (IOException e)
        {
            return new CachedClassification(ArchiveKind.UNKNOWN, false);
        }
    }

    private static boolean move(Path source, Path destination, List<String> warnings)
    {
        if (Files.exists(destination))
        {
            warnings.add("Found misplaced Flan archive '" + source.getFileName() + "', but destination '"
                + destination + "' already exists. Nothing was overwritten.");
            return false;
        }

        try
        {
            FileUtils.safeMove(source, destination);
            return true;
        }
        catch (IOException e)
        {
            warnings.add("Could not move '" + source + "' to '" + destination + "': " + e.getMessage());
            return false;
        }
    }

    private static boolean isArchiveFile(Path entry)
    {
        if (!Files.isRegularFile(entry) || Files.isSymbolicLink(entry))
            return false;
        String name = entry.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(FileUtils.JAR_EXTENSION) || name.endsWith(FileUtils.ZIP_EXTENSION);
    }

    private static boolean areSafeDistinctFolders(Path modsFolder, Path flanFolder)
    {
        return !modsFolder.equals(flanFolder)
            && !modsFolder.startsWith(flanFolder)
            && !flanFolder.startsWith(modsFolder);
    }

    private static Path normalize(Path path)
    {
        return path.toAbsolutePath().normalize();
    }

    private static ClassificationCache loadCache(Path cachePath)
    {
        if (!Files.isRegularFile(cachePath))
            return new ClassificationCache(CACHE_VERSION, Map.of());
        try (Reader reader = Files.newBufferedReader(cachePath, StandardCharsets.UTF_8))
        {
            ClassificationCache cache = GSON.fromJson(reader, ClassificationCache.class);
            return cache == null || cache.entries == null
                ? new ClassificationCache(CACHE_VERSION, Map.of())
                : cache;
        }
        catch (Exception e)
        {
            return new ClassificationCache(CACHE_VERSION, Map.of());
        }
    }

    private static void saveCache(Path cachePath, ClassificationCache cache, List<String> warnings)
    {
        Path normalizedCache = normalize(cachePath);
        Path temporary = normalizedCache.resolveSibling(normalizedCache.getFileName() + ".tmp");
        try
        {
            Files.createDirectories(normalizedCache.getParent());
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8))
            {
                GSON.toJson(cache, writer);
            }
            try
            {
                Files.move(temporary, normalizedCache, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
            }
            catch (java.nio.file.AtomicMoveNotSupportedException e)
            {
                Files.move(temporary, normalizedCache, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        catch (IOException e)
        {
            warnings.add("Could not update Flan archive classification cache '" + normalizedCache + "': " + e.getMessage());
            try
            {
                Files.deleteIfExists(temporary);
            }
            catch (IOException ignored)
            {
            }
        }
    }

    private static RelocationResult finish(long startNanos, int movedContentPacks, int movedBundles,
                                           int inspectedArchives, Set<Path> excludedFromContentLoading,
                                           List<String> warnings)
    {
        long elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000L;
        if (elapsedMillis > SLOW_SCAN_WARNING_MILLIS)
            warnings.add("Flan archive location verification took " + elapsedMillis + " ms.");
        return new RelocationResult(movedContentPacks, movedBundles, inspectedArchives, elapsedMillis,
            Set.copyOf(excludedFromContentLoading), List.copyOf(warnings));
    }

    enum ArchiveKind
    {
        CONTENT_PACK,
        PACK_MOD_BUNDLE,
        UNKNOWN
    }

    record RelocationResult(int movedContentPacks, int movedBundles, int inspectedArchives,
                            long elapsedMillis, Set<Path> excludedFromContentLoading,
                            List<String> warnings)
    {
        boolean restartRequired()
        {
            return movedBundles > 0;
        }
    }

    private record CachedClassification(ArchiveKind kind, boolean inspected) {}

    private static final class ClassificationCache
    {
        final int version;
        final Map<String, CacheEntry> entries;

        ClassificationCache(int version, Map<String, CacheEntry> entries)
        {
            this.version = version;
            this.entries = Map.copyOf(entries);
        }
    }

    private static final class CacheEntry
    {
        final long size;
        final long lastModifiedMillis;
        final ArchiveKind kind;

        CacheEntry(long size, long lastModifiedMillis, ArchiveKind kind)
        {
            this.size = size;
            this.lastModifiedMillis = lastModifiedMillis;
            this.kind = kind;
        }
    }
}
