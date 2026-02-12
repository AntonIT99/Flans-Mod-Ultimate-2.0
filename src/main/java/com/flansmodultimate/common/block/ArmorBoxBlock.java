package com.flansmodultimate.common.block;

import com.flansmodultimate.common.inventory.ArmorBoxMenu;
import com.flansmodultimate.common.types.ArmorBoxType;
import lombok.Getter;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

public class ArmorBoxBlock extends Block implements IFlanBlock<ArmorBoxType>
{
    @Getter
    protected final ArmorBoxType configType;

    public ArmorBoxBlock(ArmorBoxType type)
    {
        super(BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOD)
            .strength(2.0F, 4.0F)
            .sound(SoundType.WOOD)
        );
        configType = type;
    }

    @Override
    @NotNull
    public Block asBlock()
    {
        return this;
    }

    @Override
    @NotNull
    public MenuProvider getMenuProvider(BlockState state, @NotNull Level level, @NotNull BlockPos pos)
    {
        return new SimpleMenuProvider((containerId, inv, player) -> new ArmorBoxMenu(containerId), state.getBlock().getName());
    }

    @Override
    @NotNull
    public InteractionResult use(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit)
    {
        if (player.isShiftKeyDown())
            return InteractionResult.PASS;

        if (!level.isClientSide)
        {
            MenuProvider provider = getMenuProvider(state, level, pos);
            NetworkHooks.openScreen((ServerPlayer) player, provider, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * DO NOT call this directly from client in 1.20.1.
     * If a button in your screen triggers buying, send a packet to the server.
     */
    /*public void buyArmourServer(String shortName, int piece, ServerPlayer player)
    {
        ArmourBoxType.ArmourBoxEntry entryPicked = null;
        for (var page : type.pages) {
            if (page.shortName.equals(shortName)) {
                entryPicked = page;
                break;
            }
        }
        if (entryPicked == null) return;

        // 1.20.1 ItemStack construction:
        ItemStack resultStack = new ItemStack(entryPicked.armours[piece].item);

        // Your CraftingInstance needs a rewrite too:
        // - InventoryPlayer -> player.getInventory()
        // - canCraft / craft should be server-side only
        CraftingInstance crafting = new CraftingInstance(player.getInventory(),
            entryPicked.requiredStacks[piece],
            resultStack);

        if (crafting.canCraft(player)) {
            crafting.craft(player);
        }
    }*/
}
