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
import com.flansmodultimate.config.ModCommonConfig;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public final class ApocalypseContent
{
    public static final String APOCALYPSE_ID = "flansmodapocalypse";

    public static final ResourceKey<Level> APOCALYPSE_LEVEL = ResourceKey.create(Registries.DIMENSION, id("apocalypse"));
    public static final ResourceKey<DamageType> SULPHURIC_ACID_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, id("sulphuric_acid"));

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, APOCALYPSE_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, APOCALYPSE_ID);
    private static final DeferredRegister<net.minecraft.world.level.material.Fluid> FLUIDS = DeferredRegister.create(ForgeRegistries.FLUIDS, APOCALYPSE_ID);
    private static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, APOCALYPSE_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, APOCALYPSE_ID);
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, APOCALYPSE_ID);
    private static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FlansMod.MOD_ID);

    public static final RegistryObject<FluidType> SULPHURIC_ACID_FLUID_TYPE = FLUID_TYPES.register("sulphuric_acid", () ->
        new FluidType(FluidType.Properties.create()
            .descriptionId("fluid." + APOCALYPSE_ID + ".sulphuric_acid")
            .temperature(300)
            .viscosity(800)
            .density(1200)
        )
    );

    public static final RegistryObject<FlowingFluid> SULPHURIC_ACID = FLUIDS.register("sulphuric_acid", () -> new ForgeFlowingFluid.Source(sulphuricAcidProperties()));
    public static final RegistryObject<FlowingFluid> FLOWING_SULPHURIC_ACID = FLUIDS.register("flowing_sulphuric_acid", () -> new ForgeFlowingFluid.Flowing(sulphuricAcidProperties()));

    public static final RegistryObject<Block> BLOCK_SULPHUR = BLOCKS.register("blocksulphur", () -> new SulphurBlock(BlockBehaviour.Properties.of()
        .mapColor(MapColor.SAND)
        .strength(0.5F)
        .sound(SoundType.SAND))
    );
    public static final RegistryObject<Block> BLOCK_LAB_STONE = BLOCKS.register("blocklabstone", () -> new Block(BlockBehaviour.Properties.of()
        .mapColor(MapColor.STONE)
        .strength(3.0F, 5.0F)
        .sound(SoundType.STONE)
        .requiresCorrectToolForDrops())
    );
    public static final RegistryObject<Block> BLOCK_POWER_CUBE = BLOCKS.register("blockpowercube", () -> new PowerCubeBlock(BlockBehaviour.Properties.of()
        .mapColor(MapColor.METAL)
        .strength(3.0F, 5.0F)
        .sound(SoundType.METAL)
        .lightLevel(state -> 8)
        .noOcclusion()
        .requiresCorrectToolForDrops()
        .pushReaction(PushReaction.BLOCK))
    );
    public static final RegistryObject<SulphuricAcidBlock> BLOCK_SULPHURIC_ACID = BLOCKS.register("blocksulphuricacid", () -> new SulphuricAcidBlock(SULPHURIC_ACID, BlockBehaviour.Properties.copy(Blocks.WATER)
        .mapColor(MapColor.COLOR_YELLOW)
        .noLootTable())
    );

    public static final RegistryObject<Item> SULPHUR = ITEMS.register("flansulphur", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> BLOCK_SULPHUR_ITEM = ITEMS.register("blocksulphur", () -> new BlockItem(BLOCK_SULPHUR.get(), new Item.Properties()));
    public static final RegistryObject<Item> BLOCK_LAB_STONE_ITEM = ITEMS.register("blocklabstone", () -> new BlockItem(BLOCK_LAB_STONE.get(), new Item.Properties()));
    public static final RegistryObject<Item> BLOCK_POWER_CUBE_ITEM = ITEMS.register("blockpowercube", () -> new BlockItem(BLOCK_POWER_CUBE.get(), new Item.Properties()));
    public static final RegistryObject<Item> SULPHURIC_ACID_BUCKET = ITEMS.register("sulphuric_acid_bucket", () -> new BucketItem(SULPHURIC_ACID.get(), new Item.Properties()
        .craftRemainder(Items.BUCKET)
        .stacksTo(1))
    );

    public static final RegistryObject<BlockEntityType<PowerCubeBlockEntity>> POWER_CUBE_BLOCK_ENTITY = BLOCK_ENTITIES.register("powercube", () ->
        BlockEntityType.Builder.of(PowerCubeBlockEntity::new, BLOCK_POWER_CUBE.get()).build(null)
    );

    public static final RegistryObject<EntityType<TeleporterEntity>> TELEPORTER = ENTITIES.register("teleporter", () -> EntityType.Builder.<TeleporterEntity>of(TeleporterEntity::new, MobCategory.MISC)
        .sized(4.0F, 3.0F)
        .clientTrackingRange(64)
        .updateInterval(10)
        .build(id("teleporter").toString())
    );
    public static final RegistryObject<EntityType<NukeDropEntity>> NUKE_DROP = ENTITIES.register("nukedrop", () -> EntityType.Builder.<NukeDropEntity>of(NukeDropEntity::new, MobCategory.MISC)
        .sized(1.0F, 1.0F)
        .clientTrackingRange(256)
        .updateInterval(2)
        .build(id("nukedrop").toString())
    );
    public static final RegistryObject<EntityType<SurvivorEntity>> SURVIVOR = ENTITIES.register("survivor", () -> EntityType.Builder.of(SurvivorEntity::new, MobCategory.CREATURE)
        .sized(0.6F, 1.95F)
        .clientTrackingRange(80)
        .updateInterval(3)
        .build(id("survivor").toString())
    );
    public static final RegistryObject<EntityType<SkullDroneEntity>> SKULL_DRONE = ENTITIES.register("autodrone", () -> EntityType.Builder.of(SkullDroneEntity::new, MobCategory.MONSTER)
        .sized(1.6F, 1.0F)
        .clientTrackingRange(128)
        .updateInterval(2)
        .build(id("autodrone").toString())
    );
    public static final RegistryObject<EntityType<SkullBossEntity>> SKULL_BOSS = ENTITIES.register("skullboss", () -> EntityType.Builder.of(SkullBossEntity::new, MobCategory.MONSTER)
        .sized(8.0F, 8.0F)
        .clientTrackingRange(256)
        .updateInterval(2)
        .fireImmune()
        .build(id("skullboss").toString())
    );

    public static final RegistryObject<CreativeModeTab> APOCALYPSE_TAB = CREATIVE_TABS.register("creative_tab_apocalypse", () -> CreativeModeTab.builder()
        .title(net.minecraft.network.chat.Component.translatable("creativetab." + FlansMod.MOD_ID + ".creative_tab_apocalypse"))
        .icon(() -> new ItemStack(BLOCK_POWER_CUBE_ITEM.get()))
        .displayItems((parameters, output) -> {
            if (!ModCommonConfig.apocalypseShowItemsInCreative())
                return;

            output.accept(SULPHUR.get());
            output.accept(BLOCK_SULPHUR_ITEM.get());
            output.accept(BLOCK_LAB_STONE_ITEM.get());
            output.accept(BLOCK_POWER_CUBE_ITEM.get());
            output.accept(SULPHURIC_ACID_BUCKET.get());
        })
        .build()
    );

    private ApocalypseContent()
    {
    }

    public static void register(IEventBus modEventBus)
    {
        FLUID_TYPES.register(modEventBus);
        FLUIDS.register(modEventBus);
        BLOCKS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        ITEMS.register(modEventBus);
        ENTITIES.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
    }

    public static ResourceLocation id(String path)
    {
        return ResourceLocation.fromNamespaceAndPath(APOCALYPSE_ID, path);
    }

    private static ForgeFlowingFluid.Properties sulphuricAcidProperties()
    {
        return new ForgeFlowingFluid.Properties(SULPHURIC_ACID_FLUID_TYPE, SULPHURIC_ACID, FLOWING_SULPHURIC_ACID)
            .slopeFindDistance(2)
            .levelDecreasePerBlock(2)
            .block(BLOCK_SULPHURIC_ACID)
            .bucket(SULPHURIC_ACID_BUCKET);
    }
}
