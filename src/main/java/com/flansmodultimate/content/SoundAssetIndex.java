package com.flansmodultimate.content;

import com.flansmodultimate.util.FlansLog;
import com.flansmodultimate.util.OggDurationReader;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

/** Measures audio only on an asset-cache miss; warm archives require just their outer file stamp. */
final class SoundAssetIndex
{
    // 2: a fingerprint of the file stamps and the source path instead of every stamp.
    private static final int VERSION = 2;
    private record Cache(String source, int version, String files, SoundPriorityPlan.Assets assets) {}

    private SoundAssetIndex() {}

    static SoundPriorityPlan.Assets read(SoundPriority.Source source) throws IOException
    {
        Map<String, ContentFileCache.Stamp> files;
        if (source.archive() == null)
        {
            Path assets = source.root().resolve("assets/flansmod");
            files = new TreeMap<>(ContentFileCache.snapshot(assets.resolve("sounds")));
            Path events = assets.resolve("sounds.json");
            if (Files.isRegularFile(events))
                files.put("@sounds.json", ContentFileCache.stamp(events));
        }
        else
            files = Map.of("archive", ContentFileCache.stamp(source.archive()));
        String key = "sound-assets:" + source.id() + ":" + (source.archive() == null ? source.root() : source.archive());
        String fingerprint = ContentFileCache.fingerprint(files);
        Cache previous = ContentFileCache.read(ContentFileCache.Kind.SOUND_INDEX, key, Cache.class);
        if (previous != null && previous.version() == VERSION && fingerprint.equals(previous.files())
            && previous.assets() != null && previous.assets().events() != null && previous.assets().files() != null
            && !previous.assets().files().containsValue(null))
            return previous.assets();
        SoundPriorityPlan.Assets assets;
        if (source.archive() == null)
            assets = readAssets(source.root().resolve("assets/flansmod"));
        else
            try (FileSystem fs = FileSystems.newFileSystem(source.archive()))
            {
                assets = readAssets(fs.getPath("/assets/flansmod"));
            }
        String location = ContentFileCache.source(source.archive() == null ? source.root() : source.archive());
        ContentFileCache.write(ContentFileCache.Kind.SOUND_INDEX, key, new Cache(location, VERSION, fingerprint, assets));
        return assets;
    }

    private static SoundPriorityPlan.Assets readAssets(Path assets) throws IOException
    {
        JsonObject events = new JsonObject();
        Path json = assets.resolve("sounds.json");
        if (Files.isRegularFile(json))
            try
            {
                events = JsonParser.parseString(Files.readString(json)).getAsJsonObject();
            }
            catch (IOException | RuntimeException exception)
            {
                FlansLog.log.warn("Could not read sound definitions '{}': {}", json, exception.toString());
            }
        Map<String, Integer> files = new TreeMap<>();
        Path sounds = assets.resolve("sounds");
        if (Files.isDirectory(sounds))
            try (Stream<Path> paths = Files.walk(sounds))
            {
                for (Path path : paths.filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().endsWith(".ogg")).sorted().toList())
                {
                    String relative = sounds.relativize(path).toString().replace('\\', '/');
                    files.put(relative.substring(0, relative.length() - 4), OggDurationReader.readDurationTicks(path).orElse(0));
                }
            }
        return new SoundPriorityPlan.Assets(events, files);
    }
}
