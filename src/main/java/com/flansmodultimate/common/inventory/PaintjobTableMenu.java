package com.flansmodultimate.common.inventory;

import com.flansmodultimate.FlansMod;
import org.jetbrains.annotations.NotNull;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class PaintjobTableMenu extends AbstractContainerMenu
{
    private static final double MAX_DISTANCE = 64.0;

    private final ContainerLevelAccess access;
    /** Per-menu workspace: each player paints in their own two slots. */
    private final SimpleContainer workspace = new SimpleContainer(TE_SLOTS);

    // indices
    private static final int TE_SLOTS = 2;
    private static final int PLAYER_INV_START = TE_SLOTS;
    private static final int PLAYER_INV_END = PLAYER_INV_START + 27;
    private static final int HOTBAR_START = PLAYER_INV_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;
    private static final int GUI_TOP_H = 92;

    public PaintjobTableMenu(int id, Inventory playerInv, BlockPos blockPos)
    {
        super(FlansMod.paintjobTableMenu.get(), id);
        this.access = ContainerLevelAccess.create(playerInv.player.level(), blockPos);

        // The two working slots belong to this menu, not to the block, so several players
        // can paint at the same table at once without seeing or taking each other's items
        addSlot(new Slot(workspace, 0, 187, GUI_TOP_H + 17));
        addSlot(new Slot(workspace, 1, 187, GUI_TOP_H + 71));

        // Player inventory
        for (int row = 0; row < 3; row++)
        {
            for (int col = 0; col < 9; col++)
            {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, GUI_TOP_H + 62 + row * 18));
            }
        }
        // Hotbar
        for (int col = 0; col < 9; col++)
        {
            addSlot(new Slot(playerInv, col, 8 + col * 18, GUI_TOP_H + 120));
        }
    }

    public static PaintjobTableMenu createFromNetwork(int id, Inventory playerInv, FriendlyByteBuf buf)
    {
        return new PaintjobTableMenu(id, playerInv, buf.readBlockPos());
    }

    @Override
    public boolean stillValid(@NotNull Player player)
    {
        return access.evaluate((level, pos) -> {
            Block block = level.getBlockState(pos).getBlock();
            return block == FlansMod.paintjobTable.get() && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= MAX_DISTANCE;
        }, true);
    }

    /**
     * The table is a workspace, not a chest: like the gun workbench, whatever is left
     * in it goes back to the player who closes it rather than staying there for the
     * next person to pick up.
     */
    @Override
    public void removed(@NotNull Player player)
    {
        super.removed(player);
        clearContainer(player, workspace);
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

