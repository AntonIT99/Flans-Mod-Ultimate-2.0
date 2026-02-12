package com.flansmodultimate.common.inventory;

import com.flansmodultimate.FlansMod;
import org.jetbrains.annotations.NotNull;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class GunBoxMenu extends AbstractContainerMenu
{
    public GunBoxMenu(int id)
    {
        super(FlansMod.gunBoxMenu.get(), id);
    }

    @Override
    @NotNull
    public ItemStack quickMoveStack(@NotNull Player pPlayer, int pIndex)
    {
        return null;
    }

    @Override
    public boolean stillValid(@NotNull Player pPlayer)
    {
        return false;
    }
}
