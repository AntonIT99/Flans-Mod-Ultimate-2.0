package com.flansmodultimate;

import com.flansmodultimate.apocalyse.ApocalypseContent;
import com.flansmodultimate.common.block.GunWorkbenchBlock;
import com.flansmodultimate.common.block.PaintjobTableBlock;
import com.flansmodultimate.common.block.TeamSpawnerBlock;
import com.flansmodultimate.common.block.entity.ItemHolderBlockEntity;
import com.flansmodultimate.common.block.entity.PaintjobTableBlockEntity;
import com.flansmodultimate.common.block.entity.TeamSpawnerBlockEntity;
import com.flansmodultimate.common.entity.AAGun;
import com.flansmodultimate.common.entity.Bullet;
import com.flansmodultimate.common.entity.DeployedGun;
import com.flansmodultimate.common.entity.Flag;
import com.flansmodultimate.common.entity.Flagpole;
import com.flansmodultimate.common.entity.Grenade;
import com.flansmodultimate.common.entity.GunItemEntity;
import com.flansmodultimate.common.entity.Mecha;
import com.flansmodultimate.common.entity.Parachute;
import com.flansmodultimate.common.entity.Plane;
import com.flansmodultimate.common.entity.Seat;
import com.flansmodultimate.common.entity.Shootable;
import com.flansmodultimate.common.entity.Vehicle;
import com.flansmodultimate.common.entity.Wheel;
import com.flansmodultimate.common.inventory.ArmorBoxMenu;
import com.flansmodultimate.common.inventory.DriveableCraftingMenu;
import com.flansmodultimate.common.inventory.DriveableInventoryMenu;
import com.flansmodultimate.common.inventory.GunBoxMenu;
import com.flansmodultimate.common.inventory.GunWorkbenchMenu;
import com.flansmodultimate.common.inventory.PaintjobTableMenu;
import com.flansmodultimate.common.item.FlagpoleItem;
import com.flansmodultimate.common.item.ItemOpStick;
import com.flansmodultimate.common.teams.TeamsManager;
import com.flansmodultimate.common.types.EnumType;
import com.flansmodultimate.common.types.TypeFile;
import com.flansmodultimate.config.CategoryManager;
import com.flansmodultimate.config.ModApocalypseConfig;
import com.flansmodultimate.config.ModClientConfig;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.platform.neoforge.NeoForgeChunkTickets;
import com.flansmodultimate.util.ModLogFile;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.mojang.logging.LogUtils;
import lombok.Getter;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.slf4j.Logger;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;
import java.util.function.Function;

@Mod(FlansMod.MOD_ID)
public class FlansMod
{
    public static final String MOD_ID = "flansmodultimate";
    public static final String FLANSMOD_ID = "flansmod";
    public static final String APOCALYPSE_ID = "flansmodapocalypse";
    public static final String PACKS_ID = "flansmodultimate_packs";
    private static final int PACKS_EXTRACTION_STATE_PROTOCOL_VERSION = 1;
    private static final String PACKS_EXTRACTION_STATE_FILE_NAME = ".flansmod_packs_extraction_state.json";
    private static final String PACKS_EXTRACTION_STATE_COMPLETE = "complete";
    private static final String PACKS_EXTRACTION_STATE_FAILED = "failed";
    private static final int TIMEOUT_PACKS_EXTRACTION = 120;

    public static final Logger log = LogUtils.getLogger();
    public static final TeamsManager teamsManager = new TeamsManager();
    public static final int DUNGEON_LOOT_CHANCE = 500;

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
    public static final Identifier paintjob = Identifier.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "paintjob");
    public static final Identifier defaultMuzzleFlashTexture = Identifier.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "textures/skins/defaultmuzzleflash.png");
    /** Valid fallback for render APIs which no longer accept an empty resource path. */
    public static final Identifier defaultFallbackTexture = Identifier.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "textures/skins/defaultbullet.png");
    public static final Identifier hitmarkerTexture = Identifier.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "textures/gui/basic_hitmarker.png");
    public static final Identifier gunWorkbenchGuiTexture = Identifier.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "textures/gui/gun_workbench.png");
    public static final Identifier paintjobTableGuiTexture = Identifier.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "textures/gui/paintjob_table.png");
    public static final Identifier armorBoxGuiTexture = Identifier.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "textures/gui/armour_box.png");
    public static final Identifier gunBoxGuiTexture = Identifier.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "textures/gui/weaponboxdefault.png");
    public static final Identifier ammoGuiTexture = Identifier.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, "textures/gui/ammo_gui.png");
    public static final Identifier teamsLoadoutEditorGuiTexture = Identifier.fromNamespaceAndPath(FlansMod.MOD_ID, "textures/gui/teams_loadout_editor.png");
    public static final Identifier teamsLandingPageGuiTexture = Identifier.fromNamespaceAndPath(FlansMod.MOD_ID, "textures/gui/teams_landing_page.png");
    public static final Identifier teamsMissionResultsGuiTexture = Identifier.fromNamespaceAndPath(FlansMod.MOD_ID, "textures/gui/teams_mission_results.png");
    public static final Identifier teamsOpenCreatesGuiTexture = Identifier.fromNamespaceAndPath(FlansMod.MOD_ID, "textures/gui/teams_open_crates.png");

    // Registries
    private static final DeferredRegister.Blocks blockRegistry = DeferredRegister.createBlocks(FlansMod.FLANSMOD_ID);
    private static final DeferredRegister.Items itemRegistry = DeferredRegister.createItems(FlansMod.FLANSMOD_ID);
    private static final DeferredRegister<MenuType<?>> menuRegistry = DeferredRegister.create(BuiltInRegistries.MENU, FlansMod.MOD_ID);
    private static final DeferredRegister<ParticleType<?>> particleRegistry = DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, FlansMod.FLANSMOD_ID);
    private static final DeferredRegister<SoundEvent> soundEventRegistry = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, FlansMod.FLANSMOD_ID);
    private static final DeferredRegister<CreativeModeTab> creativeModeTabRegistry = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FlansMod.MOD_ID);
    private static final DeferredRegister<EntityType<?>> entityRegistry = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, FlansMod.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> blockEntityRegistry = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, FlansMod.MOD_ID);

    // Blocks
    public static final DeferredHolder<Block, ? extends Block> gunWorkbench = blockRegistry.registerBlock("gunworkbench", GunWorkbenchBlock::new, () -> BlockBehaviour.Properties.of()
        .mapColor(MapColor.METAL)
        .strength(3F, 6F)
        .sound(SoundType.METAL)
        .requiresCorrectToolForDrops()
        .pushReaction(PushReaction.BLOCK));
    public static final DeferredHolder<Block, ? extends Block> paintjobTable = blockRegistry.registerBlock("paintjobtable", PaintjobTableBlock::new, () -> BlockBehaviour.Properties.of()
        .strength(2F, 4F)
        .sound(SoundType.STONE));
    public static final DeferredHolder<Block, ? extends Block> playerSpawner = blockRegistry.registerBlock("teams_player_spawner",
        properties -> new TeamSpawnerBlock(TeamSpawnerBlockEntity.Mode.PLAYER, properties),
        properties -> properties.mapColor(MapColor.METAL).strength(1F, 2F).sound(SoundType.METAL).noOcclusion());
    public static final DeferredHolder<Block, ? extends Block> itemSpawner = blockRegistry.registerBlock("teams_item_spawner",
        properties -> new TeamSpawnerBlock(TeamSpawnerBlockEntity.Mode.ITEM, properties),
        properties -> properties.mapColor(MapColor.METAL).strength(1F, 2F).sound(SoundType.METAL).noOcclusion());
    public static final DeferredHolder<Block, ? extends Block> vehicleSpawner = blockRegistry.registerBlock("teams_vehicle_spawner",
        properties -> new TeamSpawnerBlock(TeamSpawnerBlockEntity.Mode.VEHICLE, properties),
        properties -> properties.mapColor(MapColor.METAL).strength(1F, 2F).sound(SoundType.METAL).noOcclusion());

    // Block Entities
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PaintjobTableBlockEntity>> paintjobTableBlockEntity = blockEntityRegistry.register("paintjobtable", () -> new BlockEntityType<>(PaintjobTableBlockEntity::new, paintjobTable.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ItemHolderBlockEntity>> itemHolderBlockEntity = blockEntityRegistry.register("item_holder", () -> new BlockEntityType<>(ItemHolderBlockEntity::new, getRegisteredBlocks(EnumType.ITEM_HOLDER)));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TeamSpawnerBlockEntity>> teamSpawnerBlockEntity = blockEntityRegistry.register("teams_spawner", () -> new BlockEntityType<>(TeamSpawnerBlockEntity::new, playerSpawner.get(), itemSpawner.get(), vehicleSpawner.get()));

    // Items
    public static final DeferredHolder<Item, ? extends Item> rainbowPaintcan = itemRegistry.registerSimpleItem("rainbowpaintcan");
    public static final DeferredHolder<Item, ? extends Item> gunWorkbenchItem = itemRegistry.registerSimpleBlockItem(gunWorkbench);
    public static final DeferredHolder<Item, ? extends Item> paintjobTableItem = itemRegistry.registerSimpleBlockItem(paintjobTable);
    public static final DeferredHolder<Item, ? extends Item> playerSpawnerItem = itemRegistry.registerSimpleBlockItem(playerSpawner);
    public static final DeferredHolder<Item, ? extends Item> itemSpawnerItem = itemRegistry.registerSimpleBlockItem(itemSpawner);
    public static final DeferredHolder<Item, ? extends Item> vehicleSpawnerItem = itemRegistry.registerSimpleBlockItem(vehicleSpawner);
    public static final DeferredHolder<Item, ? extends Item> opStick = itemRegistry.registerItem("op_stick", ItemOpStick::new,
        properties -> properties.stacksTo(1));
    public static final DeferredHolder<Item, ? extends Item> flagpoleItem = itemRegistry.registerItem("flagpole", FlagpoleItem::new,
        properties -> properties.stacksTo(16));

    // Menus
    public static final DeferredHolder<MenuType<?>, MenuType<GunWorkbenchMenu>> gunWorkbenchMenu = menuRegistry.register("gunworkbench_menu", () -> menuType((windowId, inv, buf) -> new GunWorkbenchMenu(windowId, inv, buf.readBlockPos())));
    public static final DeferredHolder<MenuType<?>, MenuType<DriveableCraftingMenu>> driveableCraftingMenu = menuRegistry.register("driveable_crafting_menu", () -> menuType((windowId, inv, buf) -> new DriveableCraftingMenu(windowId, inv, buf.readBlockPos())));
    public static final DeferredHolder<MenuType<?>, MenuType<DriveableInventoryMenu>> driveableInventoryMenu = menuRegistry.register("driveable_inventory_menu", () -> menuType(DriveableInventoryMenu::createFromNetwork));
    public static final DeferredHolder<MenuType<?>, MenuType<PaintjobTableMenu>> paintjobTableMenu = menuRegistry.register("paintjob_table_menu", () -> menuType(PaintjobTableMenu::createFromNetwork));
    public static final DeferredHolder<MenuType<?>, MenuType<ArmorBoxMenu>> armorBoxMenu = menuRegistry.register("armorbox_menu", () -> menuType(ArmorBoxMenu::createFromNetwork));
    public static final DeferredHolder<MenuType<?>, MenuType<GunBoxMenu>> gunBoxMenu = menuRegistry.register("gunbox_menu", () -> menuType(GunBoxMenu::createFromNetwork));

    // Particles
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> afterburnParticle = particleRegistry.register("afterburn", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> bigSmokeParticle = particleRegistry.register("big_smoke", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> debris1Particle = particleRegistry.register("debris_1", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> explodeParticle = particleRegistry.register("explode", () -> new SimpleParticleType(false));
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

    // Entities
    public static final DeferredHolder<EntityType<?>, EntityType<Bullet>> bulletEntity = entityRegistry.register("bullet", () -> EntityType.Builder.<Bullet>of(Bullet::new, MobCategory.MISC)
        .sized(Shootable.DEFAULT_HITBOX_SIZE, Shootable.DEFAULT_HITBOX_SIZE)
        .clientTrackingRange(ModCommonConfig.bulletRegistrationTrackingRange())
        .updateInterval(20)
        .setShouldReceiveVelocityUpdates(true)
        .build(entityTypeKey("bullet"))
    );
    public static final DeferredHolder<EntityType<?>, EntityType<Grenade>> grenadeEntity = entityRegistry.register("grenade", () -> EntityType.Builder.<Grenade>of(Grenade::new, MobCategory.MISC)
        .sized(Shootable.DEFAULT_HITBOX_SIZE, Shootable.DEFAULT_HITBOX_SIZE)
        .clientTrackingRange(ModCommonConfig.grenadeRegistrationTrackingRange())
        .updateInterval(20)
        .setShouldReceiveVelocityUpdates(true)
        .build(entityTypeKey("grenade"))
    );
    public static final DeferredHolder<EntityType<?>, EntityType<DeployedGun>> deployedGunEntity = entityRegistry.register("deployed_gun", () -> EntityType.Builder.<DeployedGun>of(DeployedGun::new, MobCategory.MISC)
        .sized(DeployedGun.DEFAULT_HITBOX_SIZE, DeployedGun.DEFAULT_HITBOX_SIZE)
        .clientTrackingRange(ModCommonConfig.deployedGunRegistrationTrackingRange())
        .updateInterval(5)
        .setShouldReceiveVelocityUpdates(true)
        .build(entityTypeKey("deployed_gun"))
    );
    public static final DeferredHolder<EntityType<?>, EntityType<GunItemEntity>> gunItemEntity = entityRegistry.register("gun_item", () -> EntityType.Builder.<GunItemEntity>of(GunItemEntity::new, MobCategory.MISC)
        .sized(1F, 1F)
        .clientTrackingRange(16)
        .updateInterval(20)
        .build(entityTypeKey("gun_item"))
    );
    public static final DeferredHolder<EntityType<?>, EntityType<AAGun>> aaGunEntity = entityRegistry.register("aa_gun", () -> EntityType.Builder.<AAGun>of(AAGun::new, MobCategory.MISC)
        .sized(AAGun.DEFAULT_HITBOX_SIZE, AAGun.DEFAULT_HITBOX_SIZE)
        .clientTrackingRange(ModCommonConfig.aaGunRegistrationTrackingRange())
        .updateInterval(2)
        .setShouldReceiveVelocityUpdates(true)
        .build(entityTypeKey("aa_gun"))
    );
    public static final DeferredHolder<EntityType<?>, EntityType<Parachute>> parachuteEntity = entityRegistry.register("parachute", () -> EntityType.Builder.<Parachute>of(Parachute::new, MobCategory.MISC)
        .sized(Parachute.DEFAULT_HITBOX_WIDTH, Parachute.DEFAULT_HITBOX_HEIGHT)
        .clientTrackingRange(64)
        .updateInterval(2)
        .setShouldReceiveVelocityUpdates(true)
        .build(entityTypeKey("parachute"))
    );
    public static final DeferredHolder<EntityType<?>, EntityType<Plane>> planeEntity = entityRegistry.register("plane", () -> EntityType.Builder.<Plane>of(Plane::new, MobCategory.MISC)
        .sized(3F, 2F)
        .clientTrackingRange(128)
        .updateInterval(1)
        .setShouldReceiveVelocityUpdates(true)
        .build(entityTypeKey("plane"))
    );
    public static final DeferredHolder<EntityType<?>, EntityType<Vehicle>> vehicleEntity = entityRegistry.register("vehicle", () -> EntityType.Builder.<Vehicle>of(Vehicle::new, MobCategory.MISC)
        .sized(2.5F, 2F)
        .clientTrackingRange(128)
        .updateInterval(1)
        .setShouldReceiveVelocityUpdates(true)
        .build(entityTypeKey("vehicle"))
    );
    public static final DeferredHolder<EntityType<?>, EntityType<Mecha>> mechaEntity = entityRegistry.register("mecha", () -> EntityType.Builder.<Mecha>of(Mecha::new, MobCategory.MISC)
        .sized(2F, 4F)
        .clientTrackingRange(128)
        .updateInterval(1)
        .setShouldReceiveVelocityUpdates(true)
        .build(entityTypeKey("mecha"))
    );
    public static final DeferredHolder<EntityType<?>, EntityType<Seat>> seatEntity = entityRegistry.register("driveable_seat", () -> EntityType.Builder.<Seat>of(Seat::new, MobCategory.MISC)
        .sized(0.6F, 0.6F)
        .clientTrackingRange(128)
        .updateInterval(1)
        .setShouldReceiveVelocityUpdates(false)
        .noSave()
        .noSummon()
        .build(entityTypeKey("driveable_seat"))
    );
    public static final DeferredHolder<EntityType<?>, EntityType<Wheel>> wheelEntity = entityRegistry.register("driveable_wheel", () -> EntityType.Builder.<Wheel>of(Wheel::new, MobCategory.MISC)
        .sized(0.75F, 0.75F)
        .clientTrackingRange(128)
        .updateInterval(1)
        .setShouldReceiveVelocityUpdates(false)
        .noSave()
        .noSummon()
        .build(entityTypeKey("driveable_wheel"))
    );
    public static final DeferredHolder<EntityType<?>, EntityType<Flagpole>> flagpoleEntity = entityRegistry.register("flagpole", () -> EntityType.Builder.<Flagpole>of(Flagpole::new, MobCategory.MISC)
        .sized(0.75F, 2.5F).clientTrackingRange(64).updateInterval(10)
        .build(entityTypeKey("flagpole")));
    public static final DeferredHolder<EntityType<?>, EntityType<Flag>> flagEntity = entityRegistry.register("flag", () -> EntityType.Builder.<Flag>of(Flag::new, MobCategory.MISC)
        .sized(0.75F, 0.75F).clientTrackingRange(64).updateInterval(2)
        .build(entityTypeKey("flag")));

    private static ResourceKey<EntityType<?>> entityTypeKey(String path)
    {
        return ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, path));
    }

    private static final Map<EnumType, List<DeferredHolder<Item, ? extends Item>>> items = new EnumMap<>(EnumType.class);
    @Getter
    private static final Map<EnumType, Map<String, DeferredHolder<Block, ? extends Block>>> blocks = new EnumMap<>(EnumType.class);
    private static final Map<Identifier, DeferredHolder<SoundEvent, SoundEvent>> sounds = new HashMap<>();
    @Getter
    private static final Map<Identifier, TypeFile> soundsOrigins = new HashMap<>();

    public FlansMod(IEventBus modEventBus, ModContainer modContainer)
    {
        modEventBus.addListener(PacketHandler::register);
        modEventBus.addListener(NeoForgeChunkTickets::register);
        ModLogFile.initialize(MOD_ID);
        Arrays.stream(EnumType.values()).filter(EnumType::isHasItem).forEach(type -> items.put(type, new ArrayList<>()));
        Arrays.stream(EnumType.values()).filter(EnumType::isHasBlock).forEach(type -> blocks.put(type, new HashMap<>()));
        // Init Configs
        modContainer.registerConfig(ModConfig.Type.COMMON, ModCommonConfig.configSpec);
        modContainer.registerConfig(ModConfig.Type.COMMON, ModApocalypseConfig.configSpec, ModApocalypseConfig.CONFIG_FILE_NAME);
        modContainer.registerConfig(ModConfig.Type.CLIENT, ModClientConfig.configSpec);

        ApocalypseContent.register(modEventBus);

        // Init Registries
        blockRegistry.register(modEventBus);
        blockEntityRegistry.register(modEventBus);
        itemRegistry.register(modEventBus);
        particleRegistry.register(modEventBus);
        soundEventRegistry.register(modEventBus);
        creativeModeTabRegistry.register(modEventBus);
        entityRegistry.register(modEventBus);
        menuRegistry.register(modEventBus);

        waitForPacksExtractionIfPresent();

        // Register Everything
        CategoryManager.loadAll();
        ContentManager.searchForContentPacksInModsFolder();
        ContentManager.findContentInFlanFolder();
        ContentManager.readContentPacks();
        registerSounds();
        registerCreativeModeTabs();
    }

    private static <T extends net.minecraft.world.inventory.AbstractContainerMenu> MenuType<T> menuType(IContainerFactory<T> factory)
    {
        return new MenuType<>(factory, FeatureFlags.DEFAULT_FLAGS);
    }

    private static void waitForPacksExtractionIfPresent()
    {
        if (!ModList.get().isLoaded(PACKS_ID))
            return;

        if (!FMLEnvironment.isProduction())
        {
            log.info("Flan's Mod Ultimate Packs Extractor found, but extraction is disabled outside production. Continuing without waiting.");
            return;
        }

        log.info("Flan's Mod Ultimate Packs Extractor found. Waiting for extraction...");

        Path stateFile = FMLPaths.GAMEDIR.get().toAbsolutePath().normalize().resolve(PACKS_EXTRACTION_STATE_FILE_NAME);

        long deadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_PACKS_EXTRACTION);
        while (true)
        {
            PacksExtractionWaitState state = readPacksExtractionWaitState(stateFile);
            if (state == PacksExtractionWaitState.COMPLETE)
            {
                log.info("Packs extraction state is complete.");
                return;
            }
            if (state == PacksExtractionWaitState.FAILED)
            {
                log.error("Packs extraction failed. Continuing without waiting longer. See the packs extraction state file: {}", stateFile);
                return;
            }
            if (state == PacksExtractionWaitState.UNSUPPORTED)
            {
                log.error("Unsupported packs extraction state file protocol. Continuing without waiting longer: {}", stateFile);
                return;
            }

            if (System.nanoTime() > deadlineNanos)
            {
                log.error("Timed out waiting for packs extraction state to complete: {}", stateFile);
                return;
            }

            // Light sleep to avoid burning CPU
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(25));
        }
    }

    private static PacksExtractionWaitState readPacksExtractionWaitState(Path stateFile)
    {
        if (!Files.isRegularFile(stateFile))
            return PacksExtractionWaitState.WAITING;

        try
        {
            JsonObject object = JsonParser.parseString(Files.readString(stateFile, StandardCharsets.UTF_8)).getAsJsonObject();
            if (!object.has("protocolVersion") || !object.has("state"))
                return PacksExtractionWaitState.WAITING;

            if (object.get("protocolVersion").getAsInt() != PACKS_EXTRACTION_STATE_PROTOCOL_VERSION)
                return PacksExtractionWaitState.UNSUPPORTED;

            String state = object.get("state").getAsString();
            if (PACKS_EXTRACTION_STATE_COMPLETE.equals(state))
                return PacksExtractionWaitState.COMPLETE;
            if (PACKS_EXTRACTION_STATE_FAILED.equals(state))
                return PacksExtractionWaitState.FAILED;

            return PacksExtractionWaitState.WAITING;
        }
        catch (IOException | IllegalStateException | JsonSyntaxException e)
        {
            return PacksExtractionWaitState.WAITING;
        }
    }

    private enum PacksExtractionWaitState
    {
        WAITING,
        COMPLETE,
        FAILED,
        UNSUPPORTED
    }

    @SuppressWarnings("unchecked") // Java cannot create a generic ResourceKey varargs array directly.
    private static void registerCreativeModeTabs()
    {
        ResourceKey<CreativeModeTab> creativeTabMainKey = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath(MOD_ID, CreativeTabs.TAB_GENERAL));
        ResourceKey<CreativeModeTab>[] creativeTabsFlansModReloadedKey = new ResourceKey[]
        {
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath(FLANSMOD_ID, "creative_tab_guns")),
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath(FLANSMOD_ID, "creative_tab_modifiers")),
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath(FLANSMOD_ID, "creative_tab_parts")),
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath(FLANSMOD_ID, "creative_tab_bullets"))
        };

        List<DeferredHolder<Item, ? extends Item>> generalItemList = new ArrayList<>();
        generalItemList.add(FlansMod.gunWorkbenchItem);
        generalItemList.add(FlansMod.paintjobTableItem);
        generalItemList.add(FlansMod.rainbowPaintcan);
        generalItemList.add(FlansMod.flagpoleItem);
        generalItemList.add(FlansMod.playerSpawnerItem);
        generalItemList.add(FlansMod.itemSpawnerItem);
        generalItemList.add(FlansMod.vehicleSpawnerItem);
        generalItemList.add(FlansMod.opStick);
        if (ModApocalypseConfig.apocalypseEnabled()) {
            generalItemList.add(ApocalypseContent.SULPHUR);
            generalItemList.add(ApocalypseContent.BLOCK_SULPHUR_ITEM);
            generalItemList.add(ApocalypseContent.BLOCK_LAB_STONE_ITEM);
            generalItemList.add(ApocalypseContent.BLOCK_POWER_CUBE_ITEM);
            generalItemList.add(ApocalypseContent.SULPHURIC_ACID_BUCKET);
        }
        generalItemList.addAll(FlansMod.getItems(EnumSet.of(
            EnumType.ITEM_HOLDER,
            EnumType.ARMOR_BOX,
            EnumType.GUN_BOX
        )));

        CreativeTabs.registerCreativeTab(FlansMod.creativeModeTabRegistry, CreativeTabs.TAB_GENERAL, generalItemList, Collections.emptyList(), CreativeModeTabs.SPAWN_EGGS, creativeTabsFlansModReloadedKey);
        CreativeTabs.registerCreativeTab(FlansMod.creativeModeTabRegistry, CreativeTabs.TAB_ARMORS, FlansMod.getItems(EnumType.ARMOR), List.of(EnumType.ARMOR), creativeTabMainKey, creativeTabsFlansModReloadedKey);
        CreativeTabs.registerCreativeTab(FlansMod.creativeModeTabRegistry, CreativeTabs.TAB_ATTACHMENTS, FlansMod.getItems(EnumType.ATTACHMENT), List.of(EnumType.ATTACHMENT), creativeTabMainKey, creativeTabsFlansModReloadedKey);
        CreativeTabs.registerCreativeTab(FlansMod.creativeModeTabRegistry, CreativeTabs.TAB_GUNS, FlansMod.getItems(EnumSet.of(EnumType.GUN, EnumType.BULLET)), List.of(EnumType.GUN), creativeTabMainKey, creativeTabsFlansModReloadedKey);
        CreativeTabs.registerCreativeTab(FlansMod.creativeModeTabRegistry, CreativeTabs.TAB_GRENADES, FlansMod.getItems(EnumType.GRENADE), List.of(EnumType.GRENADE), creativeTabMainKey, creativeTabsFlansModReloadedKey);
        CreativeTabs.registerCreativeTab(FlansMod.creativeModeTabRegistry, CreativeTabs.TAB_TOOLS, FlansMod.getItems(EnumSet.of(EnumType.TOOL, EnumType.GLOVE)), List.of(EnumType.TOOL, EnumType.GLOVE), creativeTabMainKey, creativeTabsFlansModReloadedKey);
        CreativeTabs.registerCreativeTab(FlansMod.creativeModeTabRegistry, CreativeTabs.TAB_BOMBS_AND_SHELLS, FlansMod.getItems(EnumSet.of(EnumType.BULLET)), List.of(EnumType.BULLET), creativeTabMainKey, creativeTabsFlansModReloadedKey);
        CreativeTabs.registerCreativeTab(FlansMod.creativeModeTabRegistry, CreativeTabs.TAB_AA_GUNS, FlansMod.getItems(EnumType.AA_GUN), List.of(EnumType.AA_GUN), creativeTabMainKey, creativeTabsFlansModReloadedKey);
        CreativeTabs.registerCreativeTab(FlansMod.creativeModeTabRegistry, CreativeTabs.TAB_MECHAS, FlansMod.getItems(EnumSet.of(EnumType.MECHA, EnumType.MECHA_ITEM)), List.of(EnumType.MECHA), creativeTabMainKey, creativeTabsFlansModReloadedKey);
        CreativeTabs.registerCreativeTab(FlansMod.creativeModeTabRegistry, CreativeTabs.TAB_PLANES, FlansMod.getItems(EnumSet.of(EnumType.PLANE)), List.of(EnumType.PLANE), creativeTabMainKey, creativeTabsFlansModReloadedKey);
        CreativeTabs.registerCreativeTab(FlansMod.creativeModeTabRegistry, CreativeTabs.TAB_VEHICLES, FlansMod.getItems(EnumSet.of(EnumType.VEHICLE)), List.of(EnumType.VEHICLE), creativeTabMainKey, creativeTabsFlansModReloadedKey);
        CreativeTabs.registerCreativeTab(FlansMod.creativeModeTabRegistry, CreativeTabs.TAB_PARTS, FlansMod.getItems(EnumSet.of(EnumType.PART)), List.of(EnumType.PART), creativeTabMainKey, creativeTabsFlansModReloadedKey);
    }

    private static Block[] getRegisteredBlocks(EnumType type)
    {
        Map<String, DeferredHolder<Block, ? extends Block>> registeredBlocks = blocks.get(type);
        if (registeredBlocks == null)
            return new Block[0];
        return registeredBlocks.values().stream().map(DeferredHolder::get).toArray(Block[]::new);
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

    public static void registerItem(String itemName, EnumType type, Function<Item.Properties, ? extends Item> initItem)
    {
        items.get(type).add(itemRegistry.registerItem(itemName, initItem));
    }

    public static void registerBlock(String blockName, EnumType type, Function<BlockBehaviour.Properties, ? extends Block> initBlock)
    {
        blocks.get(type).put(blockName, blockRegistry.registerBlock(blockName, initBlock));
    }

    public static void registerSound(String soundName, @Nullable TypeFile typeFile)
    {
        Identifier rl = Identifier.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, soundName);
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
        Identifier rl = Identifier.fromNamespaceAndPath(FlansMod.FLANSMOD_ID, soundName);
        return Optional.ofNullable(sounds.get(rl));
    }
}
