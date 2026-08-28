package com.flansmodultimate.config;

import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.digitalammo.DigitalAmmoSupplyHandler;
import com.flansmodultimate.common.guns.penetration.PenetrableBlock;
import com.flansmodultimate.common.types.EnumType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.loading.FMLPaths;

import net.minecraft.resources.ResourceLocation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ModCommonConfig
{
    public static final int DEFAULT_BULLET_TRACKING_RANGE = 128;
    public static final int DEFAULT_GRENADE_TRACKING_RANGE = 64;
    public static final int DEFAULT_DEPLOYED_GUN_TRACKING_RANGE = 64;
    public static final int DEFAULT_AA_GUN_TRACKING_RANGE = 128;

    private static final int MIN_ENTITY_TRACKING_RANGE = 1;
    private static final int MAX_ENTITY_TRACKING_RANGE = 4096;
    private static final String ENTITY_TRACKING_CONFIG_SECTION = "Entity Tracking Settings";

    public static final ForgeConfigSpec configSpec;

    private static final ForgeConfigSpec.BooleanValue ADD_ALL_PAINTJOBS_TO_CREATIVE;
    private static final ForgeConfigSpec.BooleanValue VALIDATE_CONTENT_REFERENCES_ON_WORLD_LOAD;
    private static final ForgeConfigSpec.ConfigValue<String> DEFAULT_VEHICLE_ENGINE;
    private static final ForgeConfigSpec.ConfigValue<String> DEFAULT_PLANE_ENGINE;
    private static final ForgeConfigSpec.ConfigValue<String> DEFAULT_MECHA_ENGINE;

    private static final ForgeConfigSpec.BooleanValue DISABLE_CROSSHAIR_FOR_GUNS;
    private static final ForgeConfigSpec.BooleanValue EXPLOSIONS_BREAK_BLOCKS;
    private static final ForgeConfigSpec.BooleanValue FLAN_EXPLOSIONS_DROP_BLOCKS;
    private static final ForgeConfigSpec.IntValue BONUS_REGEN_AMOUNT;
    private static final ForgeConfigSpec.IntValue BONUS_REGEN_TICK_DELAY;
    private static final ForgeConfigSpec.IntValue BONUS_REGEN_FOOD_LIMIT;
    private static final ForgeConfigSpec.IntValue BULLET_TRACKING_RANGE;
    private static final ForgeConfigSpec.IntValue GRENADE_TRACKING_RANGE;
    private static final ForgeConfigSpec.IntValue DEPLOYED_GUN_TRACKING_RANGE;
    private static final ForgeConfigSpec.IntValue AA_GUN_TRACKING_RANGE;

    private static final ForgeConfigSpec.DoubleValue HEADSHOT_DAMAGE_MODIFIER;
    private static final ForgeConfigSpec.DoubleValue CHESTSHOT_DAMAGE_MODIFIER;
    private static final ForgeConfigSpec.DoubleValue ARMSHOT_DAMAGE_MODIFIER;
    private static final ForgeConfigSpec.DoubleValue LEGSHOT_MODIFIER;
    private static final ForgeConfigSpec.DoubleValue VEHICLE_WHEEL_SEAT_EXPLOSION_MODIFIER;

    private static final ForgeConfigSpec.IntValue BREAKABLE_ARMOR;
    private static final ForgeConfigSpec.IntValue DEFAULT_ARMOR_DURABILITY;
    private static final ForgeConfigSpec.IntValue DEFAULT_ARMOR_ENCHANTABILITY;
    private static final ForgeConfigSpec.BooleanValue FORCE_DEFENSE_AS_MODERN_ARMOR;

    private static final ForgeConfigSpec.BooleanValue GUNS_ALWAYS_USABLE_BY_PLAYERS_IN_CREATIVE_MODE;
    private static final ForgeConfigSpec.BooleanValue FORCE_ALLOW_ALL_ATTACHMENTS;
    private static final ForgeConfigSpec.DoubleValue GUN_DAMAGE_MODIFIER;
    private static final ForgeConfigSpec.DoubleValue GUN_RECOIL_MODIFIER;
    private static final ForgeConfigSpec.DoubleValue GUN_DISPERSION_MODIFIER;
    private static final ForgeConfigSpec.DoubleValue GUN_ACCURACY_SPREAD_MODIFIER;
    private static final ForgeConfigSpec.DoubleValue DEFAULT_ADS_SPREAD_MULTIPLIER;
    private static final ForgeConfigSpec.DoubleValue DEFAULT_ADS_SPREAD_MULTIPLIER_SHOTGUN;
    private static final ForgeConfigSpec.BooleanValue CANCEL_RELOAD_ON_WEAPON_SWITCH;
    private static final ForgeConfigSpec.BooleanValue COMBINE_AMMO_ON_RELOAD;
    private static final ForgeConfigSpec.BooleanValue AMMO_TO_UPPER_INVENTORY_ON_RELOAD;
    private static final ForgeConfigSpec.BooleanValue REALISTIC_RECOIL;
    private static final ForgeConfigSpec.BooleanValue ENABLE_SIGHT_DOWNWARD_MOVEMENT;
    private static final ForgeConfigSpec.BooleanValue DISABLE_SPRINT_HIP_FIRE_BY_DEFAULT;
    private static final ForgeConfigSpec.BooleanValue MUZZLE_FLASH_PARTICLES_DEFAULT;

    private static final ForgeConfigSpec.BooleanValue SHOOTABLES_CAN_BREAK_GLASS;
    private static final ForgeConfigSpec.DoubleValue NEW_DAMAGE_SYSTEM_DAMAGE_REFERENCE;
    private static final ForgeConfigSpec.DoubleValue NEW_DAMAGE_SYSTEM_EXPLOSIVE_DAMAGE_REFERENCE;
    private static final ForgeConfigSpec.DoubleValue NEW_DAMAGE_SYSTEM_EXPLOSIVE_POWER_REFERENCE;
    private static final ForgeConfigSpec.DoubleValue NEW_DAMAGE_SYSTEM_EXPLOSIVE_RADIUS_REFERENCE;
    private static final ForgeConfigSpec.DoubleValue NEW_DAMAGE_SYSTEM_BLAST_TO_EXPLOSION_RADIUS_RATIO;
    private static final ForgeConfigSpec.IntValue SHOOTABLE_DEFAULT_RESPAWN_TIME;
    private static final ForgeConfigSpec.BooleanValue SHOOTABLE_PROXIMITY_TRIGGER_FRIENDLY_FIRE;
    private static final ForgeConfigSpec.DoubleValue LOCK_ON_RANGE;
    private static final ForgeConfigSpec.IntValue FLAK_PARTICLES_RANGE;
    private static final ForgeConfigSpec.DoubleValue ENTITY_HIT_PARTICLE_RANGE;
    private static final ForgeConfigSpec.DoubleValue BLOCK_HIT_PARTICLE_RANGE;
    private static final ForgeConfigSpec.IntValue SMOKE_PARTICLES_COUNT;
    private static final ForgeConfigSpec.DoubleValue SMOKE_PARTICLES_RANGE;

    private static final ForgeConfigSpec.DoubleValue SOUND_RANGE;
    private static final ForgeConfigSpec.DoubleValue GUN_FIRE_SOUND_RANGE;
    private static final ForgeConfigSpec.DoubleValue EXPLOSION_SOUND_RANGE;

    private static final ForgeConfigSpec.BooleanValue USE_NEW_PENETRATION_SYSTEM;
    private static final ForgeConfigSpec.BooleanValue ENABLE_BLOCK_PENETRATION;
    private static final ForgeConfigSpec.DoubleValue BLOCK_PENETRATION_MODIFIER;

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> PENETRABLE_BLOCKS_RAW;

    private static final ForgeConfigSpec.BooleanValue ENABLE_DIGITAL_AMMO_SYSTEM;
    private static final ForgeConfigSpec.IntValue DIGITAL_AMMO_DEFAULT_AMOUNT;
    private static final ForgeConfigSpec.IntValue DIGITAL_AMMO_MAX_AMOUNT;
    private static final ForgeConfigSpec.IntValue DIGITAL_AMMO_NUM_TYPES;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> DIGITAL_AMMO_SUPPLY_BLOCKS;
    private static final ForgeConfigSpec.IntValue DIGITAL_AMMO_SUPPLY_AMOUNT;

    private static final ForgeConfigSpec.BooleanValue ENCHANTMENT_MODULE_ENABLED;

    private static final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
    private static final AtomicReference<CommonConfigSnapshot> instance = new AtomicReference<>();
    private static final AtomicReference<CommonConfigSnapshot> serverOverride = new AtomicReference<>();
    private static final AtomicReference<EntityTrackingRanges> earlyEntityTrackingRanges = new AtomicReference<>();

    static
    {
        builder.push("General Settings");
        ADD_ALL_PAINTJOBS_TO_CREATIVE = builder
            .comment("Whether all paintjobs should appear in creative")
            .define("addAllPaintjobsToCreative", true);
        VALIDATE_CONTENT_REFERENCES_ON_WORLD_LOAD = builder
            .comment("Force selected content references to resolve once when a server world loads.",
                "Disabled by default because normal gameplay resolves these lazily.",
                "Enable this while developing content packs or modpacks to log unresolved recipes and Box outputs at startup.")
            .define("validateContentReferencesOnWorldLoad", false);
        DEFAULT_VEHICLE_ENGINE = builder
            .comment("Optional default vehicle engine item shortname or item ID. Empty uses automatic selection.")
            .define("defaultVehicleEngine", "");
        DEFAULT_PLANE_ENGINE = builder
            .comment("Optional default plane engine item shortname or item ID. Empty uses automatic selection.")
            .define("defaultPlaneEngine", "");
        DEFAULT_MECHA_ENGINE = builder
            .comment("Optional default mecha engine item shortname or item ID. Empty uses automatic selection.")
            .define("defaultMechaEngine", "");
        DISABLE_CROSSHAIR_FOR_GUNS = builder
            .comment("Disables crosshair for guns except melee weapons")
            .define("disableCrosshairForGuns", false);
        EXPLOSIONS_BREAK_BLOCKS = builder
            .comment("Whether explosions can break blocks")
            .define("explosionBreakBlocks", true);
        FLAN_EXPLOSIONS_DROP_BLOCKS = builder
            .comment("Whether blocks broken by Flan's Mod explosions should drop items")
            .define("flanExplosionsDropBlocks", true);
        BONUS_REGEN_AMOUNT = builder
            .comment("Allows you to increase health regen, best used alongside increased max health")
            .defineInRange("bonusRegenAmount", 0, 0, 1000);
        BONUS_REGEN_TICK_DELAY = builder
            .comment("Number of ticks between heals, vanilla is 80")
            .defineInRange("bonusRegenTickDelay", 80, 0, 1000);
        BONUS_REGEN_FOOD_LIMIT = builder
            .comment("Amount of food required to activate this regen, vanilla is 18")
            .defineInRange("bonusRegenFoodLimit", 18, 0, 20);
        builder.pop();

        builder.push("Entity Tracking Settings");
        BULLET_TRACKING_RANGE = builder
            .comment("Server-side tracking range in blocks for bullets. Requires restart because entity types are registered during startup.")
            .defineInRange("bulletTrackingRange", DEFAULT_BULLET_TRACKING_RANGE, MIN_ENTITY_TRACKING_RANGE, MAX_ENTITY_TRACKING_RANGE);
        GRENADE_TRACKING_RANGE = builder
            .comment("Server-side tracking range in blocks for grenades. Requires restart because entity types are registered during startup.")
            .defineInRange("grenadeTrackingRange", DEFAULT_GRENADE_TRACKING_RANGE, MIN_ENTITY_TRACKING_RANGE, MAX_ENTITY_TRACKING_RANGE);
        DEPLOYED_GUN_TRACKING_RANGE = builder
            .comment("Server-side tracking range in blocks for deployed guns. Requires restart because entity types are registered during startup.")
            .defineInRange("deployedGunTrackingRange", DEFAULT_DEPLOYED_GUN_TRACKING_RANGE, MIN_ENTITY_TRACKING_RANGE, MAX_ENTITY_TRACKING_RANGE);
        AA_GUN_TRACKING_RANGE = builder
            .comment("Server-side tracking range in blocks for AA guns. Requires restart because entity types are registered during startup.")
            .defineInRange("aaGunTrackingRange", DEFAULT_AA_GUN_TRACKING_RANGE, MIN_ENTITY_TRACKING_RANGE, MAX_ENTITY_TRACKING_RANGE);
        builder.pop();

        builder.push("Damage Settings");
        HEADSHOT_DAMAGE_MODIFIER = builder
            .comment("All headshot damage will be modified by this amount")
            .defineInRange("headshotDamageModifier", 2.0, 0.0, 1000.0);
        CHESTSHOT_DAMAGE_MODIFIER = builder
            .comment("All chest shot damage will be modified by this amount")
            .defineInRange("chestshotDamageModifier", 1.0, 0.0, 1000.0);
        ARMSHOT_DAMAGE_MODIFIER = builder
            .comment("All arm shot damage will be modified by this amount")
            .defineInRange("armshotDamageModifier", 0.7, 0.0, 1000.0);
        LEGSHOT_MODIFIER = builder
            .comment("All leg shot damage will be modified by this amount")
            .defineInRange("legshotModifier", 0.8, 0.0, 1000.0);
        VEHICLE_WHEEL_SEAT_EXPLOSION_MODIFIER = builder
            .comment("Proportion of damage from an explosion when it has hit a wheel or seat")
            .defineInRange("vehicleWheelSeatExplosionModifier", 1.0, 0.0, 1.0);
        builder.pop();

        builder.push("Armor Settings");
        BREAKABLE_ARMOR = builder
            .comment("0 = Non-breakable, 1 = All breakable, 2 = Refer to armor config")
            .defineInRange("breakableArmor", 2, 0, 2);
        DEFAULT_ARMOR_DURABILITY = builder
            .comment("Default durability if breakableArmor = 1")
            .defineInRange("defaultArmorDurability", 500, 1, Integer.MAX_VALUE);
        DEFAULT_ARMOR_ENCHANTABILITY = builder
            .comment("The quality of enchantments received for the same level of XP 0=UnEnchantable 25=Gold armor")
            .defineInRange("defaultArmorEnchantability", 0, 0, Integer.MAX_VALUE);
        FORCE_DEFENSE_AS_MODERN_ARMOR = builder
            .comment("Force Defence / Defense values to be interpreted as vanilla Minecraft armor points instead of legacy ratio-based armor reduction.",
                "DamageReduction and OtherDefence always remain legacy ratio-based values.")
            .define("forceDefenseAsModernArmor", false);
        builder.pop();

        builder.push("Gun Settings");
        GUNS_ALWAYS_USABLE_BY_PLAYERS_IN_CREATIVE_MODE = builder
            .comment("Guns will be always usable by players in creative mode, regardless of the parameter 'UsableByPlayers' in gun configs")
            .define("gunsAlwaysUsableByPlayersInCreativeMode", true);
        FORCE_ALLOW_ALL_ATTACHMENTS = builder
            .comment("Always allow all attachments on all guns, regardless of the 'AllowAllAttachments' value in gun configs.")
            .define("forceAllowAllAttachments", false);
        GUN_DAMAGE_MODIFIER = builder
            .comment("All gun damage will be modified by this amount")
            .defineInRange("gunDamageModifier", 1.0, 0.0, 100.0);
        GUN_RECOIL_MODIFIER = builder
            .comment("All gun recoil will be modified by this amount")
            .defineInRange("gunRecoilModifier", 1.0, 0.0, 100.0);
        GUN_DISPERSION_MODIFIER = builder
            .comment("All gun dispersion will be modified by this amount (only applies to 'Dispersion')")
            .defineInRange("gunDispersionModifier", 1.0, 0.0, 100.0);
        GUN_ACCURACY_SPREAD_MODIFIER = builder
            .comment("All gun accuracy / spread will be modified by this amount (applies to 'Accuracy' and 'Spread')")
            .defineInRange("gunAccuracySpreadModifier", 1.0, 0.0, 100.0);
        DEFAULT_ADS_SPREAD_MULTIPLIER = builder
            .comment("Modifier for spread when the player is aiming.")
            .defineInRange("defaultADSSpreadMultiplier", 0.2, 0.0, 10.0);
        DEFAULT_ADS_SPREAD_MULTIPLIER_SHOTGUN = builder
            .comment("Modifier for spread when the player is aiming. (Multishot guns only).")
            .defineInRange("defaultADSSpreadMultiplierShotgun", 0.8, 0.0, 10.0);
        CANCEL_RELOAD_ON_WEAPON_SWITCH = builder
            .comment("Cancel reload when switching to a different item")
            .define("cancelReloadOnWeaponSwitch", true);
        COMBINE_AMMO_ON_RELOAD = builder
            .comment("Combine unloaded ammo with damaged ammo in the inventory")
            .define("combineAmmoOnReload", true);
        AMMO_TO_UPPER_INVENTORY_ON_RELOAD = builder
            .comment("Try to put unloaded ammo in the upper inventory first")
            .define("ammoToUpperInventoryOnReload", false);
        REALISTIC_RECOIL = builder
            .comment("Changes recoil to be more realistic")
            .define("realisticRecoil", false);
        ENABLE_SIGHT_DOWNWARD_MOVEMENT = builder
            .comment("Enable downward movement of the sight after shot")
            .define("enableSightDownwardMovement", true);
        DISABLE_SPRINT_HIP_FIRE_BY_DEFAULT = builder
            .comment("Disallow guns from hip-firing while sprinting by default. Gun configs can override this with HipFireWhileSprinting.")
            .define("disableSprintHipFireByDefault", false);
        MUZZLE_FLASH_PARTICLES_DEFAULT = builder
            .comment("Enable muzzle flash particles by default. Gun configs can override this with ShowMuzzleFlashParticle.")
            .define("muzzleFlashParticlesDefault", false);
        builder.pop();

        builder.push("Shootable Settings");
        SHOOTABLES_CAN_BREAK_GLASS = builder
            .comment("Whether guns and grenades can break glass")
            .define("shootablesCanBreakGlass", true);
        NEW_DAMAGE_SYSTEM_DAMAGE_REFERENCE = builder
            .comment("Damage reference for the new damage system using kinetic energy (when 'Mass' is set). Is approximately equal to the damage of a 9g bullet at 333 m/s")
            .defineInRange("newDamageSystemDamageReference", 5.0, 0.0, 1000.0);
        NEW_DAMAGE_SYSTEM_EXPLOSIVE_DAMAGE_REFERENCE = builder
            .comment("Explosion damage reference for the new damage system using explosive mass as TNT equivalent (when 'ExplosiveMass' is set). Is equal to the damage of 1kg TNT")
            .defineInRange("newDamageSystemExplosiveDamageReference", 80.0, 0.0, 1000.0);
        NEW_DAMAGE_SYSTEM_EXPLOSIVE_POWER_REFERENCE = builder
            .comment("Explosion power reference for the new damage system using explosive mass as TNT equivalent (when 'ExplosiveMass' is set). Is equal to the power of 1kg TNT")
            .defineInRange("newDamageSystemExplosivePowerReference", 4.0, 0.0, 1000.0);
        NEW_DAMAGE_SYSTEM_EXPLOSIVE_RADIUS_REFERENCE = builder
            .comment("Explosion radius reference for the new damage system using explosive mass as TNT equivalent (when 'ExplosiveMass' is set). Is equal to the radius of 1kg TNT")
            .defineInRange("newDamageSystemExplosiveRadiusReference", 10.0, 0.0, 1000.0);
        NEW_DAMAGE_SYSTEM_BLAST_TO_EXPLOSION_RADIUS_RATIO = builder
            .comment("Ratio of the blast radius (damage area) relative to the explosion radius (block breaking and particles area)")
            .defineInRange("newDamageSystemBlastToExplosionRadiusRatio", 2.5, 0.0, 10.0);
        SHOOTABLE_PROXIMITY_TRIGGER_FRIENDLY_FIRE = builder
            .comment("Whether proximity triggers can get triggered by allies and cause friendly fire")
            .define("shootableProximityTriggerFriendlyFire", false);
        SHOOTABLE_DEFAULT_RESPAWN_TIME = builder
            .comment("Max despawn time in ticks (0.05s). 0 means no despawn time.")
            .defineInRange("shootableDefaultRespawnTime", 0, 0, Integer.MAX_VALUE);
        LOCK_ON_RANGE = builder
            .comment("Range in blocks used by lock-on missiles when searching for targets.")
            .defineInRange("lockOnRange", 128.0, 1.0, 4096.0);
        FLAK_PARTICLES_RANGE = builder
            .comment("Range in blocks for sending flak particle packets to clients.")
            .defineInRange("flakParticlesRange", 256, 1, 4096);
        ENTITY_HIT_PARTICLE_RANGE = builder
            .comment("Range in blocks for sending entity-hit particle packets to clients.")
            .defineInRange("entityHitParticleRange", 64.0, 1.0, 4096.0);
        BLOCK_HIT_PARTICLE_RANGE = builder
            .comment("Range in blocks for sending block-hit particle packets to clients.")
            .defineInRange("blockHitParticleRange", 64.0, 1.0, 4096.0);
        SMOKE_PARTICLES_COUNT = builder
            .comment("Number of smoke particles spawned per smoke packet.")
            .defineInRange("smokeParticlesCount", 50, 0, 10000);
        SMOKE_PARTICLES_RANGE = builder
            .comment("Range in blocks for sending smoke particle packets to clients.")
            .defineInRange("smokeParticlesRange", 32.0, 1.0, 4096.0);
        builder.pop();

        builder.push("Sound Settings");
        SOUND_RANGE = builder
            .comment("Range in blocks for general sound packets (also determines volume).")
            .defineInRange("soundRange", 48.0, 1.0, 4096.0);
        GUN_FIRE_SOUND_RANGE = builder
            .comment("Range in blocks for gun fire sound packets (also determines volume).")
            .defineInRange("gunFireSoundRange", 128.0, 1.0, 4096.0);
        EXPLOSION_SOUND_RANGE = builder
            .comment("Range in blocks for explosion sound packets (also determines volume).")
            .defineInRange("explosionSoundRange", 256.0, 1.0, 4096.0);
        builder.pop();

        builder.push("Penetration System Settings");
        USE_NEW_PENETRATION_SYSTEM = builder
            .comment("Whether to use new penetration system")
            .define("useNewPenetrationSystem", false);
        ENABLE_BLOCK_PENETRATION = builder
            .comment("Enable the block penetration system")
            .define("enableBlockPenetration", false);
        BLOCK_PENETRATION_MODIFIER = builder
            .comment("Default block penetration modifier power. Individual bullets will override")
            .defineInRange("blockPenetrationModifier", 0.0, 0.0, 100.0);
        PENETRABLE_BLOCKS_RAW = builder
            .comment("Per-block penetration data.",
                "Format per line: <namespace:block>; <hardness>; <breaksOnPenetration>",
                "Example: minecraft:stone; 3.0; false")
            .defineList("blocks", Collections.emptyList(), String.class::isInstance);
        builder.pop();

        builder.push("Digital Ammo System Settings");
        ENABLE_DIGITAL_AMMO_SYSTEM = builder
            .comment("Enable the digital ammo system. When enabled, players have a virtual ammo pool",
                "instead of needing physical magazines. Ammo is stored per-player and synced to client.")
            .define("enableDigitalAmmoSystem", false);
        DIGITAL_AMMO_DEFAULT_AMOUNT = builder
            .comment("Default amount of ammo for each type when a player first joins")
            .defineInRange("digitalAmmoDefaultAmount", 100, 0, Integer.MAX_VALUE);
        DIGITAL_AMMO_MAX_AMOUNT = builder
            .comment("Maximum amount of ammo allowed for each type",
                "Players cannot have more than this amount per ammo type")
            .defineInRange("digitalAmmoMaxAmount", 1000, 1, Integer.MAX_VALUE);
        DIGITAL_AMMO_NUM_TYPES = builder
            .comment("Number of different ammo types supported by the digital ammo system")
            .defineInRange("digitalAmmoNumTypes", 7, 1, 20);
        DIGITAL_AMMO_SUPPLY_BLOCKS = builder
            .comment("List of block IDs that act as supply blocks for digital ammo.",
                "When a player right-clicks these blocks, their digital ammo is replenished.",
                "Format: namespace:block (e.g., minecraft:iron_block)")
            .defineList("digitalAmmoSupplyBlocks", Collections.emptyList(), String.class::isInstance);
        DIGITAL_AMMO_SUPPLY_AMOUNT = builder
            .comment("Amount of ammo to restore for each type when using supply blocks")
            .defineInRange("digitalAmmoSupplyAmount", 100, 1, Integer.MAX_VALUE);
        builder.pop();

        builder.push("Enchantment Module");
        ENCHANTMENT_MODULE_ENABLED = builder
            .comment("Enable the Flan's Mod enchantment module (Steady, Nimble, Lumberjack, Duelist, Sharpshooter, Juggernaut)")
            .define("enchantmentModuleEnabled", true);
        builder.pop();

        configSpec = builder.build();
    }

    private static CommonConfigSnapshot readConfig()
    {
        return new CommonConfigSnapshot
        (
            CommonConfigSnapshot.CURRENT_VERSION,

            ADD_ALL_PAINTJOBS_TO_CREATIVE.get(),
            VALIDATE_CONTENT_REFERENCES_ON_WORLD_LOAD.get(),
            DEFAULT_VEHICLE_ENGINE.get(),
            DEFAULT_PLANE_ENGINE.get(),
            DEFAULT_MECHA_ENGINE.get(),

            DISABLE_CROSSHAIR_FOR_GUNS.get(),
            EXPLOSIONS_BREAK_BLOCKS.get(),
            FLAN_EXPLOSIONS_DROP_BLOCKS.get(),
            BONUS_REGEN_AMOUNT.get(),
            BONUS_REGEN_TICK_DELAY.get(),
            BONUS_REGEN_FOOD_LIMIT.get(),
            BULLET_TRACKING_RANGE.get(),
            GRENADE_TRACKING_RANGE.get(),
            DEPLOYED_GUN_TRACKING_RANGE.get(),
            AA_GUN_TRACKING_RANGE.get(),

            HEADSHOT_DAMAGE_MODIFIER.get().floatValue(),
            CHESTSHOT_DAMAGE_MODIFIER.get().floatValue(),
            ARMSHOT_DAMAGE_MODIFIER.get().floatValue(),
            LEGSHOT_MODIFIER.get().floatValue(),
            VEHICLE_WHEEL_SEAT_EXPLOSION_MODIFIER.get().floatValue(),

            BREAKABLE_ARMOR.get(),
            DEFAULT_ARMOR_DURABILITY.get(),
            DEFAULT_ARMOR_ENCHANTABILITY.get(),
            FORCE_DEFENSE_AS_MODERN_ARMOR.get(),

            GUNS_ALWAYS_USABLE_BY_PLAYERS_IN_CREATIVE_MODE.get(),
            FORCE_ALLOW_ALL_ATTACHMENTS.get(),
            GUN_DAMAGE_MODIFIER.get().floatValue(),
            GUN_RECOIL_MODIFIER.get().floatValue(),
            GUN_DISPERSION_MODIFIER.get().floatValue(),
            GUN_ACCURACY_SPREAD_MODIFIER.get().floatValue(),
            DEFAULT_ADS_SPREAD_MULTIPLIER.get().floatValue(),
            DEFAULT_ADS_SPREAD_MULTIPLIER_SHOTGUN.get().floatValue(),
            CANCEL_RELOAD_ON_WEAPON_SWITCH.get(),
            COMBINE_AMMO_ON_RELOAD.get(),
            AMMO_TO_UPPER_INVENTORY_ON_RELOAD.get(),
            REALISTIC_RECOIL.get(),
            ENABLE_SIGHT_DOWNWARD_MOVEMENT.get(),
            DISABLE_SPRINT_HIP_FIRE_BY_DEFAULT.get(),
            MUZZLE_FLASH_PARTICLES_DEFAULT.get(),

            SHOOTABLES_CAN_BREAK_GLASS.get(),
            NEW_DAMAGE_SYSTEM_DAMAGE_REFERENCE.get().floatValue(),
            NEW_DAMAGE_SYSTEM_EXPLOSIVE_DAMAGE_REFERENCE.get().floatValue(),
            NEW_DAMAGE_SYSTEM_EXPLOSIVE_POWER_REFERENCE.get().floatValue(),
            NEW_DAMAGE_SYSTEM_EXPLOSIVE_RADIUS_REFERENCE.get().floatValue(),
            NEW_DAMAGE_SYSTEM_BLAST_TO_EXPLOSION_RADIUS_RATIO.get().floatValue(),
            SHOOTABLE_DEFAULT_RESPAWN_TIME.get(),
            SHOOTABLE_PROXIMITY_TRIGGER_FRIENDLY_FIRE.get(),
            LOCK_ON_RANGE.get(),
            FLAK_PARTICLES_RANGE.get(),
            ENTITY_HIT_PARTICLE_RANGE.get(),
            BLOCK_HIT_PARTICLE_RANGE.get(),
            SMOKE_PARTICLES_COUNT.get(),
            SMOKE_PARTICLES_RANGE.get(),

            SOUND_RANGE.get().floatValue(),
            GUN_FIRE_SOUND_RANGE.get().floatValue(),
            EXPLOSION_SOUND_RANGE.get().floatValue(),

            USE_NEW_PENETRATION_SYSTEM.get(),
            ENABLE_BLOCK_PENETRATION.get(),
            BLOCK_PENETRATION_MODIFIER.get(),

            List.copyOf(PENETRABLE_BLOCKS_RAW.get()),

            ENABLE_DIGITAL_AMMO_SYSTEM.get(),
            DIGITAL_AMMO_DEFAULT_AMOUNT.get(),
            DIGITAL_AMMO_MAX_AMOUNT.get(),
            DIGITAL_AMMO_NUM_TYPES.get(),
            List.copyOf(DIGITAL_AMMO_SUPPLY_BLOCKS.get()),
            DIGITAL_AMMO_SUPPLY_AMOUNT.get(),

            ENCHANTMENT_MODULE_ENABLED.get()
        );
    }

    public static CommonConfigSnapshot get()
    {
        CommonConfigSnapshot override = serverOverride.get();
        return override != null ? override : instance.get();
    }

    public static boolean forceDefenseAsModernArmor()
    {
        CommonConfigSnapshot config = get();
        return config != null && config.forceDefenseAsModernArmor();
    }

    public static String defaultEngine(EnumType type)
    {
        CommonConfigSnapshot config = get();
        if (config == null || type == null)
            return "";
        return switch (type)
        {
            case VEHICLE -> config.defaultVehicleEngine();
            case PLANE -> config.defaultPlaneEngine();
            case MECHA -> config.defaultMechaEngine();
            default -> "";
        };
    }

    public static boolean forceAllowAllAttachments()
    {
        CommonConfigSnapshot config = get();
        return config != null && config.forceAllowAllAttachments();
    }

    public static int aaGunTrackingRange()
    {
        CommonConfigSnapshot config = get();
        return config == null ? aaGunRegistrationTrackingRange() : config.aaGunTrackingRange();
    }

    public static int bulletRegistrationTrackingRange()
    {
        return earlyEntityTrackingRanges().bullet();
    }

    public static int grenadeRegistrationTrackingRange()
    {
        return earlyEntityTrackingRanges().grenade();
    }

    public static int deployedGunRegistrationTrackingRange()
    {
        return earlyEntityTrackingRanges().deployedGun();
    }

    public static int aaGunRegistrationTrackingRange()
    {
        return earlyEntityTrackingRanges().aaGun();
    }

    private static EntityTrackingRanges earlyEntityTrackingRanges()
    {
        EntityTrackingRanges cached = earlyEntityTrackingRanges.get();
        if (cached != null)
            return cached;

        EntityTrackingRanges loaded = readEarlyEntityTrackingRanges();
        if (earlyEntityTrackingRanges.compareAndSet(null, loaded))
            return loaded;

        return earlyEntityTrackingRanges.get();
    }

    private static EntityTrackingRanges readEarlyEntityTrackingRanges()
    {
        EntityTrackingRanges defaults = new EntityTrackingRanges(
            DEFAULT_BULLET_TRACKING_RANGE,
            DEFAULT_GRENADE_TRACKING_RANGE,
            DEFAULT_DEPLOYED_GUN_TRACKING_RANGE,
            DEFAULT_AA_GUN_TRACKING_RANGE
        );
        Path configPath = FMLPaths.CONFIGDIR.get().resolve(FlansMod.MOD_ID + "-common.toml");
        if (!Files.isRegularFile(configPath))
            return defaults;

        try (FileConfig config = FileConfig.of(configPath, TomlFormat.instance()))
        {
            config.load();
            return new EntityTrackingRanges(
                readEarlyEntityTrackingRange(config, configPath, "bulletTrackingRange", DEFAULT_BULLET_TRACKING_RANGE),
                readEarlyEntityTrackingRange(config, configPath, "grenadeTrackingRange", DEFAULT_GRENADE_TRACKING_RANGE),
                readEarlyEntityTrackingRange(config, configPath, "deployedGunTrackingRange", DEFAULT_DEPLOYED_GUN_TRACKING_RANGE),
                readEarlyEntityTrackingRange(config, configPath, "aaGunTrackingRange", DEFAULT_AA_GUN_TRACKING_RANGE)
            );
        }
        catch (Exception e)
        {
            FlansMod.log.warn("Unable to read early entity tracking settings from {}. Using defaults: {}", configPath, e.toString());
            return defaults;
        }
    }

    private static int readEarlyEntityTrackingRange(FileConfig config, Path configPath, String key, int defaultValue)
    {
        Object raw = config.get(List.of(ENTITY_TRACKING_CONFIG_SECTION, key));
        if (raw == null)
            return defaultValue;

        if (raw instanceof Number number)
        {
            long value = number.longValue();
            if (value >= MIN_ENTITY_TRACKING_RANGE && value <= MAX_ENTITY_TRACKING_RANGE)
                return (int)value;
        }

        FlansMod.log.warn("Ignoring invalid {} in {}: {}. Expected integer in range [{}, {}].",
            key, configPath, raw, MIN_ENTITY_TRACKING_RANGE, MAX_ENTITY_TRACKING_RANGE);
        return defaultValue;
    }

    public static double lockOnRange()
    {
        CommonConfigSnapshot config = get();
        return config == null ? LOCK_ON_RANGE.get() : config.lockOnRange();
    }

    public static int flakParticlesRange()
    {
        CommonConfigSnapshot config = get();
        return config == null ? FLAK_PARTICLES_RANGE.get() : config.flakParticlesRange();
    }

    public static double entityHitParticleRange()
    {
        CommonConfigSnapshot config = get();
        return config == null ? ENTITY_HIT_PARTICLE_RANGE.get() : config.entityHitParticleRange();
    }

    public static double blockHitParticleRange()
    {
        CommonConfigSnapshot config = get();
        return config == null ? BLOCK_HIT_PARTICLE_RANGE.get() : config.blockHitParticleRange();
    }

    public static int smokeParticlesCount()
    {
        CommonConfigSnapshot config = get();
        return config == null ? SMOKE_PARTICLES_COUNT.get() : config.smokeParticlesCount();
    }

    public static double smokeParticlesRange()
    {
        CommonConfigSnapshot config = get();
        return config == null ? SMOKE_PARTICLES_RANGE.get() : config.smokeParticlesRange();
    }

    public static void applyServerSnapshot(CommonConfigSnapshot config)
    {
        serverOverride.set(config);
        rebuildPenetrableBlocks(config.penetrableBlocksLines());
    }

    public static void clearServerOverride()
    {
        serverOverride.set(null);
        CommonConfigSnapshot config = instance.get();
        if (config != null)
            rebuildPenetrableBlocks(config.penetrableBlocksLines());
    }

    public static void bake()
    {
        CommonConfigSnapshot config = readConfig();
        instance.set(config);
        rebuildPenetrableBlocks(config.penetrableBlocksLines());
        DigitalAmmoSupplyHandler.reloadSupplyBlocks();
    }

    private static void rebuildPenetrableBlocks(List<String> lines)
    {
        PenetrableBlock.clear();

        for (String line : lines)
        {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#"))
                continue;

            String[] parts = trimmed.split(";");
            if (parts.length != 3)
            {
                FlansMod.log.warn("Invalid config line: {}", line);
                continue;
            }

            String idStr = parts[0].trim();
            String hardnessStr = parts[1].trim();
            String breaksStr = parts[2].trim();

            try
            {
                ResourceLocation id = ResourceLocation.parse(idStr);
                double hardness = Double.parseDouble(hardnessStr);
                boolean breaks = Boolean.parseBoolean(breaksStr);
                PenetrableBlock.put(id, new PenetrableBlock(hardness, breaks));
            }
            catch (Exception e)
            {
                FlansMod.log.error("Failed to parse line '{}': {}", line, e.getMessage());
            }
        }
    }

    private record EntityTrackingRanges(int bullet, int grenade, int deployedGun, int aaGun)
    {
    }

}
