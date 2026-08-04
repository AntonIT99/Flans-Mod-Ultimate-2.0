package com.flansmodultimate.common.block;

import com.flansmodultimate.common.inventory.DriveableCraftingMenu;
import com.flansmodultimate.common.inventory.GunWorkbenchMenu;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
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
    @NotNull
    public MenuProvider getMenuProvider(BlockState state, @NotNull Level level, @NotNull BlockPos pos)
    {
        return new SimpleMenuProvider((containerId, inv, player) -> new GunWorkbenchMenu(containerId, inv, pos), state.getBlock().getName());
    }

    @Override
    @NotNull
    public InteractionResult use(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit)
    {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer)
        {
            MenuProvider provider = player.isShiftKeyDown()
                ? new SimpleMenuProvider((containerId, inventory, ignored) -> new DriveableCraftingMenu(containerId, inventory, pos),
                    Component.translatable("gui.flansmodultimate.driveable.crafting"))
                : getMenuProvider(state, level, pos);
            NetworkHooks.openScreen(serverPlayer, provider, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
