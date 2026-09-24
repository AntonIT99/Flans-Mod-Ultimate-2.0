package com.flansmodultimate.platform.creative;

import com.flansmodultimate.common.driveables.DriveableData;
import com.flansmodultimate.common.types.DriveableType;
import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Version boundary for reading driveable stacks while building creative tabs. */
public final class CreativeTabPlatform
{
    private CreativeTabPlatform() {}

    public static DriveableData readDriveableData(DriveableType type, ItemStack stack,
                                                 CreativeModeTab.ItemDisplayParameters parameters)
    {
        return DriveableData.fromStack(type, stack);
    }

    public static ResourceLocation itemId(Item item)
    {
        return ForgeRegistries.ITEMS.getKey(item);
    }
}
