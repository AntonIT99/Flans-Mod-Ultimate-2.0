package com.flansmodultimate.config;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.guns.penetration.PenetrableBlock;
import net.minecraftforge.common.ForgeConfigSpec;

import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class ModCommonConfig
{
    public static final ForgeConfigSpec configSpec;

    public final boolean addAllPaintjobsToCreative;

    public final boolean realisticRecoil;
    public final boolean enableSightDownwardMovement;
    public final int bonusRegenAmount;
    public final int bonusRegenTickDelay;
    public final double bonusRegenFoodLimit;

    public final double newDamageSystemReference;
    public final double headshotDamageModifier;
    public final double chestshotDamageModifier;
    public final double armshotDamageModifier;
    public final double legshotModifier;
    public final double vehicleWheelSeatExplosionModifier;

    public final int breakableArmor;
    public final int defaultArmorDurability;
    public final int defaultArmorEnchantability;
    public final boolean enableOldArmorRatioSystem;

    public final double gunDamageModifier;
    public final double gunRecoilModifier;
    public final double defaultADSSpreadMultiplier;
    public final double defaultADSSpreadMultiplierShotgun;
    public final boolean cancelReloadOnWeaponSwitch;
    public final boolean combineAmmoOnReload;
    public final boolean ammoToUpperInventoryOnReload;

    public final int shootableDefaultRespawnTime;
    public final boolean shootableProximityTriggerFriendlyFire;

    // Range for which sound packets are sent
    public final float soundRange;
    public final float gunFireSoundRange;
    public final float explosionSoundRange;

    public final boolean useNewPenetrationSystem;
    public final boolean enableBlockPenetration;
    public final double blockPenetrationModifier;

    public final List<String> penetrableBlocksLines;

    private static final ForgeConfigSpec.BooleanValue ADD_ALL_PAINTJOBS_TO_CREATIVE;

    private static final ForgeConfigSpec.BooleanValue REALISTIC_RECOIL;
    private static final ForgeConfigSpec.BooleanValue ENABLE_SIGHT_DOWNWARD_MOVEMENT;
    private static final ForgeConfigSpec.IntValue BONUS_REGEN_AMOUNT;
    private static final ForgeConfigSpec.IntValue BONUS_REGEN_TICK_DELAY;
    private static final ForgeConfigSpec.DoubleValue BONUS_REGEN_FOOD_LIMIT;

    private static final ForgeConfigSpec.DoubleValue NEW_DAMAGE_SYSTEM_REFERENCE;
    private static final ForgeConfigSpec.DoubleValue HEADSHOT_DAMAGE_MODIFIER;
    private static final ForgeConfigSpec.DoubleValue CHESTSHOT_DAMAGE_MODIFIER;
    private static final ForgeConfigSpec.DoubleValue ARMSHOT_DAMAGE_MODIFIER;
    private static final ForgeConfigSpec.DoubleValue LEGSHOT_MODIFIER;
    private static final ForgeConfigSpec.DoubleValue VEHICLE_WHEEL_SEAT_EXPLOSION_MODIFIER;

    private static final ForgeConfigSpec.IntValue BREAKABLE_ARMOR;
    private static final ForgeConfigSpec.IntValue DEFAULT_ARMOR_DURABILITY;
    private static final ForgeConfigSpec.IntValue DEFAULT_ARMOR_ENCHANTABILITY;
    private static final ForgeConfigSpec.BooleanValue ENABLE_OLD_ARMOR_RATIO_SYSTEM;

    private static final ForgeConfigSpec.DoubleValue GUN_DAMAGE_MODIFIER;
    private static final ForgeConfigSpec.DoubleValue GUN_RECOIL_MODIFIER;
    private static final ForgeConfigSpec.DoubleValue DEFAULT_ADS_SPREAD_MULTIPLIER;
    private static final ForgeConfigSpec.DoubleValue DEFAULT_ADS_SPREAD_MULTIPLIER_SHOTGUN;
    private static final ForgeConfigSpec.BooleanValue CANCEL_RELOAD_ON_WEAPON_SWITCH;
    private static final ForgeConfigSpec.BooleanValue COMBINE_AMMO_ON_RELOAD;
    private static final ForgeConfigSpec.BooleanValue AMMO_TO_UPPER_INVENTORY_ON_RELOAD;

    private static final ForgeConfigSpec.IntValue SHOOTABLE_DEFAULT_RESPAWN_TIME;
    private static final ForgeConfigSpec.BooleanValue SHOOTABLE_PROXIMITY_TRIGGER_FRIENDLY_FIRE;

    private static final ForgeConfigSpec.DoubleValue SOUND_RANGE;
    private static final ForgeConfigSpec.DoubleValue GUN_FIRE_SOUND_RANGE;
    private static final ForgeConfigSpec.DoubleValue EXPLOSION_SOUND_RANGE;

    private static final ForgeConfigSpec.BooleanValue USE_NEW_PENETRATION_SYSTEM;
    private static final ForgeConfigSpec.BooleanValue ENABLE_BLOCK_PENETRATION;
    private static final ForgeConfigSpec.DoubleValue BLOCK_PENETRATION_MODIFIER;

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> PENETRABLE_BLOCKS_RAW;

    private static final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
    private static final AtomicReference<ModCommonConfig> instance = new AtomicReference<>();
    private static final AtomicReference<ModCommonConfig> serverOverride = new AtomicReference<>();

    static
    {
        builder.push("General Settings");
        ADD_ALL_PAINTJOBS_TO_CREATIVE = builder
            .comment("Whether all paintjobs should appear in creative")
            .define("addAllPaintjobsToCreative", true);
        builder.pop();

        builder.push("Gameplay Settings");
        BONUS_REGEN_AMOUNT = builder
            .comment("Allows you to increase health regen, best used alongside increased max health")
            .defineInRange("bonusRegenAmount", 0, 0, 1000);
        BONUS_REGEN_TICK_DELAY = builder
            .comment("Number of ticks between heals, vanilla is 80")
            .defineInRange("bonusRegenTickDelay", 80, 0, 1000);
        BONUS_REGEN_FOOD_LIMIT = builder
            .comment("Amount of food required to activate this regen, vanilla is 18")
            .defineInRange("bonusRegenFoodLimit", 18.0, 0.0, 20.0);
        builder.pop();

        builder.push("Damage Settings");
        NEW_DAMAGE_SYSTEM_REFERENCE = builder
            .comment("Reference for the new damage system using kinetic energy (when 'Mass' is set).")
            .defineInRange("newDamageSystemReference", 5.0, 0.0, 1000.0);
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
        ENABLE_OLD_ARMOR_RATIO_SYSTEM = builder
            .comment("Enable the old ratio-based armor reduction system")
            .define("enableOldArmorRatioSystem", false);
        builder.pop();

        builder.push("Gun Settings");
        GUN_DAMAGE_MODIFIER = builder
            .comment("All gun damage will be modified by this amount")
            .defineInRange("gunDamageModifier", 1.0, 0.0, 100.0);
        GUN_RECOIL_MODIFIER = builder
            .comment("All gun recoil will be modified by this amount")
            .defineInRange("gunRecoilModifier", 1.0, 0.0, 100.0);
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
        builder.pop();

        builder.push("Shootable Settings");
        SHOOTABLE_PROXIMITY_TRIGGER_FRIENDLY_FIRE = builder
            .comment("Whether proximity triggers can get triggered by allies and cause friendly fire")
            .define("shootableProximityTriggerFriendlyFire", false);
        SHOOTABLE_DEFAULT_RESPAWN_TIME = builder
            .comment("Max despawn time in ticks (0.05s). 0 means no despawn time.")
            .defineInRange("shootableDefaultRespawnTime", 0, 0, Integer.MAX_VALUE);
        builder.pop();

        builder.push("Networking Settings");
        SOUND_RANGE = builder
            .comment("Range in blocks for general sound packets.")
            .defineInRange("soundRange", 32.0, 1.0, 4096.0);
        GUN_FIRE_SOUND_RANGE = builder
            .comment("Range in blocks for gun fire sound packets.")
            .defineInRange("gunFireSoundRange", 96.0, 1.0, 4096.0);
        EXPLOSION_SOUND_RANGE = builder
            .comment("Range in blocks for explosion sound packets.")
            .defineInRange("explosionSoundRange", 128.0, 1.0, 4096.0);
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

        configSpec = builder.build();
    }

    private ModCommonConfig()
    {
        addAllPaintjobsToCreative = ADD_ALL_PAINTJOBS_TO_CREATIVE.get();

        realisticRecoil = REALISTIC_RECOIL.get();
        enableSightDownwardMovement = ENABLE_SIGHT_DOWNWARD_MOVEMENT.get();
        bonusRegenAmount = BONUS_REGEN_AMOUNT.get();
        bonusRegenTickDelay = BONUS_REGEN_TICK_DELAY.get();
        bonusRegenFoodLimit = BONUS_REGEN_FOOD_LIMIT.get();

        newDamageSystemReference = NEW_DAMAGE_SYSTEM_REFERENCE.get();
        headshotDamageModifier = HEADSHOT_DAMAGE_MODIFIER.get();
        chestshotDamageModifier = CHESTSHOT_DAMAGE_MODIFIER.get();
        armshotDamageModifier = ARMSHOT_DAMAGE_MODIFIER.get();
        legshotModifier = LEGSHOT_MODIFIER.get();
        vehicleWheelSeatExplosionModifier = VEHICLE_WHEEL_SEAT_EXPLOSION_MODIFIER.get();

        breakableArmor = BREAKABLE_ARMOR.get();
        defaultArmorDurability = DEFAULT_ARMOR_DURABILITY.get();
        defaultArmorEnchantability = DEFAULT_ARMOR_ENCHANTABILITY.get();
        enableOldArmorRatioSystem = ENABLE_OLD_ARMOR_RATIO_SYSTEM.get();

        gunDamageModifier = GUN_DAMAGE_MODIFIER.get();
        gunRecoilModifier = GUN_RECOIL_MODIFIER.get();
        defaultADSSpreadMultiplier = DEFAULT_ADS_SPREAD_MULTIPLIER.get();
        defaultADSSpreadMultiplierShotgun = DEFAULT_ADS_SPREAD_MULTIPLIER_SHOTGUN.get();
        cancelReloadOnWeaponSwitch = CANCEL_RELOAD_ON_WEAPON_SWITCH.get();
        combineAmmoOnReload = COMBINE_AMMO_ON_RELOAD.get();
        ammoToUpperInventoryOnReload = AMMO_TO_UPPER_INVENTORY_ON_RELOAD.get();

        shootableDefaultRespawnTime = SHOOTABLE_DEFAULT_RESPAWN_TIME.get();
        shootableProximityTriggerFriendlyFire = SHOOTABLE_PROXIMITY_TRIGGER_FRIENDLY_FIRE.get();

        soundRange = (float) SOUND_RANGE.get().doubleValue();
        gunFireSoundRange = (float) GUN_FIRE_SOUND_RANGE.get().doubleValue();
        explosionSoundRange = (float) EXPLOSION_SOUND_RANGE.get().doubleValue();

        useNewPenetrationSystem = USE_NEW_PENETRATION_SYSTEM.get();
        enableBlockPenetration = ENABLE_BLOCK_PENETRATION.get();
        blockPenetrationModifier = BLOCK_PENETRATION_MODIFIER.get();

        penetrableBlocksLines = List.copyOf(PENETRABLE_BLOCKS_RAW.get());
    }

    private ModCommonConfig(CommonConfigSnapshot s)
    {
        addAllPaintjobsToCreative = s.addAllPaintjobsToCreative();

        realisticRecoil = s.realisticRecoil();
        enableSightDownwardMovement = s.enableSightDownwardMovement();
        bonusRegenAmount = s.bonusRegenAmount();
        bonusRegenTickDelay = s.bonusRegenTickDelay();
        bonusRegenFoodLimit = s.bonusRegenFoodLimit();

        newDamageSystemReference = s.newDamageSystemReference();
        headshotDamageModifier = s.headshotDamageModifier();
        chestshotDamageModifier = s.chestshotDamageModifier();
        armshotDamageModifier = s.armshotDamageModifier();
        legshotModifier = s.legshotModifier();
        vehicleWheelSeatExplosionModifier = s.vehicleWheelSeatExplosionModifier();

        breakableArmor = s.breakableArmor();
        defaultArmorDurability = s.defaultArmorDurability();
        defaultArmorEnchantability = s.defaultArmorEnchantability();
        enableOldArmorRatioSystem = s.enableOldArmorRatioSystem();

        gunDamageModifier = s.gunDamageModifier();
        gunRecoilModifier = s.gunRecoilModifier();
        defaultADSSpreadMultiplier = s.defaultADSSpreadMultiplier();
        defaultADSSpreadMultiplierShotgun = s.defaultADSSpreadMultiplierShotgun();
        cancelReloadOnWeaponSwitch = s.cancelReloadOnWeaponSwitch();
        combineAmmoOnReload = s.combineAmmoOnReload();
        ammoToUpperInventoryOnReload = s.ammoToUpperInventoryOnReload();

        shootableDefaultRespawnTime = s.shootableDefaultRespawnTime();
        shootableProximityTriggerFriendlyFire = s.shootableProximityTriggerFriendlyFire();

        soundRange = s.soundRange();
        gunFireSoundRange = s.gunFireSoundRange();
        explosionSoundRange = s.explosionSoundRange();

        useNewPenetrationSystem = s.useNewPenetrationSystem();
        enableBlockPenetration = s.enableBlockPenetration();
        blockPenetrationModifier = s.blockPenetrationModifier();

        penetrableBlocksLines = List.copyOf(s.penetrableBlocksLines());
    }

    public static ModCommonConfig get()
    {
        ModCommonConfig override = serverOverride.get();
        return override != null ? override : instance.get();
    }

    public static void applyServerSnapshot(CommonConfigSnapshot snap)
    {
        ModCommonConfig cfg = new ModCommonConfig(snap);
        serverOverride.set(cfg);
        rebuildPenetrableBlocks(cfg.penetrableBlocksLines);
    }

    public static void clearServerOverride()
    {
        serverOverride.set(null);
        ModCommonConfig baked = instance.get();
        if (baked != null)
            rebuildPenetrableBlocks(baked.penetrableBlocksLines);
    }

    public static void bake()
    {
        ModCommonConfig next = new ModCommonConfig();
        instance.set(next);
        rebuildPenetrableBlocks(next.penetrableBlocksLines);
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
}
