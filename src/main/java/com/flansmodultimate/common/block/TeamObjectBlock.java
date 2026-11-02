package com.flansmodultimate.common.block;

import com.flansmodultimate.common.item.ItemOpStick;
import com.flansmodultimate.common.teams.TeamsObjectProtection;
import com.flansmodultimate.platform.block.FlanEntityBlock;
import org.jetbrains.annotations.NotNull;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.PushReaction;

/** Shared direct-break and explosion policy for Teams infrastructure blocks. */
public abstract class TeamObjectBlock extends FlanEntityBlock
{
    protected TeamObjectBlock(Properties properties)
    {
        super(properties);
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid)
    {
        if (TeamsObjectProtection.isProtected(level, pos) && !ItemOpStick.canUse(player))
            return false;
        return TeamsObjectProtection.removeDirectly(level, pos, () -> super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid));
    }

    @Override
    public void onBlockExploded(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Explosion explosion)
    {
        if (!TeamsObjectProtection.isProtected(level, pos))
            super.onBlockExploded(state, level, pos, explosion);
    }

    @Override
    public boolean canDropFromExplosion(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion)
    {
        return !(level instanceof Level world && TeamsObjectProtection.isProtected(world, pos)) && super.canDropFromExplosion(state, level, pos, explosion);
    }

    @Override
    public boolean canEntityDestroy(BlockState state, BlockGetter level, BlockPos pos, Entity entity)
    {
        return !(level instanceof Level world && TeamsObjectProtection.isProtected(world, pos)) && super.canEntityDestroy(state, level, pos, entity);
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state)
    {
        // The hook has no world; infrastructure must never move independently of its saved Teams registration.
        return PushReaction.BLOCK;
    }
}
