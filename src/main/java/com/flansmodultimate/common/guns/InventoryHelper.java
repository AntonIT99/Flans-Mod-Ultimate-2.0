package com.flansmodultimate.common.guns;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/**
 * Adds access to the ContainerPlayer stack combination methods for arbitrary inventories
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class InventoryHelper
{
    public static boolean addItemStackToContainer(Container inv, ItemStack stack, boolean creative, boolean combine, boolean toUpperContainer)
    {
        if (creative || stack.isEmpty())
            return true;

        if (combine)
        {
            mergeIntoExisting(inv, stack, toUpperContainer);
            if (stack.isEmpty())
                return true;
        }

        int empty = findEmptySlot(inv, toUpperContainer);
        if (empty != -1)
        {
            inv.setItem(empty, stack.copy());
            stack.setCount(0);
            return true;
        }

        return false;
    }

    private static void mergeIntoExisting(Container inv, ItemStack stack, boolean upper)
    {
        mergeRange(inv, stack, primaryStart(inv, upper), primaryEnd(inv, upper));

        if (!stack.isEmpty())
            mergeRange(inv, stack, secondaryStart(inv, upper), secondaryEnd(inv, upper));
    }

    private static void mergeRange(Container inv, ItemStack stack, int start, int end)
    {
        for (int i = start; i < end && !stack.isEmpty(); i++)
        {
            ItemStack slot = inv.getItem(i);
            if (slot.isEmpty())
                continue;
            if (!ItemStack.isSameItemSameTags(slot, stack))
                continue;
            if (!slot.isStackable())
                continue;

            int max = Math.min(slot.getMaxStackSize(), inv.getMaxStackSize());
            int space = max - slot.getCount();
            if (space <= 0)
                continue;

            int move = Math.min(space, stack.getCount());
            slot.setCount(slot.getCount() + move);
            stack.setCount(stack.getCount() - move);
        }
    }

    private static int findEmptySlot(Container inv, boolean upper)
    {
        int s = primaryStart(inv, upper);
        int e = primaryEnd(inv, upper);

        for (int i = s; i < e; i++)
            if (inv.getItem(i).isEmpty())
                return i;

        s = secondaryStart(inv, upper); e = secondaryEnd(inv, upper);

        for (int i = s; i < e; i++)
            if (inv.getItem(i).isEmpty())
                return i;

        return -1;
    }

    private static int primaryStart(Container inv, boolean upper)
    {
        return upper ? 0 : 9;
    }

    private static int primaryEnd(Container inv, boolean upper)
    {
        return upper ? 9 : inv.getContainerSize();
    }

    private static int secondaryStart(Container inv, boolean upper)
    {
        return upper ? 9 : 0;
    }

    private static int secondaryEnd(Container inv, boolean upper)
    {
        return upper ? inv.getContainerSize() : 9;
    }
}
