package com.flansmodultimate.common.block;

import com.flansmodultimate.common.inventory.GunWorkbenchMenu;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class GunWorkbenchBlock extends Block
{
    public GunWorkbenchBlock(Properties props)
    {
        super(props);
    }

    @Override
    public InteractionResult use(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit)
    {
        if (level.isClientSide)
            return InteractionResult.SUCCESS;

        if (player instanceof ServerPlayer sp)
        {
            MenuProvider provider = new SimpleMenuProvider((containerId, inv, p) -> new GunWorkbenchMenu(containerId, inv, ContainerLevelAccess.create(level, pos)), state.getBlock().getName());
            NetworkHooks.openScreen(sp, provider, pos);
        }

        return InteractionResult.CONSUME;
    }
}
