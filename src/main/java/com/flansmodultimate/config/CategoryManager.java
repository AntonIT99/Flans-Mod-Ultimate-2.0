package com.flansmodultimate.config;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.types.EnumType;
import com.flansmodultimate.common.types.TypeFile;
import com.flansmodultimate.util.FileUtils;
import com.flansmodultimate.util.ResourceUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.flansmodultimate.util.TypeReaderUtils.readValue;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CategoryManager
{
    private static final Map<EnumType, List<Category>> categories = new EnumMap<>(EnumType.class);
    private static final Map<String, List<Category>> itemCategories = new HashMap<>();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void applyCategoriesToFile(TypeFile file)
    {
        String shortname = ResourceUtils.sanitize(readValue("ShortName", null, file));
        if (StringUtils.isBlank(shortname) || !itemCategories.containsKey(shortname))
            return;

        for (Category category : itemCategories.get(shortname))
            file.addCategoryConfigMap(category);
    }

    public static void loadAll() {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve(FlansMod.MOD_ID);
        Path defaultConfigDir = configDir.resolve("default");

        try
        {
            Files.createDirectories(configDir);
        }
        catch (IOException e)
        {
            FlansMod.log.error("Failed to create directory at {}", configDir.toAbsolutePath(), e);
            return;
        }

        try
        {
            Files.createDirectories(defaultConfigDir);
        }
        catch (IOException e)
        {
            FlansMod.log.error("Failed to create directory at {}", defaultConfigDir.toAbsolutePath(), e);
            return;
        }

        loadCategories(configDir, defaultConfigDir);
    }

    private static void loadCategories(Path configDir, Path defaultConfigDir)
    {
        FlansMod.log.info("Loading categories");

        for (EnumType type : EnumType.values())
        {
            String fileName = type.getDisplayName() + "_categories.json";
            Path defaultFile = defaultConfigDir.resolve(fileName);
            Path userFile = configDir.resolve(fileName);

            try (InputStream in = CategoryManager.class.getResourceAsStream("/config/" + fileName))
            {
                if (in != null)
                {
                    byte[] data = in.readAllBytes();
                    boolean shouldCopy = !Files.exists(defaultFile) || FileUtils.differentBytes(data, Files.readAllBytes(defaultFile));

                    if (shouldCopy)
                        Files.write(defaultFile, data);
                }
            }
            catch (IOException e)
            {
                FlansMod.log.error("Failed to copy {}", defaultFile, e);
            }

            categories.putIfAbsent(type, new ArrayList<>());
            if (ContentLoadingConfig.useDefaultCategories)
                categories.get(type).addAll(loadForType(type, defaultFile));
            categories.get(type).addAll(loadForType(type, userFile));

            FlansMod.log.info("Loaded {} categories for {} type", categories.get(type).size(), type.getDisplayName());
        }

        FlansMod.log.info("Finished loading categories");
    }

    private static List<Category> loadForType(EnumType type, Path file)
    {
        FlansMod.log.debug("Loading categories for type {} from file {}", type, file.toAbsolutePath());

        if (!Files.exists(file))
            return List.of();

        try (Reader reader = Files.newBufferedReader(file))
        {
            Type mapType = new TypeToken<Map<String, Category>>() {}.getType();
            Map<String, Category> map = gson.fromJson(reader, mapType);
            if (map == null)
            {
                FlansMod.log.warn("Category config file {} for type {} is empty or invalid. Using empty category map.", file.toAbsolutePath(), type);
                return List.of();
            }

            for (Map.Entry<String, Category> e : map.entrySet())
            {
                Category category = e.getValue();
                category.setType(type);
                category.setName(e.getKey());
                for (String item : category.getItems())
                {
                    itemCategories.putIfAbsent(item, new ArrayList<>());
                    itemCategories.get(item).add(category);
                }
            }

            List<Category> list = map.values().stream().toList();
            FlansMod.log.debug("Successfully parsed {} categories from {}", list.size(), file.toAbsolutePath());
            return list;
        }
        catch (IOException e)
        {
            FlansMod.log.error("Failed to read category config file {} for type {}", file.toAbsolutePath(), type, e);
            return List.of();
        }
    }
}