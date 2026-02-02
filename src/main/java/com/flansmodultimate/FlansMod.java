package com.flansmodultimate;

import com.flansmodultimate.common.block.GunWorkbenchBlock;
import com.flansmodultimate.common.block.PaintjobTableBlock;
import com.flansmodultimate.common.block.entity.PaintjobTableBlockEntity;
import com.flansmodultimate.common.entity.Bullet;
import com.flansmodultimate.common.entity.DeployedGun;
import com.flansmodultimate.common.entity.Grenade;
import com.flansmodultimate.common.entity.GunItemEntity;
import com.flansmodultimate.common.entity.Shootable;
import com.flansmodultimate.common.inventory.GunWorkbenchMenu;
import com.flansmodultimate.common.inventory.PaintjobTableMenu;
import com.flansmodultimate.common.teams.TeamsManager;
import com.flansmodultimate.common.types.EnumType;
import com.flansmodultimate.common.types.TypeFile;
import com.flansmodultimate.config.CategoryManager;
import com.flansmodultimate.config.ModClientConfig;
import com.flansmodultimate.config.ModCommonConfig;
import com.mojang.logging.LogUtils;
import lombok.Getter;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixins;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

@Mod(FlansMod.MOD_ID)
public class FlansMod
{
    public static final String MOD_ID = "flansmodultimate";
    public static final String FLANSMOD_ID = "flansmod";

    public static final Logger log = LogUtils.getLogger();
    public static final TeamsManager teamsManager = new TeamsManager();

    // Sounds and Textures
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

    // Resource Locations
    public static final ResourceLocation paintjob = ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "paintjob");
    public static final ResourceLocation defaultMuzzleFlashTexture = ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "textures/skins/defaultmuzzleflash.png");
    public static final ResourceLocation hitmarkerTexture = ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "textures/gui/basic_hitmarker.png");
    public static final ResourceLocation gunWorkbenchGuiTexture = ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "textures/gui/gun_workbench.png");
    public static final ResourceLocation paintjobTableGuiTexture = ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "textures/gui/paintjob_table.png");

    // Registries
    private static final DeferredRegister<Block> blockRegistry = DeferredRegister.create(ForgeRegistries.BLOCKS, FlansMod.FLANSMOD_ID);
    private static final DeferredRegister<Item> itemRegistry = DeferredRegister.create(ForgeRegistries.ITEMS, FlansMod.FLANSMOD_ID);
    public static final DeferredRegister<MenuType<?>> menuRegistry = DeferredRegister.create(ForgeRegistries.MENU_TYPES, FlansMod.MOD_ID);
    private static final DeferredRegister<ParticleType<?>> particleRegistry = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, FlansMod.FLANSMOD_ID);
    private static final DeferredRegister<SoundEvent> soundEventRegistry = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, FlansMod.FLANSMOD_ID);
    private static final DeferredRegister<CreativeModeTab> creativeModeTabRegistry = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FlansMod.MOD_ID);
    private static final DeferredRegister<EntityType<?>> entityRegistry = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, FlansMod.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> blockEntityRegistry = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, FlansMod.MOD_ID);

    // Blocks
    public static final RegistryObject<Block> gunWorkbench = blockRegistry.register("gunworkbench", () -> new GunWorkbenchBlock(BlockBehaviour.Properties.of()
        .mapColor(MapColor.METAL)
        .strength(3F, 6F)
        .sound(SoundType.METAL)
        .requiresCorrectToolForDrops()
        .pushReaction(PushReaction.BLOCK))
    );
    public static final RegistryObject<Block> paintjobTable = blockRegistry.register("paintjobtable", () -> new PaintjobTableBlock(BlockBehaviour.Properties.of()
        .strength(2F, 4F)
        .sound(SoundType.STONE))
    );

    // Block Entities
    public static final RegistryObject<BlockEntityType<PaintjobTableBlockEntity>> paintjobTableEntity = blockEntityRegistry.register("paintjobtable", () -> BlockEntityType.Builder.of(PaintjobTableBlockEntity::new, paintjobTable.get()).build(null));

    // Items
    public static final RegistryObject<Item> rainbowPaintcan = itemRegistry.register("rainbowpaintcan", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> gunWorkbenchItem = itemRegistry.register("gunworkbench", () -> new BlockItem(gunWorkbench.get(), new Item.Properties()));
    public static final RegistryObject<Item> paintjobTableItem = itemRegistry.register("paintjobtable", () -> new BlockItem(paintjobTable.get(), new Item.Properties()));

    // Menus
    public static final RegistryObject<MenuType<GunWorkbenchMenu>> gunWorkbenchMenu = menuRegistry.register("gunworkbench_menu", () -> IForgeMenuType.create(
        (int windowId, Inventory inv, FriendlyByteBuf buf) -> new GunWorkbenchMenu(windowId, inv, ContainerLevelAccess.create(inv.player.level(), buf.readBlockPos())))
    );
    public static final RegistryObject<MenuType<PaintjobTableMenu>> paintjobTableMenu = menuRegistry.register("paintjob_table", () -> IForgeMenuType.create(PaintjobTableMenu::fromNetwork));

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

    // Entities
    public static final RegistryObject<EntityType<Bullet>> bulletEntity = entityRegistry.register("bullet", () -> EntityType.Builder.<Bullet>of(Bullet::new, MobCategory.MISC)
        .sized(Shootable.DEFAULT_HITBOX_SIZE, Shootable.DEFAULT_HITBOX_SIZE)
        .clientTrackingRange(Bullet.RENDER_DISTANCE)
        .updateInterval(20)
        .setShouldReceiveVelocityUpdates(true)
        .build(ResourceLocation.fromNamespaceAndPath(MOD_ID, "bullet").toString())
    );
    public static final RegistryObject<EntityType<Grenade>> grenadeEntity = entityRegistry.register("grenade", () -> EntityType.Builder.<Grenade>of(Grenade::new, MobCategory.MISC)
        .sized(Shootable.DEFAULT_HITBOX_SIZE, Shootable.DEFAULT_HITBOX_SIZE)
        .clientTrackingRange(Grenade.RENDER_DISTANCE)
        .updateInterval(100)
        .setShouldReceiveVelocityUpdates(true)
        .build(ResourceLocation.fromNamespaceAndPath(MOD_ID, "grenade").toString())
    );
    public static final RegistryObject<EntityType<DeployedGun>> deployedGunEntity = entityRegistry.register("deployed_gun", () -> EntityType.Builder.<DeployedGun>of(DeployedGun::new, MobCategory.MISC)
        .sized(DeployedGun.DEFAULT_HITBOX_SIZE, DeployedGun.DEFAULT_HITBOX_SIZE)
        .clientTrackingRange(DeployedGun.RENDER_DISTANCE)
        .updateInterval(2)
        .setShouldReceiveVelocityUpdates(true)
        .build(ResourceLocation.fromNamespaceAndPath(MOD_ID, "deployed_gun").toString())
    );
    public static final RegistryObject<EntityType<GunItemEntity>> gunItemEntity = entityRegistry.register("gun_item", () -> EntityType.Builder.<GunItemEntity>of(GunItemEntity::new, MobCategory.MISC)
        .sized(1F, 1F)
        .clientTrackingRange(16)
        .updateInterval(20)
        .build("gun_item")
    );

    private static final Map<EnumType, List<RegistryObject<Item>>> items = new EnumMap<>(EnumType.class);
    private static final Map<ResourceLocation, RegistryObject<SoundEvent>> sounds = new HashMap<>();
    @Getter
    private static final Map<ResourceLocation, TypeFile> soundsOrigins = new HashMap<>();

    public FlansMod(FMLJavaModLoadingContext context)
    {
        Arrays.stream(EnumType.values()).forEach(type -> items.put(type, new ArrayList<>()));
        Mixins.addConfiguration(MOD_ID + ".mixins.json");

        IEventBus modEventBus = context.getModEventBus();

        // Init Configs
        context.registerConfig(ModConfig.Type.COMMON, ModCommonConfig.configSpec);
        context.registerConfig(ModConfig.Type.CLIENT, ModClientConfig.configSpec);

        // Init Registries
        blockRegistry.register(modEventBus);
        blockEntityRegistry.register(modEventBus);
        itemRegistry.register(modEventBus);
        particleRegistry.register(modEventBus);
        soundEventRegistry.register(modEventBus);
        creativeModeTabRegistry.register(modEventBus);
        entityRegistry.register(modEventBus);
        menuRegistry.register(modEventBus);

        // Register Everything
        CategoryManager.loadAll();
        ContentManager.findContentInFlanFolder();
        ContentManager.readContentPacks();
        registerSounds();
        registerCreativeModeTabs();

        MinecraftForge.EVENT_BUS.register(this);
    }

    private static void registerCreativeModeTabs()
    {
        ResourceKey<CreativeModeTab> creativeTabMainKey = ResourceKey.create(Registries.CREATIVE_MODE_TAB, ResourceLocation.fromNamespaceAndPath(MOD_ID, "creative_tab_flansmod"));
        ResourceKey<CreativeModeTab>[] creativeTabsFlansModReloadedKey = new ResourceKey[]
        {
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, ResourceLocation.fromNamespaceAndPath(FLANSMOD_ID, "creative_tab_guns")),
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, ResourceLocation.fromNamespaceAndPath(FLANSMOD_ID, "creative_tab_modifiers")),
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, ResourceLocation.fromNamespaceAndPath(FLANSMOD_ID, "creative_tab_parts")),
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, ResourceLocation.fromNamespaceAndPath(FLANSMOD_ID, "creative_tab_bullets"))
        };

        CreativeTabs.registerCreativeTab(FlansMod.creativeModeTabRegistry, "creative_tab_flansmod", List.of(FlansMod.gunWorkbenchItem, FlansMod.rainbowPaintcan, FlansMod.paintjobTableItem), false, false, CreativeModeTabs.SPAWN_EGGS, creativeTabsFlansModReloadedKey);
        CreativeTabs.registerCreativeTab(FlansMod.creativeModeTabRegistry, "creative_tab_armors", FlansMod.getItems(EnumType.ARMOR), false, false, creativeTabMainKey, creativeTabsFlansModReloadedKey);
        CreativeTabs.registerCreativeTab(FlansMod.creativeModeTabRegistry, "creative_tab_attachments", FlansMod.getItems(EnumType.ATTACHMENT), false, false, creativeTabMainKey, creativeTabsFlansModReloadedKey);
        CreativeTabs.registerCreativeTab(FlansMod.creativeModeTabRegistry, "creative_tab_guns", FlansMod.getItems(EnumSet.of(EnumType.GUN, EnumType.BULLET, EnumType.GRENADE)), true, false, creativeTabMainKey, creativeTabsFlansModReloadedKey);
        CreativeTabs.registerCreativeTab(FlansMod.creativeModeTabRegistry, "creative_tab_vehicles", FlansMod.getItems(EnumType.BULLET), false, true, creativeTabMainKey, creativeTabsFlansModReloadedKey);
    }

    private static void registerSounds()
    {
        registerSound(SOUND_EMPTY_CLICK, null);
        registerSound(SOUND_DEFAULT_SHELL_INSERT, null);
        registerSound(SOUND_IMPACT_DIRT, null);
        registerSound(SOUND_IMPACT_METAL, null);
        registerSound(SOUND_IMPACT_BRICKS, null);
        registerSound(SOUND_IMPACT_GLASS, null);
        registerSound(SOUND_IMPACT_ROCK, null);
        registerSound(SOUND_IMPACT_WOOD, null);
        registerSound(SOUND_IMPACT_WATER, null);
        registerSound(SOUND_BULLET, null);
        registerSound(SOUND_BULLETFLYBY, null);
        registerSound(SOUND_UNLOCKNOTCH, null);
        registerSound(SOUND_SKULLBOSSLAUGH, null);
        registerSound(SOUND_SKULLBOSSSPAWN, null);
    }

    public static void registerItem(String itemName, EnumType type, Supplier<? extends Item> initItem)
    {
        items.get(type).add(itemRegistry.register(itemName, initItem));
    }

    public static void registerSound(String soundName, @Nullable TypeFile typeFile)
    {
        ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, soundName);
        if (sounds.containsKey(rl))
            return;

        RegistryObject<SoundEvent> soundEvent = soundEventRegistry.register(soundName, () -> SoundEvent.createVariableRangeEvent(rl));
        sounds.put(rl, soundEvent);
        if (typeFile != null)
            soundsOrigins.put(rl, typeFile);
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
        ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, soundName);
        return Optional.ofNullable(sounds.get(rl));
    }
}
