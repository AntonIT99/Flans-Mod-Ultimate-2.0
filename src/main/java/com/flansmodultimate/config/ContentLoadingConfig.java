package com.flansmodultimate.config;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.util.FileUtils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ContentLoadingConfig
{
    @Getter
    private static String contentPacksRelativePath = "flan";
    @Getter
    private static boolean forceRegenContentPacksAssetsAndIds = false;
    @Getter
    private static boolean useDefaultCategories = true;

    private static final int CONTENT_LOADING_SYSTEM_VERSION = 2;
    private static final String FILE_NAME = FlansMod.MOD_ID + "-content-loading.properties";

    static
    {
        load();
    }

    public static void load()
    {
        Path configDir = FMLPaths.CONFIGDIR.get();
        Path file = configDir.resolve(FILE_NAME);

        Properties props = new Properties();

        if (Files.exists(file))
        {
            try (Reader reader = Files.newBufferedReader(file))
            {
                props.load(reader);
            }
            catch (IOException e)
            {
                FlansMod.log.error("Could not read config file {}", FILE_NAME, e);
            }
        }

        contentPacksRelativePath = Objects.requireNonNullElse(props.getProperty("contentPacksRelativePath"), contentPacksRelativePath);
        forceRegenContentPacksAssetsAndIds = parseBoolean(props.getProperty("forceRegenContentPacksAssetsAndIds"), forceRegenContentPacksAssetsAndIds);
        int lastContentLoadingSystemVersion = parseInt(props.getProperty("contentLoadingSystemVersion"), CONTENT_LOADING_SYSTEM_VERSION);
        useDefaultCategories = parseBoolean(props.getProperty("useDefaultCategories"), useDefaultCategories);
        save();

        // when the loader version changes, force regen
        if (lastContentLoadingSystemVersion < CONTENT_LOADING_SYSTEM_VERSION)
            forceRegenContentPacksAssetsAndIds = true;
    }

    private static void save()
    {
        Path configDir = FMLPaths.CONFIGDIR.get();
        FileUtils.tryCreateDirectories(configDir);

        Path file = configDir.resolve(FILE_NAME);

        Properties props = new Properties();
        props.setProperty("contentPacksRelativePath", contentPacksRelativePath);
        props.setProperty("forceRegenContentPacksAssetsAndIds", Boolean.toString(forceRegenContentPacksAssetsAndIds));
        props.setProperty("contentLoadingSystemVersion", Integer.toString(CONTENT_LOADING_SYSTEM_VERSION));
        props.setProperty("useDefaultCategories", Boolean.toString(useDefaultCategories));

        try (Writer writer = Files.newBufferedWriter(file))
        {
            props.store(writer, """
                contentPacksRelativePath:
                    Path to your content packs, relative to the .minecraft directory.
                forceRegenContentPacksAssetsAndIds:
                    Set to true to force asset and ids regeneration. This will increase the startup time significantly.
                    Only do this once when you modified some of your content packs (new assets or new ids).
                contentLoadingSystemVersion:
                    Version of the content loading system.
                    Will be incremented when the content loading process is undergoing significant changes.
                    When the version changes, asset and ids regeneration will be automatically performed once.
                useDefaultCategories:
                    The new category system allows items to be grouped and modified without modifying their config files in content packs.
                    Categories can apply or override settings for all items within them.
                    By default, this mod provides preconfigured categories in .minecraft/config/flansmodultimate/default.
                    Set this option to false if you want to disable these default categories.
                """);
        }
        catch (IOException e)
        {
            FlansMod.log.error("Could not write to config file {}", file, e);
        }
    }

    private static boolean parseBoolean(String value, boolean defaultValue)
    {
        if (value == null)
            return defaultValue;
        return Boolean.parseBoolean(value.trim());
    }

    private static int parseInt(String value, int defaultValue)
    {
        if (value == null)
            return defaultValue;
        try
        {
            return Integer.parseInt(value.trim());
        }
        catch (NumberFormatException e)
        {
            return defaultValue;
        }
    }
}
