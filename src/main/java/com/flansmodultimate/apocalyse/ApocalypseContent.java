package com.flansmodultimate.apocalyse;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.apocalyse.common.block.PowerCubeBlock;
import com.flansmodultimate.apocalyse.common.block.SulphurBlock;
import com.flansmodultimate.apocalyse.common.block.SulphuricAcidBlock;
import com.flansmodultimate.apocalyse.common.block.entity.PowerCubeBlockEntity;
import com.flansmodultimate.apocalyse.common.entity.AiMechaEntity;
import com.flansmodultimate.apocalyse.common.entity.FlyByPlaneEntity;
import com.flansmodultimate.apocalyse.common.entity.InventoryHolderEntity;
import com.flansmodultimate.apocalyse.common.entity.NukeDropEntity;
import com.flansmodultimate.apocalyse.common.entity.SkullBossEntity;
import com.flansmodultimate.apocalyse.common.entity.SkullDroneEntity;
import com.flansmodultimate.apocalyse.common.entity.SurvivorEntity;
import com.flansmodultimate.apocalyse.common.entity.TeleporterEntity;
import com.flansmodultimate.apocalyse.common.entity.WorldgenSpawnMarker;
import com.flansmodultimate.apocalyse.common.world.ApocalypseChunkFeature;
import lombok.NoArgsConstructor;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import java.util.function.Consumer;
import java.util.function.Supplier;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class ApocalypseContent
{
    // Resource Locations
    public static final ResourceLocation SURVIVOR_TEXTURE = ResourceLocation.fromNamespaceAndPath(FlansMod.APOCALYPSE_ID, "textures/entity/survivor.png");
    public static final ResourceLocation SULPHURIC_ACID_STILL_TEXTURE = ResourceLocation.fromNamespaceAndPath(FlansMod.APOCALYPSE_ID, "block/sulphuricacidstill");
    public static final ResourceLocation SULPHURIC_ACID_FLOWING_TEXTURE = ResourceLocation.fromNamespaceAndPath(FlansMod.APOCALYPSE_ID, "block/sulphuricacidflowing");
    public static final ResourceLocation SULPHURIC_ACID_OVERLAY_TEXTURE = ResourceLocation.fromNamespaceAndPath(FlansMod.APOCALYPSE_ID, "textures/misc/sulphuric_acid_overlay.png");

    public static final ResourceKey<Level> APOCALYPSE_LEVEL = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(FlansMod.APOCALYPSE_ID, "apocalypse"));

    // Biomes of the wasteland. Supplied by the built-in apocalypse datapack; worldgen reads
    // them back to decide what belongs where, as the 1.7.10 biome decorators did.
    public static final ResourceKey<Biome> BIOME_DESERT = biome("apocalypse");
    public static final ResourceKey<Biome> BIOME_DEEP_CANYON = biome("deep_canyon");
    public static final ResourceKey<Biome> BIOME_CANYON = biome("canyon");
    public static final ResourceKey<Biome> BIOME_PLATEAU = biome("plateau");
    public static final ResourceKey<Biome> BIOME_HIGH_PLATEAU = biome("high_plateau");
    public static final ResourceKey<Biome> BIOME_SULPHUR_PITS = biome("sulphur_pits");

    private static ResourceKey<Biome> biome(String name)
    {
        return ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(FlansMod.APOCALYPSE_ID, name));
    }

    // Registries
    private static final DeferredRegister<Block> blockRegistry = DeferredRegister.create(Registries.BLOCK, FlansMod.APOCALYPSE_ID);
    private static final DeferredRegister<Item> itemRegistry = DeferredRegister.create(Registries.ITEM, FlansMod.APOCALYPSE_ID);
    private static final DeferredRegister<Fluid> fluidRegistry = DeferredRegister.create(Registries.FLUID, FlansMod.APOCALYPSE_ID);
    private static final DeferredRegister<FluidType> fluidTypeRegistry = DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, FlansMod.APOCALYPSE_ID);
    private static final DeferredRegister<BlockEntityType<?>> blockEntityRegistry = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, FlansMod.APOCALYPSE_ID);
    private static final DeferredRegister<EntityType<?>> entityRegistry = DeferredRegister.create(Registries.ENTITY_TYPE, FlansMod.APOCALYPSE_ID);
    private static final DeferredRegister<Feature<?>> featureRegistry = DeferredRegister.create(Registries.FEATURE, FlansMod.APOCALYPSE_ID);

    // Fluid Types
    public static final Supplier<FluidType> sulphuricAcidFluidType = fluidTypeRegistry.register("sulphuric_acid", () ->
        new FluidType(FluidType.Properties.create()
            .descriptionId("fluid." + FlansMod.APOCALYPSE_ID + ".sulphuric_acid")
            .temperature(300)
            .viscosity(800)
            .density(1200)
        )
        {
            @Override
            public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer)
            {
                consumer.accept(new IClientFluidTypeExtensions()
                {
                    @Override
                    public ResourceLocation getStillTexture()
                    {
                        return SULPHURIC_ACID_STILL_TEXTURE;
                    }

                    @Override
                    public ResourceLocation getFlowingTexture()
                    {
                        return SULPHURIC_ACID_FLOWING_TEXTURE;
                    }

                    @Override
                    public ResourceLocation getRenderOverlayTexture(Minecraft mc)
                    {
                        return SULPHURIC_ACID_OVERLAY_TEXTURE;
                    }
                });
            }
        }
    );

    // Fluids
    public static final Supplier<? extends FlowingFluid> sulphuricAcid = fluidRegistry.register("sulphuric_acid", () -> new ForgeFlowingFluid.Source(sulphuricAcidProperties()));
    public static final Supplier<? extends FlowingFluid> flowingSulphuricAcid = fluidRegistry.register("flowing_sulphuric_acid", () -> new ForgeFlowingFluid.Flowing(sulphuricAcidProperties()));

    // Blocks
    public static final Supplier<? extends Block> blockSulphur = blockRegistry.register("blocksulphur", () -> new SulphurBlock(BlockBehaviour.Properties.of()
        .mapColor(MapColor.SAND)
        .strength(0.5F)
        .sound(SoundType.SAND))
    );
    public static final Supplier<? extends Block> blockLabStone = blockRegistry.register("blocklabstone", () -> new Block(BlockBehaviour.Properties.of()
        .mapColor(MapColor.STONE)
        .strength(3.0F, 5.0F)
        .sound(SoundType.STONE)
        .requiresCorrectToolForDrops())
    );
    public static final Supplier<? extends Block> blockPowerCube = blockRegistry.register("blockpowercube", () -> new PowerCubeBlock(BlockBehaviour.Properties.of()
        .mapColor(MapColor.METAL)
        .strength(3.0F, 5.0F)
        .sound(SoundType.METAL)
        .lightLevel(state -> 8)
        .noOcclusion()
        .requiresCorrectToolForDrops()
        .pushReaction(PushReaction.BLOCK))
    );
    public static final Supplier<? extends SulphuricAcidBlock> blockSulphuricAcid = blockRegistry.register("blocksulphuricacid", () -> new SulphuricAcidBlock(sulphuricAcid, BlockBehaviour.Properties.copy(Blocks.WATER)
        .mapColor(MapColor.COLOR_YELLOW)
        .noLootTable())
    );

    // Items
    public static final Supplier<? extends Item> SULPHUR = itemRegistry.register("flansulphur", () -> new Item(new Item.Properties()));
    public static final Supplier<? extends Item> BLOCK_SULPHUR_ITEM = itemRegistry.register("blocksulphur", () -> new BlockItem(blockSulphur.get(), new Item.Properties()));
    public static final Supplier<? extends Item> BLOCK_LAB_STONE_ITEM = itemRegistry.register("blocklabstone", () -> new BlockItem(blockLabStone.get(), new Item.Properties()));
    public static final Supplier<? extends Item> BLOCK_POWER_CUBE_ITEM = itemRegistry.register("blockpowercube", () -> new BlockItem(blockPowerCube.get(), new Item.Properties()));
    public static final Supplier<? extends Item> SULPHURIC_ACID_BUCKET = itemRegistry.register("sulphuric_acid_bucket", () -> new BucketItem(sulphuricAcid, new Item.Properties()
        .craftRemainder(Items.BUCKET)
        .stacksTo(1))
    );

    // Block Entities
    public static final Supplier<? extends BlockEntityType<PowerCubeBlockEntity>> powerCubeBlockEntity = blockEntityRegistry.register("powercube", () ->
        BlockEntityType.Builder.of(PowerCubeBlockEntity::new, blockPowerCube.get()).build(null)
    );

    // Worldgen features, placed by the apocalypse biomes and by an overworld biome modifier
    public static final Supplier<? extends Feature<NoneFeatureConfiguration>> wastelandFeature = featureRegistry.register("wasteland", () -> new ApocalypseChunkFeature(true));
    public static final Supplier<? extends Feature<NoneFeatureConfiguration>> abandonedPortalFeature = featureRegistry.register("abandoned_portal", () -> new ApocalypseChunkFeature(false));

    // Entities
    public static final Supplier<? extends EntityType<TeleporterEntity>> teleporter = entityRegistry.register("teleporter", () -> EntityType.Builder.of(TeleporterEntity::new, MobCategory.MISC)
        .sized(4.0F, 3.0F)
        .clientTrackingRange(64)
        .updateInterval(10)
        .build(ResourceLocation.fromNamespaceAndPath(FlansMod.APOCALYPSE_ID, "teleporter").toString())
    );
    public static final Supplier<? extends EntityType<WorldgenSpawnMarker>> worldgenSpawnMarker = entityRegistry.register("worldgen_spawn_marker", () -> EntityType.Builder.<WorldgenSpawnMarker>of(WorldgenSpawnMarker::new, MobCategory.MISC)
        .sized(0.0F, 0.0F)
        .clientTrackingRange(0)
        .updateInterval(20)
        .noSummon()
        .fireImmune()
        .build(ResourceLocation.fromNamespaceAndPath(FlansMod.APOCALYPSE_ID, "worldgen_spawn_marker").toString())
    );
    public static final Supplier<? extends EntityType<NukeDropEntity>> nukeDrop = entityRegistry.register("nukedrop", () -> EntityType.Builder.of(NukeDropEntity::new, MobCategory.MISC)
        .sized(1.0F, 1.0F)
        .clientTrackingRange(256)
        .updateInterval(2)
        .build(ResourceLocation.fromNamespaceAndPath(FlansMod.APOCALYPSE_ID, "nukedrop").toString())
    );
    public static final Supplier<? extends EntityType<SurvivorEntity>> survivor = entityRegistry.register("survivor", () -> EntityType.Builder.of(SurvivorEntity::new, MobCategory.CREATURE)
        .sized(0.6F, 1.95F)
        .clientTrackingRange(80)
        .updateInterval(3)
        .build(ResourceLocation.fromNamespaceAndPath(FlansMod.APOCALYPSE_ID, "survivor").toString())
    );
    public static final Supplier<? extends EntityType<SkullDroneEntity>> skullDrone = entityRegistry.register("autodrone", () -> EntityType.Builder.of(SkullDroneEntity::new, MobCategory.MONSTER)
        .sized(1.6F, 1.0F)
        .clientTrackingRange(128)
        .updateInterval(2)
        .build(ResourceLocation.fromNamespaceAndPath(FlansMod.APOCALYPSE_ID, "autodrone").toString())
    );
    public static final Supplier<? extends EntityType<InventoryHolderEntity>> inventoryHolder = entityRegistry.register("fakeplayer", () -> EntityType.Builder.of(InventoryHolderEntity::new, MobCategory.CREATURE)
        .sized(0.6F, 1.95F)
        .clientTrackingRange(80)
        .updateInterval(3)
        .build(ResourceLocation.fromNamespaceAndPath(FlansMod.APOCALYPSE_ID, "fakeplayer").toString())
    );
    // The two autonomous driveables mirror the plane and mecha entity types they extend, so
    // they are tracked and sized exactly like the piloted ones.
    public static final Supplier<? extends EntityType<FlyByPlaneEntity>> flyByPlane = entityRegistry.register("flybyplane", () -> EntityType.Builder.<FlyByPlaneEntity>of(FlyByPlaneEntity::new, MobCategory.MISC)
        .sized(3F, 2F)
        .clientTrackingRange(128)
        .updateInterval(1)
        .setShouldReceiveVelocityUpdates(true)
        .build(ResourceLocation.fromNamespaceAndPath(FlansMod.APOCALYPSE_ID, "flybyplane").toString())
    );
    public static final Supplier<? extends EntityType<AiMechaEntity>> aiMecha = entityRegistry.register("aimecha", () -> EntityType.Builder.<AiMechaEntity>of(AiMechaEntity::new, MobCategory.MISC)
        .sized(2F, 4F)
        .clientTrackingRange(128)
        .updateInterval(1)
        .setShouldReceiveVelocityUpdates(true)
        .build(ResourceLocation.fromNamespaceAndPath(FlansMod.APOCALYPSE_ID, "aimecha").toString())
    );
    public static final Supplier<? extends EntityType<SkullBossEntity>> skullBoss = entityRegistry.register("skullboss", () -> EntityType.Builder.of(SkullBossEntity::new, MobCategory.MONSTER)
        .sized(8.0F, 8.0F)
        .clientTrackingRange(256)
        .updateInterval(2)
        .fireImmune()
        .build(ResourceLocation.fromNamespaceAndPath(FlansMod.APOCALYPSE_ID, "skullboss").toString())
    );

    public static void register(IEventBus modEventBus)
    {
        fluidTypeRegistry.register(modEventBus);
        fluidRegistry.register(modEventBus);
        blockRegistry.register(modEventBus);
        blockEntityRegistry.register(modEventBus);
        itemRegistry.register(modEventBus);
        entityRegistry.register(modEventBus);
        featureRegistry.register(modEventBus);
    }

    private static ForgeFlowingFluid.Properties sulphuricAcidProperties()
    {
        return new ForgeFlowingFluid.Properties(sulphuricAcidFluidType, sulphuricAcid, flowingSulphuricAcid)
            .slopeFindDistance(2)
            .levelDecreasePerBlock(2)
            .block(blockSulphuricAcid)
            .bucket(SULPHURIC_ACID_BUCKET);
    }
}
