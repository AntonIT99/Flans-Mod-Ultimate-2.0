package com.flansmodultimate.common.block;

import com.mojang.serialization.MapCodec;
import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.block.entity.TeamSpawnerBlockEntity;
import com.flansmodultimate.common.item.ItemOpStick;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class TeamSpawnerBlock extends BaseEntityBlock
{
    private static final VoxelShape SHAPE = box(0D, 0D, 0D, 16D, 1D, 16D);
    private final TeamSpawnerBlockEntity.Mode mode;

    public TeamSpawnerBlock(TeamSpawnerBlockEntity.Mode mode, BlockBehaviour.Properties properties)
    {
        super(properties);
        this.mode = mode;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return MapCodec.unit(this); }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state)
    {
        return new TeamSpawnerBlockEntity(pos, state, mode);
    }

    @NotNull
    @Override
    public RenderShape getRenderShape(@NotNull BlockState state)
    {
        return RenderShape.MODEL;
    }

    @NotNull
    @Override
    public VoxelShape getShape(@NotNull BlockState state, @NotNull net.minecraft.world.level.BlockGetter level,
                               @NotNull BlockPos pos, @NotNull CollisionContext context)
    {
        return SHAPE;
    }

    @NotNull
    @Override
    public VoxelShape getCollisionShape(@NotNull BlockState state, @NotNull net.minecraft.world.level.BlockGetter level,
                                        @NotNull BlockPos pos, @NotNull CollisionContext context)
    {
        return Shapes.empty();
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type)
    {
        return level.isClientSide() ? null : createTickerHelper(type, FlansMod.teamSpawnerBlockEntity.get(), TeamSpawnerBlockEntity::serverTick);
    }

    @NotNull
    @Override
    protected InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                                               @NotNull Player player, @NotNull BlockHitResult hit)
    {
        return interact(level, pos, player, InteractionHand.MAIN_HAND);
    }

    @Override
    protected InteractionResult useItemOn(@NotNull ItemStack stack, @NotNull BlockState state, @NotNull Level level,
                                               @NotNull BlockPos pos, @NotNull Player player,
                                               @NotNull InteractionHand hand, @NotNull BlockHitResult hit)
    {
        InteractionResult result = interact(level, pos, player, hand);
        return result == InteractionResult.PASS ? InteractionResult.TRY_WITH_EMPTY_HAND
            : level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }

    private InteractionResult interact(Level level, BlockPos pos, Player player, InteractionHand hand)
    {
        if (!(level.getBlockEntity(pos) instanceof TeamSpawnerBlockEntity spawner))
            return InteractionResult.PASS;
        if (level.isClientSide())
            return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)
            || !serverPlayer.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
            return InteractionResult.CONSUME;

        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof ItemOpStick stick)
        {
            stick.useOnTeamObject(serverPlayer, spawner, held);
            return InteractionResult.CONSUME;
        }
        if (spawner.getMode() != TeamSpawnerBlockEntity.Mode.PLAYER && !held.isEmpty())
        {
            spawner.addTemplate(held.copyWithCount(held.getCount()));
            player.sendSystemMessage(Component.literal("Added " + held.getHoverName().getString() + " to this spawner"));
        }
        else if (held.isEmpty())
        {
            spawner.cycleSpawnDelay();
            player.sendSystemMessage(Component.literal("Spawn delay: " + spawner.getSpawnDelayTicks() / 20 + " seconds"));
        }
        return InteractionResult.CONSUME;
    }
}
