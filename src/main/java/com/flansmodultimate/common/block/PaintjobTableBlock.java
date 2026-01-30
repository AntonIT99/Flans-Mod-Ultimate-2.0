package com.flansmodultimate.common.block;

import com.flansmodultimate.common.block.entity.PaintjobTableBlockEntity;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class PaintjobTableBlock extends BaseEntityBlock
{
    public PaintjobTableBlock(Properties props)
    {
        super(props);
    }

    @Override
    public RenderShape getRenderShape(@NotNull BlockState state)
    {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state)
    {
        return new PaintjobTableBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit)
    {
        if (level.isClientSide)
            return InteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof PaintjobTableBlockEntity table && player instanceof ServerPlayer sp)
            NetworkHooks.openScreen(sp, table, pos);

        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, @NotNull Level level, @NotNull BlockPos pos, BlockState newState, boolean isMoving)
    {
        if (!state.is(newState.getBlock()))
        {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof PaintjobTableBlockEntity table)
            {
                table.dropContents(level, pos);
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}
