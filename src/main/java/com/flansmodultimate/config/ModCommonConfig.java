package com.flansmodultimate.config;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.guns.penetration.PenetrableBlock;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.common.ForgeConfigSpec;

import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ModCommonConfig
{
    public static final ForgeConfigSpec configSpec;

    private static final ForgeConfigSpec.BooleanValue ADD_ALL_PAINTJOBS_TO_CREATIVE;

    private static final ForgeConfigSpec.BooleanValue DISABLE_CROSSHAIR_FOR_GUNS;
    private static final ForgeConfigSpec.BooleanValue EXPLOSIONS_BREAK_BLOCKS;
    private static final ForgeConfigSpec.IntValue BONUS_REGEN_AMOUNT;
    private static final ForgeConfigSpec.IntValue BONUS_REGEN_TICK_DELAY;
    private static final ForgeConfigSpec.IntValue BONUS_REGEN_FOOD_LIMIT;

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
    private static final ForgeConfigSpec.DoubleValue GUN_DISPERSION_MODIFIER;
    private static final ForgeConfigSpec.DoubleValue GUN_ACCURACY_SPREAD_MODIFIER;
    private static final ForgeConfigSpec.DoubleValue DEFAULT_ADS_SPREAD_MULTIPLIER;
    private static final ForgeConfigSpec.DoubleValue DEFAULT_ADS_SPREAD_MULTIPLIER_SHOTGUN;
    private static final ForgeConfigSpec.BooleanValue CANCEL_RELOAD_ON_WEAPON_SWITCH;
    private static final ForgeConfigSpec.BooleanValue COMBINE_AMMO_ON_RELOAD;
    private static final ForgeConfigSpec.BooleanValue AMMO_TO_UPPER_INVENTORY_ON_RELOAD;
    private static final ForgeConfigSpec.BooleanValue REALISTIC_RECOIL;
    private static final ForgeConfigSpec.BooleanValue ENABLE_SIGHT_DOWNWARD_MOVEMENT;

    private static final ForgeConfigSpec.DoubleValue NEW_DAMAGE_SYSTEM_DAMAGE_REFERENCE;
    private static final ForgeConfigSpec.DoubleValue NEW_DAMAGE_SYSTEM_EXPLOSIVE_DAMAGE_REFERENCE;
    private static final ForgeConfigSpec.DoubleValue NEW_DAMAGE_SYSTEM_EXPLOSIVE_POWER_REFERENCE;
    private static final ForgeConfigSpec.DoubleValue NEW_DAMAGE_SYSTEM_EXPLOSIVE_RADIUS_REFERENCE;
    private static final ForgeConfigSpec.DoubleValue NEW_DAMAGE_SYSTEM_BLAST_TO_EXPLOSION_RADIUS_RATIO;
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
    private static final AtomicReference<CommonConfigSnapshot> instance = new AtomicReference<>();
    private static final AtomicReference<CommonConfigSnapshot> serverOverride = new AtomicReference<>();

    static
    {
        builder.push("General Settings");
        ADD_ALL_PAINTJOBS_TO_CREATIVE = builder
            .comment("Whether all paintjobs should appear in creative")
            .define("addAllPaintjobsToCreative", true);
        builder.pop();

        builder.push("Gameplay Settings");
        DISABLE_CROSSHAIR_FOR_GUNS = builder
            .comment("Disables crosshair for guns except melee weapons")
            .define("disableCrosshairForGuns", true);
        EXPLOSIONS_BREAK_BLOCKS = builder
            .comment("Whether explosions can break blocks")
            .define("explosionBreakBlocks", true);
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
        builder.pop();

        builder.push("Shootable Settings");
        NEW_DAMAGE_SYSTEM_DAMAGE_REFERENCE = builder
            .comment("Damage reference for the new damage system using kinetic energy (when 'Mass' is set). Is approximately equal to a the damage of a 9g bullet at 333 m/s")
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

        configSpec = builder.build();
    }

    private static CommonConfigSnapshot readConfig()
    {
        return new CommonConfigSnapshot
        (
            CommonConfigSnapshot.CURRENT_VERSION,

            ADD_ALL_PAINTJOBS_TO_CREATIVE.get(),

            DISABLE_CROSSHAIR_FOR_GUNS.get(),
            EXPLOSIONS_BREAK_BLOCKS.get(),
            BONUS_REGEN_AMOUNT.get(),
            BONUS_REGEN_TICK_DELAY.get(),
            BONUS_REGEN_FOOD_LIMIT.get(),

            HEADSHOT_DAMAGE_MODIFIER.get().floatValue(),
            CHESTSHOT_DAMAGE_MODIFIER.get().floatValue(),
            ARMSHOT_DAMAGE_MODIFIER.get().floatValue(),
            LEGSHOT_MODIFIER.get().floatValue(),
            VEHICLE_WHEEL_SEAT_EXPLOSION_MODIFIER.get().floatValue(),

            BREAKABLE_ARMOR.get(),
            DEFAULT_ARMOR_DURABILITY.get(),
            DEFAULT_ARMOR_ENCHANTABILITY.get(),
            ENABLE_OLD_ARMOR_RATIO_SYSTEM.get(),

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

            NEW_DAMAGE_SYSTEM_DAMAGE_REFERENCE.get().floatValue(),
            NEW_DAMAGE_SYSTEM_EXPLOSIVE_DAMAGE_REFERENCE.get().floatValue(),
            NEW_DAMAGE_SYSTEM_EXPLOSIVE_POWER_REFERENCE.get().floatValue(),
            NEW_DAMAGE_SYSTEM_EXPLOSIVE_RADIUS_REFERENCE.get().floatValue(),
            NEW_DAMAGE_SYSTEM_BLAST_TO_EXPLOSION_RADIUS_RATIO.get().floatValue(),
            SHOOTABLE_DEFAULT_RESPAWN_TIME.get(),
            SHOOTABLE_PROXIMITY_TRIGGER_FRIENDLY_FIRE.get(),

            SOUND_RANGE.get().floatValue(),
            GUN_FIRE_SOUND_RANGE.get().floatValue(),
            EXPLOSION_SOUND_RANGE.get().floatValue(),

            USE_NEW_PENETRATION_SYSTEM.get(),
            ENABLE_BLOCK_PENETRATION.get(),
            BLOCK_PENETRATION_MODIFIER.get(),

            List.copyOf(PENETRABLE_BLOCKS_RAW.get())
        );
    }

    public static CommonConfigSnapshot get()
    {
        CommonConfigSnapshot override = serverOverride.get();
        return override != null ? override : instance.get();
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
