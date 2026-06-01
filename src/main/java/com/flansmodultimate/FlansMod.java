package com.flansmodultimate;

import com.flansmodultimate.common.block.GunWorkbenchBlock;
import com.flansmodultimate.common.block.PaintjobTableBlock;
import com.flansmodultimate.common.block.entity.PaintjobTableBlockEntity;
import com.flansmodultimate.common.digitalammo.DigitalAmmoCommand;
import com.flansmodultimate.common.digitalammo.DigitalAmmoSupplyHandler;
import com.flansmodultimate.common.entity.AAGun;
import com.flansmodultimate.common.entity.Bullet;
import com.flansmodultimate.common.entity.DeployedGun;
import com.flansmodultimate.common.entity.Grenade;
import com.flansmodultimate.common.entity.GunItemEntity;
import com.flansmodultimate.common.entity.Shootable;
import com.flansmodultimate.common.inventory.ArmorBoxMenu;
import com.flansmodultimate.common.inventory.GunBoxMenu;
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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.slf4j.Logger;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

@Mod(FlansMod.MOD_ID)
public class FlansMod
{
    public static final String MOD_ID = "flansmodultimate";
    public static final String FLANSMOD_ID = "flansmod";
    public static final String PACKS_ID = "flansmodultimate_packs";

    public static final Logger log = LogUtils.getLogger();
    public static final TeamsManager teamsManager = new TeamsManager();

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
    public static final ResourceLocation defaultMuzzleFlashTexture = ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "textures/skins/defaultmuzzleflash.png");
    public static final ResourceLocation hitmarkerTexture = ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "textures/gui/basic_hitmarker.png");
    public static final ResourceLocation gunWorkbenchGuiTexture = ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "textures/gui/gun_workbench.png");
    public static final ResourceLocation paintjobTableGuiTexture = ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "textures/gui/paintjob_table.png");
    public static final ResourceLocation armorBoxGuiTexture = ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "textures/gui/armour_box.png");
    public static final ResourceLocation gunBoxGuiTexture = ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "textures/gui/weaponboxdefault.png");

    private static final DeferredRegister<Block> blockRegistry = DeferredRegister.create(BuiltInRegistries.BLOCK, FlansMod.FLANSMOD_ID);
    private static final DeferredRegister<Item> itemRegistry = DeferredRegister.create(BuiltInRegistries.ITEM, FlansMod.FLANSMOD_ID);
    public static final DeferredRegister<MenuType<?>> menuRegistry = DeferredRegister.create(BuiltInRegistries.MENU, FlansMod.MOD_ID);
    private static final DeferredRegister<ParticleType<?>> particleRegistry = DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, FlansMod.FLANSMOD_ID);
    private static final DeferredRegister<SoundEvent> soundEventRegistry = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, FlansMod.FLANSMOD_ID);
    private static final DeferredRegister<CreativeModeTab> creativeModeTabRegistry = DeferredRegister.create(BuiltInRegistries.CREATIVE_MODE_TAB, FlansMod.MOD_ID);
    private static final DeferredRegister<EntityType<?>> entityRegistry = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, FlansMod.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> blockEntityRegistry = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, FlansMod.MOD_ID);

    public static final DeferredHolder<Block, Block> gunWorkbench = blockRegistry.register("gunworkbench", () -> new GunWorkbenchBlock(BlockBehaviour.Properties.of()
        .mapColor(MapColor.METAL)
        .strength(3F, 6F)
        .sound(SoundType.METAL)
        .requiresCorrectToolForDrops()
        .pushReaction(PushReaction.BLOCK))
    );
    public static final DeferredHolder<Block, Block> paintjobTable = blockRegistry.register("paintjobtable", () -> new PaintjobTableBlock(BlockBehaviour.Properties.of()
        .strength(2F, 4F)
        .sound(SoundType.STONE))
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PaintjobTableBlockEntity>> paintjobTableEntity = blockEntityRegistry.register("paintjobtable", () -> BlockEntityType.Builder.of(PaintjobTableBlockEntity::new, paintjobTable.get()).build(null));

    public static final DeferredHolder<Item, Item> rainbowPaintcan = itemRegistry.register("rainbowpaintcan", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> gunWorkbenchItem = itemRegistry.register("gunworkbench", () -> new BlockItem(gunWorkbench.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> paintjobTableItem = itemRegistry.register("paintjobtable", () -> new BlockItem(paintjobTable.get(), new Item.Properties()));

    public static final DeferredHolder<MenuType<?>, MenuType<GunWorkbenchMenu>> gunWorkbenchMenu = menuRegistry.register("gunworkbench_menu", () -> new MenuType<>((int windowId, net.minecraft.world.entity.player.Inventory inv) -> new GunWorkbenchMenu(windowId, inv, net.minecraft.core.BlockPos.ZERO), net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<MenuType<?>, MenuType<PaintjobTableMenu>> paintjobTableMenu = menuRegistry.register("paintjob_table_menu", () -> new MenuType<PaintjobTableMenu>((net.neoforged.neoforge.network.IContainerFactory<PaintjobTableMenu>) (int id, net.minecraft.world.entity.player.Inventory inv, net.minecraft.network.RegistryFriendlyByteBuf buf) -> PaintjobTableMenu.createFromNetwork(id, inv, buf), net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<MenuType<?>, MenuType<ArmorBoxMenu>> armorBoxMenu = menuRegistry.register("armorbox_menu", () -> new MenuType<ArmorBoxMenu>((net.neoforged.neoforge.network.IContainerFactory<ArmorBoxMenu>) (int id, net.minecraft.world.entity.player.Inventory inv, net.minecraft.network.RegistryFriendlyByteBuf buf) -> ArmorBoxMenu.createFromNetwork(id, inv, buf), net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<MenuType<?>, MenuType<GunBoxMenu>> gunBoxMenu = menuRegistry.register("gunbox_menu", () -> new MenuType<GunBoxMenu>((net.neoforged.neoforge.network.IContainerFactory<GunBoxMenu>) (int id, net.minecraft.world.entity.player.Inventory inv, net.minecraft.network.RegistryFriendlyByteBuf buf) -> GunBoxMenu.createFromNetwork(id, inv, buf), net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> afterburnParticle = particleRegistry.register("afterburn", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> bigSmokeParticle = particleRegistry.register("big_smoke", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> debris1Particle = particleRegistry.register("debris_1", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> flareParticle = particleRegistry.register("flare", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> flashParticle = particleRegistry.register("flash", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> fmFlameParticle = particleRegistry.register("fm_flame", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> fmMuzzleFlashParticle = particleRegistry.register("fm_muzzle_flash", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> fmSmokeParticle = particleRegistry.register("fm_smoke", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> fmTracerParticle = particleRegistry.register("fm_tracer", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> fmTracerGreenParticle = particleRegistry.register("fm_tracer_green", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> fmTracerRedParticle = particleRegistry.register("fm_tracer_red", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> rocketExhaustParticle = particleRegistry.register("rocket_exhaust", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> smokeBurstParticle = particleRegistry.register("smoke_burst", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> smokeGrenadeParticle = particleRegistry.register("smoke_grenade", () -> new SimpleParticleType(false));

    public static final DeferredHolder<EntityType<?>, EntityType<Bullet>> bulletEntity = entityRegistry.register("bullet", () -> EntityType.Builder.<Bullet>of(Bullet::new, MobCategory.MISC)
        .sized(Shootable.DEFAULT_HITBOX_SIZE, Shootable.DEFAULT_HITBOX_SIZE)
        .clientTrackingRange(Bullet.RENDER_DISTANCE)
        .updateInterval(1)
        .setShouldReceiveVelocityUpdates(true)
        .build(ResourceLocation.fromNamespaceAndPath(MOD_ID, "bullet").toString())
    );
    public static final DeferredHolder<EntityType<?>, EntityType<Grenade>> grenadeEntity = entityRegistry.register("grenade", () -> EntityType.Builder.<Grenade>of(Grenade::new, MobCategory.MISC)
        .sized(Shootable.DEFAULT_HITBOX_SIZE, Shootable.DEFAULT_HITBOX_SIZE)
        .clientTrackingRange(Grenade.RENDER_DISTANCE)
        .updateInterval(2)
        .setShouldReceiveVelocityUpdates(true)
        .build(ResourceLocation.fromNamespaceAndPath(MOD_ID, "grenade").toString())
    );
    public static final DeferredHolder<EntityType<?>, EntityType<DeployedGun>> deployedGunEntity = entityRegistry.register("deployed_gun", () -> EntityType.Builder.<DeployedGun>of(DeployedGun::new, MobCategory.MISC)
        .sized(DeployedGun.DEFAULT_HITBOX_SIZE, DeployedGun.DEFAULT_HITBOX_SIZE)
        .clientTrackingRange(DeployedGun.RENDER_DISTANCE)
        .updateInterval(2)
        .setShouldReceiveVelocityUpdates(true)
        .build(ResourceLocation.fromNamespaceAndPath(MOD_ID, "deployed_gun").toString())
    );
    public static final DeferredHolder<EntityType<?>, EntityType<AAGun>> aaGunEntity = entityRegistry.register("aa_gun", () -> EntityType.Builder.<AAGun>of(AAGun::new, MobCategory.MISC)
        .sized(AAGun.DEFAULT_HITBOX_SIZE, AAGun.DEFAULT_HITBOX_SIZE)
        .clientTrackingRange(AAGun.RENDER_DISTANCE)
        .updateInterval(2)
        .setShouldReceiveVelocityUpdates(true)
        .build(ResourceLocation.fromNamespaceAndPath(MOD_ID, "aa_gun").toString())
    );
    public static final DeferredHolder<EntityType<?>, EntityType<GunItemEntity>> gunItemEntity = entityRegistry.register("gun_item", () -> EntityType.Builder.<GunItemEntity>of(GunItemEntity::new, MobCategory.MISC)
        .sized(1F, 1F)
        .clientTrackingRange(16)
        .updateInterval(20)
        .build("gun_item")
    );

    private static final Map<EnumType, List<DeferredHolder<Item, ? extends Item>>> items = new EnumMap<>(EnumType.class);
    @Getter
    private static final Map<EnumType, Map<String, DeferredHolder<Block, ? extends Block>>> blocks = new EnumMap<>(EnumType.class);
    private static final Map<ResourceLocation, DeferredHolder<SoundEvent, SoundEvent>> sounds = new HashMap<>();
    @Getter
    private static final Map<ResourceLocation, TypeFile> soundsOrigins = new HashMap<>();

    public FlansMod(IEventBus modEventBus, ModContainer modContainer)
    {
        Arrays.stream(EnumType.values()).filter(EnumType::isHasItem).forEach(type -> items.put(type, new ArrayList<>()));
        Arrays.stream(EnumType.values()).filter(EnumType::isHasBlock).forEach(type -> blocks.put(type, new HashMap<>()));

        modContainer.registerConfig(ModConfig.Type.COMMON, ModCommonConfig.configSpec);
        modContainer.registerConfig(ModConfig.Type.CLIENT, ModClientConfig.configSpec);

        blockRegistry.register(modEventBus);
        blockEntityRegistry.register(modEventBus);
        itemRegistry.register(modEventBus);
        particleRegistry.register(modEventBus);
        soundEventRegistry.register(modEventBus);
        creativeModeTabRegistry.register(modEventBus);
        entityRegistry.register(modEventBus);
        menuRegistry.register(modEventBus);

        waitForPacksExtractionIfPresent();

        CategoryManager.loadAll();
        ContentManager.searchForContentPacksInModsFolder();
        ContentManager.findContentInFlanFolder();
        ContentManager.readContentPacks();
        registerSounds();
        registerCreativeModeTabs();

        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event)
    {
        DigitalAmmoCommand.register(event.getDispatcher());
        DigitalAmmoSupplyHandler.reloadSupplyBlocks();
    }

    private static void waitForPacksExtractionIfPresent()
    {
        if (!ModList.get().isLoaded(PACKS_ID))
            return;

        log.info("Flan's Mod Ultimate Packs Extractor found. Waiting for extraction...");

        String version = ModList.get().getModContainerById(PACKS_ID)
            .map(c -> c.getModInfo().getVersion().toString())
            .orElse("unknown");

        String safeVersion = version.replaceAll("[^A-Za-z0-9._-]", "_");
        Path flanDir = FMLPaths.GAMEDIR.get().resolve("flan");
        Path marker = flanDir.resolve(".extracted_" + PACKS_ID + "_" + safeVersion + ".marker");

        long deadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(120);
        while (!Files.exists(marker))
        {
            if (System.nanoTime() > deadlineNanos)
            {
                log.error("Timed out waiting for packs extraction marker: {}", marker);
                return;
            }
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(25));
        }

        log.info("Packs extraction marker found: {}", marker.getFileName());
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

        List<DeferredHolder<Item, ? extends Item>> generalItemList = new ArrayList<>();
        generalItemList.add(FlansMod.gunWorkbenchItem);
        generalItemList.add(FlansMod.paintjobTableItem);
        generalItemList.add(FlansMod.rainbowPaintcan);
        generalItemList.addAll(FlansMod.getItems(EnumSet.of(EnumType.ARMOR_BOX, EnumType.GUN_BOX, EnumType.AAGUN)));

        @SuppressWarnings("unchecked")
        List<Supplier<Item>> generalItems = (List<Supplier<Item>>) (List<?>) generalItemList;

        CreativeTabs.registerCreativeTab(FlansMod.creativeModeTabRegistry, "creative_tab_flansmod", generalItems, false, false, CreativeModeTabs.SPAWN_EGGS, creativeTabsFlansModReloadedKey);
        CreativeTabs.registerCreativeTab(FlansMod.creativeModeTabRegistry, "creative_tab_armors", castToSupplierList(FlansMod.getItems(EnumType.ARMOR)), false, false, creativeTabMainKey, creativeTabsFlansModReloadedKey);
        CreativeTabs.registerCreativeTab(FlansMod.creativeModeTabRegistry, "creative_tab_attachments", castToSupplierList(FlansMod.getItems(EnumType.ATTACHMENT)), false, false, creativeTabMainKey, creativeTabsFlansModReloadedKey);
        CreativeTabs.registerCreativeTab(FlansMod.creativeModeTabRegistry, "creative_tab_guns", castToSupplierList(FlansMod.getItems(EnumSet.of(EnumType.GUN, EnumType.BULLET))), true, false, creativeTabMainKey, creativeTabsFlansModReloadedKey);
        CreativeTabs.registerCreativeTab(FlansMod.creativeModeTabRegistry, "creative_tab_grenades", castToSupplierList(FlansMod.getItems(EnumType.GRENADE)), false, false, creativeTabMainKey, creativeTabsFlansModReloadedKey);
        CreativeTabs.registerCreativeTab(FlansMod.creativeModeTabRegistry, "creative_tab_tools", castToSupplierList(FlansMod.getItems(EnumType.TOOLS)), false, false, creativeTabMainKey, creativeTabsFlansModReloadedKey);
        CreativeTabs.registerCreativeTab(FlansMod.creativeModeTabRegistry, "creative_tab_vehicles", castToSupplierList(FlansMod.getItems(EnumType.BULLET)), false, true, creativeTabMainKey, creativeTabsFlansModReloadedKey);
        CreativeTabs.registerCreativeTab(FlansMod.creativeModeTabRegistry, "creative_tab_parts", castToSupplierList(FlansMod.getItems(EnumType.PARTS)), false, false, creativeTabMainKey, creativeTabsFlansModReloadedKey);
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

    public static void registerItem(String itemName, EnumType type, java.util.function.Supplier<? extends Item> initItem)
    {
        items.get(type).add(itemRegistry.register(itemName, initItem));
    }

    public static void registerBlock(String blockName, EnumType type, java.util.function.Supplier<? extends Block> initItem)
    {
        blocks.get(type).put(blockName, blockRegistry.register(blockName, initItem));
    }

    public static void registerSound(String soundName, @Nullable TypeFile typeFile)
    {
        ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, soundName);
        if (sounds.containsKey(rl))
            return;

        DeferredHolder<SoundEvent, SoundEvent> soundEvent = soundEventRegistry.register(soundName, () -> SoundEvent.createVariableRangeEvent(rl));
        sounds.put(rl, soundEvent);
        if (typeFile != null)
            soundsOrigins.put(rl, typeFile);
    }

    @Unmodifiable
    public static List<DeferredHolder<Item, ? extends Item>> getItems()
    {
        return items.values().stream().flatMap(List::stream).toList();
    }

    public static List<DeferredHolder<Item, ? extends Item>> getItems(EnumType type)
    {
        return items.get(type);
    }

    @Unmodifiable
    public static List<DeferredHolder<Item, ? extends Item>> getItems(Set<EnumType> types)
    {
        return types.stream().map(items::get).flatMap(List::stream).toList();
    }

    public static Optional<DeferredHolder<SoundEvent, SoundEvent>> getSoundEvent(String soundName)
    {
        ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, soundName.toLowerCase(Locale.ROOT));
        return Optional.ofNullable(sounds.get(rl));
    }

    @SuppressWarnings("unchecked")
    private static List<Supplier<Item>> castToSupplierList(List<?> list)
    {
        return (List<Supplier<Item>>) list;
    }
}