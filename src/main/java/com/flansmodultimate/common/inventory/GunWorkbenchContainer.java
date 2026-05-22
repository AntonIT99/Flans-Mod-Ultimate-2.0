package com.flansmodultimate.common.inventory;

import net.minecraft.world.SimpleContainer;

public class GunWorkbenchContainer extends SimpleContainer
{
    private final GunWorkbenchMenu menu;

    public GunWorkbenchContainer(GunWorkbenchMenu menu, int size)
    {
        super(size);
        this.menu = menu;
    }

    @Override
    public void setChanged()
    {
        super.setChanged();
        menu.onWorkbenchChanged(this);
    }
}
