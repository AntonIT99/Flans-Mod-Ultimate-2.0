package com.flansmodultimate.network.server;

import com.flansmodultimate.common.inventory.GunWorkbenchMenu;
import com.flansmodultimate.common.inventory.PaintjobTableMenu;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.item.IPaintableItem;
import com.flansmodultimate.common.paintjob.Paintjob;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.common.types.PaintableType;
import com.flansmodultimate.network.IServerPacket;
import com.flansmodultimate.platform.network.PacketBuffer;
import com.flansmodultimate.util.InventoryHelper;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
    public void encodeInto(PacketBuffer data)
    {
        data.writeInt(paintjobId);
    }

    @Override
    public void decodeInto(PacketBuffer data)
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

        if (!InventoryHelper.tryConsumeAll(player.getInventory(), pj.getDyesNeeded(), player.getAbilities().instabuild))
            return;

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

    private void handlePaintjobTable(ServerPlayer player, PaintjobTableMenu menu)
    {
        ItemStack paintableStack = menu.getPaintableStack();
        if (paintableStack.isEmpty() || !(paintableStack.getItem() instanceof IPaintableItem<?> paintableItem))
            return;

        PaintableType type = paintableItem.getPaintableType();

        Paintjob pj = findApplicablePaintjob(type, paintjobId);
        if (pj == null)
            return;

        if (!InventoryHelper.tryConsumeAll(player.getInventory(), menu.getPaintCanStack(), pj.getDyesNeeded(), player.getAbilities().instabuild))
            return;

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

}
