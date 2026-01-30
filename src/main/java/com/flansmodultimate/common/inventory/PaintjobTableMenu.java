package com.flansmodultimate.common.inventory;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.client.gui.PaintjobTableScreen;
import com.flansmodultimate.common.block.entity.PaintjobTableBlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.SlotItemHandler;
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
import net.minecraft.world.level.block.entity.BlockEntity;

public class PaintjobTableMenu extends AbstractContainerMenu
{
    private static final double MAX_DISTANCE = 64.0;

    private final ContainerLevelAccess access;

    // indices
    private static final int TE_SLOTS = 2;
    private static final int PLAYER_INV_START = TE_SLOTS;
    private static final int PLAYER_INV_END = PLAYER_INV_START + 27;
    private static final int HOTBAR_START = PLAYER_INV_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    public PaintjobTableMenu(int id, Inventory playerInv, ContainerLevelAccess access, PaintjobTableBlockEntity table)
    {
        super(FlansMod.paintjobTableMenu.get(), id);
        this.access = access;

        // TE slots via capability
        table.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
            addSlot(new SlotItemHandler(handler, 0, 187, PaintjobTableScreen.TOP_H + 17));
            addSlot(new SlotItemHandler(handler, 1, 187, PaintjobTableScreen.TOP_H + 71));
        });

        // Player inventory
        for (int row = 0; row < 3; row++)
        {
            for (int col = 0; col < 9; col++)
            {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, PaintjobTableScreen.TOP_H + 62 + row * 18));
            }
        }
        // Hotbar
        for (int col = 0; col < 9; col++)
        {
            addSlot(new Slot(playerInv, col, 8 + col * 18, PaintjobTableScreen.TOP_H + 120));
        }
    }

    public static PaintjobTableMenu fromNetwork(int id, Inventory inv, FriendlyByteBuf buf)
    {
        Level level = inv.player.level();
        BlockPos pos = buf.readBlockPos();
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof PaintjobTableBlockEntity table))
            throw new IllegalStateException("PaintjobTable BE missing at " + pos);
        return new PaintjobTableMenu(id, inv, ContainerLevelAccess.create(level, pos), table);
    }

    @Override
    public boolean stillValid(@NotNull Player player)
    {
        return access.evaluate((level, pos) -> {
            Block block = level.getBlockState(pos).getBlock();
            return block == FlansMod.paintjobTable.get() && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= MAX_DISTANCE;
        }, true);
    }

    @Override
    @NotNull
    public ItemStack quickMoveStack(@NotNull Player player, int index)
    {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stackInSlot = slot.getItem();
        ItemStack copy = stackInSlot.copy();

        // shift-click from TE -> player
        if (index < TE_SLOTS)
        {
            if (!moveItemStackTo(stackInSlot, PLAYER_INV_START, HOTBAR_END, true))
                return ItemStack.EMPTY;
        }
        // shift-click from player -> TE (try paintable slot then paintcans)
        else if (!moveItemStackTo(stackInSlot, 0, 1, false) &&
            !moveItemStackTo(stackInSlot, 1, 2, false))
        {
            return ItemStack.EMPTY;
        }

        if (stackInSlot.isEmpty())
            slot.set(ItemStack.EMPTY);
        else
            slot.setChanged();

        return copy;
    }

    public ItemStack getPaintableStack()
    {
        return slots.get(0).getItem();
    }

    public ItemStack getPaintCanStack()
    {
        return slots.get(1).getItem();
    }
}

