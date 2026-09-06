package com.flansmodultimate.config;

import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.digitalammo.DigitalAmmoSupplyHandler;
import com.flansmodultimate.common.driveables.physics.EnumVehicleCategory;
import com.flansmodultimate.common.guns.penetration.PenetrableBlock;
import com.flansmodultimate.common.types.EnumType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.loading.FMLPaths;
import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.ResourceLocation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ModCommonConfig
{
    public static final ForgeConfigSpec configSpec;

    /** Arcade lift scaling keeps fixed-wing takeoff runs practical in Minecraft worlds. */
    public static final double DEFAULT_REALISTIC_AIRCRAFT_REFERENCE_SPEED_SCALE = 0.25D;
    /** A throttle lever meters engine power, so the physical exponent is one. */
    public static final double DEFAULT_REALISTIC_AIRCRAFT_THROTTLE_RESPONSE = 3.0D;
    /** Per-class speed scales, at their neutral value unless an operator slows a class down. */
    public static final double DEFAULT_REALISTIC_PLANE_SPEED_SCALE = 1.0D;
    public static final double DEFAULT_REALISTIC_GROUND_VEHICLE_SPEED_SCALE = 1.0D;
    /** Absolute speed ceilings, high enough by default to be inert until an operator lowers them. */
    public static final double DEFAULT_MAX_PLANE_SPEED_KMH = 10000.0D;
    public static final double DEFAULT_MAX_VEHICLE_SPEED_KMH = 10000.0D;
    public static final double DEFAULT_REALISTIC_VEHICLE_HEALTH_SCALE = 5.0D;
    public static final double DEFAULT_MAX_ARMOR_IMPACT_ANGLE_DEG = 80.0D;
    /**
     * Default scaling coefficient of the kinetic penetration formula.
     * Yields the legacy {@code DEFAULT_PENETRATING_POWER} of 0.7 for an 8 g projectile at 360 m/s
     * (about 520 J, a 9x19mm service pistol round).
     */
    public static final double DEFAULT_KINETIC_PENETRATION_REFERENCE = 0.087D;
    public static final double DEFAULT_ARMORED_BLAST_RESISTANCE_KPA_PER_MM = 150.0D;
    public static final double DEFAULT_MINIMUM_BLAST_DISTANCE_METERS = 0.5D;
    /**
     * Hard ceiling on the CRATER radius, in blocks. This is the one radius that drives the
     * block-breaking loop, so it is the expensive one. Conventional ordnance stays far below
     * it - a 250 kg bomb craters about 21 blocks - and only nuclear-scale charges clamp.
     */
    public static final double DEFAULT_MAX_EXPLOSION_RADIUS = 128D;
    /**
     * Hard ceiling on the blast and fragmentation radii, in blocks. These only drive an entity
     * query rather than the block-breaking loop, so they are far cheaper than the crater radius
     * and can be allowed a much larger ceiling before anything needs clamping.
     */
    public static final double DEFAULT_MAX_BLAST_RADIUS = 512D;

    private static final double MIN_REALISTIC_AIRCRAFT_REFERENCE_SPEED_SCALE = 0.05D;
    private static final double MAX_REALISTIC_AIRCRAFT_REFERENCE_SPEED_SCALE = 1.0D;
    private static final double MIN_REALISTIC_CLASS_SPEED_SCALE = 0.1D;
    private static final double MAX_REALISTIC_CLASS_SPEED_SCALE = 1.0D;
    private static final double MIN_HARD_SPEED_CAP_KMH = 1.0D;
    private static final double MAX_HARD_SPEED_CAP_KMH = 100000.0D;
    private static final double MIN_REALISTIC_AIRCRAFT_THROTTLE_RESPONSE = 1.0D;
    private static final double MAX_REALISTIC_AIRCRAFT_THROTTLE_RESPONSE = 5.0D;

    private static final int DEFAULT_BULLET_TRACKING_RANGE = 128;
    private static final int DEFAULT_GRENADE_TRACKING_RANGE = 64;
    private static final int DEFAULT_DEPLOYED_GUN_TRACKING_RANGE = 64;
    private static final int DEFAULT_AA_GUN_TRACKING_RANGE = 128;

    private static final int MIN_ENTITY_TRACKING_RANGE = 1;
    private static final int MAX_ENTITY_TRACKING_RANGE = 4096;
    private static final String ENTITY_TRACKING_CONFIG_SECTION = "Entity Tracking Settings";

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
    private static final ForgeConfigSpec.BooleanValue DRIVEABLE_COLLISIONS_BREAK_BLOCKS;

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
    private static final ForgeConfigSpec.DoubleValue NEW_DAMAGE_SYSTEM_BLAST_RADIUS_REFERENCE;
    private static final ForgeConfigSpec.DoubleValue NEW_DAMAGE_SYSTEM_BLAST_FALLOFF_SHARPNESS;
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
    private static final ForgeConfigSpec.DoubleValue KINETIC_PENETRATION_REFERENCE;

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> PENETRABLE_BLOCKS_RAW;

    private static final ForgeConfigSpec.BooleanValue ENABLE_DIGITAL_AMMO_SYSTEM;
    private static final ForgeConfigSpec.IntValue DIGITAL_AMMO_DEFAULT_AMOUNT;
    private static final ForgeConfigSpec.IntValue DIGITAL_AMMO_MAX_AMOUNT;
    private static final ForgeConfigSpec.IntValue DIGITAL_AMMO_NUM_TYPES;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> DIGITAL_AMMO_SUPPLY_BLOCKS;
    private static final ForgeConfigSpec.IntValue DIGITAL_AMMO_SUPPLY_AMOUNT;

    private static final ForgeConfigSpec.BooleanValue FORCE_LEGACY_PLANE_PHYSICS;
    private static final ForgeConfigSpec.BooleanValue FORCE_LEGACY_VEHICLE_PHYSICS;
    private static final ForgeConfigSpec.DoubleValue REALISTIC_AIRCRAFT_REFERENCE_SPEED_SCALE;
    private static final ForgeConfigSpec.DoubleValue REALISTIC_AIRCRAFT_THROTTLE_RESPONSE;
    private static final ForgeConfigSpec.DoubleValue REALISTIC_PLANE_SPEED_SCALE;
    private static final ForgeConfigSpec.DoubleValue REALISTIC_GROUND_VEHICLE_SPEED_SCALE;
    private static final ForgeConfigSpec.DoubleValue MAX_PLANE_SPEED_KMH;
    private static final ForgeConfigSpec.DoubleValue MAX_VEHICLE_SPEED_KMH;
    private static final ForgeConfigSpec.DoubleValue REALISTIC_VEHICLE_HEALTH_SCALE;
    private static final ForgeConfigSpec.DoubleValue MAX_ARMOR_IMPACT_ANGLE_DEG;
    private static final ForgeConfigSpec.DoubleValue ARMORED_BLAST_RESISTANCE_KPA_PER_MM;
    private static final ForgeConfigSpec.DoubleValue MINIMUM_BLAST_DISTANCE_METERS;
    private static final ForgeConfigSpec.DoubleValue MAX_EXPLOSION_RADIUS;
    private static final ForgeConfigSpec.DoubleValue MAX_BLAST_RADIUS;

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
            .comment("Whether blocks broken by Flan's Mod explosions should drop items. Off by default: ordnance obliterates blocks rather than harvesting them.")
            .define("flanExplosionsDropBlocks", false);
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

        builder.push(ENTITY_TRACKING_CONFIG_SECTION);
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
        DRIVEABLE_COLLISIONS_BREAK_BLOCKS = builder
            .comment("Whether driveables may destroy blocks when collision points strike them.",
                "Disabled by default so aircraft crashes damage the aircraft without altering terrain.")
            .define("driveableCollisionsBreakBlocks", false);
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
            .comment("Damage reference for the kinetic damage system (when 'Mass' is set).",
                "A 9 g projectile retains the established baseline; heavier projectiles scale with mass^(2/3) so shell damage remains proportional to normalized vehicle health.",
                "The default gives approximately 5 damage to a 9 g bullet at 333 m/s.")
            .defineInRange("newDamageSystemDamageReference", 5.0, 0.0, 1000.0);
        NEW_DAMAGE_SYSTEM_EXPLOSIVE_DAMAGE_REFERENCE = builder
            .comment("Explosion damage reference for the new damage system using explosive mass as TNT equivalent (when 'ExplosiveMass' is set). Is equal to the damage of 1kg TNT")
            .defineInRange("newDamageSystemExplosiveDamageReference", 80.0, 0.0, 1000.0);
        NEW_DAMAGE_SYSTEM_EXPLOSIVE_POWER_REFERENCE = builder
            .comment("Explosion power reference for the new damage system using explosive mass as TNT equivalent (when 'ExplosiveMass' is set). Is equal to the power of 1kg TNT")
            .defineInRange("newDamageSystemExplosivePowerReference", 4.0, 0.0, 1000.0);
        NEW_DAMAGE_SYSTEM_EXPLOSIVE_RADIUS_REFERENCE = builder
            .comment("Explosion radius reference for the new damage system using explosive mass as TNT equivalent (when 'ExplosiveMass' is set). Is equal to the radius of 1kg TNT.",
                "This is the CRATERING radius: block breaking and explosion particles. Both radii follow the cube root of the charge (Hopkinson-Cranz scaling),",
                "so this constant is the scaled distance Z in m/kg^(1/3) at which blocks stop being destroyed.",
                "The default of 4.0 puts a 1 kg charge at the same 4-block radius as vanilla TNT, which is the reference players already know,",
                "and corresponds to roughly 100 kPa of peak overpressure - the point at which ordinary structures are destroyed outright.")
            .defineInRange("newDamageSystemExplosiveRadiusReference", 4.0, 0.0, 1000.0);
        NEW_DAMAGE_SYSTEM_BLAST_RADIUS_REFERENCE = builder
            .comment("Blast radius reference for the new damage system using explosive mass as TNT equivalent (when 'ExplosiveMass' is set). Is equal to the blast radius of 1kg TNT.",
                "This is the DAMAGE radius: how far the blast hurts entities and vehicles. It is derived from the charge directly, not from the cratering radius,",
                "so the two can be tuned independently. The default of 25.0 is the scaled distance at which overpressure falls to roughly 3 kPa.")
            .defineInRange("newDamageSystemBlastRadiusReference", 25.0, 0.0, 1000.0);
        NEW_DAMAGE_SYSTEM_BLAST_FALLOFF_SHARPNESS = builder
            .comment("Shape of the blast damage falloff inside the blast radius. Higher values concentrate damage near the centre; lower values spread it out.",
                "This only shapes the curve, it does not change any radius.")
            .defineInRange("newDamageSystemBlastFalloffSharpness", 2.5, 0.1, 10.0);
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
        KINETIC_PENETRATION_REFERENCE = builder
            .comment("Scaling coefficient of the kinetic penetration formula, used when a bullet uses the kinetic damage system (when 'Mass' is set).",
                "Penetrating power = this * cbrt(muzzle kinetic energy in joules), which supersedes 'Penetration' / 'PenetratingPower'.",
                "The default yields the legacy penetrating power of 0.7 for an 8 g projectile at 360 m/s (about 520 J, a 9x19mm service pistol round),",
                "about 1.1 for 7.62x39mm, 1.3 for 7.62x51mm NATO, 2.2 for .50 BMG and 12.6 for an 88mm armour-piercing shell.",
                "Raise it to make every kinetic round punch through more targets and blocks, lower it to make armour dominate.")
            .defineInRange("kineticPenetrationReference", DEFAULT_KINETIC_PENETRATION_REFERENCE, 0.0, 100.0);
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

        builder.push("Vehicle Physics Settings");
        FORCE_LEGACY_PLANE_PHYSICS = builder
            .comment("Force all planes to use their legacy movement physics, even when Real* aircraft parameters are present.",
                "This bypasses derived fixed-wing propulsion, lift, controls, manoeuvre drag, draft, movement scaling and speed caps.",
                "Helicopter, VTOL and six-DOF movement is already legacy, but the switch also bypasses global movement caps for them.")
            .define("forceLegacyPlanePhysics", false);
        FORCE_LEGACY_VEHICLE_PHYSICS = builder
            .comment("Force all ground and water vehicles to use their legacy movement physics, even when Real* vehicle parameters are present.",
                "This bypasses derived propulsion, turning loss, slope and reverse overrides, draft, movement scaling and speed caps.")
            .define("forceLegacyVehiclePhysics", false);
        REALISTIC_AIRCRAFT_REFERENCE_SPEED_SCALE = builder
            .comment("Scale applied to the wing-loading-derived reference airspeed of real-world fixed-wing aircraft.",
                "This changes the speed at which lift equals weight, so it affects takeoff, low-speed lift and stall-like behaviour together.",
                "0.5 halves the physically derived reference speed for shorter Minecraft runways; 1.0 keeps the physical result.",
                "Legacy aircraft, helicopters, VTOL and six-DOF craft are unaffected.")
            .defineInRange("realisticAircraftReferenceSpeedScale",
                DEFAULT_REALISTIC_AIRCRAFT_REFERENCE_SPEED_SCALE,
                MIN_REALISTIC_AIRCRAFT_REFERENCE_SPEED_SCALE,
                MAX_REALISTIC_AIRCRAFT_REFERENCE_SPEED_SCALE);
        REALISTIC_AIRCRAFT_THROTTLE_RESPONSE = builder
            .comment("Exponent applied to the throttle lever before it scales real-world fixed-wing thrust.",
                "Thrust is multiplied by throttle^n, so this changes how the lever maps onto power and speed.",
                "1.0 is the physically realistic value: the lever meters engine power directly,",
                "so half throttle really is half power. Because level-flight drag power rises with the cube of",
                "speed, half power still gives about 79% of top speed, which is how real aircraft behave.",
                "3.0 is the default and makes the lever linear in SPEED (half throttle = half top speed), which reads more",
                "naturally on a HUD but is not physical: half throttle then produces only an eighth of rated power.",
                "2.0 sits between the two. Values below 1.0 make the aircraft reach high speed even sooner.",
                "Legacy aircraft, helicopters, VTOL and six-DOF craft are unaffected.")
            .defineInRange("realisticAircraftThrottleResponse",
                DEFAULT_REALISTIC_AIRCRAFT_THROTTLE_RESPONSE,
                MIN_REALISTIC_AIRCRAFT_THROTTLE_RESPONSE,
                MAX_REALISTIC_AIRCRAFT_THROTTLE_RESPONSE);
        REALISTIC_PLANE_SPEED_SCALE = builder
            .comment("Scale applied to the real-world speeds of aircraft, declared with RealMaxSpeedKmh and RealMaxReverseSpeedKmh.",
                "The mod treats 1 block as 1 metre and 20 ticks as 1 second, so km/h becomes blocks per tick as kmh / 72.",
                "1.0 runs aircraft at their full real-world speed and is the default; 0.5 runs them at half speed.",
                "Only speeds are scaled. Mass, engine power, thrust, wing area, slope and draft are never scaled.",
                "Top speed, reverse speed, climb rate and the derived stall reference speed all follow it together,",
                "so a slowed aircraft still takes off and stalls at sensible fractions of its own top speed.",
                "Aircraft that declare no real-world parameters are unaffected.")
            .defineInRange("realisticPlaneSpeedScale", DEFAULT_REALISTIC_PLANE_SPEED_SCALE,
                MIN_REALISTIC_CLASS_SPEED_SCALE, MAX_REALISTIC_CLASS_SPEED_SCALE);
        REALISTIC_GROUND_VEHICLE_SPEED_SCALE = builder
            .comment("Scale applied to the real-world speeds of ground and water vehicles.",
                "As realisticPlaneSpeedScale, but for vehicles, and independent of it, so the two classes",
                "can be tuned against each other. 1.0 is no scaling and the default.",
                "Vehicles that declare no real-world parameters are unaffected.")
            .defineInRange("realisticGroundVehicleSpeedScale", DEFAULT_REALISTIC_GROUND_VEHICLE_SPEED_SCALE,
                MIN_REALISTIC_CLASS_SPEED_SCALE, MAX_REALISTIC_CLASS_SPEED_SCALE);
        MAX_PLANE_SPEED_KMH = builder
            .comment("Absolute speed ceiling for aircraft, in km/h. No plane may exceed it, whatever its pack",
                "or physics profile says. This is an enforced cap on the resulting velocity, not a target speed:",
                "an aircraft slower than the cap is completely unaffected by it.",
                "The default is high enough to be inert; lower it to rein in a pack that authors absurd speeds.",
                "Applies to every plane, including legacy ones, helicopters, VTOL and six-DOF craft.")
            .defineInRange("maxPlaneSpeedKmh", DEFAULT_MAX_PLANE_SPEED_KMH,
                MIN_HARD_SPEED_CAP_KMH, MAX_HARD_SPEED_CAP_KMH);
        MAX_VEHICLE_SPEED_KMH = builder
            .comment("Absolute speed ceiling for ground and water vehicles, in km/h. As maxPlaneSpeedKmh,",
                "but for vehicles, and independent of it.")
            .defineInRange("maxVehicleSpeedKmh", DEFAULT_MAX_VEHICLE_SPEED_KMH,
                MIN_HARD_SPEED_CAP_KMH, MAX_HARD_SPEED_CAP_KMH);
        builder.pop();

        builder.push("Vehicle Damage Settings");
        REALISTIC_VEHICLE_HEALTH_SCALE = builder
            .comment("Total HP scale for vehicles and AA guns opting into UseRealisticVehicleHealth.",
                "Total HP = scale * RealMassKg^(2/3). Legacy definitions are unaffected.")
            .defineInRange("realisticVehicleHealthScale", DEFAULT_REALISTIC_VEHICLE_HEALTH_SCALE, 0.01D, 1000D);
        MAX_ARMOR_IMPACT_ANGLE_DEG = builder
            .comment("Maximum impact angle used for effective armour thickness before grazing-angle capping.")
            .defineInRange("maxArmorImpactAngleDeg", DEFAULT_MAX_ARMOR_IMPACT_ANGLE_DEG, 0D, 89.9D);
        ARMORED_BLAST_RESISTANCE_KPA_PER_MM = builder
            .comment("Gameplay calibration: required blast pressure in kPa per millimetre of nominal armour.")
            .defineInRange("armoredBlastResistanceKPaPerMm", DEFAULT_ARMORED_BLAST_RESISTANCE_KPA_PER_MM,
                0.1D, 100000D);
        MINIMUM_BLAST_DISTANCE_METERS = builder
            .comment("Minimum physical distance used by armoured blast pressure calculations to avoid a singularity.")
            .defineInRange("minimumBlastDistanceMeters", DEFAULT_MINIMUM_BLAST_DISTANCE_METERS, 0.01D, 100D);
        MAX_EXPLOSION_RADIUS = builder
            .comment("Hard ceiling in blocks on the explosion, blast and fragmentation radii of any single detonation.",
                "Most ordnance is far below this: a 250 kg bomb craters about 13 blocks and damages out to about 157.",
                "The largest conventional charges do reach it, however - a GBU-43/B MOAB damages out to about 556 blocks - so this is not a nuclear-only clamp.",
                "It exists so that a nuclear or otherwise extreme charge cannot ask the server for a radius of tens of thousands of blocks.",
                "Raise it if your hardware can process a larger detonation, lower it if it cannot.")
            .defineInRange("maxExplosionRadius", DEFAULT_MAX_EXPLOSION_RADIUS, 1D, 4096D);
        MAX_BLAST_RADIUS = builder
            .comment("Hard ceiling in blocks on the blast and fragmentation radii of any single detonation.",
                "These drive an entity query rather than the block-breaking loop, so they are much cheaper than the crater radius and get a far higher ceiling.",
                "At the default only nuclear-scale charges clamp; every conventional charge keeps its full damage reach.")
            .defineInRange("maxBlastRadius", DEFAULT_MAX_BLAST_RADIUS, 1D, 8192D);
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
            DRIVEABLE_COLLISIONS_BREAK_BLOCKS.get(),

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
            NEW_DAMAGE_SYSTEM_BLAST_RADIUS_REFERENCE.get().floatValue(),
            NEW_DAMAGE_SYSTEM_BLAST_FALLOFF_SHARPNESS.get().floatValue(),
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
            KINETIC_PENETRATION_REFERENCE.get(),

            List.copyOf(PENETRABLE_BLOCKS_RAW.get()),

            ENABLE_DIGITAL_AMMO_SYSTEM.get(),
            DIGITAL_AMMO_DEFAULT_AMOUNT.get(),
            DIGITAL_AMMO_MAX_AMOUNT.get(),
            DIGITAL_AMMO_NUM_TYPES.get(),
            List.copyOf(DIGITAL_AMMO_SUPPLY_BLOCKS.get()),
            DIGITAL_AMMO_SUPPLY_AMOUNT.get(),

            FORCE_LEGACY_PLANE_PHYSICS.get(),
            FORCE_LEGACY_VEHICLE_PHYSICS.get(),
            REALISTIC_AIRCRAFT_REFERENCE_SPEED_SCALE.get(),
            REALISTIC_AIRCRAFT_THROTTLE_RESPONSE.get(),
            REALISTIC_PLANE_SPEED_SCALE.get(),
            REALISTIC_GROUND_VEHICLE_SPEED_SCALE.get(),
            MAX_PLANE_SPEED_KMH.get(),
            MAX_VEHICLE_SPEED_KMH.get(),
            REALISTIC_VEHICLE_HEALTH_SCALE.get(),
            MAX_ARMOR_IMPACT_ANGLE_DEG.get(),
            ARMORED_BLAST_RESISTANCE_KPA_PER_MM.get(),
            MINIMUM_BLAST_DISTANCE_METERS.get(),
            MAX_EXPLOSION_RADIUS.get(),
            MAX_BLAST_RADIUS.get(),

            ENCHANTMENT_MODULE_ENABLED.get()
        );
    }

    public static CommonConfigSnapshot get()
    {
        CommonConfigSnapshot override = serverOverride.get();
        return override != null ? override : instance.get();
    }

    /** Whether all aircraft movement must ignore the real-world profile and global movement tuning. */
    public static boolean forceLegacyPlanePhysics()
    {
        CommonConfigSnapshot config = get();
        return config != null && config.forceLegacyPlanePhysics();
    }

    /** Whether all ground and water vehicle movement must ignore the real-world profile and global movement tuning. */
    public static boolean forceLegacyVehiclePhysics()
    {
        CommonConfigSnapshot config = get();
        return config != null && config.forceLegacyVehiclePhysics();
    }

    /** Category-aware legacy movement override used by shared driveable collision and flotation code. */
    public static boolean forceLegacyMovement(@Nullable EnumVehicleCategory category)
    {
        return category == EnumVehicleCategory.AIRCRAFT ? forceLegacyPlanePhysics()
            : category == EnumVehicleCategory.GROUND && forceLegacyVehiclePhysics();
    }

    /** Server-authoritative arcade scale for derived fixed-wing lift and takeoff speed. */
    public static double realisticAircraftReferenceSpeedScale()
    {
        CommonConfigSnapshot config = get();
        return config == null ? DEFAULT_REALISTIC_AIRCRAFT_REFERENCE_SPEED_SCALE
            : config.realisticAircraftReferenceSpeedScale();
    }

    public static double realisticAircraftThrottleResponse()
    {
        CommonConfigSnapshot config = get();
        return config == null ? DEFAULT_REALISTIC_AIRCRAFT_THROTTLE_RESPONSE
            : config.realisticAircraftThrottleResponse();
    }

    /** Speed scale for real-world aircraft. */
    public static double realisticPlaneSpeedScale()
    {
        CommonConfigSnapshot config = get();
        return config == null ? DEFAULT_REALISTIC_PLANE_SPEED_SCALE : config.realisticPlaneSpeedScale();
    }

    /** Speed scale for real-world ground and water vehicles. */
    public static double realisticGroundVehicleSpeedScale()
    {
        CommonConfigSnapshot config = get();
        return config == null ? DEFAULT_REALISTIC_GROUND_VEHICLE_SPEED_SCALE
            : config.realisticGroundVehicleSpeedScale();
    }

    /**
     * The single place a real-world speed scale is read, selected by driveable
     * class. Physics, item tooltips and the debug command all go through here,
     * so top speed, reverse speed, climb rate, the derived stall reference speed
     * and the wheel look-ahead can never disagree about how fast a driveable is
     * meant to be, and a config that has not loaded yet falls back to the
     * documented default rather than to zero.
     *
     * <p>Mechas, which have no real-world profile of their own, are unscaled.
     */
    public static double realisticSpeedScale(@Nullable EnumVehicleCategory category)
    {
        if (category == EnumVehicleCategory.AIRCRAFT)
            return realisticPlaneSpeedScale();
        if (category == EnumVehicleCategory.GROUND)
            return realisticGroundVehicleSpeedScale();
        return 1D;
    }

    /** Enforced ceiling on aircraft speed in km/h, independent of any pack or physics profile. */
    public static double maxPlaneSpeedKmh()
    {
        CommonConfigSnapshot config = get();
        return config == null ? DEFAULT_MAX_PLANE_SPEED_KMH : config.maxPlaneSpeedKmh();
    }

    /** Enforced ceiling on ground and water vehicle speed in km/h. */
    public static double maxVehicleSpeedKmh()
    {
        CommonConfigSnapshot config = get();
        return config == null ? DEFAULT_MAX_VEHICLE_SPEED_KMH : config.maxVehicleSpeedKmh();
    }

    public static double realisticVehicleHealthScale()
    {
        CommonConfigSnapshot config = get();
        return config == null ? DEFAULT_REALISTIC_VEHICLE_HEALTH_SCALE : config.realisticVehicleHealthScale();
    }

    public static boolean driveableCollisionsBreakBlocks()
    {
        CommonConfigSnapshot config = get();
        return config != null && config.driveableCollisionsBreakBlocks();
    }

    public static double maxArmorImpactAngleDeg()
    {
        CommonConfigSnapshot config = get();
        return config == null ? DEFAULT_MAX_ARMOR_IMPACT_ANGLE_DEG : config.maxArmorImpactAngleDeg();
    }

    public static double armoredBlastResistanceKPaPerMm()
    {
        CommonConfigSnapshot config = get();
        return config == null ? DEFAULT_ARMORED_BLAST_RESISTANCE_KPA_PER_MM
            : config.armoredBlastResistanceKPaPerMm();
    }

    public static double minimumBlastDistanceMeters()
    {
        CommonConfigSnapshot config = get();
        return config == null ? DEFAULT_MINIMUM_BLAST_DISTANCE_METERS : config.minimumBlastDistanceMeters();
    }

    /** Hard ceiling in blocks on any explosion, blast or fragmentation radius. */
    public static double maxExplosionRadius()
    {
        CommonConfigSnapshot config = get();
        return config == null ? DEFAULT_MAX_EXPLOSION_RADIUS : config.maxExplosionRadius();
    }

    /** Hard ceiling in blocks on the blast and fragmentation radii. */
    public static double maxBlastRadius()
    {
        CommonConfigSnapshot config = get();
        return config == null ? DEFAULT_MAX_BLAST_RADIUS : config.maxBlastRadius();
    }

    public static double kineticPenetrationReference()
    {
        CommonConfigSnapshot config = get();
        return config == null ? DEFAULT_KINETIC_PENETRATION_REFERENCE : config.kineticPenetrationReference();
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
