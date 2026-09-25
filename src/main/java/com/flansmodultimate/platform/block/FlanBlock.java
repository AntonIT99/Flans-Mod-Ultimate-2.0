package com.flansmodultimate.platform.block;

import org.jetbrains.annotations.NotNull;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * {@link Block} whose right-click is handled by one shared {@link #interact} method, called from
 * the 1.20.1 {@code use} method.
 */
public abstract class FlanBlock extends Block
{
    protected FlanBlock(Properties properties)
    {
        super(properties);
    }

    /** Handles a right-click on this block with the given hand. */
    protected abstract InteractionResult interact(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand);

    @Override
    @NotNull
    @SuppressWarnings("deprecation") // Block#use is the 1.20.1 interaction entry point.
    public final InteractionResult use(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player,
                                       @NotNull InteractionHand hand, @NotNull BlockHitResult hit)
    {
        return interact(state, level, pos, player, hand);
    }
}
