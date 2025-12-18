package com.flansmodultimate.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class InventoryHelper
{
    public static boolean addItemStackToInventory(Container inv, ItemStack stack, boolean creative, boolean combine, boolean toUpperInventory)
    {
        if (creative || stack.isEmpty())
            return true;

        // combine into existing stacks (including partial mags) first (optional)
        if (combine && tryMerge(inv, stack, toUpperInventory))
            return stack.isEmpty();


        // then place into an empty slot
        int empty = findEmptySlot(inv, toUpperInventory);
        if (empty != -1)
        {
            inv.setItem(empty, stack.copy());
            stack.setCount(0);
            return true;
        }

        return false;
    }

    private static boolean tryMerge(Container inv, ItemStack stack, boolean toUpperInventory)
    {
        // This merges by Item + tags (NBT) + damage value.
        int start = toUpperInventory ? 0 : 9;
        int end = inv.getContainerSize();
        int altStart = toUpperInventory ? 9 : 0;
        int altEnd = toUpperInventory ? inv.getContainerSize() : 9;

        if (mergeRange(inv, stack, start, end))
            return true;

        return mergeRange(inv, stack, altStart, altEnd);
    }

    private static boolean mergeRange(Container inv, ItemStack stack, int start, int end)
    {
        for (int i = start; i < end; i++)
        {
            if (stack.isEmpty())
                return true;
            ItemStack inSlot = inv.getItem(i);
            if (inSlot.isEmpty())
                continue;

            if (!ItemStack.isSameItemSameTags(inSlot, stack))
                continue;
            if (!inSlot.isStackable())
                continue;

            int max = Math.min(inSlot.getMaxStackSize(), inv.getMaxStackSize());
            int space = max - inSlot.getCount();
            if (space <= 0)
                continue;

            int move = Math.min(space, stack.getCount());
            inSlot.setCount(inSlot.getCount() + move);
            stack.setCount(stack.getCount() - move);
        }
        return stack.isEmpty();
    }

    private static int findEmptySlot(Container inv, boolean toUpperInventory)
    {
        int start = toUpperInventory ? 0 : 9;
        int end = inv.getContainerSize();
        int altStart = toUpperInventory ? 9 : 0;
        int altEnd = toUpperInventory ? inv.getContainerSize() : 9;

        int slot = emptyInRange(inv, start, end);
        if (slot != -1)
            return slot;

        return emptyInRange(inv, altStart, altEnd);
    }

    private static int emptyInRange(Container inv, int start, int end)
    {
        for (int i = start; i < end; i++) {
            if (inv.getItem(i).isEmpty())
                return i;
        }
        return -1;
    }
}
