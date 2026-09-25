package com.flansmodultimate.platform.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.SetNbtFunction;

/** Version boundary for loot table identifiers: 1.20.1 names tables by id, 1.21 by resource key. */
public final class LootTablePlatform
{
    private LootTablePlatform() {}

    public static ResourceLocation id(ResourceLocation table)
    {
        return table;
    }

    /** A loot function that sets the stack's custom data to the tag. */
    public static LootItemFunction.Builder setCustomData(CompoundTag tag)
    {
        return SetNbtFunction.setTag(tag);
    }
}
