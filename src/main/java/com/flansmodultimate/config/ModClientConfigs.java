package com.flansmodultimate.config;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.common.ForgeConfigSpec;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ModClientConfigs
{
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec CONFIG;

    public static final ForgeConfigSpec.BooleanValue shootOnRightClick;
    public static final ForgeConfigSpec.BooleanValue showPackNameInItemDescriptions;

    static {
        BUILDER.push("Client Settings");

        shootOnRightClick = BUILDER
                .comment("If true, then shoot will be on right click")
                .define("shootOnRightClick", false);
        showPackNameInItemDescriptions = BUILDER
                .comment("Show pack names in item descriptions")
                .define("showPackNameInItemDescriptions", true);

        BUILDER.pop();
        CONFIG = BUILDER.build();
    }
}
