package com.flansmodultimate.config;

import com.flansmodultimate.client.input.EnumAimType;
import com.flansmodultimate.client.input.EnumMouseButton;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.common.ForgeConfigSpec;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ModClientConfigs
{
    public static final ForgeConfigSpec config;

    public static final ForgeConfigSpec.EnumValue<EnumMouseButton> shootButton;
    public static final ForgeConfigSpec.EnumValue<EnumMouseButton> shootButtonOffhand;
    public static final ForgeConfigSpec.EnumValue<EnumMouseButton> aimButton;
    public static final ForgeConfigSpec.EnumValue<EnumAimType> aimType;
    public static final ForgeConfigSpec.BooleanValue showPackNameInItemDescriptions;

    public static final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

    static {
        builder.push("Client General Settings");
        showPackNameInItemDescriptions = builder
                .comment("Show content pack names in item descriptions")
                .define("showPackNameInItemDescriptions", true);
        builder.pop();

        builder.push("Input Settings");
        shootButton = builder
                .comment("Primary shooting button")
                .defineEnum("shootButton", EnumMouseButton.MOUSE_LEFT);
        shootButtonOffhand = builder
                .comment("Offhand shooting button")
                .defineEnum("shootButtonOffhand", EnumMouseButton.MOUSE_RIGHT);
        aimButton = builder
                .comment("Aiming button")
                .defineEnum("aimButton", EnumMouseButton.MOUSE_RIGHT);
        aimType = builder
                .comment("Aim behavior")
                .defineEnum("aimType", EnumAimType.TOGGLE);
        builder.pop();

        config = builder.build();
    }
}
