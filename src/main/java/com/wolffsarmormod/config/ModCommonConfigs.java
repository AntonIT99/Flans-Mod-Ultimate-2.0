package com.wolffsarmormod.config;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.common.ForgeConfigSpec;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ModCommonConfigs
{
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec CONFIG;

    public static final ForgeConfigSpec.IntValue breakableArmor;
    public static final ForgeConfigSpec.IntValue defaultArmorDurability;
    public static final ForgeConfigSpec.BooleanValue addAllPaintjobsToCreative;

    static {
        BUILDER.push("General Settings");

        breakableArmor = BUILDER
                .comment("0 = Non-breakable, 1 = All breakable, 2 = Refer to armor config")
                .defineInRange("breakableArmor", 2, 0, 2);
        defaultArmorDurability = BUILDER
                .comment("Default durability if breakableArmor = 1")
                .defineInRange("defaultArmorDurability", 500, 1, 10000);
        addAllPaintjobsToCreative = BUILDER
                .comment("Whether all paintjobs should appear in creative")
                .define("addAllPaintjobsToCreative", true);

        BUILDER.pop();
        CONFIG = BUILDER.build();
    }
}
