package com.flansmodultimate;

import com.flansmodultimate.common.entity.Bullet;
import com.flansmodultimate.common.entity.Grenade;
import com.flansmodultimate.common.entity.Shootable;
import com.flansmodultimate.common.teams.TeamsManager;
import com.flansmodultimate.common.types.EnumType;
import com.flansmodultimate.config.ModClientConfigs;
import com.flansmodultimate.config.ModCommonConfigs;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixins;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

@Mod(FlansMod.MOD_ID)
public class FlansMod
{
    //TODO: check for conflicts with Flan's Mod Reloaded
    public static final String MOD_ID = "flansmodultimate";
    public static final String FLANSMOD_ID = "flansmod";

    // Range for which sound packets are sent
    public static final float SOUND_RANGE = 64F;
    public static final float SOUND_VOLUME = SOUND_RANGE / 16F;

    public static final String SOUND_EMPTY_CLICK = "emptyclick";
    public static final String SOUND_DEFAULT_SHELL_INSERT = "defaultshellinsert";
    public static final String SOUND_IMPACT_DIRT = "impact_dirt";
    public static final String SOUND_IMPACT_METAL = "impact_metal";
    public static final String SOUND_IMPACT_BRICKS = "impact_bricks";
    public static final String SOUND_IMPACT_GLASS = "impact_glass";
    public static final String SOUND_IMPACT_ROCK = "impact_rock";
    public static final String SOUND_IMPACT_WOOD = "impact_wood";
    public static final String SOUND_IMPACT_WATER = "impact_water";
    public static final String SOUND_BULLET = "bullet";
    public static final String SOUND_BULLETFLYBY = "bulletflyby";
    public static final String SOUND_UNLOCKNOTCH = "unlocknotch";
    public static final String SOUND_SKULLBOSSLAUGH = "skullboss_laugh";
    public static final String SOUND_SKULLBOSSSPAWN = "skullboss_spawn";
    public static final String DEFAULT_BULLET_TEXTURE = "defaultbullet";
    public static final String DEFAULT_BULLET_TRAIL_TEXTURE = "defaultbullettrail";

    public static final ResourceLocation paintjob = ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "paintjob");
    public static final ResourceLocation muzzleFlashTexture = ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "textures/skins/muzzleflash.png");
    public static final ResourceLocation hitmarkerTexture = ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "textures/gui/hitmarker.png");

    public static final Logger log = LogUtils.getLogger();
    //TODO: Make forceRecompileAllPacks configurable (does not work with mod config)
    //TODO: forceRecompileAllPacks to true if mod version changed compared to last start up
    //TODO: unzip/rezip in separate temp file to make sure the process can be safely interrupted
    public static final boolean FORCE_RECOMPILE_ALL_PACKS = false;

    public static final TeamsManager teamsManager = new TeamsManager();

    // Registries
    private static final DeferredRegister<Item> itemRegistry = DeferredRegister.create(ForgeRegistries.ITEMS, FlansMod.FLANSMOD_ID);
    private static final DeferredRegister<CreativeModeTab> creativeModeTabRegistry = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FlansMod.MOD_ID);
    private static final DeferredRegister<EntityType<?>> entityRegistry = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, FlansMod.MOD_ID);
    private static final DeferredRegister<ParticleType<?>> particleRegistry = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, FlansMod.FLANSMOD_ID);
    private static final DeferredRegister<SoundEvent> soundEventRegistry = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, FlansMod.FLANSMOD_ID);

    // Items
    public static final RegistryObject<Item> rainbowPaintcan = itemRegistry.register("rainbow_paintcan", () -> new Item(new Item.Properties()));

    // Entities
    public static final RegistryObject<EntityType<Bullet>> bulletEntity = entityRegistry.register("bullet", () ->
        EntityType.Builder.<Bullet>of(Bullet::new, MobCategory.MISC)
            .sized(Shootable.DEFAULT_HITBOX_SIZE, Shootable.DEFAULT_HITBOX_SIZE)
            .clientTrackingRange(Bullet.RENDER_DISTANCE)
            .updateInterval(20)
            .build(ResourceLocation.fromNamespaceAndPath(MOD_ID, "bullet").toString()));
    public static final RegistryObject<EntityType<Grenade>> grenadeEntity = entityRegistry.register("grenade", () ->
        EntityType.Builder.<Grenade>of(Grenade::new, MobCategory.MISC)
            .sized(Shootable.DEFAULT_HITBOX_SIZE, Shootable.DEFAULT_HITBOX_SIZE)
            .clientTrackingRange(Grenade.RENDER_DISTANCE)
            .updateInterval(100)
            .build(ResourceLocation.fromNamespaceAndPath(MOD_ID, "grenade").toString()));

    // Particles
    public static final RegistryObject<SimpleParticleType> afterburnParticle = particleRegistry.register("afterburn", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> bigSmokeParticle = particleRegistry.register("big_smoke", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> debris1Particle = particleRegistry.register("debris_1", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> flareParticle = particleRegistry.register("flare", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> flashParticle = particleRegistry.register("flash", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> fmFlameParticle = particleRegistry.register("fm_flame", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> fmMuzzleFlashParticle = particleRegistry.register("fm_muzzle_flash", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> fmSmokeParticle = particleRegistry.register("fm_smoke", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> fmTracerParticle = particleRegistry.register("fm_tracer", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> fmTracerGreenParticle = particleRegistry.register("fm_tracer_green", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> fmTracerRedParticle = particleRegistry.register("fm_tracer_red", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> rocketExhaustParticle = particleRegistry.register("rocket_exhaust", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> smokeBurstParticle = particleRegistry.register("smoke_burst", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> smokeGrenadeParticle = particleRegistry.register("smoke_grenade", () -> new SimpleParticleType(false));

    private static final Map<EnumType, List<RegistryObject<Item>>> items = new EnumMap<>(EnumType.class);
    private static final Map<String, RegistryObject<SoundEvent>> sounds = new HashMap<>();

    public FlansMod(FMLJavaModLoadingContext context)
    {
        Arrays.stream(EnumType.values()).forEach(type -> items.put(type, new ArrayList<>()));
        Mixins.addConfiguration(MOD_ID + ".mixins.json");

        IEventBus modEventBus = context.getModEventBus();

        // Configs
        context.registerConfig(ModConfig.Type.COMMON, ModCommonConfigs.CONFIG);
        context.registerConfig(ModConfig.Type.CLIENT, ModClientConfigs.CONFIG);

        // Registries
        itemRegistry.register(modEventBus);
        particleRegistry.register(modEventBus);
        soundEventRegistry.register(modEventBus);
        creativeModeTabRegistry.register(modEventBus);
        entityRegistry.register(modEventBus);

        // Read content packs and register items & sounds
        ContentManager.findContentInFlanFolder();
        ContentManager.readContentPacks();

        CreativeTabs.registerCreativeModeTabs(creativeModeTabRegistry);
        registerSound(SOUND_EMPTY_CLICK);
        registerSound(SOUND_DEFAULT_SHELL_INSERT);
        registerSound(SOUND_IMPACT_DIRT);
        registerSound(SOUND_IMPACT_METAL);
        registerSound(SOUND_IMPACT_BRICKS);
        registerSound(SOUND_IMPACT_GLASS);
        registerSound(SOUND_IMPACT_ROCK);
        registerSound(SOUND_IMPACT_WOOD);
        registerSound(SOUND_IMPACT_WATER);
        registerSound(SOUND_BULLET);
        registerSound(SOUND_BULLETFLYBY);
        registerSound(SOUND_UNLOCKNOTCH);
        registerSound(SOUND_SKULLBOSSLAUGH);
        registerSound(SOUND_SKULLBOSSSPAWN);

        MinecraftForge.EVENT_BUS.register(this);
    }

    public static void registerItem(String itemName, EnumType type, Supplier<? extends Item> initItem)
    {
        items.get(type).add(itemRegistry.register(itemName, initItem));
    }

    public static void registerSound(String soundName)
    {
        if (sounds.containsKey(soundName))
            return;

        RegistryObject<SoundEvent> soundEvent = soundEventRegistry.register(soundName, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, soundName)));
        sounds.put(soundName, soundEvent);
    }

    public static List<RegistryObject<Item>> getItems()
    {
        return items.values().stream().flatMap(List::stream).toList();
    }

    public static List<RegistryObject<Item>> getItems(EnumType type)
    {
        return items.get(type);
    }

    public static List<RegistryObject<Item>> getItems(Set<EnumType> types)
    {
        return types.stream().map(items::get).flatMap(List::stream).toList();
    }

    public static Optional<RegistryObject<SoundEvent>> getSoundEvent(String soundName)
    {
        return Optional.ofNullable(sounds.get(soundName));
    }
}
