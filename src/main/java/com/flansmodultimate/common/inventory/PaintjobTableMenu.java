package com.flansmodultimate.common.inventory;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.block.entity.PaintjobTableBlockEntity;
import com.flansmodultimate.common.item.IPaintableItem;
import com.flansmodultimate.common.paintjob.Paintjob;
import com.flansmodultimate.common.types.PaintableType;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
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
    public final PaintjobTableBlockEntity table;
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
        this.table = table;

        // TE slots via capability
        table.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
            addSlot(new SlotItemHandler(handler, 0, 187, 139));
            addSlot(new SlotItemHandler(handler, 1, 187, 193));
        });

        // Player inventory
        for (int row = 0; row < 3; row++)
        {
            for (int col = 0; col < 9; col++)
            {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 184 + row * 18));
            }
        }
        // Hotbar
        for (int col = 0; col < 9; col++)
        {
            addSlot(new Slot(playerInv, col, 8 + col * 18, 242));
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
            return block == FlansMod.gunWorkbench.get() && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
        }, true);
    }

    @Override
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
        else
        {
            // shift-click from player -> TE (try paintable slot then paintcans)
            if (!moveItemStackTo(stackInSlot, 0, 1, false) &&
                !moveItemStackTo(stackInSlot, 1, 2, false))
            {
                return ItemStack.EMPTY;
            }
        }

        if (stackInSlot.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();

        return copy;
    }

    /** SERVER-SIDE paint selection entry point (called from packet handler). */
    public void applyPaintjob(ServerPlayer player, int paintjobId)
    {
        ItemStack paintable = table.getPaintableStack();
        if (paintable.isEmpty())
            return;
        if (!(paintable.getItem() instanceof IPaintableItem<?> p))
            return;

        PaintableType type = p.getPaintableType();
        Paintjob pj = type.getPaintjob(paintjobId); // implement lookup however you do

        if (pj == null) return;

        if (!player.getAbilities().instabuild) {
            if (!hasAllDyes(player.getInventory(), pj)) return;
            consumeDyes(player.getInventory(), pj);
        }

        // IMPORTANT: In 1.20, item damage is durability. If your guns are damageable, don’t use damage for paint.
        // Prefer NBT or a data component.
        setPaintjobId(paintable, pj.getId());

        table.setChanged();
    }

    private boolean hasAllDyes(Inventory inv, Paintjob pj)
    {
        /* implement */
        return true;
    }

    private void consumeDyes(Inventory inv, Paintjob pj)
    {
        /* implement */
    }

    private static void setPaintjobId(ItemStack stack, int id)
    {
        stack.getOrCreateTag().putInt("FlansPaintjob", id);
    }
}

