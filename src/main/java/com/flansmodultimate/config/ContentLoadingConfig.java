package com.flansmodultimate.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.flansmodultimate.FlansMod;
import com.flansmodultimate.util.FileUtils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ContentLoadingConfig
{
    @Getter
    private static String contentPacksRelativePath = "flan";
    @Getter
    private static boolean forceRegenContentPacksAssetsAndIds = false;
    @Getter
    private static boolean useDefaultCategories = true;

    private static final int CONTENT_LOADING_SYSTEM_VERSION = 6;
    private static final String FILE_NAME = FlansMod.MOD_ID + "-content-loading.toml";

    static
    {
        load();
    }

    public static void load()
    {
        Path configDir = FMLPaths.CONFIGDIR.get();
        Path file = configDir.resolve(FILE_NAME);

        FileUtils.tryCreateDirectories(configDir);
        try (CommentedFileConfig config = CommentedFileConfig.of(file, TomlFormat.instance()))
        {
            if (Files.isRegularFile(file))
                config.load();

            contentPacksRelativePath = readString(config, "contentPacksRelativePath", contentPacksRelativePath);
            forceRegenContentPacksAssetsAndIds = readBoolean(config, "forceRegenContentPacksAssetsAndIds", forceRegenContentPacksAssetsAndIds);
            int lastContentLoadingSystemVersion = readInt(config, "contentLoadingSystemVersion", CONTENT_LOADING_SYSTEM_VERSION);
            useDefaultCategories = readBoolean(config, "useDefaultCategories", useDefaultCategories);

            save(config);

            // When the loader version changes, force regen once without persisting the forced value.
            if (lastContentLoadingSystemVersion < CONTENT_LOADING_SYSTEM_VERSION)
                forceRegenContentPacksAssetsAndIds = true;
        }
        catch (Exception e)
        {
            FlansMod.log.error("Could not read config file {}", FILE_NAME, e);
            writeDefaults(file);
        }
    }

    private static void writeDefaults(Path file)
    {
        try (CommentedFileConfig config = CommentedFileConfig.of(file, TomlFormat.instance()))
        {
            save(config);
        }
        catch (Exception e)
        {
            FlansMod.log.error("Could not write config file {}", FILE_NAME, e);
        }
    }

    private static void save(CommentedFileConfig config)
    {
        config.set("contentPacksRelativePath", contentPacksRelativePath);
        config.setComment("contentPacksRelativePath", "Path to your content packs, relative to the .minecraft directory.");

        config.set("forceRegenContentPacksAssetsAndIds", forceRegenContentPacksAssetsAndIds);
        config.setComment("forceRegenContentPacksAssetsAndIds", """
            Set to true to force asset and ids regeneration. This will increase the startup time significantly.
            Only do this once when you modified some of your content packs (new assets or new ids).""");

        config.set("contentLoadingSystemVersion", CONTENT_LOADING_SYSTEM_VERSION);
        config.setComment("contentLoadingSystemVersion", """
            Version of the content loading system.
            Will be incremented when the content loading process is undergoing significant changes.
            When the version changes, asset and ids regeneration will be automatically performed once.""");

        config.set("useDefaultCategories", useDefaultCategories);
        config.setComment("useDefaultCategories", """
            The new category system allows items to be grouped and modified without modifying their config files in content packs.
            Categories can apply or override settings for all items within them.
            By default, this mod provides preconfigured categories in .minecraft/config/flansmodultimate/default.
            Set this option to false if you want to disable these default categories.""");

        config.save();
    }

    private static String readString(CommentedFileConfig config, String key, String defaultValue)
    {
        Object value = config.get(key);
        if (value == null)
            return defaultValue;
        if (value instanceof String str)
            return str;

        FlansMod.log.warn("Ignoring invalid {} in {}: {}. Expected string.", key, FILE_NAME, value);
        return defaultValue;
    }

    private static boolean readBoolean(CommentedFileConfig config, String key, boolean defaultValue)
    {
        Object value = config.get(key);
        if (value == null)
            return defaultValue;
        if (value instanceof Boolean bool)
            return bool;

        FlansMod.log.warn("Ignoring invalid {} in {}: {}. Expected boolean.", key, FILE_NAME, value);
        return defaultValue;
    }

    private static int readInt(CommentedFileConfig config, String key, int defaultValue)
    {
        Object value = config.get(key);
        if (value == null)
            return defaultValue;
        if (value instanceof Number number)
        {
            long longValue = number.longValue();
            if (longValue >= Integer.MIN_VALUE && longValue <= Integer.MAX_VALUE)
                return (int)longValue;
        }

        FlansMod.log.warn("Ignoring invalid {} in {}: {}. Expected integer.", key, FILE_NAME, value);
        return defaultValue;
    }
}
