package com.flansmodultimate.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.flansmodultimate.FlansMod;
import com.flansmodultimate.client.AimPoseClient;
import com.flansmodultimate.client.ReloadPreferencesSync;
import com.flansmodultimate.client.UncensoredResources;
import com.flansmodultimate.client.input.EnumAimType;
import com.flansmodultimate.client.input.EnumMouseButton;
import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.client.render.entity.DriveableImpostorCache;
import com.flansmodultimate.client.render.gpu.GpuModelCache;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.platform.PlatformEnvironment;
import com.flansmodultimate.platform.PlatformPaths;
import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.BooleanUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public final class ModClientConfig
{
    private static final String CONFIG_FILE_NAME = FlansMod.MOD_ID + "-client.toml";
    private static final String UNCENSORED_CONTENT_CONFIG_PATH = "General Settings.enableUncensoredContent";
    public static final ForgeConfigSpec configSpec;

    private static volatile Boolean startupUncensoredContentEnabled;

    public final boolean showPackNameInItemDescriptions;
    public final boolean showDetailedItemDescriptions;
    public final boolean enableUncensoredContent;
    public final EnumOptionsButtonPlacement optionsButtonPlacement;
    public final boolean showFlansHud;
    public final boolean hideCrosshairForGuns;
    public final boolean loadAllModelsInCache;
    public final boolean searchModelsInOtherContentPacks;
    public final boolean preferBuiltInModelClasses;
    public final boolean showShootableDurabilityBars;
    public final boolean showArmorDamageAbsorptionBar;
    public final boolean showAmmoHud;
    public final EnumAmmoHudLayout ammoHudLayout;
    public final EnumSpeedUnit driveableSpeedUnit;
    public final EnumSpeedUnit driveableVerticalSpeedUnit;
    public final EnumHitMarkerStyle hitMarkerStyle;
    public final boolean hdHitMarker;
    public final boolean fancyHitMarker;
    public final boolean showHitMarker;
    public final float hitMarkerRed;
    public final float hitMarkerGreen;
    public final float hitMarkerBlue;
    public final float hitMarkerAlpha;
    public final boolean showFlashesWhenWounded;
    public final boolean vehicleScreenShake;
    public final boolean enablePlayerClassSkinOverrides;
    public final double driveableRenderDistanceMultiplier;
    public final double flanNpcRenderDistanceMultiplier;
    public final int bulletRenderDistance;
    public final int grenadeRenderDistance;
    public final int deployedGunRenderDistance;
    public final int aaGunRenderDistance;
    public final double minimumDriveablePartPixelSize;
    public final boolean enableDriveableLod;
    public final boolean enableGpuModelCache;
    public final int gpuModelCacheMegabytes;
    public final double maximumDriveableLodPartPixelSize;
    public final double driveableLodDetailMultiplier;
    public final double groundVehicleLodDistanceFactor;
    public final int driveableImpostorQualityMultiplier;
    public final double driveableTrackLinkLodPixelSize;
    public final double driveableTrackLinkGroupingPixelSize;
    public final double driveableImpostorPixelSize;
    public final int driveableImpostorMinimumDistance;
    public final int driveableImpostorMaximumDistance;
    public final int driveableImpostorResolution;
    public final int driveableImpostorYawAngles;
    public final int driveableImpostorCacheEntries;
    public final double particleDistanceScale;
    public final double fullParticleDensityShare;
    public final double distantParticleDensity;
    public final int maxFlansParticlesPerTick;
    public final int maxFragSparks;
    public final double fragSparkMultiplier;

    public final EnumMouseButton shootButton;
    public final EnumMouseButton shootButtonOffhand;
    public final EnumMouseButton aimButton;
    public final EnumAimType aimType;
    public final EnumGunBlockInteraction gunBlockInteraction;
    public final boolean predictDriveableMovement;

    public final boolean combineAmmoOnReload;
    public final boolean ammoToUpperInventoryOnReload;

    public final boolean enableArms;
    public final EnumAimPose aimPose;
    public final boolean enableGunAnimationsInThirdPerson;
    public final boolean enableWeaponSprintStance;
    public final boolean enableRandomSprintStance;
    public final boolean showCasingEjections;
    public final EnumMuzzleFlashStyle muzzleFlashStyle;

    public final boolean enableFastTranslucentRendering;
    public final boolean alwaysEnableArmorTranslucentRenderingByDefault;
    public final boolean alwaysEnableGunTranslucentRenderingByDefault;
    public final boolean alwaysEnableGrenadeTranslucentRenderingByDefault;
    public final boolean alwaysEnableBulletTranslucentRenderingByDefault;
    public final boolean alwaysEnableAttachmentTranslucentRenderingByDefault;
    public final boolean alwaysEnableAAGunTranslucentRenderingByDefault;
    public final boolean alwaysEnableVehicleTranslucentRenderingByDefault;
    public final boolean alwaysEnablePlaneTranslucentRenderingByDefault;
    public final boolean alwaysEnableMechaTranslucentRenderingByDefault;

    public final boolean alwaysEnableArmorCullingByDefault;
    public final boolean alwaysEnableGunCullingByDefault;
    public final boolean alwaysEnableGrenadeCullingByDefault;
    public final boolean alwaysEnableBulletCullingByDefault;
    public final boolean alwaysEnableAttachmentCullingByDefault;
    public final boolean alwaysEnableAAGunCullingByDefault;
    public final boolean alwaysEnableVehicleCullingByDefault;
    public final boolean alwaysEnablePlaneCullingByDefault;
    public final boolean alwaysEnableMechaCullingByDefault;

    public final boolean distantHorizonsIntegration;
    public final boolean distantHorizonsRangefinder;
    public final boolean distantHorizonsContacts;
    public final boolean distantHorizonsExplosions;
    public final boolean distantHorizonsDriveableRendering;

    private static final Supplier<Boolean> SHOW_PACK_NAME_IN_ITEM_DESCRIPTIONS;
    private static final Supplier<Boolean> SHOW_DETAILED_ITEM_DESCRIPTIONS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_UNCENSORED_CONTENT;
    public static final ForgeConfigSpec.EnumValue<EnumOptionsButtonPlacement> OPTIONS_BUTTON_PLACEMENT;
    public static final ForgeConfigSpec.BooleanValue SHOW_FLANS_HUD;
    public static final ForgeConfigSpec.BooleanValue HIDE_CROSSHAIR_FOR_GUNS;
    private static final Supplier<Boolean> LOAD_ALL_MODELS_IN_CACHE;
    private static final Supplier<Boolean> SEARCH_MODELS_IN_OTHER_CONTENT_PACKS;
    private static final Supplier<Boolean> PREFER_BUILT_IN_MODEL_CLASSES;
    public static final ForgeConfigSpec.BooleanValue SHOW_SHOOTABLE_DURABILITY_BARS;
    public static final ForgeConfigSpec.BooleanValue SHOW_ARMOR_DAMAGE_ABSORPTION_BAR;
    public static final ForgeConfigSpec.BooleanValue SHOW_AMMO_HUD;
    public static final ForgeConfigSpec.EnumValue<EnumAmmoHudLayout> AMMO_HUD_LAYOUT;
    public static final ForgeConfigSpec.EnumValue<EnumSpeedUnit> DRIVEABLE_SPEED_UNIT;
    public static final ForgeConfigSpec.EnumValue<EnumSpeedUnit> DRIVEABLE_VERTICAL_SPEED_UNIT;
    public static final ForgeConfigSpec.DoubleValue VEHICLE_ENGINE_PITCH_BASE;
    public static final ForgeConfigSpec.DoubleValue VEHICLE_ENGINE_PITCH_AT_50;
    public static final ForgeConfigSpec.DoubleValue VEHICLE_ENGINE_PITCH_AT_100;
    public static final ForgeConfigSpec.DoubleValue PLANE_ENGINE_PITCH_BASE;
    public static final ForgeConfigSpec.DoubleValue PLANE_ENGINE_PITCH_AT_50;
    public static final ForgeConfigSpec.DoubleValue PLANE_ENGINE_PITCH_AT_100;
    public static final ForgeConfigSpec.IntValue VEHICLE_HUD_LEFT_X;
    public static final ForgeConfigSpec.IntValue VEHICLE_HUD_LEFT_Y;
    public static final ForgeConfigSpec.IntValue VEHICLE_HUD_RIGHT_X;
    public static final ForgeConfigSpec.IntValue VEHICLE_HUD_RIGHT_Y;
    private static final int DEFAULT_VEHICLE_HUD_LEFT_X = 2;
    private static final int DEFAULT_VEHICLE_HUD_RIGHT_X = 12;
    private static final int DEFAULT_VEHICLE_HUD_TOP = 2;
    private static final int MAX_VEHICLE_HUD_OFFSET = 500;
    public static final ForgeConfigSpec.EnumValue<EnumHitMarkerStyle> HIT_MARKER_STYLE;
    private static final Supplier<Boolean> HD_HIT_MARKER;
    public static final ForgeConfigSpec.BooleanValue FANCY_HIT_MARKER;
    private static final Supplier<Boolean> SHOW_HIT_MARKER;
    private static final Supplier<Double> HIT_MARKER_RED;
    private static final Supplier<Double> HIT_MARKER_GREEN;
    private static final Supplier<Double> HIT_MARKER_BLUE;
    private static final Supplier<Double> HIT_MARKER_ALPHA;
    public static final ForgeConfigSpec.BooleanValue SHOW_FLASHES_WHEN_WOUNDED;
    public static final ForgeConfigSpec.BooleanValue VEHICLE_SCREEN_SHAKE;
    private static final Supplier<Boolean> ENABLE_PLAYER_CLASS_SKIN_OVERRIDES;
    public static final ForgeConfigSpec.DoubleValue DRIVEABLE_RENDER_DISTANCE_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue FLAN_NPC_RENDER_DISTANCE_MULTIPLIER;
    private static final Supplier<Integer> BULLET_RENDER_DISTANCE;
    private static final Supplier<Integer> GRENADE_RENDER_DISTANCE;
    private static final Supplier<Integer> DEPLOYED_GUN_RENDER_DISTANCE;
    private static final Supplier<Integer> AA_GUN_RENDER_DISTANCE;
    private static final ForgeConfigSpec.DoubleValue MINIMUM_DRIVEABLE_PART_PIXEL_SIZE;
    private static final ForgeConfigSpec.BooleanValue ENABLE_DRIVEABLE_LOD;
    public static final ForgeConfigSpec.BooleanValue ENABLE_GPU_MODEL_CACHE;
    public static final ForgeConfigSpec.IntValue GPU_MODEL_CACHE_MEGABYTES;
    private static final ForgeConfigSpec.DoubleValue MAXIMUM_DRIVEABLE_LOD_PART_PIXEL_SIZE;
    private static final ForgeConfigSpec.DoubleValue DRIVEABLE_LOD_DETAIL_MULTIPLIER;
    private static final Supplier<Double> GROUND_VEHICLE_LOD_DISTANCE_FACTOR;
    private static final ForgeConfigSpec.IntValue DRIVEABLE_IMPOSTOR_QUALITY_MULTIPLIER;
    private static final ForgeConfigSpec.DoubleValue DRIVEABLE_TRACK_LINK_LOD_PIXEL_SIZE;
    private static final ForgeConfigSpec.DoubleValue DRIVEABLE_TRACK_LINK_GROUPING_PIXEL_SIZE;
    private static final ForgeConfigSpec.DoubleValue DRIVEABLE_IMPOSTOR_PIXEL_SIZE;
    private static final ForgeConfigSpec.IntValue DRIVEABLE_IMPOSTOR_MINIMUM_DISTANCE;
    private static final ForgeConfigSpec.IntValue DRIVEABLE_IMPOSTOR_MAXIMUM_DISTANCE;
    private static final ForgeConfigSpec.IntValue DRIVEABLE_IMPOSTOR_RESOLUTION;
    private static final ForgeConfigSpec.IntValue DRIVEABLE_IMPOSTOR_YAW_ANGLES;
    private static final Supplier<Integer> DRIVEABLE_IMPOSTOR_CACHE_ENTRIES;
    private static final Supplier<Double> PARTICLE_DISTANCE_SCALE;
    private static final Supplier<Double> FULL_PARTICLE_DENSITY_SHARE;
    private static final Supplier<Double> DISTANT_PARTICLE_DENSITY;
    private static final Supplier<Integer> MAX_FLANS_PARTICLES_PER_TICK;
    private static final Supplier<Integer> MAX_FRAG_SPARKS;
    private static final Supplier<Double> FRAG_SPARK_MULTIPLIER;

    private static final Supplier<EnumMouseButton> SHOOT_BUTTON;
    private static final Supplier<EnumMouseButton> SHOOT_BUTTON_OFFHAND;
    private static final Supplier<EnumMouseButton> AIM_BUTTON;
    public static final ForgeConfigSpec.EnumValue<EnumAimType> AIM_TYPE;
    public static final ForgeConfigSpec.EnumValue<EnumGunBlockInteraction> GUN_BLOCK_INTERACTION;
    public static final ForgeConfigSpec.BooleanValue PREDICT_DRIVEABLE_MOVEMENT;

    private static final Supplier<Boolean> COMBINE_AMMO_ON_RELOAD;
    private static final Supplier<Boolean> AMMO_TO_UPPER_INVENTORY_ON_RELOAD;

    private static final Supplier<Boolean> ENABLE_ARMS;
    public static final ForgeConfigSpec.EnumValue<EnumAimPose> AIM_POSE;
    private static final Supplier<Boolean> ENABLE_GUN_ANIMATIONS_IN_THIRD_PERSON;
    private static final Supplier<Boolean> ENABLE_WEAPON_SPRINT_STANCE;
    private static final Supplier<Boolean> ENABLE_RANDOM_SPRINT_STANCE;
    private static final Supplier<Boolean> SHOW_CASING_EJECTIONS;
    public static final ForgeConfigSpec.EnumValue<EnumMuzzleFlashStyle> MUZZLE_FLASH_STYLE;

    private static final Supplier<Boolean> ENABLE_FAST_TRANSLUCENT_RENDERING;
    private static final Supplier<Boolean> ALWAYS_ENABLE_ARMOR_TRANSLUCENT_RENDERING_BY_DEFAULT;
    private static final Supplier<Boolean> ALWAYS_ENABLE_GUN_TRANSLUCENT_RENDERING_BY_DEFAULT;
    private static final Supplier<Boolean> ALWAYS_ENABLE_GRENADE_TRANSLUCENT_RENDERING_BY_DEFAULT;
    private static final Supplier<Boolean> ALWAYS_ENABLE_BULLET_TRANSLUCENT_RENDERING_BY_DEFAULT;
    private static final Supplier<Boolean> ALWAYS_ENABLE_ATTACHMENT_TRANSLUCENT_RENDERING_BY_DEFAULT;
    private static final Supplier<Boolean> ALWAYS_ENABLE_AA_GUN_TRANSLUCENT_RENDERING_BY_DEFAULT;
    private static final Supplier<Boolean> ALWAYS_ENABLE_VEHICLE_TRANSLUCENT_RENDERING_BY_DEFAULT;
    private static final Supplier<Boolean> ALWAYS_ENABLE_PLANE_TRANSLUCENT_RENDERING_BY_DEFAULT;
    private static final Supplier<Boolean> ALWAYS_ENABLE_MECHA_TRANSLUCENT_RENDERING_BY_DEFAULT;

    private static final Supplier<Boolean> ALWAYS_ENABLE_ARMOR_CULLING_BY_DEFAULT;
    private static final Supplier<Boolean> ALWAYS_ENABLE_GUN_CULLING_BY_DEFAULT;
    private static final Supplier<Boolean> ALWAYS_ENABLE_GRENADE_CULLING_BY_DEFAULT;
    private static final Supplier<Boolean> ALWAYS_ENABLE_BULLET_CULLING_BY_DEFAULT;
    private static final Supplier<Boolean> ALWAYS_ENABLE_ATTACHMENT_CULLING_BY_DEFAULT;
    private static final Supplier<Boolean> ALWAYS_ENABLE_AA_GUN_CULLING_BY_DEFAULT;
    private static final Supplier<Boolean> ALWAYS_ENABLE_VEHICLE_CULLING_BY_DEFAULT;
    private static final Supplier<Boolean> ALWAYS_ENABLE_PLANE_CULLING_BY_DEFAULT;
    private static final Supplier<Boolean> ALWAYS_ENABLE_MECHA_CULLING_BY_DEFAULT;

    public static final ForgeConfigSpec.BooleanValue DISTANT_HORIZONS_INTEGRATION;
    public static final ForgeConfigSpec.BooleanValue DISTANT_HORIZONS_RANGEFINDER;
    public static final ForgeConfigSpec.BooleanValue DISTANT_HORIZONS_CONTACTS;
    public static final ForgeConfigSpec.BooleanValue DISTANT_HORIZONS_EXPLOSIONS;
    public static final ForgeConfigSpec.BooleanValue DISTANT_HORIZONS_DRIVEABLE_RENDERING;

    private static final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
    private static final AtomicReference<ModClientConfig> instance = new AtomicReference<>();
    /** Option changes the options screen has made in memory but not written to the file yet. */
    private static final AtomicBoolean pendingChanges = new AtomicBoolean();

    static
    {
        builder.push("General Settings");
        SHOW_PACK_NAME_IN_ITEM_DESCRIPTIONS = builder
                .comment("Show content pack names in item descriptions")
                .define("showPackNameInItemDescriptions", true);
        SHOW_DETAILED_ITEM_DESCRIPTIONS = builder
                .comment("Show the detailed descriptions of guns, ammunition and other Flan's items: loaded ammunition, statistics",
                    "and the full statistics shown while sneaking. When disabled, only the item's own description is listed.")
                .define("showDetailedItemDescriptions", true);
        ENABLE_UNCENSORED_CONTENT = builder
                .comment("Use optional encrypted uncensored names and textures supplied by packaged content packs. Changing this reloads client resources.")
                .define("enableUncensoredContent", false);
        OPTIONS_BUTTON_PLACEMENT = builder
                .comment("""
                    Where the button opening the Flan's Mod options screen is added.
                    OPTIONS_SCREEN: in the vanilla options screen. PAUSE_MENU: in the pause menu.
                    BOTH: in either one. NONE: nowhere; the screen stays reachable from the mod list.
                    """)
                .defineEnum("optionsButtonPlacement", EnumOptionsButtonPlacement.OPTIONS_SCREEN);
        SHOW_FLANS_HUD = builder
                .comment("""
                    Show Flan's Mod's own in-world HUD: gun and vehicle ammunition, AA gun and deployed gun
                    readouts, team information and the kill feed. The vanilla HUD is not affected.
                    """)
                .define("showFlansHud", true);
        HIDE_CROSSHAIR_FOR_GUNS = builder
                .comment("""
                    Personal preference: hide the vanilla crosshair while holding a non-melee Flan gun.
                    The server's own disableCrosshairForGuns setting hides it for everyone regardless of this
                    option, which can only hide the crosshair, never bring it back.
                    """)
                .define("hideCrosshairForGuns", false);
        LOAD_ALL_MODELS_IN_CACHE = builder
                .comment("""
                    If true, loads and caches ALL models up-front during resource reload.
                    ⚠ Warning:
                    Do NOT enable this if you use many content packs / large amounts of content.
                    This can be very RAM-hungry and may significantly increase client reload times.
                    Recommended: leave this OFF and let models load on-demand.
                    """)
                .define("loadAllModelsInCache", false);
        SEARCH_MODELS_IN_OTHER_CONTENT_PACKS = builder
                .comment("When a model class is missing from its content pack, search for it in other loaded content packs")
                .define("searchModelsInOtherContentPacks", true);
        PREFER_BUILT_IN_MODEL_CLASSES = builder
                .comment("""
                    Let the model classes compiled into the mod always win over the model class files shipped
                    inside a content pack.
                    By default a content pack's own model class file wins for the items of that pack, so a pack
                    shipping its own version of a model that also exists in the mod renders with its own version.
                    Content packs keep loading the model classes the mod does not provide either way.
                    """)
                .define("preferBuiltInModelClasses", false);
        SHOW_SHOOTABLE_DURABILITY_BARS = builder
                .comment("Show a durability-style bar for shootable items when their current round item is not full")
                .define("showShootableDurabilityBars", true);
        SHOW_ARMOR_DAMAGE_ABSORPTION_BAR = builder
                .comment("Show the armor-style HUD bar for legacy armor damage absorption")
                .define("showArmorDamageAbsorptionBar", true);
        DRIVEABLE_SPEED_UNIT = builder
                .comment("Unit used for vehicle and plane speed on the HUD")
                .defineEnum("driveableSpeedUnit", EnumSpeedUnit.KMH);
        DRIVEABLE_VERTICAL_SPEED_UNIT = builder
                .comment("Unit used for plane vertical speed on the HUD")
                .defineEnum("driveableVerticalSpeedUnit", EnumSpeedUnit.METERS_PER_SECOND);
        builder.push("Engine Sound Pitch Defaults");
        VEHICLE_ENGINE_PITCH_BASE = builder.comment("Vehicle engine pitch at zero throttle")
                .defineInRange("vehiclePitchBase", 0.5D, 0D, Float.MAX_VALUE);
        VEHICLE_ENGINE_PITCH_AT_50 = builder.comment("Vehicle engine pitch at 50% throttle")
                .defineInRange("vehiclePitchAt50", 0.8D, 0D, Float.MAX_VALUE);
        VEHICLE_ENGINE_PITCH_AT_100 = builder.comment("Vehicle engine pitch at 100% throttle")
                .defineInRange("vehiclePitchAt100", 1.2D, 0D, Float.MAX_VALUE);
        PLANE_ENGINE_PITCH_BASE = builder.comment("Plane engine pitch at zero throttle")
                .defineInRange("planePitchBase", 0.5D, 0D, Float.MAX_VALUE);
        PLANE_ENGINE_PITCH_AT_50 = builder.comment("Plane engine pitch at 50% throttle")
                .defineInRange("planePitchAt50", 1D, 0D, Float.MAX_VALUE);
        PLANE_ENGINE_PITCH_AT_100 = builder.comment("Plane engine pitch at 100% throttle")
                .defineInRange("planePitchAt100", 1.5D, 0D, Float.MAX_VALUE);
        builder.pop();
        HIT_MARKER_STYLE = builder
                .comment("""
                    Visual style of the hit marker.
                    ULTIMATE: the hit marker from Flan's Mod Ultimate 1.7.10.
                    CLASSIC: the hit marker from Flan's Mod 1.12.2.
                    """)
                .defineEnum("hitMarkerStyle", EnumHitMarkerStyle.ULTIMATE);
        HD_HIT_MARKER = builder
                .comment("Use the higher resolution texture for the ULTIMATE hit marker style")
                .define("hdHitMarker", false);
        FANCY_HIT_MARKER = builder
                .comment("""
                    Colour the ULTIMATE hit marker based on the hit.
                    Red = no penetration, green = full damage, light blue = headshot, yellow = explosion.
                    """)
                .define("fancyHitMarker", true);
        SHOW_HIT_MARKER = builder
                .comment("Show a hit marker when one of your shots hits")
                .define("showHitMarker", true);
        HIT_MARKER_RED = builder
                .comment("Red component of the hit marker colour, used whenever the fancy hit marker does not colour it")
                .defineInRange("hitMarkerRed", 1D, 0D, 1D);
        HIT_MARKER_GREEN = builder
                .comment("Green component of the hit marker colour, used whenever the fancy hit marker does not colour it")
                .defineInRange("hitMarkerGreen", 1D, 0D, 1D);
        HIT_MARKER_BLUE = builder
                .comment("Blue component of the hit marker colour, used whenever the fancy hit marker does not colour it")
                .defineInRange("hitMarkerBlue", 1D, 0D, 1D);
        HIT_MARKER_ALPHA = builder
                .comment("Opacity of the hit marker, applied to every style and colour")
                .defineInRange("hitMarkerAlpha", 1D, 0D, 1D);
        SHOW_FLASHES_WHEN_WOUNDED = builder
                .comment("Show the red blood overlay flash when the player takes damage")
                .define("showFlashesWhenWounded", true);
        VEHICLE_SCREEN_SHAKE = builder
                .comment("Shake the camera when a nearby driveable that opts into FancyScreenShake fires its main or coaxial gun")
                .define("vehicleScreenShake", true);
        ENABLE_PLAYER_CLASS_SKIN_OVERRIDES = builder
                .comment("""
                    Let a Teams player class replace the skin of the players wearing it, when its content pack
                    defines a SkinOverride. Overrides are ignored anyway when the texture is missing, is not a
                    valid player skin sheet, or when another mod draws that player with its own model.
                    """)
                .define("enablePlayerClassSkinOverrides", true);
        builder.pop();

        builder.push("Ammo HUD Settings");
        SHOW_AMMO_HUD = builder
            .comment("Show the held-gun ammunition HUD independently of the rest of the Minecraft HUD.")
            .define("showAmmoHud", true);
        AMMO_HUD_LAYOUT = builder
            .comment("Ammo HUD layout. CURRENT preserves the existing Ultimate 2.0 layout; LEGACY_FANCY and LEGACY_DEFAULT reproduce the two Ultimate 1.7.10 placements.")
            .defineEnum("ammoHudLayout", EnumAmmoHudLayout.CURRENT);
        builder.pop();

        builder.push("Vehicle HUD Settings");
        VEHICLE_HUD_LEFT_X = builder
            .comment("Distance in GUI pixels from the left screen edge to the vehicle HUD's left block (name, health, fuel, speed). Also used by AA gun and deployed gun readouts.")
            .defineInRange("vehicleHudLeftX", DEFAULT_VEHICLE_HUD_LEFT_X, 0, MAX_VEHICLE_HUD_OFFSET);
        VEHICLE_HUD_LEFT_Y = builder
            .comment("Distance in GUI pixels from the top screen edge to the vehicle HUD's left block.")
            .defineInRange("vehicleHudLeftY", DEFAULT_VEHICLE_HUD_TOP, 0, MAX_VEHICLE_HUD_OFFSET);
        VEHICLE_HUD_RIGHT_X = builder
            .comment("Distance in GUI pixels from the right screen edge to the vehicle HUD's right block (aim, weapons, ammunition). Also used by AA gun and deployed gun readouts.")
            .defineInRange("vehicleHudRightX", DEFAULT_VEHICLE_HUD_RIGHT_X, 0, MAX_VEHICLE_HUD_OFFSET);
        VEHICLE_HUD_RIGHT_Y = builder
            .comment("Distance in GUI pixels from the top screen edge to the vehicle HUD's right block.")
            .defineInRange("vehicleHudRightY", DEFAULT_VEHICLE_HUD_TOP, 0, MAX_VEHICLE_HUD_OFFSET);
        builder.pop();

        builder.push("Entity Rendering Settings");
        ENABLE_GPU_MODEL_CACHE = builder
            .comment("Experimental GPU cache for rigid full-detail vehicle and gun model parts. Keeps animated transforms, tint and lighting live. Unsupported geometry, sorted transparency, Fabulous graphics, OptiFine and active Oculus/Iris shader packs use the standard renderer. Disable if rendering artifacts or slower frame times occur. No restart required.")
            .define("enableGpuModelCache", true);
        GPU_MODEL_CACHE_MEGABYTES = builder
            .comment("Video memory in MiB the GPU model cache may keep for vertex buffers; 0 selects it automatically: a sixteenth of dedicated video memory within 128-512 MiB, or 128 MiB when the driver does not report it. When visible meshes keep being evicted, the automatic value grows by quarters up to an eighth of video memory (at most 1 GiB) while 512 MiB of video memory stays free; it returns to its starting value on resource reload. It must hold every distinct model drawn in a frame, otherwise meshes are evicted and re-uploaded each frame; /flansrenderstats reports such evictions. Memory is used only as models are drawn. Values from 1 to 15 count as 16. No restart required.")
            .defineInRange("gpuModelCacheMegabytes", 0, 0, GpuModelCache.MAX_CONFIGURED_MEGABYTES);
        DRIVEABLE_RENDER_DISTANCE_MULTIPLIER = builder
            .comment("Multiplier of the size-based Minecraft entity render distance for vehicles, planes and mechas. Keeps collision-box proportionality and Minecraft Entity Distance scaling. Server tracking and view distance still limit visibility; Distant Horizons chunk-edge extension can override this cutoff.")
            .defineInRange("driveableRenderDistanceMultiplier", 1D, 0.25D, 4D);
        FLAN_NPC_RENDER_DISTANCE_MULTIPLIER = builder
            .comment("Multiplier of the size-based Minecraft render distance for Flan-model NPCs and explicitly opted-in soldiers. Requires the NPC module. Server tracking and view distance still limit visibility; NPCs have no distant proxies.")
            .defineInRange("flanNpcRenderDistanceMultiplier", 1D, 0.25D, 4D);
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
        MINIMUM_DRIVEABLE_PART_PIXEL_SIZE = builder
            .comment("Skip individual driveable model parts whose projected bounding diameter is smaller than this many physical screen pixels. Set to 0 to disable. Only affects driveables rendered in the world.")
            .defineInRange("minimumDriveablePartPixelSize", 0.5D, 0D, 16D);
        ENABLE_DRIVEABLE_LOD = builder
            .comment("Enable automatic world-rendered driveable LOD. Medium-distance models use stronger part culling and supported tank track links use simplified geometry. Distant vehicles / planes may use generated impostors. Mechas do not use impostors because held add-ons are not part of their base model.")
            .define("enableDriveableLod", true);
        MAXIMUM_DRIVEABLE_LOD_PART_PIXEL_SIZE = builder
            .comment("Base far-distance projected part diameter for whole-model LOD, before driveableLodDetailMultiplier. Must exceed minimumDriveablePartPixelSize to have an effect.")
            .defineInRange("maximumDriveableLodPartPixelSize", 1D, 0D, 32D);
        DRIVEABLE_LOD_DETAIL_MULTIPLIER = builder
            .comment("Multiply the far-distance part-culling threshold for the whole vehicle, including hull/turret details. Ramps smoothly from 24 to 80 size-scaled blocks, independently of impostor eligibility. 1 keeps the configured far threshold; 2 doubles it. Does not change the near threshold or enable explicitly disabled part culling.")
            .defineInRange("driveableLodDetailMultiplier", 1D, 1D, 4D);
        GROUND_VEHICLE_LOD_DISTANCE_FACTOR = builder
            .comment("Distance multiplier for earlier whole-model LOD and impostors on small land vehicles. 0.5 halves their size-scaled distances; 1 removes the discount. Boats/aircraft are excluded. The discount fades out between model radii of 6 and 12 blocks, protecting very large ground models too.")
            .defineInRange("groundVehicleLodDistanceFactor", 0.5D, 0.25D, 1D);
        DRIVEABLE_IMPOSTOR_QUALITY_MULTIPLIER = builder
            .comment("Multiply impostor capture resolution by this factor and yaw-view count by its square, capped at 256 pixels and 64 yaw views. Default 2 upgrades existing 64px/8-view configs to 128px/32 views. 1 uses the configured values directly. Atlas pages are allocated as views are requested; more visited angles use more memory. Screen-size quality limits can delay distance-triggered impostors.")
            .defineInRange("driveableImpostorQualityMultiplier", 2, 1, 2);
        DRIVEABLE_IMPOSTOR_PIXEL_SIZE = builder
            .comment("Use a generated far-distance impostor when a vehicle or plane projects to at most this many physical screen pixels. Small land vehicles gradually increase this allowance toward the image-quality limit with distance. Set this and driveableImpostorMaximumDistance to 0 to disable impostors.")
            .defineInRange("driveableImpostorPixelSize", 16D, 0D, 256D);
        DRIVEABLE_TRACK_LINK_LOD_PIXEL_SIZE = builder
            .comment("Simplify supported multipart tank track links to textured envelopes when a link projects to at most this many physical screen pixels, beyond 32 blocks. Requires enableDriveableLod; 0 disables track geometry LOD. Previews and the locally controlled vehicle retain full detail.")
            .defineInRange("driveableTrackLinkLodPixelSize", 4D, 0D, 32D);
        DRIVEABLE_TRACK_LINK_GROUPING_PIXEL_SIZE = builder
            .comment("When simplified tank links project to at most this many physical screen pixels, represent pairs with longer envelopes; at half this size, represent groups of four. Per-link animation still advances. 0 disables grouping while keeping single-link geometry LOD. Requires enableDriveableLod and driveableTrackLinkLodPixelSize. The actual threshold never exceeds the single-link LOD threshold.")
            .defineInRange("driveableTrackLinkGroupingPixelSize", 0D, 0D, 16D);
        DRIVEABLE_IMPOSTOR_MINIMUM_DISTANCE = builder
            .comment("Base minimum camera distance in blocks before a generated driveable impostor may be used. Small land vehicles also apply groundVehicleLodDistanceFactor; scaled up automatically for physically larger driveables (e.g. battleships) so they keep their exact model much longer.")
            .defineInRange("driveableImpostorMinimumDistance", 96, 8, 4096);
        DRIVEABLE_IMPOSTOR_MAXIMUM_DISTANCE = builder
            .comment("Prefer a ready generated driveable impostor at or beyond this camera distance, subject to the capture's screen-size quality limit. Distances scale with model size without an upper size cap; small land vehicles also use groundVehicleLodDistanceFactor and gradually promote impostors as this distance approaches. Set to 0 to use only the configured projected-pixel threshold.")
            .defineInRange("driveableImpostorMaximumDistance", 192, 0, 4096);
        DRIVEABLE_IMPOSTOR_RESOLUTION = builder
            .comment("Resolution of each generated driveable impostor view. Changing this clears and regenerates the runtime cache.")
            .defineInRange("driveableImpostorResolution", 64, 32, 256);
        DRIVEABLE_IMPOSTOR_YAW_ANGLES = builder
            .comment("Base number of horizontal views per driveable impostor, scaled by the square of driveableImpostorQualityMultiplier up to 64. Nine vertical views cover -90 to +90 degrees automatically. Views are captured on demand.")
            .defineInRange("driveableImpostorYawAngles", 8, 4, 64);
        DRIVEABLE_IMPOSTOR_CACHE_ENTRIES = builder
            .comment("Maximum generated model / paintjob impostor atlases retained in memory.")
            .defineInRange("driveableImpostorCacheEntries", 32, 1, 128);
        builder.pop();

        builder.push("Particle Rendering Settings");
        PARTICLE_DISTANCE_SCALE = builder
            .comment("Multiplies how far Flan's Mod particles are drawn. Every effect has its own natural drawing distance: 128 blocks for ordinary particles, and further for large effects such as an explosion's rising column, which grows with the charge up to 256 blocks. 0.5 halves every distance; 2 doubles it.")
            .defineInRange("particleDistanceScale", 1.0D, 0.125D, 4.0D);
        FULL_PARTICLE_DENSITY_SHARE = builder
            .comment("Share of each effect's drawing distance within which its particles keep full density. Density gradually falls beyond it, down to distantParticleDensity at the edge. Large landmark effects are never thinned.")
            .defineInRange("fullParticleDensityShare", 0.25D, 0D, 1D);
        DISTANT_PARTICLE_DENSITY = builder
            .comment("Fraction of Flan's Mod particles retained at the edge of each effect's drawing distance.")
            .defineInRange("distantParticleDensity", 0.25D, 0D, 1D);
        MAX_FLANS_PARTICLES_PER_TICK = builder
            .comment("Maximum particles Flan's Mod may create in one client tick. Nearby particles are considered first by normal packet and entity processing order.")
            .defineInRange("maxFlansParticlesPerTick", 2048, 16, 100000);
        MAX_FRAG_SPARKS = builder
            .comment("Maximum fragmentation sparks from one explosion. The overall maxFlansParticlesPerTick budget can limit the visible result further.")
            .defineInRange("maxFragSparks", 512, 0, 100000);
        FRAG_SPARK_MULTIPLIER = builder
            .comment("Multiplier applied to the fragmentation spark count after limiting it to the burst's effective fragments. The result still cannot exceed the effective fragment count or maxFragSparks.")
            .defineInRange("fragSparkMultiplier", 0.25D, 0D, 4D);
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
        GUN_BLOCK_INTERACTION = builder
                .comment("""
                    What right-clicking a block does while holding a gun, since aiming is a right-click too.
                    ALLOW: blocks behave as they do with any other item.
                    NO_CONTAINERS: chests, furnaces and anything else that opens a screen are left alone;
                    sneak to open one anyway, without putting the gun away.
                    NONE: no block is used while armed, doors, levers and buttons included, sneaking or not.
                    """)
                .defineEnum("gunBlockInteraction", EnumGunBlockInteraction.NO_CONTAINERS);
        PREDICT_DRIVEABLE_MOVEMENT = builder
                .comment("Simulate the vehicle or plane you are driving on your own client, so it answers the controls at once instead of a network round trip later. The server stays authoritative and corrects any difference. Off shows only the movement the server reports.")
                .define("predictDriveableMovement", true);
        builder.pop();

        builder.push("Reload Settings");
        COMBINE_AMMO_ON_RELOAD = builder
                .comment("""
                    Combine the unloaded ammo with matching damaged ammo in the inventory.
                    This is a personal preference. A server that turns its own combineAmmoOnReload
                    setting off forbids it for everyone and this option then has no effect.
                    """)
                .define("combineAmmoOnReload", true);
        AMMO_TO_UPPER_INVENTORY_ON_RELOAD = builder
                .comment("""
                    Try to put the unloaded ammo in the upper inventory first instead of the hotbar.
                    This is a personal preference and overrides the server's ammoToUpperInventoryOnReload setting.
                    """)
                .define("ammoToUpperInventoryOnReload", false);
        builder.pop();

        builder.push("Gun Rendering Settings");
        ENABLE_ARMS = builder
            .comment("Enable arms rendering")
            .define("enableArms", true);
        AIM_POSE = builder
            .comment("""
                How your arms hold a gun, as other players see you. The Toggle Aim Pose key switches it while playing.
                ENFORCED: a held gun always keeps your arms raised in the aiming pose.
                DYNAMIC: a gun is raised only while you fire or aim it. Shields always stay raised.
                A server can force one of the two on every player with its playerAimPose setting.
                """)
            .defineEnum("aimPose", EnumAimPose.DYNAMIC);
        ENABLE_GUN_ANIMATIONS_IN_THIRD_PERSON = builder
            .comment("This will display gun animations such as melee and reloading, not only in first person view but also in third person view including animations from other players")
            .define("enableGunAnimationsInThirdPerson", true);
        ENABLE_WEAPON_SPRINT_STANCE = builder
            .comment("This will move weapons to a lowered position when sprinting")
            .define("enableWeaponSprintStance", true);
        ENABLE_RANDOM_SPRINT_STANCE = builder
            .comment("This will randomly generate unique positions for each weapon using the weapon name as a seed")
            .define("enableRandomSprintStance", false);
        SHOW_CASING_EJECTIONS = builder
            .comment("Render animated casing ejections for guns that provide a casing model")
            .define("showCasingEjections", true);
        MUZZLE_FLASH_STYLE = builder
            .comment("""
                Preferred muzzle flash model: FMU_1_7_10 uses FlashModel (FMU 1.7.10 Style),
                MC_1_12_2 uses MuzzleFlashModel (1.12.2 Style).
                Falls back to the available model when a pack supplies only one style.
                Applies immediately to held, deployed and mounted guns on this client.
                """)
            .defineEnum("muzzleFlashStyle", EnumMuzzleFlashStyle.FMU_1_7_10);
        builder.pop();

        builder.push("Translucent Rendering Defaults");
        ENABLE_FAST_TRANSLUCENT_RENDERING = builder
            .comment("Use Flan's Mod's faster unsorted translucent render type. This avoids expensive per-frame vertex sorting, but intersecting translucent surfaces may occasionally render in the wrong order.")
            .define("enableFastTranslucentRendering", true);
        ALWAYS_ENABLE_ARMOR_TRANSLUCENT_RENDERING_BY_DEFAULT = defineTranslucentDefault("armors", "alwaysEnableArmorsTranslucentRenderingByDefault", true);
        ALWAYS_ENABLE_GUN_TRANSLUCENT_RENDERING_BY_DEFAULT = defineTranslucentDefault("guns", "alwaysEnableGunsTranslucentRenderingByDefault", true);
        ALWAYS_ENABLE_GRENADE_TRANSLUCENT_RENDERING_BY_DEFAULT = defineTranslucentDefault("grenades", "alwaysEnableGrenadesTranslucentRenderingByDefault", true);
        ALWAYS_ENABLE_BULLET_TRANSLUCENT_RENDERING_BY_DEFAULT = defineTranslucentDefault("bullets", "alwaysEnableBulletsTranslucentRenderingByDefault", true);
        ALWAYS_ENABLE_ATTACHMENT_TRANSLUCENT_RENDERING_BY_DEFAULT = defineTranslucentDefault("attachments", "alwaysEnableAttachmentsTranslucentRenderingByDefault", true);
        ALWAYS_ENABLE_AA_GUN_TRANSLUCENT_RENDERING_BY_DEFAULT = defineTranslucentDefault("aa-guns", "alwaysEnableAAGunsTranslucentRenderingByDefault", true);
        ALWAYS_ENABLE_VEHICLE_TRANSLUCENT_RENDERING_BY_DEFAULT = defineTranslucentDefault("vehicles", "alwaysEnableVehiclesTranslucentRenderingByDefault", true);
        ALWAYS_ENABLE_PLANE_TRANSLUCENT_RENDERING_BY_DEFAULT = defineTranslucentDefault("planes", "alwaysEnablePlanesTranslucentRenderingByDefault", true);
        ALWAYS_ENABLE_MECHA_TRANSLUCENT_RENDERING_BY_DEFAULT = defineTranslucentDefault("mechas", "alwaysEnableMechasTranslucentRenderingByDefault", true);
        builder.pop();

        builder.push("Culling Defaults");
        ALWAYS_ENABLE_ARMOR_CULLING_BY_DEFAULT = defineCullingDefault("armors", "alwaysEnableArmorsCullingByDefault", false);
        ALWAYS_ENABLE_GUN_CULLING_BY_DEFAULT = defineCullingDefault("guns", "alwaysEnableGunsCullingByDefault", false);
        ALWAYS_ENABLE_GRENADE_CULLING_BY_DEFAULT = defineCullingDefault("grenades", "alwaysEnableGrenadesCullingByDefault", true);
        ALWAYS_ENABLE_BULLET_CULLING_BY_DEFAULT = defineCullingDefault("bullets", "alwaysEnableBulletsCullingByDefault", true);
        ALWAYS_ENABLE_ATTACHMENT_CULLING_BY_DEFAULT = defineCullingDefault("attachments", "alwaysEnableAttachmentsCullingByDefault", false);
        ALWAYS_ENABLE_AA_GUN_CULLING_BY_DEFAULT = defineCullingDefault("aa-guns", "alwaysEnableAAGunsCullingByDefault", true);
        ALWAYS_ENABLE_VEHICLE_CULLING_BY_DEFAULT = defineCullingDefault("vehicles", "alwaysEnableVehiclesCullingByDefault", true);
        ALWAYS_ENABLE_PLANE_CULLING_BY_DEFAULT = defineCullingDefault("planes", "alwaysEnablePlanesCullingByDefault", true);
        ALWAYS_ENABLE_MECHA_CULLING_BY_DEFAULT = defineCullingDefault("mechas", "alwaysEnableMechasCullingByDefault", true);
        builder.pop();

        builder.push("Distant Horizons Integration");
        DISTANT_HORIZONS_INTEGRATION = builder
            .comment("Use Distant Horizons, when it is installed, to show and measure what lies beyond the vanilla render distance.",
                "The settings below only apply while this is enabled.")
            .define("distantHorizonsIntegration", true);
        DISTANT_HORIZONS_RANGEFINDER = builder
            .comment("Let vehicle rangefinders measure the far terrain beyond the loaded chunks. Such ranges are approximate and shown with a ~.")
            .define("distantHorizonsRangefinder", true);
        DISTANT_HORIZONS_CONTACTS = builder
            .comment("Show driveables beyond the entity render distance as simplified shapes on the far terrain, including those the server",
                "only reports from beyond the tracking range, when the server allows it.")
            .define("distantHorizonsContacts", true);
        DISTANT_HORIZONS_EXPLOSIONS = builder
            .comment("Show large explosions beyond the particle range as a flash, a fireball and a smoke column, when the server allows it.")
            .define("distantHorizonsExplosions", true);
        DISTANT_HORIZONS_DRIVEABLE_RENDERING = builder
            .comment("Keep drawing driveables as entities up to the edge of the vanilla chunks, where their far-terrain shapes take over,",
                "This explicitly extends the configured size-based render cutoff. Off keeps that cutoff and uses distant contact shapes beyond it.")
            .define("distantHorizonsDriveableRendering", false);
        builder.pop();

        configSpec = builder.build();
    }

    public static com.flansmodultimate.common.driveables.EngineSoundPitch defaultVehicleEnginePitch()
    {
        return new com.flansmodultimate.common.driveables.EngineSoundPitch(
            VEHICLE_ENGINE_PITCH_BASE.get().floatValue(), VEHICLE_ENGINE_PITCH_AT_50.get().floatValue(),
            VEHICLE_ENGINE_PITCH_AT_100.get().floatValue());
    }

    public static com.flansmodultimate.common.driveables.EngineSoundPitch defaultPlaneEnginePitch()
    {
        return new com.flansmodultimate.common.driveables.EngineSoundPitch(
            PLANE_ENGINE_PITCH_BASE.get().floatValue(), PLANE_ENGINE_PITCH_AT_50.get().floatValue(),
            PLANE_ENGINE_PITCH_AT_100.get().floatValue());
    }

    private ModClientConfig()
    {
        showPackNameInItemDescriptions = SHOW_PACK_NAME_IN_ITEM_DESCRIPTIONS.get();
        showDetailedItemDescriptions = SHOW_DETAILED_ITEM_DESCRIPTIONS.get();
        enableUncensoredContent = ENABLE_UNCENSORED_CONTENT.get();
        optionsButtonPlacement = OPTIONS_BUTTON_PLACEMENT.get();
        showFlansHud = SHOW_FLANS_HUD.get();
        hideCrosshairForGuns = HIDE_CROSSHAIR_FOR_GUNS.get();
        loadAllModelsInCache = LOAD_ALL_MODELS_IN_CACHE.get();
        searchModelsInOtherContentPacks = SEARCH_MODELS_IN_OTHER_CONTENT_PACKS.get();
        preferBuiltInModelClasses = PREFER_BUILT_IN_MODEL_CLASSES.get();
        showShootableDurabilityBars = SHOW_SHOOTABLE_DURABILITY_BARS.get();
        showArmorDamageAbsorptionBar = SHOW_ARMOR_DAMAGE_ABSORPTION_BAR.get();
        showAmmoHud = SHOW_AMMO_HUD.get();
        ammoHudLayout = AMMO_HUD_LAYOUT.get();
        driveableSpeedUnit = DRIVEABLE_SPEED_UNIT.get();
        driveableVerticalSpeedUnit = DRIVEABLE_VERTICAL_SPEED_UNIT.get();
        hitMarkerStyle = HIT_MARKER_STYLE.get();
        hdHitMarker = HD_HIT_MARKER.get();
        fancyHitMarker = FANCY_HIT_MARKER.get();
        showHitMarker = SHOW_HIT_MARKER.get();
        hitMarkerRed = HIT_MARKER_RED.get().floatValue();
        hitMarkerGreen = HIT_MARKER_GREEN.get().floatValue();
        hitMarkerBlue = HIT_MARKER_BLUE.get().floatValue();
        hitMarkerAlpha = HIT_MARKER_ALPHA.get().floatValue();
        showFlashesWhenWounded = SHOW_FLASHES_WHEN_WOUNDED.get();
        vehicleScreenShake = VEHICLE_SCREEN_SHAKE.get();
        enablePlayerClassSkinOverrides = ENABLE_PLAYER_CLASS_SKIN_OVERRIDES.get();
        driveableRenderDistanceMultiplier = DRIVEABLE_RENDER_DISTANCE_MULTIPLIER.get();
        flanNpcRenderDistanceMultiplier = FLAN_NPC_RENDER_DISTANCE_MULTIPLIER.get();
        bulletRenderDistance = BULLET_RENDER_DISTANCE.get();
        grenadeRenderDistance = GRENADE_RENDER_DISTANCE.get();
        deployedGunRenderDistance = DEPLOYED_GUN_RENDER_DISTANCE.get();
        aaGunRenderDistance = AA_GUN_RENDER_DISTANCE.get();
        minimumDriveablePartPixelSize = MINIMUM_DRIVEABLE_PART_PIXEL_SIZE.get();
        enableDriveableLod = ENABLE_DRIVEABLE_LOD.get();
        enableGpuModelCache = ENABLE_GPU_MODEL_CACHE.get();
        gpuModelCacheMegabytes = GPU_MODEL_CACHE_MEGABYTES.get();
        maximumDriveableLodPartPixelSize = MAXIMUM_DRIVEABLE_LOD_PART_PIXEL_SIZE.get();
        driveableLodDetailMultiplier = DRIVEABLE_LOD_DETAIL_MULTIPLIER.get();
        groundVehicleLodDistanceFactor = GROUND_VEHICLE_LOD_DISTANCE_FACTOR.get();
        driveableImpostorQualityMultiplier = DRIVEABLE_IMPOSTOR_QUALITY_MULTIPLIER.get();
        driveableTrackLinkLodPixelSize = DRIVEABLE_TRACK_LINK_LOD_PIXEL_SIZE.get();
        driveableTrackLinkGroupingPixelSize = DRIVEABLE_TRACK_LINK_GROUPING_PIXEL_SIZE.get();
        driveableImpostorPixelSize = DRIVEABLE_IMPOSTOR_PIXEL_SIZE.get();
        driveableImpostorMinimumDistance = DRIVEABLE_IMPOSTOR_MINIMUM_DISTANCE.get();
        driveableImpostorMaximumDistance = DRIVEABLE_IMPOSTOR_MAXIMUM_DISTANCE.get();
        driveableImpostorResolution = DRIVEABLE_IMPOSTOR_RESOLUTION.get();
        driveableImpostorYawAngles = DRIVEABLE_IMPOSTOR_YAW_ANGLES.get();
        driveableImpostorCacheEntries = DRIVEABLE_IMPOSTOR_CACHE_ENTRIES.get();
        particleDistanceScale = PARTICLE_DISTANCE_SCALE.get();
        fullParticleDensityShare = FULL_PARTICLE_DENSITY_SHARE.get();
        distantParticleDensity = DISTANT_PARTICLE_DENSITY.get();
        maxFlansParticlesPerTick = MAX_FLANS_PARTICLES_PER_TICK.get();
        maxFragSparks = MAX_FRAG_SPARKS.get();
        fragSparkMultiplier = FRAG_SPARK_MULTIPLIER.get();

        shootButton = SHOOT_BUTTON.get();
        shootButtonOffhand = SHOOT_BUTTON_OFFHAND.get();
        aimButton = AIM_BUTTON.get();
        aimType = AIM_TYPE.get();
        gunBlockInteraction = GUN_BLOCK_INTERACTION.get();
        predictDriveableMovement = PREDICT_DRIVEABLE_MOVEMENT.get();

        combineAmmoOnReload = COMBINE_AMMO_ON_RELOAD.get();
        ammoToUpperInventoryOnReload = AMMO_TO_UPPER_INVENTORY_ON_RELOAD.get();

        enableArms = ENABLE_ARMS.get();
        aimPose = AIM_POSE.get();
        enableGunAnimationsInThirdPerson = ENABLE_GUN_ANIMATIONS_IN_THIRD_PERSON.get();
        enableWeaponSprintStance = ENABLE_WEAPON_SPRINT_STANCE.get();
        enableRandomSprintStance = ENABLE_RANDOM_SPRINT_STANCE.get();
        showCasingEjections = SHOW_CASING_EJECTIONS.get();
        muzzleFlashStyle = MUZZLE_FLASH_STYLE.get();

        enableFastTranslucentRendering = ENABLE_FAST_TRANSLUCENT_RENDERING.get();
        alwaysEnableArmorTranslucentRenderingByDefault = ALWAYS_ENABLE_ARMOR_TRANSLUCENT_RENDERING_BY_DEFAULT.get();
        alwaysEnableGunTranslucentRenderingByDefault = ALWAYS_ENABLE_GUN_TRANSLUCENT_RENDERING_BY_DEFAULT.get();
        alwaysEnableGrenadeTranslucentRenderingByDefault = ALWAYS_ENABLE_GRENADE_TRANSLUCENT_RENDERING_BY_DEFAULT.get();
        alwaysEnableBulletTranslucentRenderingByDefault = ALWAYS_ENABLE_BULLET_TRANSLUCENT_RENDERING_BY_DEFAULT.get();
        alwaysEnableAttachmentTranslucentRenderingByDefault = ALWAYS_ENABLE_ATTACHMENT_TRANSLUCENT_RENDERING_BY_DEFAULT.get();
        alwaysEnableAAGunTranslucentRenderingByDefault = ALWAYS_ENABLE_AA_GUN_TRANSLUCENT_RENDERING_BY_DEFAULT.get();
        alwaysEnableVehicleTranslucentRenderingByDefault = ALWAYS_ENABLE_VEHICLE_TRANSLUCENT_RENDERING_BY_DEFAULT.get();
        alwaysEnablePlaneTranslucentRenderingByDefault = ALWAYS_ENABLE_PLANE_TRANSLUCENT_RENDERING_BY_DEFAULT.get();
        alwaysEnableMechaTranslucentRenderingByDefault = ALWAYS_ENABLE_MECHA_TRANSLUCENT_RENDERING_BY_DEFAULT.get();

        alwaysEnableArmorCullingByDefault = ALWAYS_ENABLE_ARMOR_CULLING_BY_DEFAULT.get();
        alwaysEnableGunCullingByDefault = ALWAYS_ENABLE_GUN_CULLING_BY_DEFAULT.get();
        alwaysEnableGrenadeCullingByDefault = ALWAYS_ENABLE_GRENADE_CULLING_BY_DEFAULT.get();
        alwaysEnableBulletCullingByDefault = ALWAYS_ENABLE_BULLET_CULLING_BY_DEFAULT.get();
        alwaysEnableAttachmentCullingByDefault = ALWAYS_ENABLE_ATTACHMENT_CULLING_BY_DEFAULT.get();
        alwaysEnableAAGunCullingByDefault = ALWAYS_ENABLE_AA_GUN_CULLING_BY_DEFAULT.get();
        alwaysEnableVehicleCullingByDefault = ALWAYS_ENABLE_VEHICLE_CULLING_BY_DEFAULT.get();
        alwaysEnablePlaneCullingByDefault = ALWAYS_ENABLE_PLANE_CULLING_BY_DEFAULT.get();
        alwaysEnableMechaCullingByDefault = ALWAYS_ENABLE_MECHA_CULLING_BY_DEFAULT.get();

        distantHorizonsIntegration = DISTANT_HORIZONS_INTEGRATION.get();
        distantHorizonsRangefinder = DISTANT_HORIZONS_RANGEFINDER.get();
        distantHorizonsContacts = DISTANT_HORIZONS_CONTACTS.get();
        distantHorizonsExplosions = DISTANT_HORIZONS_EXPLOSIONS.get();
        distantHorizonsDriveableRendering = DISTANT_HORIZONS_DRIVEABLE_RENDERING.get();
    }

    public static ModClientConfig get()
    {
        return instance.get();
    }

    /**
     * Reads a vehicle HUD offset live rather than from the baked snapshot, so the HUD follows an options
     * slider while it is still being dragged.
     */
    public static int vehicleHudOffset(ForgeConfigSpec.IntValue value)
    {
        return configSpec.isLoaded() ? value.get() : value.getDefault();
    }

    public static boolean isUncensoredContentEnabled()
    {
        ModClientConfig config = get();
        if (config != null)
            return config.enableUncensoredContent;

        if (configSpec.isLoaded())
            return ENABLE_UNCENSORED_CONTENT.get();

        Boolean startupValue = startupUncensoredContentEnabled;
        if (startupValue == null)
        {
            synchronized (ModClientConfig.class)
            {
                startupValue = startupUncensoredContentEnabled;
                if (startupValue == null)
                {
                    Path configPath = PlatformPaths.configDir().resolve(CONFIG_FILE_NAME);
                    startupValue = readUncensoredContentSetting(configPath);
                    startupUncensoredContentEnabled = startupValue;
                }
            }
        }
        return startupValue;
    }

    static boolean readUncensoredContentSetting(Path configPath)
    {
        if (!Files.isRegularFile(configPath))
            return false;

        try (CommentedFileConfig config = CommentedFileConfig.of(configPath, TomlFormat.instance()))
        {
            config.load();
            Object value = config.get(UNCENSORED_CONTENT_CONFIG_PATH);
            return value instanceof Boolean enabled && enabled;
        }
        catch (Exception e)
        {
            FlansMod.log.warn("Could not read {} before the initial client resource load; encrypted content will remain disabled for this launch.", CONFIG_FILE_NAME, e);
            return false;
        }
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
            case VEHICLE -> alwaysEnableVehicleTranslucentRenderingByDefault;
            case PLANE -> alwaysEnablePlaneTranslucentRenderingByDefault;
            case MECHA -> alwaysEnableMechaTranslucentRenderingByDefault;
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
            case VEHICLE -> alwaysEnableVehicleCullingByDefault;
            case PLANE -> alwaysEnablePlaneCullingByDefault;
            case MECHA -> alwaysEnableMechaCullingByDefault;
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

    /**
     * Persist a value the in-game options screen just changed and apply it immediately. The config watcher
     * would re-read the file on its own eventually; baking here makes the change visible on the next frame.
     */
    public static <T> void setAndSave(ForgeConfigSpec.ConfigValue<T> value, T newValue)
    {
        if (set(value, newValue))
            flushPendingChanges();
    }

    /**
     * Change a value without writing the file yet, for a slider the player is still dragging. The change
     * takes effect on the next {@link #flushPendingChanges()}, which the options screen does when it closes.
     *
     * @return whether the value changed
     */
    public static <T> boolean set(ForgeConfigSpec.ConfigValue<T> value, T newValue)
    {
        if (!configSpec.isLoaded())
        {
            FlansMod.log.warn("Ignoring a client option change made before {} was loaded", CONFIG_FILE_NAME);
            return false;
        }

        var spec = ConfigSpecValues.valueSpec(configSpec, value.getPath());
        if (spec == null || !spec.getClazz().isInstance(newValue) || !spec.test(newValue)
            || newValue instanceof Double number && !Double.isFinite(number))
            return false;

        if (newValue.equals(value.get()))
            return false;

        value.set(newValue);
        pendingChanges.set(true);
        return true;
    }

    /** The last position is a read-only indication that individual config values differ from every preset. */
    public enum RenderPreset
    {
        OFF, ULTRA_QUALITY, QUALITY, BALANCED, PERFORMANCE, AGGRESSIVE, EXTREME, MAXIMUM_FPS, CUSTOM;

        public static RenderPreset at(int index)
        {
            return values()[index];
        }
    }

    public record LodValues(double nearPixels, double farPixels, double detailMultiplier, double trackPixels, double groupedTrackPixels) {}

    public record ImpostorValues(double pixels, int minimumDistance, int maximumDistance, int qualityMultiplier, int resolution, int yawAngles) {}

    private static LodValues lodValues(RenderPreset preset)
    {
        return switch (preset)
        {
            case OFF -> new LodValues(0, 0, 1, 0, 0);
            case ULTRA_QUALITY -> new LodValues(0.25, 0.5, 1, 0, 0);
            case QUALITY -> new LodValues(0.5, 1, 1, 4, 0);
            case BALANCED -> new LodValues(0.75, 2, 2, 8, 8);
            case PERFORMANCE -> new LodValues(1.5, 4, 2, 12, 12);
            case AGGRESSIVE -> new LodValues(3, 8, 3, 16, 16);
            case EXTREME -> new LodValues(4, 12, 3.5, 24, 16);
            case MAXIMUM_FPS -> new LodValues(6, 16, 4, 32, 16);
            case CUSTOM -> throw new IllegalArgumentException("Custom is not a preset");
        };
    }

    private static ImpostorValues impostorValues(RenderPreset preset)
    {
        return switch (preset)
        {
            case OFF -> new ImpostorValues(0, 64, 0, 2, 64, 8);
            case ULTRA_QUALITY -> new ImpostorValues(8, 128, 256, 2, 64, 8);
            case QUALITY -> new ImpostorValues(16, 96, 192, 2, 64, 8);
            case BALANCED -> new ImpostorValues(32, 64, 128, 2, 64, 8);
            case PERFORMANCE -> new ImpostorValues(64, 48, 96, 2, 64, 8);
            case AGGRESSIVE -> new ImpostorValues(96, 32, 64, 2, 64, 8);
            case EXTREME -> new ImpostorValues(128, 24, 48, 2, 64, 8);
            case MAXIMUM_FPS -> new ImpostorValues(160, 16, 32, 2, 64, 8);
            case CUSTOM -> throw new IllegalArgumentException("Custom is not a preset");
        };
    }

    public static RenderPreset currentLodPreset()
    {
        if (BooleanUtils.isNotTrue(ENABLE_DRIVEABLE_LOD.get()))
            return RenderPreset.CUSTOM;

        LodValues current = currentLodValues();
        for (RenderPreset preset : RenderPreset.values())
            if (preset != RenderPreset.CUSTOM && lodValues(preset).equals(current))
                return preset;
        return RenderPreset.CUSTOM;
    }

    public static RenderPreset currentImpostorPreset()
    {
        if (BooleanUtils.isNotTrue(ENABLE_DRIVEABLE_LOD.get()))
            return RenderPreset.CUSTOM;

        if (DRIVEABLE_IMPOSTOR_PIXEL_SIZE.get() == 0 && DRIVEABLE_IMPOSTOR_MAXIMUM_DISTANCE.get() == 0)
            return RenderPreset.OFF;

        ImpostorValues current = currentImpostorValues();
        for (RenderPreset preset : RenderPreset.values())
            if (preset != RenderPreset.OFF && preset != RenderPreset.CUSTOM
                && impostorValues(preset).equals(current))
                return preset;
        return RenderPreset.CUSTOM;
    }

    /** Reads pending option values, so the tooltip also reflects a slider still being dragged. */
    public static LodValues currentLodValues()
    {
        return new LodValues(MINIMUM_DRIVEABLE_PART_PIXEL_SIZE.get(),
            MAXIMUM_DRIVEABLE_LOD_PART_PIXEL_SIZE.get(), DRIVEABLE_LOD_DETAIL_MULTIPLIER.get(),
            DRIVEABLE_TRACK_LINK_LOD_PIXEL_SIZE.get(), DRIVEABLE_TRACK_LINK_GROUPING_PIXEL_SIZE.get());
    }

    public static ImpostorValues currentImpostorValues()
    {
        return new ImpostorValues(DRIVEABLE_IMPOSTOR_PIXEL_SIZE.get(),
            DRIVEABLE_IMPOSTOR_MINIMUM_DISTANCE.get(), DRIVEABLE_IMPOSTOR_MAXIMUM_DISTANCE.get(),
            DRIVEABLE_IMPOSTOR_QUALITY_MULTIPLIER.get(), DRIVEABLE_IMPOSTOR_RESOLUTION.get(),
            DRIVEABLE_IMPOSTOR_YAW_ANGLES.get());
    }

    /** Changes only model/track LOD controls. Zero thresholds keep impostors available independently. */
    public static void applyLodPreset(RenderPreset preset)
    {
        if (preset == RenderPreset.CUSTOM)
            return;

        LodValues values = lodValues(preset);
        if (BooleanUtils.isNotTrue(ENABLE_DRIVEABLE_LOD.get()))
        {
            // The disabled master switch also suppresses impostors. Preserve that effective state.
            set(DRIVEABLE_IMPOSTOR_PIXEL_SIZE, 0D);
            set(DRIVEABLE_IMPOSTOR_MAXIMUM_DISTANCE, 0);
        }
        set(ENABLE_DRIVEABLE_LOD, true);
        set(MINIMUM_DRIVEABLE_PART_PIXEL_SIZE, values.nearPixels());
        set(MAXIMUM_DRIVEABLE_LOD_PART_PIXEL_SIZE, values.farPixels());
        set(DRIVEABLE_LOD_DETAIL_MULTIPLIER, values.detailMultiplier());
        set(DRIVEABLE_TRACK_LINK_LOD_PIXEL_SIZE, values.trackPixels());
        set(DRIVEABLE_TRACK_LINK_GROUPING_PIXEL_SIZE, values.groupedTrackPixels());
    }

    /** Changes only impostor controls; the shared master switch is enabled for this preset to take effect. */
    public static void applyImpostorPreset(RenderPreset preset)
    {
        if (preset == RenderPreset.CUSTOM)
            return;

        ImpostorValues values = impostorValues(preset);
        if (BooleanUtils.isNotTrue(ENABLE_DRIVEABLE_LOD.get()))
        {
            // Preserve disabled model/track LOD when enabling the shared switch for impostors.
            LodValues off = lodValues(RenderPreset.OFF);
            set(MINIMUM_DRIVEABLE_PART_PIXEL_SIZE, off.nearPixels());
            set(MAXIMUM_DRIVEABLE_LOD_PART_PIXEL_SIZE, off.farPixels());
            set(DRIVEABLE_LOD_DETAIL_MULTIPLIER, off.detailMultiplier());
            set(DRIVEABLE_TRACK_LINK_LOD_PIXEL_SIZE, off.trackPixels());
            set(DRIVEABLE_TRACK_LINK_GROUPING_PIXEL_SIZE, off.groupedTrackPixels());
        }
        set(ENABLE_DRIVEABLE_LOD, true);
        set(DRIVEABLE_IMPOSTOR_PIXEL_SIZE, values.pixels());
        set(DRIVEABLE_IMPOSTOR_MAXIMUM_DISTANCE, values.maximumDistance());
        if (preset != RenderPreset.OFF)
        {
            set(DRIVEABLE_IMPOSTOR_MINIMUM_DISTANCE, values.minimumDistance());
            set(DRIVEABLE_IMPOSTOR_QUALITY_MULTIPLIER, values.qualityMultiplier());
            set(DRIVEABLE_IMPOSTOR_RESOLUTION, values.resolution());
            set(DRIVEABLE_IMPOSTOR_YAW_ANGLES, values.yawAngles());
        }
    }

    /** Writes any deferred option changes to the config file and applies them. */
    public static void flushPendingChanges()
    {
        if (!pendingChanges.getAndSet(false) || !configSpec.isLoaded())
            return;

        configSpec.save();
        bake();
    }

    public static void bake()
    {
        ModClientConfig old = instance.get();
        instance.set(new ModClientConfig());

        if (old != null && old.enableUncensoredContent != get().enableUncensoredContent
            && PlatformEnvironment.isClient())
            UncensoredResources.reload();

        if (PlatformEnvironment.isClient()
            && (old == null
                || old.combineAmmoOnReload != get().combineAmmoOnReload
                || old.ammoToUpperInventoryOnReload != get().ammoToUpperInventoryOnReload))
            ReloadPreferencesSync.sendToServer();

        if (PlatformEnvironment.isClient() && (old == null || old.aimPose != get().aimPose))
            AimPoseClient.sendToServer();

        if (old == null)
            return;

        if (PlatformEnvironment.isClient() && old.enableGpuModelCache != get().enableGpuModelCache)
            GpuModelCache.clear();
        if (PlatformEnvironment.isClient() && old.gpuModelCacheMegabytes != get().gpuModelCacheMegabytes)
            GpuModelCache.budgetChanged();

        if (old.searchModelsInOtherContentPacks != get().searchModelsInOtherContentPacks
            || old.preferBuiltInModelClasses != get().preferBuiltInModelClasses
            || old.loadAllModelsInCache != get().loadAllModelsInCache && get().loadAllModelsInCache)
            ModelCache.reload();

        if (old.enableDriveableLod != get().enableDriveableLod
            || old.driveableImpostorQualityMultiplier != get().driveableImpostorQualityMultiplier
            || old.driveableImpostorResolution != get().driveableImpostorResolution
            || old.driveableImpostorYawAngles != get().driveableImpostorYawAngles
            || old.driveableImpostorCacheEntries != get().driveableImpostorCacheEntries)
            DriveableImpostorCache.clear();
    }
}
