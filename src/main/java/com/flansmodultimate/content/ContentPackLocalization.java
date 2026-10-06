package com.flansmodultimate.content;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.util.FileUtils;
import com.flansmodultimate.util.FlansLog;
import com.flansmodultimate.util.TextDecoding;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.flansmodultimate.content.ContentPackPaths.FOLDER_LANG;

/** Converts legacy translations and adds names for registered types and ID aliases. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class ContentPackLocalization
{
    private static final String TRANSLATION_KEY_PREFIX_ITEM = "item.";
    private static final String TRANSLATION_KEY_PREFIX_BLOCK = "block.";
    private static final String TRANSLATION_KEY_PREFIX_TYPE = "tile.";
    private static final String TRANSLATION_KEY_SUFFIX_NAME = ".name";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    static void generate(IContentProvider provider, List<InfoType> types)
    {
        Path langDir = provider.getAssetsPath().resolve(FOLDER_LANG);

        if (!FileUtils.tryCreateDirectories(langDir))
            return;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(langDir, "*.lang"))
        {
            for (Path langFile : stream)
            {
                generateLocalizationFile(langFile, types);
            }
        }
        catch (IOException e)
        {
            FlansLog.log.error("Failed to read localization files in {}", langDir, e);
        }

        Path english = langDir.resolve("en_us.json");
        Path ownership = langDir.resolve(".flansmod_generated_translations.json");
        Map<String, String> fallback = new LinkedHashMap<>();
        if (Files.isRegularFile(english))
        {
            try (java.io.Reader reader = Files.newBufferedReader(english, StandardCharsets.UTF_8))
            {
                Map<String, String> existing = gson.fromJson(reader, new com.google.gson.reflect.TypeToken<Map<String, String>>()
                {}.getType());
                if (existing != null)
                    fallback.putAll(existing);
            }
            catch (IOException | RuntimeException e)
            {
                FlansLog.log.warn("Could not read English translations '{}': {}", english, e.toString());
                return; // Do not overwrite authored malformed JSON.
            }
        }
        if (!Files.exists(langDir.resolve("en_us.lang")) && Files.isRegularFile(ownership))
        {
            try (java.io.Reader reader = Files.newBufferedReader(ownership, StandardCharsets.UTF_8))
            {
                Map<String, String> old = gson.fromJson(reader, new com.google.gson.reflect.TypeToken<Map<String, String>>()
                {}.getType());
                if (old != null)
                    old.forEach(fallback::remove);
            }
            catch (IOException | RuntimeException e)
            {
                FlansLog.log.warn("Could not read translation ownership '{}': {}", ownership, e.toString());
            }
        }
        Map<String, String> generated = new LinkedHashMap<>();
        for (InfoType type : types)
            if (type.getType().isHasItem() || type.getType().isHasBlock())
            {
                String key = generateTranslationKey(type.getShortName(), type.getType().isHasBlock());
                String original = generateTranslationKey(type.getOriginalShortName(), type.getType().isHasBlock());
                if (!fallback.containsKey(key))
                {
                    String name = fallback.getOrDefault(original, type.getName());
                    fallback.put(key, name);
                    generated.put(key, name);
                }
            }
        FileUtils.writeString(english, gson.toJson(fallback));
        FileUtils.writeString(ownership, gson.toJson(generated));
    }

    private static void generateLocalizationFile(Path langFile, List<InfoType> types)
    {
        Map<String, String> translations = readLangFile(langFile);
        for (InfoType config : types)
        {
            String shortName = config.getShortName();
            if (!shortName.equals(config.getOriginalShortName()))
            {
                String keyToAdd = generateTranslationKey(shortName, config.getType().isHasBlock());
                String keyToRemove = generateTranslationKey(config.getOriginalShortName(), config.getType().isHasBlock());
                String legacyTranslation = translations.remove(keyToRemove);
                translations.putIfAbsent(keyToAdd, legacyTranslation != null ? legacyTranslation : config.getName());
            }
            else
            {
                translations.putIfAbsent(generateTranslationKey(shortName, config.getType().isHasBlock()), config.getName());
            }
        }

        String jsonFileName = langFile.getFileName().toString().toLowerCase(Locale.ROOT).replace(FileUtils.LANG_EXTENSION, FileUtils.JSON_EXTENSION);
        Path jsonPath = langFile.getParent().resolve(jsonFileName);

        try (Writer writer = Files.newBufferedWriter(jsonPath, StandardCharsets.UTF_8))
        {
            gson.toJson(translations, writer);
        }
        catch (IOException e)
        {
            FlansLog.log.error("Failed to write to localization file {}", jsonPath, e);
        }
    }

    private static Map<String, String> readLangFile(Path langFile)
    {
        Map<String, String> translations = new LinkedHashMap<>();
        try
        {
            for (String line : readLinesUtf8OrUtf16(langFile))
            {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || line.indexOf('=') < 0)
                    continue;

                String key = line.substring(0, line.indexOf('=')).trim();
                String value = line.substring(line.indexOf('=') + 1).trim();

                // Convert key to new format
                key = convertTranslationKey(key);

                // Unescape properties-style characters
                value = value.replace("\\n", "\n").replace("\\\"", "\"");

                translations.put(key, value);
            }
        }
        catch (Exception e)
        {
            FlansLog.log.error("Failed to read localization file {}", langFile, e);
        }
        return translations;
    }

    private static List<String> readLinesUtf8OrUtf16(Path file) throws IOException
    {
        List<String> lines = TextDecoding.readLines(file);
        stripBomIfPresent(lines);
        return lines;
    }

    private static String convertTranslationKey(String legacyKey)
    {
        if (legacyKey.startsWith(TRANSLATION_KEY_PREFIX_ITEM) && legacyKey.endsWith(TRANSLATION_KEY_SUFFIX_NAME))
        {
            String id = legacyKey.substring(5, legacyKey.length() - 5).toLowerCase(Locale.ROOT);
            return TRANSLATION_KEY_PREFIX_ITEM + FlansMod.FLANSMOD_ID + "." + id;
        }
        if ((legacyKey.startsWith(TRANSLATION_KEY_PREFIX_TYPE) || legacyKey.startsWith(TRANSLATION_KEY_PREFIX_BLOCK)) && legacyKey.endsWith(TRANSLATION_KEY_SUFFIX_NAME))
        {
            String id = legacyKey.substring(legacyKey.indexOf('.') + 1, legacyKey.length() - 5).toLowerCase(Locale.ROOT);
            return TRANSLATION_KEY_PREFIX_BLOCK + FlansMod.FLANSMOD_ID + "." + id;
        }
        return legacyKey;
    }

    private static String generateTranslationKey(String itemId, boolean isBlock)
    {
        return (isBlock ? TRANSLATION_KEY_PREFIX_BLOCK : TRANSLATION_KEY_PREFIX_ITEM) + FlansMod.FLANSMOD_ID + "." + itemId;
    }

    private static void stripBomIfPresent(List<String> lines)
    {
        if (!lines.isEmpty() && !lines.get(0).isEmpty() && lines.get(0).charAt(0) == '\uFEFF')
        {
            lines.set(0, lines.get(0).substring(1));
        }
    }
}
