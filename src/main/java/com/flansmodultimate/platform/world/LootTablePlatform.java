package com.flansmodultimate.platform.world;

import net.minecraft.resources.ResourceLocation;

/** Version boundary for loot table identifiers: 1.20.1 names tables by id, 1.21 by resource key. */
public final class LootTablePlatform
{
    private LootTablePlatform() {}

    public static ResourceLocation id(ResourceLocation table)
    {
        return table;
    }
}
