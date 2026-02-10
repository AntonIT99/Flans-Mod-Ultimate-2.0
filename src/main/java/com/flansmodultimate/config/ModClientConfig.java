package com.flansmodultimate.config;

import com.flansmodultimate.client.input.EnumAimType;
import com.flansmodultimate.client.input.EnumMouseButton;
import com.flansmodultimate.client.model.ModelCache;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.concurrent.atomic.AtomicReference;

public final class ModClientConfig
{
    public static final ForgeConfigSpec configSpec;

    public final boolean showPackNameInItemDescriptions;
    public final boolean loadAllModelsInCache;

    public final EnumMouseButton shootButton;
    public final EnumMouseButton shootButtonOffhand;
    public final EnumMouseButton aimButton;
    public final EnumAimType aimType;

    public final boolean enableArms;
    public final boolean enableGunAnimationsInThirdPerson;
    public final boolean enableWeaponSprintStance;
    public final boolean enableRandomSprintStance;

    private static final ForgeConfigSpec.BooleanValue SHOW_PACK_NAME_IN_ITEM_DESCRIPTIONS;
    private static final ForgeConfigSpec.BooleanValue LOAD_ALL_MODELS_IN_CACHE;

    private static final ForgeConfigSpec.EnumValue<EnumMouseButton> SHOOT_BUTTON;
    private static final ForgeConfigSpec.EnumValue<EnumMouseButton> SHOOT_BUTTON_OFFHAND;
    private static final ForgeConfigSpec.EnumValue<EnumMouseButton> AIM_BUTTON;
    private static final ForgeConfigSpec.EnumValue<EnumAimType> AIM_TYPE;

    private static final ForgeConfigSpec.BooleanValue ENABLE_ARMS;
    private static final ForgeConfigSpec.BooleanValue ENABLE_GUN_ANIMATIONS_IN_THIRD_PERSON;
    private static final ForgeConfigSpec.BooleanValue ENABLE_WEAPON_SPRINT_STANCE;
    private static final ForgeConfigSpec.BooleanValue ENABLE_RANDOM_SPRINT_STANCE;

    private static final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
    private static final AtomicReference<ModClientConfig> instance = new AtomicReference<>();

    static
    {
        builder.push("General Settings");
        SHOW_PACK_NAME_IN_ITEM_DESCRIPTIONS = builder
                .comment("Show content pack names in item descriptions")
                .define("showPackNameInItemDescriptions", true);
        LOAD_ALL_MODELS_IN_CACHE = builder
                .comment("""
                    If true, loads and caches ALL models up-front during resource reload.
                    ⚠ Warning:
                    Do NOT enable this if you use many content packs / large amounts of content.
                    This can be very RAM-hungry and may significantly increase client reload times.
                    Recommended: leave this OFF and let models load on-demand.
                    """)
                .define("loadAllModelsInCache", false);
        builder.pop();

        builder.push("Input Settings");
        SHOOT_BUTTON = builder
                .comment("Main Hand Gun shooting / primary function button")
                .defineEnum("shootButton", EnumMouseButton.MOUSE_LEFT);
        SHOOT_BUTTON_OFFHAND = builder
                .comment("Offhand Gun shooting / primary function button")
                .defineEnum("shootButtonOffhand", EnumMouseButton.MOUSE_RIGHT);
        AIM_BUTTON = builder
                .comment("Aiming / secondary function button")
                .defineEnum("aimButton", EnumMouseButton.MOUSE_RIGHT);
        AIM_TYPE = builder
                .comment("Aim behavior")
                .defineEnum("aimType", EnumAimType.TOGGLE);
        builder.pop();

        builder.push("Gun Rendering Settings");
        ENABLE_ARMS = builder
            .comment("Enable arms rendering")
            .define("enableArms", true);
        ENABLE_GUN_ANIMATIONS_IN_THIRD_PERSON = builder
            .comment("This will display gun animations such as melee and reloading, not only in first person view but also in third person view including animations from other players")
            .define("enableGunAnimationsInThirdPerson", true);
        ENABLE_WEAPON_SPRINT_STANCE = builder
            .comment("This will move weapons to a lowered position when sprinting")
            .define("enableWeaponSprintStance", true);
        ENABLE_RANDOM_SPRINT_STANCE = builder
            .comment("This will randomly generate unique positions for each weapon using the weapon name as a seed")
            .define("enableRandomSprintStance", false);
        builder.pop();

        configSpec = builder.build();
    }

    private ModClientConfig()
    {
        showPackNameInItemDescriptions = SHOW_PACK_NAME_IN_ITEM_DESCRIPTIONS.get();
        loadAllModelsInCache = LOAD_ALL_MODELS_IN_CACHE.get();

        shootButton = SHOOT_BUTTON.get();
        shootButtonOffhand = SHOOT_BUTTON_OFFHAND.get();
        aimButton = AIM_BUTTON.get();
        aimType = AIM_TYPE.get();

        enableArms = ENABLE_ARMS.get();
        enableGunAnimationsInThirdPerson = ENABLE_GUN_ANIMATIONS_IN_THIRD_PERSON.get();
        enableWeaponSprintStance = ENABLE_WEAPON_SPRINT_STANCE.get();
        enableRandomSprintStance = ENABLE_RANDOM_SPRINT_STANCE.get();
    }

    public static ModClientConfig get()
    {
        return instance.get();
    }

    public static void bake()
    {
        ModClientConfig old = instance.get();
        instance.set(new ModClientConfig());

        if (old == null)
            return;

        if (old.loadAllModelsInCache != get().loadAllModelsInCache && get().loadAllModelsInCache)
            ModelCache.reload();
    }
}
