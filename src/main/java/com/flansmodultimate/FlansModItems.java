package com.flansmodultimate;

import com.flansmodultimate.common.item.FlagpoleItem;
import com.flansmodultimate.common.item.ItemOpStick;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FlansModItems
{
    public static final Supplier<? extends Item> rainbowPaintcan = FlansModRegistries.itemRegistry.register("rainbowpaintcan", () -> new Item(new Item.Properties()));
    public static final Supplier<? extends Item> gunWorkbenchItem = FlansModRegistries.itemRegistry.register("gunworkbench", () -> new BlockItem(FlansModBlocks.gunWorkbench.get(), new Item.Properties()));
    public static final Supplier<? extends Item> vehicleCraftingTableItem = FlansModRegistries.itemRegistry.register("vehiclecraftingtable", () -> new BlockItem(FlansModBlocks.vehicleCraftingTable.get(), new Item.Properties()));
    public static final Supplier<? extends Item> paintjobTableItem = FlansModRegistries.itemRegistry.register("paintjobtable", () -> new BlockItem(FlansModBlocks.paintjobTable.get(), new Item.Properties()));
    public static final Supplier<? extends Item> playerSpawnerItem = FlansModRegistries.itemRegistry.register("teams_player_spawner", () -> new BlockItem(FlansModBlocks.playerSpawner.get(), new Item.Properties()));
    public static final Supplier<? extends Item> itemSpawnerItem = FlansModRegistries.itemRegistry.register("teams_item_spawner", () -> new BlockItem(FlansModBlocks.itemSpawner.get(), new Item.Properties()));
    public static final Supplier<? extends Item> vehicleSpawnerItem = FlansModRegistries.itemRegistry.register("teams_vehicle_spawner", () -> new BlockItem(FlansModBlocks.vehicleSpawner.get(), new Item.Properties()));
    public static final Supplier<? extends Item> opStick = FlansModRegistries.itemRegistry.register("op_stick", ItemOpStick::new);
    public static final Supplier<? extends Item> flagpoleItem = FlansModRegistries.itemRegistry.register("flagpole", FlagpoleItem::new);

    /** Forces supplier registration without resolving any registry entries. */
    static void initialize()
    {
        // no-op
    }
}
