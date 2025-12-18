package com.flansmodultimate.config;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.PenetrableBlock;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.common.ForgeConfigSpec;

import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ModCommonConfigs
{
    public static final ForgeConfigSpec config;

    // General Settings
    public static final ForgeConfigSpec.BooleanValue addAllPaintjobsToCreative;

    // Gameplay Settings
    public static final ForgeConfigSpec.IntValue bonusRegenAmount;
    public static final ForgeConfigSpec.IntValue bonusRegenTickDelay;
    public static final ForgeConfigSpec.DoubleValue bonusRegenFoodLimit;

    // Damage Settings
    public static final ForgeConfigSpec.DoubleValue newDamageSystemReference;
    public static final ForgeConfigSpec.DoubleValue headshotDamageModifier;
    public static final ForgeConfigSpec.DoubleValue chestshotDamageModifier;
    public static final ForgeConfigSpec.DoubleValue armshotDamageModifier;
    public static final ForgeConfigSpec.DoubleValue legshotModifier;
    public static final ForgeConfigSpec.DoubleValue vehicleWheelSeatExplosionModifier;

    // Armor Settings
    public static final ForgeConfigSpec.IntValue breakableArmor;
    public static final ForgeConfigSpec.IntValue defaultArmorDurability;
    public static final ForgeConfigSpec.IntValue defaultArmorEnchantability;
    public static final ForgeConfigSpec.BooleanValue enableOldArmorRatioSystem;

    // Gun Settings
    public static final ForgeConfigSpec.DoubleValue gunDamageModifier;
    public static final ForgeConfigSpec.DoubleValue gunRecoilModifier;
    public static final ForgeConfigSpec.DoubleValue defaultADSSpreadMultiplier;
    public static final ForgeConfigSpec.DoubleValue defaultADSSpreadMultiplierShotgun;
    public static final ForgeConfigSpec.BooleanValue cancelReloadOnWeaponSwitch;
    public static final ForgeConfigSpec.BooleanValue combineAmmoOnReload;
    public static final ForgeConfigSpec.BooleanValue ammoToUpperInventoryOnReload;

    // Shootable Settings
    //TODO: idea - teams command to enable / disable friendly fire
    public static final ForgeConfigSpec.IntValue shootableDefaultRespawnTime;
    public static final ForgeConfigSpec.BooleanValue shootableProximityTriggerFriendlyFire;

    // Penetration System Settings
    public static final ForgeConfigSpec.BooleanValue useNewPenetrationSystem;
    public static final ForgeConfigSpec.BooleanValue enableBlockPenetration;
    public static final ForgeConfigSpec.DoubleValue blockPenetrationModifier;
    public static final ForgeConfigSpec.ConfigValue<List<?>> penetrableBlocksRawEntries;

    //TODO: sync common config between client and server if necessary

    private static final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

    static {
        builder.push("General Settings");
        addAllPaintjobsToCreative = builder
                .comment("Whether all paintjobs should appear in creative")
                .define("addAllPaintjobsToCreative", true);
        builder.pop();

        builder.push("General Settings");
        bonusRegenAmount = builder
                .comment("Allows you to increase health regen, best used alongside increased max health")
                .defineInRange("bonusRegenAmount", 0, 0, 1000);
        bonusRegenTickDelay = builder
                .comment("Number of ticks between heals, vanilla is 80")
                .defineInRange("bonusRegenTickDelay", 80, 0, 1000);
        bonusRegenFoodLimit = builder
                .comment("Amount of food required to activate this regen, vanilla is 18")
                .defineInRange("bonusRegenFoodLimit", 18.0, 0.0, 20.0);
        builder.pop();

        builder.push("Damage Settings");
        newDamageSystemReference = builder
                .comment("Reference for the new damage system using kinetic energy (when 'Mass' is set). This value should correspond approximate damage of a hand gun.")
                .defineInRange("newDamageSystemReference", 5.0, 0.0, 1000.0);
        headshotDamageModifier = builder
                .comment("All headshot damage will be modified by this amount")
                .defineInRange("headshotDamageModifier", 2.0, 0.0, 1000.0);
        chestshotDamageModifier = builder
                .comment("All chest shot damage will be modified by this amount")
                .defineInRange("headshotDamageModifier", 1.0, 0.0, 1000.0);
        armshotDamageModifier = builder
                .comment("All arm shot damage will be modified by this amount")
                .defineInRange("armshotDamageModifier", 0.7, 0.0, 1000.0);
        legshotModifier = builder
                .comment("All leg shot damage will be modified by this amount")
                .defineInRange("legshotModifier", 0.8, 0.0, 1000.0);
        vehicleWheelSeatExplosionModifier = builder
                .comment("Proportion of damage from an explosion when it has hit a wheel or seat")
                .defineInRange("vehicleWheelSeatExplosionModifier", 1.0, 0.0, 1.0);
        builder.pop();

        builder.push("Armor Settings");
        breakableArmor = builder
                .comment("0 = Non-breakable, 1 = All breakable, 2 = Refer to armor config")
                .defineInRange("breakableArmor", 2, 0, 2);
        defaultArmorDurability = builder
                .comment("Default durability if breakableArmor = 1")
                .defineInRange("defaultArmorDurability", 500, 1, Integer.MAX_VALUE);
        defaultArmorEnchantability = builder
                .comment("The quality of enchantments recieved for the same level of XP 0=UnEnchantable 25=Gold armor")
                .defineInRange("defaultArmorEnchantability", 0, 0, Integer.MAX_VALUE);
        enableOldArmorRatioSystem = builder
                .comment("If true, enable the old (1.7.10-like) ratio-based armor reduction system ('Defense' will be interpreted as damage reduction in percent instead of armor points)")
                .define("enableOldArmorRatioSystem", false);
        builder.pop();

        builder.push("Gun Settings");
        gunDamageModifier = builder
                .comment("All gun damage will be modified by this amount")
                .defineInRange("gunDamageModifier", 1.0, 0.0, 100.0);
        gunRecoilModifier = builder
                .comment("All gun recoil will be modified by this amount")
                .defineInRange("gunRecoilModifier", 1.0, 0.0, 100.0);
        defaultADSSpreadMultiplier = builder
                .comment("Modifier for spread when the player is aiming.")
                .defineInRange("defaultADSSpreadMultiplier", 0.2, 0.0, 10.0);
        defaultADSSpreadMultiplierShotgun = builder
                .comment("Modifier for spread when the player is aiming. (Multishot guns only).")
                .defineInRange("defaultADSSpreadMultiplierShotgun", 0.8, 0.0, 10.0);
        cancelReloadOnWeaponSwitch = builder
                .comment("This will cause the reload to be cancelled when switching to a different item")
                .define("cancelReloadOnWeaponSwitch", true);
        combineAmmoOnReload = builder
                .comment("Whether or not to combine unloaded ammo with damaged ammo in the inventory")
                .define("combineAmmoOnReload", true);
        ammoToUpperInventoryOnReload = builder
                .comment("Whether or not to first try to put unloaded ammo in the upper inventory instead of the hotbar")
                .define("ammoToUpperInventoryOnReload", false);
        builder.pop();

        builder.push("Shootable Settings");
        shootableProximityTriggerFriendlyFire = builder
                .comment("Whether proximity triggers can get triggered by allies and cause friendly fire")
                .define("shootableProximityTriggerFriendlyFire", false);
        shootableDefaultRespawnTime = builder
                .comment("Max despawn time in ticks (0.05s). After this time the entity will despawn quietly. 0 means no despawn time.")
                .defineInRange("shootableDefaultRespawnTime", 0, 0, Integer.MAX_VALUE);
        builder.pop();

        builder.push("Penetration System Settings");
        useNewPenetrationSystem = builder
                .comment("Whether to use new penetration system (only content packs designed to work with this system will work as intended with this on)")
                .define("useNewPenetrationSystem", false);
        enableBlockPenetration = builder
                .comment("This will enable the block penetration system to be used")
                .define("enableBlockPenetration", false);
        blockPenetrationModifier = builder
                .comment("Default block penetration modifier power. Individual bullets will override")
                .defineInRange("blockPenetrationModifier", 0.0, 0.0, 100.0);
        penetrableBlocksRawEntries = builder
                .comment("Per-block penetration data.",
                        "Format per line: <namespace:block>; <hardness>; <breaksOnPenetration>",
                        "Example: minecraft:stone; 3.0; false")
                .defineList("blocks", Collections.emptyList(), String.class::isInstance);
        builder.pop();

        config = builder.build();
    }

    @SuppressWarnings("unchecked")
    public static void bake()
    {
        for (String line : (List<String>) penetrableBlocksRawEntries.get())
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
