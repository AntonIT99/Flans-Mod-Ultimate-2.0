package com.flansmodultimate.config;

import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.flansmodultimate.FlansMod;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ModApocalypseConfig
{
    public static final String CONFIG_FILE_NAME = FlansMod.MOD_ID + "-apocalypse.toml";

    private static final String APOCALYPSE_CONFIG_SECTION = "Apocalypse Settings";
    private static final String LEGACY_COMMON_CONFIG_FILE_NAME = FlansMod.MOD_ID + "-common.toml";

    public static final ModConfigSpec configSpec;
    private static final AtomicReference<ApocalypseConfigSnapshot> instance = new AtomicReference<>();
    private static final AtomicReference<ApocalypseConfigSnapshot> serverOverride = new AtomicReference<>();

    private static final ModConfigSpec.BooleanValue APOCALYPSE_ENABLED;
    private static final ModConfigSpec.BooleanValue APOCALYPSE_DIMENSION_ENABLED;
    private static final ModConfigSpec.BooleanValue APOCALYPSE_PORTALS_ENABLED;
    private static final ModConfigSpec.BooleanValue APOCALYPSE_OVERWORLD_PORTAL_GENERATION_ENABLED;
    private static final ModConfigSpec.BooleanValue APOCALYPSE_WORLDGEN_ENABLED;
    private static final ModConfigSpec.BooleanValue APOCALYPSE_MOBS_ENABLED;
    private static final ModConfigSpec.BooleanValue APOCALYPSE_NUKE_DROPS_ENABLED;
    private static final ModConfigSpec.IntValue APOCALYPSE_COUNTDOWN_LENGTH;
    private static final ModConfigSpec.IntValue APOCALYPSE_SURVIVOR_RARITY;
    private static final ModConfigSpec.IntValue APOCALYPSE_WANDERING_SURVIVOR_RARITY;
    private static final ModConfigSpec.IntValue APOCALYPSE_SKELETON_RARITY;
    private static final ModConfigSpec.IntValue APOCALYPSE_DEAD_TREE_RARITY;
    private static final ModConfigSpec.IntValue APOCALYPSE_VEHICLE_RARITY;
    private static final ModConfigSpec.IntValue APOCALYPSE_AIRPORT_RARITY;
    private static final ModConfigSpec.IntValue APOCALYPSE_DYE_FACTORY_RARITY;
    private static final ModConfigSpec.IntValue APOCALYPSE_LAB_RARITY;
    private static final ModConfigSpec.IntValue APOCALYPSE_ABANDONED_PORTAL_APOC_RARITY;
    private static final ModConfigSpec.IntValue APOCALYPSE_ABANDONED_PORTAL_OVERWORLD_RARITY;
    private static final ModConfigSpec.IntValue APOCALYPSE_RETURN_RADIUS;
    private static final ModConfigSpec.IntValue APOCALYPSE_SPAWN_RADIUS;
    private static final ModConfigSpec.BooleanValue APOCALYPSE_RESPAWN_IN_APOCALYPSE;
    private static final ModConfigSpec.EnumValue<ApocalypseTeleportOption> APOCALYPSE_TELEPORT_OPTION;
    private static final ModConfigSpec.DoubleValue APOCALYPSE_ACID_DAMAGE;
    private static final ModConfigSpec.DoubleValue APOCALYPSE_NUKE_EXPLOSION_POWER;
    private static final ModConfigSpec.IntValue APOCALYPSE_NUKE_VISUAL_TICKS;

    static
    {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push(APOCALYPSE_CONFIG_SECTION);
        APOCALYPSE_ENABLED = builder
            .comment("Master switch for integrated Flan's Mod Apocalypse content and server-side behavior.")
            .define("apocalypseEnabled", true);
        APOCALYPSE_DIMENSION_ENABLED = builder
            .comment("Register and auto-enable the built-in Apocalypse dimension datapack during world loading.",
                "Requires a full game/server restart after changing because datapack repositories are built before worlds load.",
                "Existing worlds can also remember enabled datapacks in level.dat; disable this before loading the world if you want the dimension datapack unavailable.")
            .define("apocalypseDimensionEnabled", true);
        APOCALYPSE_PORTALS_ENABLED = builder
            .comment("Enable power-cube portal activation and portal entity teleporting.")
            .define("apocalypsePortalsEnabled", true);
        APOCALYPSE_OVERWORLD_PORTAL_GENERATION_ENABLED = builder
            .comment("Generate abandoned apocalypse portals outside the apocalypse dimension.")
            .define("apocalypseOverworldPortalGenerationEnabled", true);
        APOCALYPSE_WORLDGEN_ENABLED = builder
            .comment("Enable apocalypse sulphur pools, dead trees, skeleton displays, portals, and simple structure generation.")
            .define("apocalypseWorldgenEnabled", true);
        APOCALYPSE_MOBS_ENABLED = builder
            .comment("Enable apocalypse survivor, drone, and boss spawning/behavior.")
            .define("apocalypseMobsEnabled", true);
        APOCALYPSE_NUKE_DROPS_ENABLED = builder
            .comment("Enable nuke drop entities during apocalypse events.")
            .define("apocalypseNukeDropsEnabled", true);
        APOCALYPSE_COUNTDOWN_LENGTH = builder
            .comment("Time in ticks between an AI-chip apocalypse trigger and the event starting. Kept for compatibility with the old config.")
            .defineInRange("apocalypseCountdownLength", 469, 19, Integer.MAX_VALUE);
        APOCALYPSE_SURVIVOR_RARITY = builder
            .comment("Chunk generation rarity for survivors. 1 means every eligible attempt; larger values are rarer.")
            .defineInRange("apocalypseSurvivorRarity", 250, 1, Integer.MAX_VALUE);
        APOCALYPSE_WANDERING_SURVIVOR_RARITY = builder
            .comment("Per-player server tick rarity for wandering survivors in the apocalypse dimension.")
            .defineInRange("apocalypseWanderingSurvivorRarity", 500, 1, Integer.MAX_VALUE);
        APOCALYPSE_SKELETON_RARITY = builder
            .comment("Chunk generation rarity for buried skeleton displays.")
            .defineInRange("apocalypseSkeletonRarity", 50, 1, Integer.MAX_VALUE);
        APOCALYPSE_DEAD_TREE_RARITY = builder
            .comment("Chunk generation rarity for dead trees.")
            .defineInRange("apocalypseDeadTreeRarity", 100, 1, Integer.MAX_VALUE);
        APOCALYPSE_VEHICLE_RARITY = builder
            .comment("Chunk generation rarity for damaged, empty-fuel vehicles supplied by installed content packs.")
            .defineInRange("apocalypseVehicleRarity", 2000, 1, Integer.MAX_VALUE);
        APOCALYPSE_AIRPORT_RARITY = builder
            .comment("Chunk generation rarity for simple runway/airport structures.")
            .defineInRange("apocalypseAirportRarity", 125, 1, Integer.MAX_VALUE);
        APOCALYPSE_DYE_FACTORY_RARITY = builder
            .comment("Chunk generation rarity for simple dye factory structures.")
            .defineInRange("apocalypseDyeFactoryRarity", 400, 1, Integer.MAX_VALUE);
        APOCALYPSE_LAB_RARITY = builder
            .comment("Chunk generation rarity for simple research lab structures.")
            .defineInRange("apocalypseLabRarity", 100, 1, Integer.MAX_VALUE);
        APOCALYPSE_ABANDONED_PORTAL_APOC_RARITY = builder
            .comment("Chunk generation rarity for abandoned portals in the apocalypse dimension.")
            .defineInRange("apocalypseAbandonedPortalRarity", 4000, 1, Integer.MAX_VALUE);
        APOCALYPSE_ABANDONED_PORTAL_OVERWORLD_RARITY = builder
            .comment("Chunk generation rarity for abandoned portals outside the apocalypse dimension.")
            .defineInRange("apocalypseAbandonedPortalOverworldRarity", 4000, 1, Integer.MAX_VALUE);
        APOCALYPSE_RETURN_RADIUS = builder
            .comment("Distance from the recorded entry point where return portals are searched/generated.")
            .defineInRange("apocalypseReturnRadius", 100, 1, Integer.MAX_VALUE);
        APOCALYPSE_SPAWN_RADIUS = builder
            .comment("Distance from a death point used by apocalypse respawn logic.")
            .defineInRange("apocalypseSpawnRadius", 100, 1, Integer.MAX_VALUE);
        APOCALYPSE_RESPAWN_IN_APOCALYPSE = builder
            .comment("If true, players who die in the apocalypse dimension respawn near their death point instead of normal overworld spawn behavior.")
            .define("apocalypseRespawnInApocalypse", false);
        APOCALYPSE_TELEPORT_OPTION = builder
            .comment("Who is sent by legacy AI-chip apocalypse triggers. Reserved until mecha trigger support is complete.")
            .defineEnum("apocalypseTeleportOption", ApocalypseTeleportOption.PLACER_ONLY);
        APOCALYPSE_ACID_DAMAGE = builder
            .comment("Damage per tick from sulphuric acid.")
            .defineInRange("apocalypseAcidDamage", 5.0, 0.0, 1000.0);
        APOCALYPSE_NUKE_EXPLOSION_POWER = builder
            .comment("Explosion power when a nuke drop impacts. Set to 0 to keep nuke drops visual only.")
            .defineInRange("apocalypseNukeExplosionPower", 0.0, 0.0, 1000.0);
        APOCALYPSE_NUKE_VISUAL_TICKS = builder
            .comment("Lifetime after nuke impact, in ticks.")
            .defineInRange("apocalypseNukeVisualTicks", 500, 1, Integer.MAX_VALUE);
        builder.pop();

        configSpec = builder.build();
    }

    public static boolean apocalypseEnabled()
    {
        EarlyApocalypseSettings settings = readEarlyApocalypseSettings();
        return settings.apocalypseEnabled();
    }

    public static boolean apocalypseDimensionDatapackEnabled()
    {
        EarlyApocalypseSettings settings = readEarlyApocalypseSettings();
        return settings.apocalypseEnabled() && settings.apocalypseDimensionEnabled();
    }

    private static EarlyApocalypseSettings readEarlyApocalypseSettings()
    {
        Path configPath = FMLPaths.CONFIGDIR.get().resolve(CONFIG_FILE_NAME);
        if (Files.isRegularFile(configPath))
            return readEarlyApocalypseSettings(configPath);

        Path legacyConfigPath = FMLPaths.CONFIGDIR.get().resolve(LEGACY_COMMON_CONFIG_FILE_NAME);
        if (Files.isRegularFile(legacyConfigPath))
            return readEarlyApocalypseSettings(legacyConfigPath);

        return new EarlyApocalypseSettings(true, true);
    }

    private static EarlyApocalypseSettings readEarlyApocalypseSettings(Path configPath)
    {
        try (FileConfig config = FileConfig.of(configPath, TomlFormat.instance()))
        {
            config.load();
            return new EarlyApocalypseSettings(
                readEarlyBoolean(config, configPath, "apocalypseEnabled", true),
                readEarlyBoolean(config, configPath, "apocalypseDimensionEnabled", true)
            );
        }
        catch (Exception e)
        {
            FlansMod.log.warn("Unable to read early apocalypse datapack settings from {}. Using defaults: {}", configPath, e.toString());
            return new EarlyApocalypseSettings(true, true);
        }
    }

    private static boolean readEarlyBoolean(FileConfig config, Path configPath, String key, boolean defaultValue)
    {
        Object raw = config.get(List.of(APOCALYPSE_CONFIG_SECTION, key));
        if (raw == null)
            return defaultValue;
        if (raw instanceof Boolean value)
            return value;

        FlansMod.log.warn("Ignoring invalid {} in {}: {}. Expected boolean.", key, configPath, raw);
        return defaultValue;
    }

    private static ApocalypseConfigSnapshot readConfig()
    {
        return new ApocalypseConfigSnapshot(
            ApocalypseConfigSnapshot.CURRENT_VERSION,

            APOCALYPSE_ENABLED.get(),
            APOCALYPSE_DIMENSION_ENABLED.get(),
            APOCALYPSE_PORTALS_ENABLED.get(),
            APOCALYPSE_OVERWORLD_PORTAL_GENERATION_ENABLED.get(),
            APOCALYPSE_WORLDGEN_ENABLED.get(),
            APOCALYPSE_MOBS_ENABLED.get(),
            APOCALYPSE_NUKE_DROPS_ENABLED.get(),
            APOCALYPSE_COUNTDOWN_LENGTH.get(),
            APOCALYPSE_SURVIVOR_RARITY.get(),
            APOCALYPSE_WANDERING_SURVIVOR_RARITY.get(),
            APOCALYPSE_SKELETON_RARITY.get(),
            APOCALYPSE_DEAD_TREE_RARITY.get(),
            APOCALYPSE_VEHICLE_RARITY.get(),
            APOCALYPSE_AIRPORT_RARITY.get(),
            APOCALYPSE_DYE_FACTORY_RARITY.get(),
            APOCALYPSE_LAB_RARITY.get(),
            APOCALYPSE_ABANDONED_PORTAL_APOC_RARITY.get(),
            APOCALYPSE_ABANDONED_PORTAL_OVERWORLD_RARITY.get(),
            APOCALYPSE_RETURN_RADIUS.get(),
            APOCALYPSE_SPAWN_RADIUS.get(),
            APOCALYPSE_RESPAWN_IN_APOCALYPSE.get(),
            APOCALYPSE_TELEPORT_OPTION.get(),
            APOCALYPSE_ACID_DAMAGE.get().floatValue(),
            APOCALYPSE_NUKE_EXPLOSION_POWER.get().floatValue(),
            APOCALYPSE_NUKE_VISUAL_TICKS.get()
        );
    }

    public static ApocalypseConfigSnapshot get()
    {
        ApocalypseConfigSnapshot override = serverOverride.get();
        return override != null ? override : instance.get();
    }

    public static void applyServerSnapshot(ApocalypseConfigSnapshot config)
    {
        serverOverride.set(config);
    }

    public static void clearServerOverride()
    {
        serverOverride.set(null);
    }

    public static void bake()
    {
        instance.set(readConfig());
    }

    public static boolean apocalypseDimensionEnabled()
    {
        ApocalypseConfigSnapshot config = get();
        return config == null
            ? APOCALYPSE_ENABLED.get() && APOCALYPSE_DIMENSION_ENABLED.get()
            : config.apocalypseEnabled() && config.apocalypseDimensionEnabled();
    }

    public static boolean apocalypsePortalsEnabled()
    {
        ApocalypseConfigSnapshot config = get();
        return config == null
            ? APOCALYPSE_ENABLED.get() && APOCALYPSE_PORTALS_ENABLED.get()
            : config.apocalypseEnabled() && config.apocalypsePortalsEnabled();
    }

    public static boolean apocalypseOverworldPortalGenerationEnabled()
    {
        ApocalypseConfigSnapshot config = get();
        return config == null
            ? APOCALYPSE_ENABLED.get() && APOCALYPSE_OVERWORLD_PORTAL_GENERATION_ENABLED.get()
            : config.apocalypseEnabled() && config.apocalypseOverworldPortalGenerationEnabled();
    }

    public static boolean apocalypseWorldgenEnabled()
    {
        ApocalypseConfigSnapshot config = get();
        return config == null
            ? APOCALYPSE_ENABLED.get() && APOCALYPSE_WORLDGEN_ENABLED.get()
            : config.apocalypseEnabled() && config.apocalypseWorldgenEnabled();
    }

    public static boolean apocalypseMobsEnabled()
    {
        ApocalypseConfigSnapshot config = get();
        return config == null
            ? APOCALYPSE_ENABLED.get() && APOCALYPSE_MOBS_ENABLED.get()
            : config.apocalypseEnabled() && config.apocalypseMobsEnabled();
    }

    public static boolean apocalypseNukeDropsEnabled()
    {
        ApocalypseConfigSnapshot config = get();
        return config == null
            ? APOCALYPSE_ENABLED.get() && APOCALYPSE_NUKE_DROPS_ENABLED.get()
            : config.apocalypseEnabled() && config.apocalypseNukeDropsEnabled();
    }

    public static int apocalypseCountdownLength()
    {
        ApocalypseConfigSnapshot config = get();
        return config == null ? APOCALYPSE_COUNTDOWN_LENGTH.get() : config.apocalypseCountdownLength();
    }

    public static int apocalypseSurvivorRarity()
    {
        ApocalypseConfigSnapshot config = get();
        return config == null ? APOCALYPSE_SURVIVOR_RARITY.get() : config.apocalypseSurvivorRarity();
    }

    public static int apocalypseWanderingSurvivorRarity()
    {
        ApocalypseConfigSnapshot config = get();
        return config == null ? APOCALYPSE_WANDERING_SURVIVOR_RARITY.get() : config.apocalypseWanderingSurvivorRarity();
    }

    public static int apocalypseSkeletonRarity()
    {
        ApocalypseConfigSnapshot config = get();
        return config == null ? APOCALYPSE_SKELETON_RARITY.get() : config.apocalypseSkeletonRarity();
    }

    public static int apocalypseDeadTreeRarity()
    {
        ApocalypseConfigSnapshot config = get();
        return config == null ? APOCALYPSE_DEAD_TREE_RARITY.get() : config.apocalypseDeadTreeRarity();
    }

    public static int apocalypseVehicleRarity()
    {
        ApocalypseConfigSnapshot config = get();
        return config == null ? APOCALYPSE_VEHICLE_RARITY.get() : config.apocalypseVehicleRarity();
    }

    public static int apocalypseAirportRarity()
    {
        ApocalypseConfigSnapshot config = get();
        return config == null ? APOCALYPSE_AIRPORT_RARITY.get() : config.apocalypseAirportRarity();
    }

    public static int apocalypseDyeFactoryRarity()
    {
        ApocalypseConfigSnapshot config = get();
        return config == null ? APOCALYPSE_DYE_FACTORY_RARITY.get() : config.apocalypseDyeFactoryRarity();
    }

    public static int apocalypseLabRarity()
    {
        ApocalypseConfigSnapshot config = get();
        return config == null ? APOCALYPSE_LAB_RARITY.get() : config.apocalypseLabRarity();
    }

    public static int apocalypseAbandonedPortalRarity()
    {
        ApocalypseConfigSnapshot config = get();
        return config == null ? APOCALYPSE_ABANDONED_PORTAL_APOC_RARITY.get() : config.apocalypseAbandonedPortalRarity();
    }

    public static int apocalypseAbandonedPortalOverworldRarity()
    {
        ApocalypseConfigSnapshot config = get();
        return config == null ? APOCALYPSE_ABANDONED_PORTAL_OVERWORLD_RARITY.get() : config.apocalypseAbandonedPortalOverworldRarity();
    }

    public static int apocalypseReturnRadius()
    {
        ApocalypseConfigSnapshot config = get();
        return config == null ? APOCALYPSE_RETURN_RADIUS.get() : config.apocalypseReturnRadius();
    }

    public static int apocalypseSpawnRadius()
    {
        ApocalypseConfigSnapshot config = get();
        return config == null ? APOCALYPSE_SPAWN_RADIUS.get() : config.apocalypseSpawnRadius();
    }

    public static boolean apocalypseRespawnInApocalypse()
    {
        ApocalypseConfigSnapshot config = get();
        return config == null
            ? APOCALYPSE_ENABLED.get() && APOCALYPSE_RESPAWN_IN_APOCALYPSE.get()
            : config.apocalypseEnabled() && config.apocalypseRespawnInApocalypse();
    }

    public static ApocalypseTeleportOption apocalypseTeleportOption()
    {
        ApocalypseConfigSnapshot config = get();
        return config == null ? APOCALYPSE_TELEPORT_OPTION.get() : config.apocalypseTeleportOption();
    }

    public static float apocalypseAcidDamage()
    {
        ApocalypseConfigSnapshot config = get();
        return config == null ? APOCALYPSE_ACID_DAMAGE.get().floatValue() : config.apocalypseAcidDamage();
    }

    public static float apocalypseNukeExplosionPower()
    {
        ApocalypseConfigSnapshot config = get();
        return config == null ? APOCALYPSE_NUKE_EXPLOSION_POWER.get().floatValue() : config.apocalypseNukeExplosionPower();
    }

    public static int apocalypseNukeVisualTicks()
    {
        ApocalypseConfigSnapshot config = get();
        return config == null ? APOCALYPSE_NUKE_VISUAL_TICKS.get() : config.apocalypseNukeVisualTicks();
    }

    public enum ApocalypseTeleportOption
    {
        PLACER_ONLY,
        DIM,
        DIM_OPT_IN,
        NEARBY,
        NEARBY_OPT_IN
    }

    private record EarlyApocalypseSettings(boolean apocalypseEnabled, boolean apocalypseDimensionEnabled) {}
}
