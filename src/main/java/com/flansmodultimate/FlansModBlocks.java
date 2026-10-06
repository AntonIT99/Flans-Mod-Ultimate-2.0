package com.flansmodultimate;

import com.flansmodultimate.common.block.GunWorkbenchBlock;
import com.flansmodultimate.common.block.PaintjobTableBlock;
import com.flansmodultimate.common.block.TeamSpawnerBlock;
import com.flansmodultimate.common.block.VehicleCraftingTableBlock;
import com.flansmodultimate.common.block.entity.ItemHolderBlockEntity;
import com.flansmodultimate.common.block.entity.PaintjobTableBlockEntity;
import com.flansmodultimate.common.block.entity.TeamSpawnerBlockEntity;
import com.flansmodultimate.common.types.EnumType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import java.util.function.Supplier;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FlansModBlocks
{
    public static final Supplier<? extends Block> gunWorkbench = FlansModRegistries.blockRegistry.register("gunworkbench", () -> new GunWorkbenchBlock(
        BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3F, 6F).sound(SoundType.METAL).requiresCorrectToolForDrops().pushReaction(PushReaction.BLOCK)));
    public static final Supplier<? extends Block> vehicleCraftingTable = FlansModRegistries.blockRegistry.register("vehiclecraftingtable", () -> new VehicleCraftingTableBlock(
        BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3F, 6F).sound(SoundType.METAL).requiresCorrectToolForDrops().pushReaction(PushReaction.BLOCK)));
    public static final Supplier<? extends Block> paintjobTable = FlansModRegistries.blockRegistry.register("paintjobtable",
        () -> new PaintjobTableBlock(BlockBehaviour.Properties.of().strength(2F, 4F).sound(SoundType.STONE)));
    public static final Supplier<? extends Block> playerSpawner = FlansModRegistries.blockRegistry.register("teams_player_spawner",
        () -> new TeamSpawnerBlock(TeamSpawnerBlockEntity.Mode.PLAYER,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1F, 2F).sound(SoundType.METAL).noOcclusion()));
    public static final Supplier<? extends Block> itemSpawner = FlansModRegistries.blockRegistry.register("teams_item_spawner",
        () -> new TeamSpawnerBlock(TeamSpawnerBlockEntity.Mode.ITEM,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1F, 2F).sound(SoundType.METAL).noOcclusion()));
    public static final Supplier<? extends Block> vehicleSpawner = FlansModRegistries.blockRegistry.register("teams_vehicle_spawner",
        () -> new TeamSpawnerBlock(TeamSpawnerBlockEntity.Mode.VEHICLE,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1F, 2F).sound(SoundType.METAL).noOcclusion()));

    public static final Supplier<? extends BlockEntityType<PaintjobTableBlockEntity>> paintjobTableBlockEntity = FlansModRegistries.blockEntityRegistry.register("paintjobtable",
        () -> BlockEntityType.Builder.of(PaintjobTableBlockEntity::new, paintjobTable.get()).build(null));
    public static final Supplier<? extends BlockEntityType<ItemHolderBlockEntity>> itemHolderBlockEntity = FlansModRegistries.blockEntityRegistry.register("item_holder",
        () -> BlockEntityType.Builder.of(ItemHolderBlockEntity::new, FlansMod.getRegisteredBlocks(EnumType.ITEM_HOLDER)).build(null));
    public static final Supplier<? extends BlockEntityType<TeamSpawnerBlockEntity>> teamSpawnerBlockEntity = FlansModRegistries.blockEntityRegistry.register("teams_spawner",
        () -> BlockEntityType.Builder.of(TeamSpawnerBlockEntity::new, playerSpawner.get(), itemSpawner.get(), vehicleSpawner.get()).build(null));

    /** Forces supplier registration without resolving any registry entries. */
    static void initialize()
    {
        // no-op
    }
}
