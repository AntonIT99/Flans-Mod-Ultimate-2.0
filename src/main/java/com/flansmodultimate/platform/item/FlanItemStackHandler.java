package com.flansmodultimate.platform.item;

import net.minecraftforge.items.ItemStackHandler;

/**
 * The loader's item-stack inventory with a change callback, so block entities can hold and expose it
 * without naming the loader's item-handler package.
 */
public class FlanItemStackHandler extends ItemStackHandler
{
    private final Runnable onChanged;

    public FlanItemStackHandler(int size, Runnable onChanged)
    {
        super(size);
        this.onChanged = onChanged;
    }

    @Override
    protected void onContentsChanged(int slot)
    {
        onChanged.run();
    }
}
