package com.flansmodultimate.config;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.common.ForgeConfigSpec;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ModCommonConfigs
{
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec CONFIG;

    // General Settings

    public static final ForgeConfigSpec.BooleanValue addAllPaintjobsToCreative;

    // Gameplay Settings
    public static final ForgeConfigSpec.BooleanValue useNewPenetrationSystem;
    public static final ForgeConfigSpec.DoubleValue headshotDamageModifier;
    public static final ForgeConfigSpec.DoubleValue chestshotDamageModifier;
    public static final ForgeConfigSpec.DoubleValue armshotDamageModifier;
    public static final ForgeConfigSpec.DoubleValue legshotModifier;
    public static final ForgeConfigSpec.DoubleValue vehicleWheelSeatExplosionModifier;

    // Armor Settings
    public static final ForgeConfigSpec.IntValue breakableArmor;
    public static final ForgeConfigSpec.IntValue defaultArmorDurability;

    // Shootable Settings
    //TODO: idea - teams command to enable / disable friendly fire
    public static final ForgeConfigSpec.IntValue shootableDefaultRespawnTime;
    public static final ForgeConfigSpec.BooleanValue shootableProximityTriggerFriendlyFire;

    static {
        BUILDER.push("General Settings");
        addAllPaintjobsToCreative = BUILDER
                .comment("Whether all paintjobs should appear in creative")
                .define("addAllPaintjobsToCreative", true);
        BUILDER.pop();

        BUILDER.push("Gameplay Settings");
        useNewPenetrationSystem = BUILDER
                .comment("Whether to use new penetration system (only content packs designed to work with this system will work as intended with this on).")
                .define("useNewPenetrationSystem", false);
        vehicleWheelSeatExplosionModifier = BUILDER
                .comment("Proportion of damage from an explosion when it has hit a wheel or seat.")
                .defineInRange("vehicleWheelSeatExplosionModifier", 1.0, 0.0, 1.0);
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
        BUILDER.pop();

        BUILDER.push("Armor Settings");
        breakableArmor = BUILDER
                .comment("0 = Non-breakable, 1 = All breakable, 2 = Refer to armor config")
                .defineInRange("breakableArmor", 2, 0, 2);
        defaultArmorDurability = BUILDER
                .comment("Default durability if breakableArmor = 1")
                .defineInRange("defaultArmorDurability", 500, 1, Integer.MAX_VALUE);
        BUILDER.pop();

        BUILDER.push("Shootable Settings");
        shootableProximityTriggerFriendlyFire = BUILDER
                .comment("Whether proximity triggers can get triggered by allies and cause friendly fire")
                .define("shootableProximityTriggerFriendlyFire", false);
        shootableDefaultRespawnTime = BUILDER
                .comment("Max despawn time in ticks (0.05s). After this time the entity will despawn quietly. 0 means no despawn time.")
                .defineInRange("shootableDefaultRespawnTime", 0, 0, Integer.MAX_VALUE);
        BUILDER.pop();

        CONFIG = BUILDER.build();
    }
}
