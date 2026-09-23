package com.flansmodultimate.config;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.types.EnumType;
import com.flansmodultimate.common.types.TypeFile;
import com.flansmodultimate.util.FileUtils;
import com.flansmodultimate.util.ResourceUtils;
import com.flansmodultimate.util.StringOrNumberListMapAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.neoforged.fml.loading.FMLPaths;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.flansmodultimate.util.TypeReaderUtils.readValue;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CategoryManager
{
    private static final Logger log = LogUtils.getLogger();
    private static final String EMPTY_CATEGORY_CONFIG = "{}\n";
    private static final Map<EnumType, List<Category>> categories = new EnumMap<>(EnumType.class);
    private static final Map<String, List<Category>> itemCategories = new HashMap<>();
    private static final Gson gson = new GsonBuilder()
        .registerTypeAdapter(StringOrNumberListMapAdapter.targetType(), new StringOrNumberListMapAdapter())
        .setPrettyPrinting()
        .create();

    public static void applyCategoriesToFile(TypeFile file)
    {
        String shortname = ResourceUtils.sanitize(readValue("ShortName", null, file));
        if (StringUtils.isBlank(shortname) || !itemCategories.containsKey(shortname))
            return;

        for (Category category : itemCategories.get(shortname))
            if (category.getType() == file.getType())
                file.addCategoryConfigMap(category, shortname);
    }

    public static void loadAll()
    {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve(FlansMod.MOD_ID);
        Path defaultConfigDir = configDir.resolve("default");

        if (!FileUtils.tryCreateDirectories(configDir))
            return;
        if (!FileUtils.tryCreateDirectories(defaultConfigDir))
            return;

        loadCategories(configDir, defaultConfigDir, ContentLoadingConfig.isUseDefaultCategories());
    }

    static void loadCategories(Path configDir, Path defaultConfigDir, boolean useDefaults)
    {
        log.info("Loading categories");
        categories.clear();
        itemCategories.clear();

        for (EnumType type : EnumType.values())
        {
            String fileName = type.getIdentifier() + "_categories.json";
            Path defaultFile = defaultConfigDir.resolve(fileName);
            Path userFile = configDir.resolve(fileName);

            ensureUserCategoryFileExists(userFile);

            try (InputStream in = CategoryManager.class.getResourceAsStream("/config/" + fileName))
            {
                if (in != null)
                {
                    byte[] data = in.readAllBytes();
                    boolean shouldCopy = FileUtils.isDifferentFileContent(defaultFile, data, true);
                    if (shouldCopy)
                        Files.write(defaultFile, data);
                }
            }
            catch (IOException e)
            {
                log.error("Failed to copy {}", defaultFile, e);
            }

            List<Category> resolved = new CategoryResolver(type, loadForType(type, defaultFile),
                loadForType(type, userFile), useDefaults)
                .resolve(message -> log.error("{}", message));
            categories.put(type, resolved);
            for (Category category : resolved)
                for (String item : category.getItems())
                    itemCategories.computeIfAbsent(item.toLowerCase(Locale.ROOT), ignored -> new ArrayList<>()).add(category);

            int numCategoriesForType = categories.get(type).size();
            if (numCategoriesForType > 0)
                log.info("Loaded {} categories for {} type", numCategoriesForType, type.getIdentifier());
        }

        log.info("Finished loading categories");
    }

    private static void ensureUserCategoryFileExists(Path file)
    {
        if (Files.exists(file))
            return;

        try
        {
            Files.writeString(file, EMPTY_CATEGORY_CONFIG, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        }
        catch (FileAlreadyExistsException ignored)
        {
            // Ignored
        }
        catch (IOException e)
        {
            log.error("Failed to create empty category config file {}", file.toAbsolutePath(), e);
        }
    }

    private static List<Category> loadForType(EnumType type, Path file)
    {
        log.debug("Loading categories for type {} from file {}", type, file.toAbsolutePath());

        if (!Files.exists(file))
            return List.of();

        try (Reader reader = Files.newBufferedReader(file))
        {
            Type mapType = new TypeToken<Map<String, Category>>() {}.getType();
            Map<String, Category> map = gson.fromJson(reader, mapType);
            if (map == null)
            {
                log.warn("Category config file {} for type {} is empty or invalid. Using empty category map.", file.toAbsolutePath(), type);
                return List.of();
            }

            for (Map.Entry<String, Category> e : map.entrySet())
            {
                Category category = e.getValue();
                category.setType(type);
                category.setName(e.getKey());
            }

            List<Category> list = map.values().stream().toList();
            log.debug("Successfully parsed {} categories from {}", list.size(), file.toAbsolutePath());
            return list;
        }
        catch (IOException e)
        {
            log.error("Failed to read category config file {} for type {}", file.toAbsolutePath(), type, e);
            return List.of();
        }
    }
}
