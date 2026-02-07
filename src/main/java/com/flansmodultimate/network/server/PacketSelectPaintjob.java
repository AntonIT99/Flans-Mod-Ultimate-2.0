package com.flansmodultimate.network.server;

import com.flansmodultimate.common.inventory.GunWorkbenchMenu;
import com.flansmodultimate.common.inventory.PaintjobTableMenu;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.item.IPaintableItem;
import com.flansmodultimate.common.paintjob.Paintjob;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.common.types.PaintableType;
import com.flansmodultimate.network.IServerPacket;
import com.flansmodultimate.util.InventoryHelper;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

@NoArgsConstructor
public class PacketSelectPaintjob implements IServerPacket
{
    private int paintjobId;

    public PacketSelectPaintjob(int paintjobId)
    {
        this.paintjobId = paintjobId;
    }

    @Override
    public void encodeInto(FriendlyByteBuf data)
    {
        data.writeInt(paintjobId);
    }

    @Override
    public void decodeInto(FriendlyByteBuf data)
    {
        paintjobId = data.readInt();
    }

    @Override
    public void handleServerSide(@NotNull ServerPlayer player, @NotNull ServerLevel level)
    {
        if (player.containerMenu instanceof GunWorkbenchMenu gunMenu)
        {
            handleGunWorkbench(player, gunMenu);
        }
        else if (player.containerMenu instanceof PaintjobTableMenu paintMenu)
        {
            handlePaintjobTable(player, paintMenu);
        }
    }

    private void handleGunWorkbench(ServerPlayer player, GunWorkbenchMenu menu)
    {
        ItemStack gunStack = menu.getGunStack();
        if (gunStack.isEmpty() || !(gunStack.getItem() instanceof GunItem gunItem))
            return;

        GunType gunType = gunItem.getConfigType();

        Paintjob pj = findApplicablePaintjob(gunType, paintjobId);
        if (pj == null)
            return;

        if (!player.getAbilities().instabuild)
        {
            if (!hasRequiredDyes(player.getInventory(), pj))
                return;

            consumeRequiredDyes(player.getInventory(), pj);
        }

        gunType.applyPaintjobToStack(gunStack, pj);
        menu.broadcastChanges();
    }


    private static Paintjob findApplicablePaintjob(GunType gunType, int id)
    {
        if (!gunType.isAddAnyPaintjobToTables())
            return null;

        for (Paintjob pj : gunType.getPaintjobs().values())
        {
            if (pj.isAddToTables() && pj.getId() == id)
                return pj;
        }
        return null;
    }

    private static boolean hasRequiredDyes(Inventory inv, Paintjob paintjob)
    {
        List<ItemStack> needed = paintjob.getDyesNeeded();
        if (needed.isEmpty())
            return true;

        for (ItemStack want : needed)
        {
            if (want == null || want.isEmpty())
                continue;

            int have = InventoryHelper.countInInventory(inv, want);
            if (have < want.getCount())
                return false;
        }
        return true;
    }

    private static void consumeRequiredDyes(Inventory inv, Paintjob paintjob)
    {
        List<ItemStack> needed = paintjob.getDyesNeeded();
        if (needed == null || needed.isEmpty())
            return;

        for (ItemStack want : needed)
        {
            if (want == null || want.isEmpty())
                continue;

            InventoryHelper.consumeFromInventory(inv, want);
        }
    }

    private void handlePaintjobTable(ServerPlayer player, PaintjobTableMenu menu)
    {
        ItemStack paintableStack = menu.getPaintableStack();
        if (paintableStack.isEmpty() || !(paintableStack.getItem() instanceof IPaintableItem<?> paintableItem))
            return;

        PaintableType type = paintableItem.getPaintableType();

        Paintjob pj = findApplicablePaintjob(type, paintjobId);
        if (pj == null)
            return;

        if (!player.getAbilities().instabuild)
        {
            Inventory inv = player.getInventory();
            ItemStack canSlot = menu.getPaintCanStack(); // slot 1

            if (!hasRequiredDyes(inv, canSlot, pj))
                return;

            consumeRequiredDyes(inv, canSlot, pj);
        }

        type.applyPaintjobToStack(paintableStack, pj);
        menu.broadcastChanges();
    }

    private static Paintjob findApplicablePaintjob(PaintableType type, int id)
    {
        List<Paintjob> list = type.getApplicablePaintjobs();
        for (Paintjob pj : list)
        {
            if (pj.getId() == id)
                return pj;
        }
        return null;
    }

    private static boolean hasRequiredDyes(Inventory inv, ItemStack cansSlot, Paintjob paintjob)
    {
        List<ItemStack> needed = paintjob.getDyesNeeded();
        if (needed == null || needed.isEmpty())
            return true;

        for (ItemStack want : needed)
        {
            if (want == null || want.isEmpty())
                continue;

            int required = want.getCount();

            int fromCans = 0;
            if (!cansSlot.isEmpty() && ItemStack.isSameItemSameTags(cansSlot, want))
                fromCans = cansSlot.getCount();

            int fromInv = InventoryHelper.countInInventory(inv, want);

            if (fromCans + fromInv < required)
                return false;
        }

        return true;
    }

    private static void consumeRequiredDyes(Inventory inv, ItemStack cansSlot, Paintjob paintjob)
    {
        List<ItemStack> needed = paintjob.getDyesNeeded();
        if (needed == null || needed.isEmpty())
            return;

        for (ItemStack want : needed)
        {
            if (want == null || want.isEmpty())
                continue;

            int remaining = want.getCount();

            // 1) Consume from slot 1 first (if it matches)
            if (!cansSlot.isEmpty() && ItemStack.isSameItemSameTags(cansSlot, want))
            {
                int take = Math.min(remaining, cansSlot.getCount());
                cansSlot.shrink(take);
                remaining -= take;
            }

            // 2) Consume remainder from player inventory
            if (remaining > 0)
            {
                ItemStack remainder = want.copy();
                remainder.setCount(remaining);
                InventoryHelper.consumeFromInventory(inv, remainder);
            }
        }

        inv.setChanged();
    }
}
