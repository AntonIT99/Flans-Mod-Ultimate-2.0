package com.flansmodultimate.config;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.types.EnumType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CategoryManager
{
    private static final Map<EnumType, List<Category>> categories = new EnumMap<>(EnumType.class);
    private static final Map<String, List<Category>> itemCategories = new HashMap<>();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void loadAll() {
        Path configDir = FMLPaths.CONFIGDIR.get();
        Path flanDir = configDir.resolve("flan");

        FlansMod.log.info("Loading category configs from {}", flanDir.toAbsolutePath());

        try
        {
            Files.createDirectories(flanDir);
        }
        catch (IOException e)
        {
            FlansMod.log.error("Failed to create category config directory at {}", flanDir.toAbsolutePath(), e);
            return;
        }

        for (EnumType type : EnumType.values())
        {
            List<Category> list = loadForType(type, flanDir);
            categories.put(type, list);
            FlansMod.log.info("Loaded {} categories for type {}", list.size(), type);
        }

        FlansMod.log.info("Finished loading category configs");
    }



    private static List<Category> loadForType(EnumType type, Path flanDir)
    {
        String fileName = type.getDisplayName() + "_categories.json";
        Path file = flanDir.resolve(fileName);

        FlansMod.log.debug("Loading categories for type {} from file {}", type, file.toAbsolutePath());

        // If config file missing, try to copy default from jar
        if (!Files.exists(file))
        {
            copyDefaultFromJar(type, file);
        }

        if (!Files.exists(file))
        {
            FlansMod.log.warn("No category config file and no default found for type {}. Using empty category list.", type);
            return List.of();
        }

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

    private static void copyDefaultFromJar(EnumType type, Path targetFile)
    {
        String resourcePath = "/config/" + type.getDisplayName() + "_categories.json";

        try (InputStream in = CategoryManager.class.getResourceAsStream(resourcePath))
        {
            if (in == null) // no default for this type
                return;

            Files.createDirectories(targetFile.getParent());
            Files.copy(in, targetFile, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException e)
        {
            FlansMod.log.error("Failed to copy default category config for type {} to {}", type, targetFile.toAbsolutePath(), e);
        }
    }
}