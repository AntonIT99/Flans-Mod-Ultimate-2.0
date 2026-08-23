package com.flansmodultimate.common.block;

import com.mojang.serialization.MapCodec;
import com.flansmodultimate.common.block.entity.PaintjobTableBlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class PaintjobTableBlock extends BaseEntityBlock
{
    public static final MapCodec<PaintjobTableBlock> CODEC = simpleCodec(PaintjobTableBlock::new);
    public PaintjobTableBlock(Properties props)
    {
        super(props);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

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
    protected InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                                               @NotNull Player player, @NotNull BlockHitResult hit)
    {
        return open(level, pos, player);
    }

    @Override
    protected InteractionResult useItemOn(@NotNull ItemStack stack, @NotNull BlockState state, @NotNull Level level,
                                               @NotNull BlockPos pos, @NotNull Player player,
                                               @NotNull InteractionHand hand, @NotNull BlockHitResult hit)
    {
        InteractionResult result = open(level, pos, player);
        return result == InteractionResult.PASS ? InteractionResult.TRY_WITH_EMPTY_HAND
            : level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }

    private InteractionResult open(Level level, BlockPos pos, Player player)
    {
        if (player.isShiftKeyDown())
            return InteractionResult.PASS;

        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof PaintjobTableBlockEntity blockEntity)
        {
            serverPlayer.openMenu(blockEntity, buffer -> buffer.writeBlockPos(pos));
        }
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
    }

}
