package com.flansmodultimate.network.server;

import com.flansmodultimate.client.gui.GunWorkbenchScreen;
import com.flansmodultimate.common.inventory.GunWorkbenchMenu;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.paintjob.Paintjob;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.network.IServerPacket;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Supplier;

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
        // Player must have the menu open
        if (!(player.containerMenu instanceof GunWorkbenchMenu menu))
            return;

        // Slot 0 is gun input
        ItemStack gunStack = menu.getGunStack();
        if (gunStack.isEmpty() || !(gunStack.getItem() instanceof GunItem gunItem))
            return;

        GunType gunType = gunItem.getConfigType();

        // Validate paintjob exists + is applicable
        Paintjob pj = findApplicablePaintjob(gunType, paintjobId);
        if (pj == null)
            return;

        // Validate/consume dyes (skip in creative)
        if (!player.getAbilities().instabuild)
        {
            if (!hasRequiredDyes(player.getInventory(), pj))
                return;

            consumeRequiredDyes(player.getInventory(), pj);
        }

        // Apply NBT to the *actual gun stack in the menu*
        gunType.applyPaintjobToStack(gunStack, gunType.getPaintjob(paintjobId));

        // Tell the container system something changed
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
        List<Supplier<ItemStack>> needed = paintjob.getDyesNeeded();
        if (needed.isEmpty())
            return true;

        for (Supplier<ItemStack> want : needed)
        {
            if (want == null || want.get().isEmpty())
                continue;

            int have = GunWorkbenchScreen.countInInventory(inv, want.get());
            if (have < want.get().getCount())
                return false;
        }
        return true;
    }

    private static void consumeRequiredDyes(Inventory inv, Paintjob paintjob)
    {
        List<Supplier<ItemStack>> needed = paintjob.getDyesNeeded();
        if (needed.isEmpty())
            return;

        for (Supplier<ItemStack> want : needed)
        {
            if (want == null || want.get().isEmpty())
                continue;

            int remaining = want.get().getCount();
            for (int i = 0; i < inv.getContainerSize() && remaining > 0; i++)
            {
                ItemStack have = inv.getItem(i);
                if (have.isEmpty())
                    continue;

                if (GunWorkbenchScreen.sameItem(have, want.get()))
                {
                    int take = Math.min(remaining, have.getCount());
                    have.shrink(take);
                    remaining -= take;
                }
            }
        }
        inv.setChanged();
    }
}
