package com.flansmodultimate.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Per content pack index of how long every bundled {@code .ogg} plays for, so the lengths never have
 * to be measured again while the pack is unchanged.
 * <p>
 * The index is generated while a pack is reprocessed and kept next to {@code sounds.json}. Packs
 * shipped inside the mod carry an index generated at build time by
 * {@code scripts/buildSoundLengthIndexes.py}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SoundLengthIndex
{
    /** Name of the index file, written next to {@code sounds.json} in the pack assets folder. */
    public static final String FILE_NAME = "sound-lengths.json";

    /** Bumped whenever the layout below changes, which makes every existing index regenerate. */
    private static final int FORMAT_VERSION = 2;

    private static final String FIELD_VERSION = "version";
    private static final String FIELD_SOUNDS = "sounds";
    private static final String FIELD_KEY = "key";
    private static final String FIELD_SIZE = "size";
    private static final String FIELD_TICKS = "ticks";

    /** Recorded for sounds that could not be measured, so an unreadable file does not force a rescan every load. */
    private static final int TICKS_UNKNOWN = 0;

    private static final Logger log = com.mojang.logging.LogUtils.getLogger();

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    /**
     * Measured length of every sound event, in the order the content packs were loaded. The first
     * pack registering a sound event owns it, exactly like {@link com.flansmodultimate.FlansMod#registerSound}.
     */
    private static final Map<String, Integer> soundLengths = new HashMap<>();

    /** One indexed sound file: the sound event it is registered as, its size, and its length. */
    private record IndexedSound(String key, long size, int ticks) {}

    /**
     * Adds the sounds of one content pack to the lookup. Sound events already known from an earlier
     * pack keep the length they were loaded with, so the length always belongs to the file that is
     * actually registered under that name.
     *
     * @param indexFile the index of a content pack, which does not have to exist
     */
    public static void load(Path indexFile)
    {
        for (IndexedSound sound : readIndexedSounds(indexFile).orElse(Collections.emptyMap()).values())
        {
            if (sound.ticks() != TICKS_UNKNOWN)
                soundLengths.putIfAbsent(sound.key(), sound.ticks());
        }
    }

    /** Forgets every loaded sound length, so the content packs can be read again from scratch. */
    public static void clear()
    {
        soundLengths.clear();
    }

    /**
     * Looks up how long a sound plays for.
     *
     * @param sound the name a sound is registered under, as read from a content pack
     * @return the length in ticks, or empty when no loaded pack has a measurement for it
     */
    public static OptionalInt getSoundLength(String sound)
    {
        Integer ticks = soundLengths.get(ResourceUtils.sanitize(sound));
        return ticks != null ? OptionalInt.of(ticks) : OptionalInt.empty();
    }

    /**
     * Rewrites the index for the given sounds folder, measuring every {@code .ogg} it contains. The
     * index file is removed when the pack has no sounds at all.
     *
     * @param soundsDir the assets/flansmod/sounds folder of a content pack
     * @param indexFile where to write the index
     */
    public static void generate(Path soundsDir, Path indexFile)
    {
        Map<String, IndexedSound> sounds = measureSounds(soundsDir);
        if (sounds.isEmpty())
        {
            FileUtils.deleteIfExists(indexFile);
            return;
        }

        JsonObject entries = new JsonObject();
        for (Map.Entry<String, IndexedSound> sound : sounds.entrySet())
        {
            JsonObject entry = new JsonObject();
            entry.addProperty(FIELD_KEY, sound.getValue().key());
            entry.addProperty(FIELD_SIZE, sound.getValue().size());
            entry.addProperty(FIELD_TICKS, sound.getValue().ticks());
            entries.add(sound.getKey(), entry);
        }

        JsonObject index = new JsonObject();
        index.addProperty(FIELD_VERSION, FORMAT_VERSION);
        index.add(FIELD_SOUNDS, entries);

        FileUtils.writeString(indexFile, GSON.toJson(index));
        log.debug("Indexed the length of {} sound(s) in {}", sounds.size(), indexFile);
    }

    /**
     * Checks whether the index still matches the sounds on disk, without reading any audio. A sound
     * added, removed or replaced changes the set of file names and sizes the index was written from.
     * <p>
     * Sizes are used rather than checksums because this runs against packs that are still archived,
     * where a checksum would mean decompressing every sound. A replacement that happens to be exactly
     * as large as the sound it replaces therefore goes unnoticed until the pack is reprocessed for
     * another reason, or {@code forceRegenContentPacksAssetsAndIds} is set.
     *
     * @param soundsDir the assets/flansmod/sounds folder of a content pack
     * @param indexFile the index to validate
     * @return {@code true} when the index has to be regenerated
     */
    public static boolean isOutdated(Path soundsDir, Path indexFile)
    {
        Map<String, Long> actualSizes = readFileSizes(soundsDir);
        Optional<Map<String, IndexedSound>> indexedSounds = readIndexedSounds(indexFile);

        if (indexedSounds.isEmpty())
            return !actualSizes.isEmpty();

        Map<String, Long> indexedSizes = new TreeMap<>();
        indexedSounds.get().forEach((path, sound) -> indexedSizes.put(path, sound.size()));
        return !indexedSizes.equals(actualSizes);
    }

    private static Map<String, IndexedSound> measureSounds(Path soundsDir)
    {
        Map<String, IndexedSound> sounds = new TreeMap<>();
        for (Map.Entry<String, Long> soundFile : readFileSizes(soundsDir).entrySet())
        {
            Path file = soundsDir.resolve(soundFile.getKey());
            OptionalInt ticks = OggDurationReader.readDurationTicks(file);
            if (ticks.isEmpty())
                log.warn("Could not determine the length of sound file {}", file);

            sounds.put(soundFile.getKey(), new IndexedSound(toSoundEventKey(soundFile.getKey()),
                soundFile.getValue(), ticks.orElse(TICKS_UNKNOWN)));
        }
        return sounds;
    }

    /** Maps every {@code .ogg} below the sounds folder to its size, keyed by its relative path. */
    private static Map<String, Long> readFileSizes(Path soundsDir)
    {
        if (!Files.isDirectory(soundsDir))
            return Collections.emptyMap();

        try (Stream<Path> stream = Files.walk(soundsDir))
        {
            Map<String, Long> fileSizes = new TreeMap<>();
            stream.filter(Files::isRegularFile)
                .filter(FileUtils::isOgg)
                .forEach(file -> fileSizes.put(toRelativePath(soundsDir, file), readFileSize(file)));
            return fileSizes;
        }
        catch (IOException e)
        {
            log.error("Could not scan sounds folder {}", soundsDir, e);
            return Collections.emptyMap();
        }
    }

    /** Reads an index file, or empty when it is absent, unreadable or written in another format. */
    private static Optional<Map<String, IndexedSound>> readIndexedSounds(Path indexFile)
    {
        if (!Files.isRegularFile(indexFile))
            return Optional.empty();

        try
        {
            JsonObject index = JsonParser.parseString(Files.readString(indexFile, StandardCharsets.UTF_8)).getAsJsonObject();
            if (!index.has(FIELD_VERSION) || index.get(FIELD_VERSION).getAsInt() != FORMAT_VERSION)
                return Optional.empty();

            Map<String, IndexedSound> indexedSounds = new TreeMap<>();
            for (Map.Entry<String, JsonElement> entry : index.getAsJsonObject(FIELD_SOUNDS).entrySet())
            {
                JsonObject sound = entry.getValue().getAsJsonObject();
                indexedSounds.put(entry.getKey(), new IndexedSound(sound.get(FIELD_KEY).getAsString(),
                    sound.get(FIELD_SIZE).getAsLong(), sound.get(FIELD_TICKS).getAsInt()));
            }

            return Optional.of(indexedSounds);
        }
        catch (IOException | RuntimeException e)
        {
            log.warn("Could not read the sound length index {}: {}", indexFile, e.toString());
            return Optional.empty();
        }
    }

    private static long readFileSize(Path file)
    {
        try
        {
            return Files.size(file);
        }
        catch (IOException e)
        {
            return -1L;
        }
    }

    private static String toRelativePath(Path soundsDir, Path file)
    {
        return soundsDir.relativize(file).toString().replace('\\', '/');
    }

    /** Derives the sound event name a file is registered under, the same way sounds.json generation does. */
    private static String toSoundEventKey(String relativePath)
    {
        String fileName = relativePath.substring(relativePath.lastIndexOf('/') + 1);
        if (fileName.toLowerCase(Locale.ROOT).endsWith(FileUtils.OGG_EXTENSION))
            fileName = fileName.substring(0, fileName.length() - FileUtils.OGG_EXTENSION.length());

        return ResourceUtils.sanitize(fileName);
    }
}
