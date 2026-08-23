package com.flansmodultimate.apocalyse;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.apocalyse.common.block.PowerCubeBlock;
import com.flansmodultimate.apocalyse.common.block.SulphurBlock;
import com.flansmodultimate.apocalyse.common.block.SulphuricAcidBlock;
import com.flansmodultimate.apocalyse.common.block.entity.PowerCubeBlockEntity;
import com.flansmodultimate.apocalyse.common.entity.NukeDropEntity;
import com.flansmodultimate.apocalyse.common.entity.SkullBossEntity;
import com.flansmodultimate.apocalyse.common.entity.SkullDroneEntity;
import com.flansmodultimate.apocalyse.common.entity.SurvivorEntity;
import com.flansmodultimate.apocalyse.common.entity.TeleporterEntity;
import lombok.NoArgsConstructor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class ApocalypseContent
{
    // Resource Locations
    public static final Identifier survivorTexture = Identifier.fromNamespaceAndPath(FlansMod.APOCALYPSE_ID, "textures/entity/survivor.png");
    public static final ResourceKey<Level> APOCALYPSE_LEVEL = ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath(FlansMod.APOCALYPSE_ID, "apocalypse"));

    // Registries
    private static final DeferredRegister.Blocks blockRegistry = DeferredRegister.createBlocks(FlansMod.APOCALYPSE_ID);
    private static final DeferredRegister.Items itemRegistry = DeferredRegister.createItems(FlansMod.APOCALYPSE_ID);
    private static final DeferredRegister<Fluid> fluidRegistry = DeferredRegister.create(BuiltInRegistries.FLUID, FlansMod.APOCALYPSE_ID);
    private static final DeferredRegister<FluidType> fluidTypeRegistry = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, FlansMod.APOCALYPSE_ID);
    private static final DeferredRegister<BlockEntityType<?>> blockEntityRegistry = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, FlansMod.APOCALYPSE_ID);
    private static final DeferredRegister<EntityType<?>> entityRegistry = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, FlansMod.APOCALYPSE_ID);

    // Fluid Types
    public static final DeferredHolder<FluidType, FluidType> sulphuricAcidFluidType = fluidTypeRegistry.register("sulphuric_acid", () ->
        new FluidType(FluidType.Properties.create()
            .descriptionId("fluid." + FlansMod.APOCALYPSE_ID + ".sulphuric_acid")
            .temperature(300)
            .viscosity(800)
            .density(1200)
        )
    );

    // Fluids
    public static final DeferredHolder<Fluid, FlowingFluid> sulphuricAcid = fluidRegistry.register("sulphuric_acid", () -> new BaseFlowingFluid.Source(sulphuricAcidProperties()));
    public static final DeferredHolder<Fluid, FlowingFluid> flowingSulphuricAcid = fluidRegistry.register("flowing_sulphuric_acid", () -> new BaseFlowingFluid.Flowing(sulphuricAcidProperties()));

    // Blocks
    public static final DeferredHolder<Block, ? extends Block> blockSulphur = blockRegistry.registerBlock("blocksulphur", SulphurBlock::new, () -> BlockBehaviour.Properties.of()
        .mapColor(MapColor.SAND)
        .strength(0.5F)
        .sound(SoundType.SAND));
    public static final DeferredHolder<Block, ? extends Block> blockLabStone = blockRegistry.registerSimpleBlock("blocklabstone", () -> BlockBehaviour.Properties.of()
        .mapColor(MapColor.STONE)
        .strength(3.0F, 5.0F)
        .sound(SoundType.STONE)
        .requiresCorrectToolForDrops());
    public static final DeferredHolder<Block, ? extends Block> blockPowerCube = blockRegistry.registerBlock("blockpowercube", PowerCubeBlock::new, () -> BlockBehaviour.Properties.of()
        .mapColor(MapColor.METAL)
        .strength(3.0F, 5.0F)
        .sound(SoundType.METAL)
        .lightLevel(state -> 8)
        .noOcclusion()
        .requiresCorrectToolForDrops()
        .pushReaction(PushReaction.BLOCK));
    public static final DeferredHolder<Block, SulphuricAcidBlock> blockSulphuricAcid = blockRegistry.registerBlock("blocksulphuricacid", properties -> new SulphuricAcidBlock(sulphuricAcid, properties), () -> BlockBehaviour.Properties.ofFullCopy(Blocks.WATER)
        .mapColor(MapColor.COLOR_YELLOW)
        .noLootTable());

    // Items
    public static final DeferredHolder<Item, ? extends Item> SULPHUR = itemRegistry.registerSimpleItem("flansulphur");
    public static final DeferredHolder<Item, ? extends Item> BLOCK_SULPHUR_ITEM = itemRegistry.registerSimpleBlockItem(blockSulphur);
    public static final DeferredHolder<Item, ? extends Item> BLOCK_LAB_STONE_ITEM = itemRegistry.registerSimpleBlockItem(blockLabStone);
    public static final DeferredHolder<Item, ? extends Item> BLOCK_POWER_CUBE_ITEM = itemRegistry.registerSimpleBlockItem(blockPowerCube);
    public static final DeferredHolder<Item, ? extends Item> SULPHURIC_ACID_BUCKET = itemRegistry.registerItem("sulphuric_acid_bucket",
        properties -> new BucketItem(sulphuricAcid.get(), properties),
        properties -> properties.craftRemainder(Items.BUCKET).stacksTo(1));

    // Block Entities
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PowerCubeBlockEntity>> powerCubeBlockEntity = blockEntityRegistry.register("powercube", () ->
        new BlockEntityType<>(PowerCubeBlockEntity::new, blockPowerCube.get())
    );

    // Entities
    public static final DeferredHolder<EntityType<?>, EntityType<TeleporterEntity>> teleporter = entityRegistry.register("teleporter", () -> EntityType.Builder.of(TeleporterEntity::new, MobCategory.MISC)
        .sized(4.0F, 3.0F)
        .clientTrackingRange(64)
        .updateInterval(10)
        .build(entityTypeKey("teleporter"))
    );
    public static final DeferredHolder<EntityType<?>, EntityType<NukeDropEntity>> nukeDrop = entityRegistry.register("nukedrop", () -> EntityType.Builder.of(NukeDropEntity::new, MobCategory.MISC)
        .sized(1.0F, 1.0F)
        .clientTrackingRange(256)
        .updateInterval(2)
        .build(entityTypeKey("nukedrop"))
    );
    public static final DeferredHolder<EntityType<?>, EntityType<SurvivorEntity>> survivor = entityRegistry.register("survivor", () -> EntityType.Builder.of(SurvivorEntity::new, MobCategory.CREATURE)
        .sized(0.6F, 1.95F)
        .clientTrackingRange(80)
        .updateInterval(3)
        .build(entityTypeKey("survivor"))
    );
    public static final DeferredHolder<EntityType<?>, EntityType<SkullDroneEntity>> skullDrone = entityRegistry.register("autodrone", () -> EntityType.Builder.of(SkullDroneEntity::new, MobCategory.MONSTER)
        .sized(1.6F, 1.0F)
        .clientTrackingRange(128)
        .updateInterval(2)
        .build(entityTypeKey("autodrone"))
    );
    public static final DeferredHolder<EntityType<?>, EntityType<SkullBossEntity>> skullBoss = entityRegistry.register("skullboss", () -> EntityType.Builder.of(SkullBossEntity::new, MobCategory.MONSTER)
        .sized(8.0F, 8.0F)
        .clientTrackingRange(256)
        .updateInterval(2)
        .fireImmune()
        .build(entityTypeKey("skullboss"))
    );

    private static ResourceKey<EntityType<?>> entityTypeKey(String path)
    {
        return ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(FlansMod.APOCALYPSE_ID, path));
    }

    public static void register(IEventBus modEventBus)
    {
        fluidTypeRegistry.register(modEventBus);
        fluidRegistry.register(modEventBus);
        blockRegistry.register(modEventBus);
        blockEntityRegistry.register(modEventBus);
        itemRegistry.register(modEventBus);
        entityRegistry.register(modEventBus);
    }

    private static BaseFlowingFluid.Properties sulphuricAcidProperties()
    {
        return new BaseFlowingFluid.Properties(sulphuricAcidFluidType, sulphuricAcid, flowingSulphuricAcid)
            .slopeFindDistance(2)
            .levelDecreasePerBlock(2)
            .block(blockSulphuricAcid)
            .bucket(SULPHURIC_ACID_BUCKET);
    }
}
