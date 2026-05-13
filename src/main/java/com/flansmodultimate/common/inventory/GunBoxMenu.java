package com.flansmodultimate.common.inventory;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.block.GunBoxBlock;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public class GunBoxMenu extends AbstractContainerMenu
{
    public static final int PLAYER_INV_START = 0;
    public static final int PLAYER_INV_END = 36; // 27 inv + 9 hotbar

    private final ContainerLevelAccess access;
    @Getter
    private final BlockPos pos;
    @Getter
    private final GunBoxBlock block;

    public GunBoxMenu(int id, Inventory playerInv, BlockPos pos, GunBoxBlock block)
    {
        super(FlansMod.gunBoxMenu.get(), id);
        this.pos = pos;
        this.access = ContainerLevelAccess.create(playerInv.player.level(), pos);
        this.block = block;

        // Match the 1.7.10 weapon box layout.
        int invX = 57;
        int invY = 151;
        for (int row = 0; row < 3; row++)
        {
            for (int col = 0; col < 9; col++)
            {
                int slotIndex = col + (row + 1) * 9;
                addSlot(new Slot(playerInv, slotIndex, invX + col * 18, invY + row * 18));
            }
        }

        int hotbarY = 209;
        for (int col = 0; col < 9; col++)
        {
            addSlot(new Slot(playerInv, col, invX + col * 18, hotbarY));
        }
    }

    public static GunBoxMenu createFromNetwork(int id, Inventory playerInv, FriendlyByteBuf buf)
    {
        Level level = playerInv.player.level();
        BlockPos pos = buf.readBlockPos();
        Block block = level.getBlockState(pos).getBlock();
        if (!(block instanceof GunBoxBlock gunBoxBlock))
            throw new IllegalStateException("Block at " + pos + " is not an instance of " + GunBoxBlock.class.getSimpleName());
        return new GunBoxMenu(id, playerInv, pos, gunBoxBlock);
    }

    @Override
    @NotNull
    public ItemStack quickMoveStack(@NotNull Player player, int index)
    {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        if (index < 27)
        {
            if (!moveItemStackTo(stack, 27, 36, false))
                return ItemStack.EMPTY;
        }
        else
        {
            if (!moveItemStackTo(stack, 0, 27, false))
                return ItemStack.EMPTY;
        }

        if (stack.isEmpty())
            slot.set(ItemStack.EMPTY);
        else
            slot.setChanged();

        return copy;
    }

    @Override
    public boolean stillValid(@NotNull Player player)
    {
        return stillValid(access, player, block);
    }
}
