package com.flansmodultimate.common.block;

import com.flansmodultimate.common.inventory.DriveableCraftingMenu;
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

/** The legacy Vehicle Crafting Table: the only survival way to build a driveable. */
public class VehicleCraftingTableBlock extends Block
{
    public VehicleCraftingTableBlock(Properties props)
    {
        super(props);
    }

    @Override
    @NotNull
    public MenuProvider getMenuProvider(BlockState state, @NotNull Level level, @NotNull BlockPos pos)
    {
        return new SimpleMenuProvider((containerId, inventory, ignored) -> new DriveableCraftingMenu(containerId, inventory, pos),
            Component.translatable("gui.flansmodultimate.driveable.crafting"));
    }

    @Override
    @NotNull
    public InteractionResult use(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player,
                                 @NotNull InteractionHand hand, @NotNull BlockHitResult hit)
    {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer)
            NetworkHooks.openScreen(serverPlayer, getMenuProvider(state, level, pos), pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
