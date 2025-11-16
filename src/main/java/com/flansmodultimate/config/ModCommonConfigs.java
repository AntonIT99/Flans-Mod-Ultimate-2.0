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
    public static final ForgeConfigSpec.IntValue breakableArmor;
    public static final ForgeConfigSpec.IntValue defaultArmorDurability;
    public static final ForgeConfigSpec.BooleanValue addAllPaintjobsToCreative;

    // Gameplay Settings
    public static final ForgeConfigSpec.BooleanValue grenadeProximityTriggerFriendlyFire;
    public static final ForgeConfigSpec.IntValue grenadeDefaultRespawnTime;
    public static final ForgeConfigSpec.DoubleValue vehicleWheelSeatExplosionModifier;

    static {
        BUILDER.push("General Settings");
        addAllPaintjobsToCreative = BUILDER
                .comment("Whether all paintjobs should appear in creative")
                .define("addAllPaintjobsToCreative", true);
        BUILDER.pop();

        BUILDER.push("Gameplay Settings");
        vehicleWheelSeatExplosionModifier = BUILDER
                .comment("Proportion of damage from an explosion when it has hit a wheel or seat.")
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

        BUILDER.push("Grenade Settings");
        //TODO: idea - teams command to enable / disable friendly fire
        grenadeProximityTriggerFriendlyFire = BUILDER
                .comment("Whether grenade proximity triggers can get triggered by the thrower and its allies")
                .define("grenadeProximityTriggerFriendlyFire", true);
        grenadeDefaultRespawnTime = BUILDER
                .comment("Max despawn time in ticks (0.05s) for grenade entities. After this time the grenade will despawn quietly. 0 means no despawn time.")
                .defineInRange("grenadeDefaultRespawnTime", 0, 0, Integer.MAX_VALUE);
        BUILDER.pop();

        CONFIG = BUILDER.build();
    }
}
