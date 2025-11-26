package com.flansmodultimate;

import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;

public record PenetrableBlock(double hardness, boolean breaksOnPenetration)
{
    private static final Map<ResourceLocation, PenetrableBlock> penetrableBlocks = new HashMap<>();

    @Nullable
    public static PenetrableBlock get(BlockState state)
    {
        return penetrableBlocks.get(ForgeRegistries.BLOCKS.getKey(state.getBlock()));
    }

    public static PenetrableBlock put(ResourceLocation rl, PenetrableBlock penetrableBlock)
    {
        return penetrableBlocks.put(rl, penetrableBlock);
    }
}