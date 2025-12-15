package com.flansmodultimate.config;

import com.flansmodultimate.client.input.EnumActionButton;
import com.flansmodultimate.client.input.EnumAimType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.common.ForgeConfigSpec;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ModClientConfigs
{
    public static final ForgeConfigSpec config;

    public static final ForgeConfigSpec.EnumValue<EnumActionButton> shootButton;
    public static final ForgeConfigSpec.EnumValue<EnumActionButton> shootButtonOffhand;
    public static final ForgeConfigSpec.EnumValue<EnumActionButton> aimButton;
    public static final ForgeConfigSpec.EnumValue<EnumAimType> aimType;
    public static final ForgeConfigSpec.BooleanValue showPackNameInItemDescriptions;

    public static final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

    static {
        builder.push("Client General Settings");
        showPackNameInItemDescriptions = builder
                .comment("Show pack names in item descriptions")
                .define("showPackNameInItemDescriptions", true);
        builder.pop();

        builder.push("Input Settings");
        shootButton = builder
                .comment("Primary shooting button")
                .defineEnum("shootButton", EnumActionButton.LEFT_MOUSE);
        shootButtonOffhand = builder
                .comment("Offhand shooting button")
                .defineEnum("shootButtonOffhand", EnumActionButton.RIGHT_MOUSE);
        aimButton = builder
                .comment("Aiming button")
                .defineEnum("aimButton", EnumActionButton.RIGHT_MOUSE);
        aimType = builder
                .comment("Aim behavior")
                .defineEnum("aimType", EnumAimType.TOGGLE);
        builder.pop();

        config = builder.build();
    }
}
