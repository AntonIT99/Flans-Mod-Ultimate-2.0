package com.flansmodultimate.platform.world;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootTable;

/** Version boundary for loot table identifiers: 1.20.1 names tables by id, 1.21 by resource key. */
public final class LootTablePlatform
{
    private LootTablePlatform() {}

    public static ResourceLocation id(ResourceKey<LootTable> table)
    {
        return table.location();
    }
}
