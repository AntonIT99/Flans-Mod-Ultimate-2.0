package com.flansmodultimate.config;

import com.flansmodultimate.client.input.EnumAimType;
import com.flansmodultimate.client.input.EnumMouseButton;
import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.common.types.InfoType;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.concurrent.atomic.AtomicReference;

public final class ModClientConfig
{
    public static final ForgeConfigSpec configSpec;

    public final boolean showPackNameInItemDescriptions;
    public final boolean loadAllModelsInCache;
    public final boolean showShootableDurabilityBars;
    public final boolean showArmorDamageAbsorptionBar;
    public final int bulletRenderDistance;
    public final int grenadeRenderDistance;
    public final int deployedGunRenderDistance;
    public final int aaGunRenderDistance;

    public final EnumMouseButton shootButton;
    public final EnumMouseButton shootButtonOffhand;
    public final EnumMouseButton aimButton;
    public final EnumAimType aimType;

    public final boolean enableArms;
    public final boolean enableGunAnimationsInThirdPerson;
    public final boolean enableWeaponSprintStance;
    public final boolean enableRandomSprintStance;

    public final boolean alwaysEnableArmorTranslucentRenderingByDefault;
    public final boolean alwaysEnableGunTranslucentRenderingByDefault;
    public final boolean alwaysEnableGrenadeTranslucentRenderingByDefault;
    public final boolean alwaysEnableBulletTranslucentRenderingByDefault;
    public final boolean alwaysEnableAttachmentTranslucentRenderingByDefault;
    public final boolean alwaysEnableAAGunTranslucentRenderingByDefault;

    public final boolean alwaysEnableArmorCullingByDefault;
    public final boolean alwaysEnableGunCullingByDefault;
    public final boolean alwaysEnableGrenadeCullingByDefault;
    public final boolean alwaysEnableBulletCullingByDefault;
    public final boolean alwaysEnableAttachmentCullingByDefault;
    public final boolean alwaysEnableAAGunCullingByDefault;

    private static final ForgeConfigSpec.BooleanValue SHOW_PACK_NAME_IN_ITEM_DESCRIPTIONS;
    private static final ForgeConfigSpec.BooleanValue LOAD_ALL_MODELS_IN_CACHE;
    private static final ForgeConfigSpec.BooleanValue SHOW_SHOOTABLE_DURABILITY_BARS;
    private static final ForgeConfigSpec.BooleanValue SHOW_ARMOR_DAMAGE_ABSORPTION_BAR;
    private static final ForgeConfigSpec.IntValue BULLET_RENDER_DISTANCE;
    private static final ForgeConfigSpec.IntValue GRENADE_RENDER_DISTANCE;
    private static final ForgeConfigSpec.IntValue DEPLOYED_GUN_RENDER_DISTANCE;
    private static final ForgeConfigSpec.IntValue AA_GUN_RENDER_DISTANCE;

    private static final ForgeConfigSpec.EnumValue<EnumMouseButton> SHOOT_BUTTON;
    private static final ForgeConfigSpec.EnumValue<EnumMouseButton> SHOOT_BUTTON_OFFHAND;
    private static final ForgeConfigSpec.EnumValue<EnumMouseButton> AIM_BUTTON;
    private static final ForgeConfigSpec.EnumValue<EnumAimType> AIM_TYPE;

    private static final ForgeConfigSpec.BooleanValue ENABLE_ARMS;
    private static final ForgeConfigSpec.BooleanValue ENABLE_GUN_ANIMATIONS_IN_THIRD_PERSON;
    private static final ForgeConfigSpec.BooleanValue ENABLE_WEAPON_SPRINT_STANCE;
    private static final ForgeConfigSpec.BooleanValue ENABLE_RANDOM_SPRINT_STANCE;

    private static final ForgeConfigSpec.BooleanValue ALWAYS_ENABLE_ARMOR_TRANSLUCENT_RENDERING_BY_DEFAULT;
    private static final ForgeConfigSpec.BooleanValue ALWAYS_ENABLE_GUN_TRANSLUCENT_RENDERING_BY_DEFAULT;
    private static final ForgeConfigSpec.BooleanValue ALWAYS_ENABLE_GRENADE_TRANSLUCENT_RENDERING_BY_DEFAULT;
    private static final ForgeConfigSpec.BooleanValue ALWAYS_ENABLE_BULLET_TRANSLUCENT_RENDERING_BY_DEFAULT;
    private static final ForgeConfigSpec.BooleanValue ALWAYS_ENABLE_ATTACHMENT_TRANSLUCENT_RENDERING_BY_DEFAULT;
    private static final ForgeConfigSpec.BooleanValue ALWAYS_ENABLE_AA_GUN_TRANSLUCENT_RENDERING_BY_DEFAULT;

    private static final ForgeConfigSpec.BooleanValue ALWAYS_ENABLE_ARMOR_CULLING_BY_DEFAULT;
    private static final ForgeConfigSpec.BooleanValue ALWAYS_ENABLE_GUN_CULLING_BY_DEFAULT;
    private static final ForgeConfigSpec.BooleanValue ALWAYS_ENABLE_GRENADE_CULLING_BY_DEFAULT;
    private static final ForgeConfigSpec.BooleanValue ALWAYS_ENABLE_BULLET_CULLING_BY_DEFAULT;
    private static final ForgeConfigSpec.BooleanValue ALWAYS_ENABLE_ATTACHMENT_CULLING_BY_DEFAULT;
    private static final ForgeConfigSpec.BooleanValue ALWAYS_ENABLE_AA_GUN_CULLING_BY_DEFAULT;

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
        SHOW_SHOOTABLE_DURABILITY_BARS = builder
                .comment("Show a durability-style bar for shootable items when their current round item is not full")
                .define("showShootableDurabilityBars", true);
        SHOW_ARMOR_DAMAGE_ABSORPTION_BAR = builder
                .comment("Show the armor-style HUD bar for legacy armor damage absorption")
                .define("showArmorDamageAbsorptionBar", true);
        builder.pop();

        builder.push("Entity Rendering Settings");
        BULLET_RENDER_DISTANCE = builder
            .comment("Client-side render distance in blocks for bullets.")
            .defineInRange("bulletRenderDistance", 128, 1, 4096);
        GRENADE_RENDER_DISTANCE = builder
            .comment("Client-side render distance in blocks for grenades.")
            .defineInRange("grenadeRenderDistance", 64, 1, 4096);
        DEPLOYED_GUN_RENDER_DISTANCE = builder
            .comment("Client-side render distance in blocks for deployed guns.")
            .defineInRange("deployedGunRenderDistance", 64, 1, 4096);
        AA_GUN_RENDER_DISTANCE = builder
            .comment("Client-side render distance in blocks for AA guns.")
            .defineInRange("aaGunRenderDistance", 128, 1, 4096);
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

        builder.push("Translucent Rendering Defaults");
        ALWAYS_ENABLE_ARMOR_TRANSLUCENT_RENDERING_BY_DEFAULT = defineTranslucentDefault("armors", "alwaysEnableArmorsTranslucentRenderingByDefault", true);
        ALWAYS_ENABLE_GUN_TRANSLUCENT_RENDERING_BY_DEFAULT = defineTranslucentDefault("guns", "alwaysEnableGunsTranslucentRenderingByDefault", true);
        ALWAYS_ENABLE_GRENADE_TRANSLUCENT_RENDERING_BY_DEFAULT = defineTranslucentDefault("grenades", "alwaysEnableGrenadesTranslucentRenderingByDefault", true);
        ALWAYS_ENABLE_BULLET_TRANSLUCENT_RENDERING_BY_DEFAULT = defineTranslucentDefault("bullets", "alwaysEnableBulletsTranslucentRenderingByDefault", true);
        ALWAYS_ENABLE_ATTACHMENT_TRANSLUCENT_RENDERING_BY_DEFAULT = defineTranslucentDefault("attachments", "alwaysEnableAttachmentsTranslucentRenderingByDefault", true);
        ALWAYS_ENABLE_AA_GUN_TRANSLUCENT_RENDERING_BY_DEFAULT = defineTranslucentDefault("aa-guns", "alwaysEnableAAGunsTranslucentRenderingByDefault", true);
        builder.pop();

        builder.push("Culling Defaults");
        ALWAYS_ENABLE_ARMOR_CULLING_BY_DEFAULT = defineCullingDefault("armors", "alwaysEnableArmorsCullingByDefault", false);
        ALWAYS_ENABLE_GUN_CULLING_BY_DEFAULT = defineCullingDefault("guns", "alwaysEnableGunsCullingByDefault", false);
        ALWAYS_ENABLE_GRENADE_CULLING_BY_DEFAULT = defineCullingDefault("grenades", "alwaysEnableGrenadesCullingByDefault", true);
        ALWAYS_ENABLE_BULLET_CULLING_BY_DEFAULT = defineCullingDefault("bullets", "alwaysEnableBulletsCullingByDefault", true);
        ALWAYS_ENABLE_ATTACHMENT_CULLING_BY_DEFAULT = defineCullingDefault("attachments", "alwaysEnableAttachmentsCullingByDefault", false);
        ALWAYS_ENABLE_AA_GUN_CULLING_BY_DEFAULT = defineCullingDefault("aa-guns", "alwaysEnableAAGunsCullingByDefault", true);
        builder.pop();

        configSpec = builder.build();
    }

    private ModClientConfig()
    {
        showPackNameInItemDescriptions = SHOW_PACK_NAME_IN_ITEM_DESCRIPTIONS.get();
        loadAllModelsInCache = LOAD_ALL_MODELS_IN_CACHE.get();
        showShootableDurabilityBars = SHOW_SHOOTABLE_DURABILITY_BARS.get();
        showArmorDamageAbsorptionBar = SHOW_ARMOR_DAMAGE_ABSORPTION_BAR.get();
        bulletRenderDistance = BULLET_RENDER_DISTANCE.get();
        grenadeRenderDistance = GRENADE_RENDER_DISTANCE.get();
        deployedGunRenderDistance = DEPLOYED_GUN_RENDER_DISTANCE.get();
        aaGunRenderDistance = AA_GUN_RENDER_DISTANCE.get();

        shootButton = SHOOT_BUTTON.get();
        shootButtonOffhand = SHOOT_BUTTON_OFFHAND.get();
        aimButton = AIM_BUTTON.get();
        aimType = AIM_TYPE.get();

        enableArms = ENABLE_ARMS.get();
        enableGunAnimationsInThirdPerson = ENABLE_GUN_ANIMATIONS_IN_THIRD_PERSON.get();
        enableWeaponSprintStance = ENABLE_WEAPON_SPRINT_STANCE.get();
        enableRandomSprintStance = ENABLE_RANDOM_SPRINT_STANCE.get();

        alwaysEnableArmorTranslucentRenderingByDefault = ALWAYS_ENABLE_ARMOR_TRANSLUCENT_RENDERING_BY_DEFAULT.get();
        alwaysEnableGunTranslucentRenderingByDefault = ALWAYS_ENABLE_GUN_TRANSLUCENT_RENDERING_BY_DEFAULT.get();
        alwaysEnableGrenadeTranslucentRenderingByDefault = ALWAYS_ENABLE_GRENADE_TRANSLUCENT_RENDERING_BY_DEFAULT.get();
        alwaysEnableBulletTranslucentRenderingByDefault = ALWAYS_ENABLE_BULLET_TRANSLUCENT_RENDERING_BY_DEFAULT.get();
        alwaysEnableAttachmentTranslucentRenderingByDefault = ALWAYS_ENABLE_ATTACHMENT_TRANSLUCENT_RENDERING_BY_DEFAULT.get();
        alwaysEnableAAGunTranslucentRenderingByDefault = ALWAYS_ENABLE_AA_GUN_TRANSLUCENT_RENDERING_BY_DEFAULT.get();

        alwaysEnableArmorCullingByDefault = ALWAYS_ENABLE_ARMOR_CULLING_BY_DEFAULT.get();
        alwaysEnableGunCullingByDefault = ALWAYS_ENABLE_GUN_CULLING_BY_DEFAULT.get();
        alwaysEnableGrenadeCullingByDefault = ALWAYS_ENABLE_GRENADE_CULLING_BY_DEFAULT.get();
        alwaysEnableBulletCullingByDefault = ALWAYS_ENABLE_BULLET_CULLING_BY_DEFAULT.get();
        alwaysEnableAttachmentCullingByDefault = ALWAYS_ENABLE_ATTACHMENT_CULLING_BY_DEFAULT.get();
        alwaysEnableAAGunCullingByDefault = ALWAYS_ENABLE_AA_GUN_CULLING_BY_DEFAULT.get();
    }

    public static ModClientConfig get()
    {
        return instance.get();
    }

    public boolean useTranslucentRendering(InfoType type)
    {
        if (type.getRenderOptions().translucentRendering())
            return true;

        return switch (type.getType())
        {
            case ARMOR -> alwaysEnableArmorTranslucentRenderingByDefault;
            case GUN -> alwaysEnableGunTranslucentRenderingByDefault;
            case GRENADE -> alwaysEnableGrenadeTranslucentRenderingByDefault;
            case BULLET -> alwaysEnableBulletTranslucentRenderingByDefault;
            case ATTACHMENT -> alwaysEnableAttachmentTranslucentRenderingByDefault;
            case AA_GUN -> alwaysEnableAAGunTranslucentRenderingByDefault;
            default -> false;
        };
    }

    public boolean useCullingRendering(InfoType type)
    {
        if (type.getRenderOptions().disableCulling())
            return false;

        return switch (type.getType())
        {
            case ARMOR -> alwaysEnableArmorCullingByDefault;
            case GUN -> alwaysEnableGunCullingByDefault;
            case GRENADE -> alwaysEnableGrenadeCullingByDefault;
            case BULLET -> alwaysEnableBulletCullingByDefault;
            case ATTACHMENT -> alwaysEnableAttachmentCullingByDefault;
            case AA_GUN -> alwaysEnableAAGunCullingByDefault;
            default -> true;
        };
    }

    private static ForgeConfigSpec.BooleanValue defineTranslucentDefault(String typeName, String configName, boolean defaultValue)
    {
        return builder
            .comment("Render " + typeName + " with translucent render types by default. Content files with TranslucentRendering true remain translucent regardless of this option.")
            .define(configName, defaultValue);
    }

    private static ForgeConfigSpec.BooleanValue defineCullingDefault(String typeName, String configName, boolean defaultValue)
    {
        return builder
            .comment("Render " + typeName + " with face culling by default. Disable this only for content that needs double-sided model faces.")
            .define(configName, defaultValue);
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
