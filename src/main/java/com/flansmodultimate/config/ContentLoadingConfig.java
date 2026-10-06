package com.flansmodultimate.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.flansmodultimate.FlansMod;
import com.flansmodultimate.platform.PlatformPaths;
import com.flansmodultimate.util.FileUtils;
import com.flansmodultimate.util.FlansLog;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ContentLoadingConfig
{
    @Getter
    private static String contentPacksRelativePath = "flan";
    @Getter
    private static boolean forceRegenContentPacksAssetsAndIds = false;
    @Getter
    private static boolean useDefaultCategories = true;
    @Getter
    private static boolean overrideConfiguredSoundLengths = true;
    @Getter
    private static boolean overrideConfiguredShootPoints = true;
    @Getter
    private static List<String> soundPackPriority = List.of();
    @Getter
    private static int contentLoadingThreads = 0;

    // 7: normalize standalone sound assets on dedicated servers as well as clients.
    private static final int CONTENT_LOADING_SYSTEM_VERSION = 7;
    private static final String FILE_NAME = FlansMod.MOD_ID + "-content-loading.toml";

    static
    {
        load();
    }

    public static void load()
    {
        Path configDir = PlatformPaths.configDir();
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
            overrideConfiguredSoundLengths = readBoolean(config, "overrideConfiguredSoundLengths", overrideConfiguredSoundLengths);
            overrideConfiguredShootPoints = readBoolean(config, "overrideConfiguredShootPoints", overrideConfiguredShootPoints);
            soundPackPriority = SoundPriorityConfig.reconcile(config.get(SoundPriorityConfig.KEY), List.of());
            contentLoadingThreads = Math.max(0, readInt(config, "contentLoadingThreads", contentLoadingThreads));

            save(config);

            if (forceRegenContentPacksAssetsAndIds)
                FlansLog.log.warn("forceRegenContentPacksAssetsAndIds is enabled in {}: every launch regenerates the assets of all content packs "
                    + "and repacks their archives. Set it back to false once the packs have been regenerated.", FILE_NAME);

            // When the loader version changes, force regen once without persisting the forced value.
            if (lastContentLoadingSystemVersion < CONTENT_LOADING_SYSTEM_VERSION)
                forceRegenContentPacksAssetsAndIds = true;
        }
        catch (Exception e)
        {
            FlansLog.log.error("Could not read config file {}", FILE_NAME, e);
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
            FlansLog.log.error("Could not write config file {}", FILE_NAME, e);
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
            Set this option to false if you want to disable these default categories.
            User categories can still explicitly inherit them with "inherits": "default:Category Name" in category JSON.""");

        config.set("overrideConfiguredSoundLengths", overrideConfiguredSoundLengths);
        config.setComment("overrideConfiguredSoundLengths", """
            Sound lengths such as EngineSoundLength or LoopedSoundLength tell the mod when to play a looping sound again.
            Content packs often configure them only roughly, which makes looping sounds overlap or leave a gap.
            By default these values are replaced with the real length of the sound file, measured while loading the pack.
            Set this option to false if you want the values configured in the content packs to be used as they are.
            Sound lengths that are disabled with None, or that are not set at all, are never filled in automatically.""");

        config.set("overrideConfiguredShootPoints", overrideConfiguredShootPoints);
        config.setComment("overrideConfiguredShootPoints", """
            Content packs often place the muzzles of their vehicles, planes, mechas and AA guns away from the barrels of their models,
            so shots and muzzle flashes appear beside, behind or inside the gun.
            By default the muzzles of the content packs in the flan folder are moved onto the barrels measured off their models
            while the packs are loading: a primary weapon with a single shoot point, every passenger gun the model draws,
            and every AA gun barrel.
            Content packs installed as mods in the mods folder match their models and are always used as they are configured.
            Set this option to false if you want the values configured in the flan folder packs to be used as they are too.
            Either way, a twin or quad mount fired from one point fires from each barrel its model draws in turn,
            around that point, at the same rate of fire.
            Server and clients should use the same value.""");

        config.set("contentLoadingThreads", contentLoadingThreads);
        config.setComment("contentLoadingThreads", """
            Threads that read and index the content packs while the game starts. 0 chooses from the number of processors.
            The packs are still registered one after the other, in the same order, so this never changes what is loaded.
            Set it to 1 to do everything on the loading thread.""");

        config.set(SoundPriorityConfig.KEY, soundPackPriority);
        config.setComment(SoundPriorityConfig.KEY, SoundPriorityConfig.COMMENT);
        try
        {
            SoundPriorityConfig.saveIfChanged(config, PlatformPaths.configDir().resolve(FILE_NAME));
        }
        catch (java.io.IOException exception)
        {
            FlansLog.log.error("Could not write config file {}", FILE_NAME, exception);
        }
    }

    public static List<String> synchronizeSoundPackPriority(List<String> discovered)
    {
        try
        {
            soundPackPriority = SoundPriorityConfig.synchronize(PlatformPaths.configDir().resolve(FILE_NAME), discovered);
        }
        catch (Exception exception)
        {
            soundPackPriority = SoundPriorityConfig.reconcile(soundPackPriority, discovered);
            FlansLog.log.error("Could not save sound priorities in {}", FILE_NAME, exception);
        }
        return soundPackPriority;
    }

    private static String readString(CommentedFileConfig config, String key, String defaultValue)
    {
        Object value = config.get(key);
        if (value == null)
            return defaultValue;
        if (value instanceof String str)
            return str;

        FlansLog.log.warn("Ignoring invalid {} in {}: {}. Expected string.", key, FILE_NAME, value);
        return defaultValue;
    }

    private static boolean readBoolean(CommentedFileConfig config, String key, boolean defaultValue)
    {
        Object value = config.get(key);
        if (value == null)
            return defaultValue;
        if (value instanceof Boolean bool)
            return bool;

        FlansLog.log.warn("Ignoring invalid {} in {}: {}. Expected boolean.", key, FILE_NAME, value);
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
                return (int) longValue;
        }

        FlansLog.log.warn("Ignoring invalid {} in {}: {}. Expected integer.", key, FILE_NAME, value);
        return defaultValue;
    }
}
