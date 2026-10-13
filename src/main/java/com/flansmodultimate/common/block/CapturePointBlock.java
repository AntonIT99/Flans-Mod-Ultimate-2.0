package com.flansmodultimate.common.block;

import com.flansmodultimate.common.block.entity.CapturePointBlockEntity;
import com.flansmodultimate.common.item.ItemOpStick;
import com.flansmodultimate.common.teams.TeamsManager;
import com.flansmodultimate.platform.block.FlanEntityBlock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;

/**
 * The foot of a capture point. The block itself is only something to aim the operator stick at;
 * the pole and flag above it are drawn by the block entity renderer.
 */
public final class CapturePointBlock extends FlanEntityBlock
{
    private static final VoxelShape SHAPE = box(6D, 0D, 6D, 10D, 16D, 10D);

    public CapturePointBlock(BlockBehaviour.Properties properties)
    {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state)
    {
        return new CapturePointBlockEntity(pos, state);
    }

    @NotNull
    @Override
    public RenderShape getRenderShape(@NotNull BlockState state)
    {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @NotNull
    @Override
    public VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context)
    {
        return SHAPE;
    }

    @NotNull
    @Override
    public VoxelShape getCollisionShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context)
    {
        return Shapes.empty();
    }

    @Override
    public boolean canSurvive(@NotNull BlockState state, @NotNull LevelReader level, @NotNull BlockPos pos)
    {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP, SupportType.CENTER);
    }

    @Override
    public void setPlacedBy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, @Nullable LivingEntity placer, @NotNull ItemStack stack)
    {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && placer != null && level.getBlockEntity(pos) instanceof CapturePointBlockEntity point)
            point.setYaw(placer.getYRot());
    }

    @Override
    public void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState newState, boolean movedByPiston)
    {
        // Broken rather than unloaded: the map must stop counting on this point.
        if (!level.isClientSide && !state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof CapturePointBlockEntity point)
            TeamsManager.getInstance().releaseBase(point);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected InteractionResult interact(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand)
    {
        if (!(level.getBlockEntity(pos) instanceof CapturePointBlockEntity point))
            return InteractionResult.PASS;
        if (level.isClientSide)
            return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer) || !ItemOpStick.canUse(player))
            return InteractionResult.CONSUME;

        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof ItemOpStick stick)
            stick.useOnTeamObject(serverPlayer, point, held);
        else
            player.displayClientMessage(Component.translatable("message.flansmodultimate.teams.capture_point_hint"), true);
        return InteractionResult.CONSUME;
    }
}
