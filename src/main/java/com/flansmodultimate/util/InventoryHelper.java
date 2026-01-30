package com.flansmodultimate.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class InventoryHelper
{
    public static int countInInventory(Container inv, ItemStack needle)
    {
        if (needle == null || needle.isEmpty())
            return 0;

        int total = 0;
        for (int i = 0; i < inv.getContainerSize(); i++)
        {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && ItemStack.isSameItemSameTags(s, needle))
                total += s.getCount();
        }
        return total;
    }

    public static void consumeFromInventory(Container inv, ItemStack want)
    {
        if (want == null || want.isEmpty())
            return;

        int remaining = want.getCount();
        for (int i = 0; i < inv.getContainerSize() && remaining > 0; i++)
        {
            ItemStack have = inv.getItem(i);
            if (have.isEmpty())
                continue;

            if (ItemStack.isSameItemSameTags(have, want))
            {
                int take = Math.min(remaining, have.getCount());
                have.shrink(take);
                remaining -= take;
            }
        }
        inv.setChanged();
    }

    /**
     * @param inv              target container
     * @param stack            stack to insert/merge (will be decremented/emptied)
     * @param creative         if true, treat as success without modifying
     * @param combine          if true, merge into existing stacks first
     * @param toUpperContainer if true, prefer the "upper" range first, otherwise the "lower" range first
     * @param splitIndex       boundary between ranges (player inventory hotbar = 9). Use inv.getContainerSize() to mean "no split".
     */
    public static boolean addItemStackToContainer(Container inv, ItemStack stack, boolean creative, boolean combine, boolean toUpperContainer, int splitIndex)
    {
        if (creative || stack.isEmpty())
            return true;

        int size = inv.getContainerSize();
        int split = Math.max(0, Math.min(size, splitIndex));

        if (combine)
        {
            mergeIntoExisting(inv, stack, toUpperContainer, split);
            if (stack.isEmpty())
                return true;
        }

        int empty = findEmptySlot(inv, stack, toUpperContainer, split);
        if (empty != -1)
        {
            // place as much as possible into this slot (respecting max stack sizes)
            ItemStack placed = stack.copy();
            int max = Math.min(placed.getMaxStackSize(), inv.getMaxStackSize());
            placed.setCount(Math.min(max, stack.getCount()));

            inv.setItem(empty, placed);
            stack.shrink(placed.getCount());
            return stack.isEmpty();
        }

        return false;
    }

    private static void mergeIntoExisting(Container inv, ItemStack stack, boolean upperFirst, int split)
    {
        // Primary = preferred section, Secondary = the other section
        mergeRange(inv, stack, primaryStart(upperFirst, split), primaryEnd(upperFirst, split, inv.getContainerSize()));

        if (!stack.isEmpty())
            mergeRange(inv, stack, secondaryStart(upperFirst, split), secondaryEnd(upperFirst, split, inv.getContainerSize()));
    }

    private static void mergeRange(Container inv, ItemStack stack, int start, int end)
    {
        for (int i = start; i < end && !stack.isEmpty(); i++)
        {
            ItemStack slot = inv.getItem(i);
            if (slot.isEmpty() || !ItemStack.isSameItemSameTags(slot, stack) || !slot.isStackable())
                continue;

            int max = Math.min(slot.getMaxStackSize(), inv.getMaxStackSize());
            int space = max - slot.getCount();
            if (space <= 0)
                continue;

            int move = Math.min(space, stack.getCount());
            slot.grow(move);
            stack.shrink(move);
        }
    }

    private static int findEmptySlot(Container inv, ItemStack toInsert, boolean upperFirst, int split)
    {
        int slot = emptyInRange(inv, toInsert, primaryStart(upperFirst, split), primaryEnd(upperFirst, split, inv.getContainerSize()));
        if (slot != -1)
            return slot;

        return emptyInRange(inv, toInsert, secondaryStart(upperFirst, split), secondaryEnd(upperFirst, split, inv.getContainerSize()));
    }

    private static int emptyInRange(Container inv, ItemStack stack, int start, int end)
    {
        for (int i = start; i < end; i++)
        {
            if (!inv.getItem(i).isEmpty() || !inv.canPlaceItem(i, stack))
                continue;

            return i;
        }
        return -1;
    }

    private static int primaryStart(boolean upperFirst, int split)
    {
        return upperFirst ? 0 : split;
    }

    private static int primaryEnd(boolean upperFirst, int split, int containerSize)
    {
        return upperFirst ? split : containerSize;
    }

    private static int secondaryStart(boolean upperFirst, int split)
    {
        return upperFirst ? split : 0;
    }

    private static int secondaryEnd(boolean upperFirst, int split, int containerSize)
    {
        return upperFirst ? containerSize : split;
    }
}
