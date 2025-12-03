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
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec CONFIG;

    // General Settings
    public static final ForgeConfigSpec.BooleanValue addAllPaintjobsToCreative;

    // Damage Settings
    public static final ForgeConfigSpec.DoubleValue headshotDamageModifier;
    public static final ForgeConfigSpec.DoubleValue chestshotDamageModifier;
    public static final ForgeConfigSpec.DoubleValue armshotDamageModifier;
    public static final ForgeConfigSpec.DoubleValue legshotModifier;
    public static final ForgeConfigSpec.DoubleValue vehicleWheelSeatExplosionModifier;

    // Armor Settings
    public static final ForgeConfigSpec.IntValue breakableArmor;
    public static final ForgeConfigSpec.IntValue defaultArmorDurability;

    // Gun Settings
    public static final ForgeConfigSpec.DoubleValue gunDamageModifier;
    public static final ForgeConfigSpec.DoubleValue gunRecoilModifier;

    // Shootable Settings
    //TODO: idea - teams command to enable / disable friendly fire
    public static final ForgeConfigSpec.IntValue shootableDefaultRespawnTime;
    public static final ForgeConfigSpec.BooleanValue shootableProximityTriggerFriendlyFire;

    // Penetration System Settings
    public static final ForgeConfigSpec.BooleanValue useNewPenetrationSystem;
    public static final ForgeConfigSpec.BooleanValue enableBlockPenetration;
    public static final ForgeConfigSpec.DoubleValue blockPenetrationModifier;
    public static final ForgeConfigSpec.ConfigValue<List<?>> penetrableBlocksRawEntries;

    static {
        BUILDER.push("General Settings");
        addAllPaintjobsToCreative = BUILDER
                .comment("Whether all paintjobs should appear in creative")
                .define("addAllPaintjobsToCreative", true);
        BUILDER.pop();

        BUILDER.push("Damage Settings");
        headshotDamageModifier = BUILDER
                .comment("All headshot damage will be modified by this amount")
                .defineInRange("headshotDamageModifier", 2.0, 0.0, 100.0);
        chestshotDamageModifier = BUILDER
                .comment("All chest shot damage will be modified by this amount")
                .defineInRange("headshotDamageModifier", 1.0, 0.0, 100.0);
        armshotDamageModifier = BUILDER
                .comment("All arm shot damage will be modified by this amount")
                .defineInRange("armshotDamageModifier", 0.7, 0.0, 100.0);
        legshotModifier = BUILDER
                .comment("All leg shot damage will be modified by this amount")
                .defineInRange("legshotModifier", 0.8, 0.0, 100.0);
        vehicleWheelSeatExplosionModifier = BUILDER
                .comment("Proportion of damage from an explosion when it has hit a wheel or seat")
                .defineInRange("vehicleWheelSeatExplosionModifier", 1.0, 0.0, 1.0);
        BUILDER.pop();

        BUILDER.push("Armor Settings");
        breakableArmor = BUILDER
                .comment("0 = Non-breakable, 1 = All breakable, 2 = Refer to armor config")
                .defineInRange("breakableArmor", 2, 0, 2);
        defaultArmorDurability = BUILDER
                .comment("Default durability if breakableArmor = 1")
                .defineInRange("defaultArmorDurability", 500, 1, Integer.MAX_VALUE);
        BUILDER.pop();

        BUILDER.push("Gun Settings");
        gunDamageModifier = BUILDER
                .comment("All gun damage will be modified by this amount")
                .defineInRange("gunDamageModifier", 1.0, 0.0, 100.0);
        gunRecoilModifier = BUILDER
                .comment("All gun recoil will be modified by this amount")
                .defineInRange("gunRecoilModifier", 1.0, 0.0, 100.0);
        BUILDER.pop();

        BUILDER.push("Shootable Settings");
        shootableProximityTriggerFriendlyFire = BUILDER
                .comment("Whether proximity triggers can get triggered by allies and cause friendly fire")
                .define("shootableProximityTriggerFriendlyFire", false);
        shootableDefaultRespawnTime = BUILDER
                .comment("Max despawn time in ticks (0.05s). After this time the entity will despawn quietly. 0 means no despawn time.")
                .defineInRange("shootableDefaultRespawnTime", 0, 0, Integer.MAX_VALUE);
        BUILDER.pop();

        BUILDER.push("Penetration System Settings");
        useNewPenetrationSystem = BUILDER
                .comment("Whether to use new penetration system (only content packs designed to work with this system will work as intended with this on)")
                .define("useNewPenetrationSystem", false);
        enableBlockPenetration = BUILDER
                .comment("This will enable the block penetration system to be used")
                .define("enableBlockPenetration", false);
        blockPenetrationModifier = BUILDER
                .comment("Default block penetration modifier power. Individual bullets will override")
                .defineInRange("blockPenetrationModifier", 0.0, 0.0, 100.0);
        penetrableBlocksRawEntries = BUILDER
                .comment("Per-block penetration data.",
                        "Format per line: <namespace:block>; <hardness>; <breaksOnPenetration>",
                        "Example: minecraft:stone; 3.0; false")
                .defineList("blocks", Collections.emptyList(), String.class::isInstance);
        BUILDER.pop();

        CONFIG = BUILDER.build();
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
