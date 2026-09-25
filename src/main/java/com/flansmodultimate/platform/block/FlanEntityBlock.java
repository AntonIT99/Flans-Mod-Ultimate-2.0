package com.flansmodultimate.platform.block;

import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.NotNull;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * {@link BaseEntityBlock} whose right-click is handled by one shared {@link #interact} method. On 1.21,
 * {@code useWithoutItem} (main hand) and {@code useItemOn} both call it; a pass lets the default interaction run.
 */
public abstract class FlanEntityBlock extends BaseEntityBlock
{
    protected FlanEntityBlock(Properties properties)
    {
        super(properties);
    }

    /** Handles a right-click on this block with the given hand. By default the block does nothing, as in vanilla. */
    protected InteractionResult interact(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand)
    {
        return InteractionResult.PASS;
    }

    @Override
    @NotNull
    protected MapCodec<? extends BaseEntityBlock> codec()
    {
        return MapCodec.unit(this);
    }

    @Override
    @NotNull
    protected final InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                                                     @NotNull Player player, @NotNull BlockHitResult hit)
    {
        return interact(state, level, pos, player, InteractionHand.MAIN_HAND);
    }

    @Override
    @NotNull
    protected final ItemInteractionResult useItemOn(@NotNull ItemStack stack, @NotNull BlockState state, @NotNull Level level,
                                                    @NotNull BlockPos pos, @NotNull Player player,
                                                    @NotNull InteractionHand hand, @NotNull BlockHitResult hit)
    {
        InteractionResult result = interact(state, level, pos, player, hand);
        return result == InteractionResult.PASS ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
            : ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
}
