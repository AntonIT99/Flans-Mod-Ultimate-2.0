package com.flansmodultimate.common.inventory;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.block.ArmorBoxBlock;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public class ArmorBoxMenu extends AbstractContainerMenu
{
    public static final int PLAYER_INV_START = 0;
    public static final int PLAYER_INV_END = 36; // 27 inv + 9 hotbar

    private final ContainerLevelAccess access;
    @Getter
    private final BlockPos pos;
    @Getter
    private final ArmorBoxBlock block;

    public ArmorBoxMenu(int id, Inventory playerInv, BlockPos pos, ArmorBoxBlock block)
    {
        super(FlansMod.armorBoxMenu.get(), id);
        this.pos = pos;
        this.access = ContainerLevelAccess.create(playerInv.player.level(), pos);
        this.block = block;

        // Player inventory
        // 3 rows * 9 cols starting at (8, 100)
        int invX = 8;
        int invY = 100;
        for (int row = 0; row < 3; row++)
        {
            for (int col = 0; col < 9; col++)
            {
                int slotIndex = col + (row + 1) * 9;
                addSlot(new Slot(playerInv, slotIndex, invX + col * 18, invY + row * 18));
            }
        }

        // Hotbar starting at (8, 158)
        int hotbarY = 158;
        for (int col = 0; col < 9; col++)
        {
            addSlot(new Slot(playerInv, col, invX + col * 18, hotbarY));
        }
    }

    public static ArmorBoxMenu createFromNetwork(int id, Inventory playerInv, RegistryFriendlyByteBuf buf)
    {
        Level level = playerInv.player.level();
        BlockPos pos = buf.readBlockPos();
        Block block = level.getBlockState(pos).getBlock();
        if (!(block instanceof ArmorBoxBlock armorBoxBlock))
            throw new IllegalStateException("Block at " + pos + " is not an instance of " + ArmorBoxBlock.class.getSimpleName());
        return new ArmorBoxMenu(id, playerInv, pos, armorBoxBlock);
    }

    @Override
    public boolean stillValid(@NotNull Player player)
    {
        return stillValid(access, player, block);
    }

    @Override
    @NotNull
    public ItemStack quickMoveStack(@NotNull Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        // Only player inventory exists; shift-click can swap between inv and hotbar
        // Slots are 0..35 in our menu
        if (index < 27)
        {
            // inventory -> hotbar
            if (!moveItemStackTo(stack, 27, 36, false))
                return ItemStack.EMPTY;
        }
        else
        {
            // hotbar -> inventory
            if (!moveItemStackTo(stack, 0, 27, false))
                return ItemStack.EMPTY;
        }

        if (stack.isEmpty())
            slot.set(ItemStack.EMPTY);
        else
            slot.setChanged();

        return copy;
    }
}
