package com.flansmodultimate.common.block;

import com.flansmodultimate.FlansModBlocks;
import com.flansmodultimate.common.block.entity.TeamBannerBlockEntity;
import com.flansmodultimate.common.item.ItemOpStick;
import com.flansmodultimate.common.teams.TeamsManager;
import com.flansmodultimate.platform.block.FlanEntityBlock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.*;

/** Wall decoration linked to a Teams base; the cloth hangs below its attachment block, like a vanilla wall banner. */
@SuppressWarnings("deprecation") // These are the supported 1.20.1 block hooks; vanilla deprecates direct calls, not overrides.
public final class TeamBannerBlock extends FlanEntityBlock
{
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public TeamBannerBlock(Properties properties)
    {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext context)
    {
        for (Direction direction : context.getNearestLookingDirections())
        {
            if (!direction.getAxis().isHorizontal())
                continue;
            BlockState state = defaultBlockState().setValue(FACING, direction.getOpposite());
            if (state.canSurvive(context.getLevel(), context.getClickedPos()))
                return state;
        }
        return null;
    }

    @Override
    public boolean canSurvive(@NotNull BlockState state, @NotNull LevelReader level, @NotNull BlockPos pos)
    {
        Direction facing = state.getValue(FACING);
        BlockPos support = pos.relative(facing.getOpposite());
        return level.getBlockState(support).isFaceSturdy(level, support, facing);
    }

    @NotNull
    @Override
    public BlockState updateShape(@NotNull BlockState state, @NotNull Direction direction, @NotNull BlockState neighbour, @NotNull LevelAccessor level, @NotNull BlockPos pos,
        @NotNull BlockPos neighbourPos)
    {
        return direction == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos)
            ? Blocks.AIR.defaultBlockState()
            : super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
    }

    @NotNull
    @Override
    public BlockState rotate(@NotNull BlockState state, @NotNull Rotation rotation)
    {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @NotNull
    @Override
    public BlockState mirror(@NotNull BlockState state, @NotNull Mirror mirror)
    {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @NotNull
    @Override
    public VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context)
    {
        return switch (state.getValue(FACING))
        {
            case NORTH -> box(4D, 0D, 14D, 12D, 16D, 16D);
            case SOUTH -> box(4D, 0D, 0D, 12D, 16D, 2D);
            case WEST -> box(14D, 0D, 4D, 16D, 16D, 12D);
            case EAST -> box(0D, 0D, 4D, 2D, 16D, 12D);
            default -> Shapes.empty();
        };
    }

    @NotNull
    @Override
    public RenderShape getRenderShape(@NotNull BlockState state)
    {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state)
    {
        return new TeamBannerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type)
    {
        return level.isClientSide ? null : createTickerHelper(type, FlansModBlocks.teamBannerBlockEntity.get(), TeamBannerBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult interact(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand)
    {
        ItemStack held = player.getItemInHand(hand);
        if (!(held.getItem() instanceof ItemOpStick stick) || !(level.getBlockEntity(pos) instanceof TeamBannerBlockEntity banner))
            return InteractionResult.PASS;
        if (player instanceof ServerPlayer serverPlayer)
            stick.useOnTeamObject(serverPlayer, banner, held);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState newState, boolean movedByPiston)
    {
        if (!level.isClientSide && !state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof TeamBannerBlockEntity banner && banner.getBaseId() != null)
            TeamsManager.getInstance().getBase(banner.getBaseId()).ifPresent(base -> base.removeObject(banner.getObjectId()));
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
