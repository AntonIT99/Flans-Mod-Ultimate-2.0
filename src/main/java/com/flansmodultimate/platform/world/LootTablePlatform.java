package com.flansmodultimate.platform.world;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.SetComponentsFunction;

/** Version boundary for loot table identifiers: 1.20.1 names tables by id, 1.21 by resource key. */
public final class LootTablePlatform
{
    private LootTablePlatform() {}

    public static ResourceLocation id(ResourceKey<LootTable> table)
    {
        return table.location();
    }

    /** A loot function that sets the stack's custom data to the tag. */
    public static LootItemFunction.Builder setCustomData(CompoundTag tag)
    {
        return SetComponentsFunction.setComponent(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}
