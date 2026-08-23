package com.flansmodultimate.common.guns.penetration;

import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;

public record PenetrableBlock(double hardness, boolean breaksOnPenetration)
{
    private static final Map<Identifier, PenetrableBlock> penetrableBlocks = new HashMap<>();

    @Nullable
    public static PenetrableBlock get(BlockState state)
    {
        return penetrableBlocks.get(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
    }

    public static PenetrableBlock put(Identifier rl, PenetrableBlock penetrableBlock)
    {
        return penetrableBlocks.put(rl, penetrableBlock);
    }

    public static void clear()
    {
        penetrableBlocks.clear();
    }
}