package com.flansmodultimate.common.inventory;

import org.jetbrains.annotations.NotNull;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

/**
 * One visible slot in the scrollable driveable inventory window. Modern menu
 * slot coordinates are immutable, so scrolling remaps these small fixed slots
 * onto the relevant range in the driveable container instead of creating hundreds
 * of permanently hidden slots.
 */
final class DriveableMappedSlot extends Slot
{
    private final Container data;
    private final Predicate<ItemStack> placementFilter;
    private int mappedIndex = -1;

    DriveableMappedSlot(Container data, int x, int y, Predicate<ItemStack> placementFilter)
    {
        super(data, 0, x, y);
        this.data = data;
        this.placementFilter = placementFilter;
    }

    void mapTo(int index)
    {
        mappedIndex = index >= 0 && index < data.getContainerSize() ? index : -1;
    }

    @Override
    public boolean isActive()
    {
        return mappedIndex >= 0;
    }

    @Override
    @NotNull
    public ItemStack getItem()
    {
        return isActive() ? data.getItem(mappedIndex) : ItemStack.EMPTY;
    }

    @Override
    public void set(@NotNull ItemStack stack)
    {
        if (isActive())
            data.setItem(mappedIndex, stack);
    }

    @Override
    public void setChanged()
    {
        if (isActive())
            data.setChanged();
    }

    @Override
    @NotNull
    public ItemStack remove(int amount)
    {
        return isActive() ? data.removeItem(mappedIndex, amount) : ItemStack.EMPTY;
    }

    @Override
    public boolean mayPlace(@NotNull ItemStack stack)
    {
        return isActive() && placementFilter.test(stack) && data.canPlaceItem(mappedIndex, stack);
    }

    @Override
    public boolean mayPickup(@NotNull Player player)
    {
        return isActive();
    }

    @Override
    public int getContainerSlot()
    {
        return mappedIndex;
    }
}
