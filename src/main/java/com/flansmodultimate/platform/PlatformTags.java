package com.flansmodultimate.platform;

import net.neoforged.neoforge.common.Tags;

import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/** Version boundary for loader convention tags whose names changed (`forge:` versus `c:`). */
public final class PlatformTags
{
    /** Glass blocks, without panes. */
    public static final TagKey<Block> GLASS_BLOCKS = Tags.Blocks.GLASS_BLOCKS;

    private PlatformTags() {}
}
