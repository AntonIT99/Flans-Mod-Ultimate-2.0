package com.wolffsarmormod.common.guns;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/**
 * Adds access to the InventoryPlayer stack combination methods for arbitrary inventories
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class InventoryHelper
{
    /** Tries to insert the whole stack into the container. Returns true if *anything* was inserted.
     *  If {@code creative} is true and insertion couldn’t find space, the input stack is cleared. */
    public static boolean addItemStackToInventory(Container inventory, ItemStack stack, boolean creative)
    {
        if (stack == null || stack.isEmpty() || stack.getCount() <= 0) return false;

        try {
            // Non-stackable (or damaged items that don’t stack): place single copy into an empty slot.
            if (stack.getMaxStackSize() == 1 || stack.isDamaged()) {
                int slot = getFirstEmptyStack(inventory);
                if (slot >= 0) {
                    ItemStack toAdd = stack.copy();
                    toAdd.setPopTime(5);
                    inventory.setItem(slot, toAdd);
                    stack.setCount(0);
                    return true;
                } else if (creative) {
                    stack.setCount(0);
                    return true;
                }
                return false;
            }

            // Stackable path: keep merging until no progress
            boolean insertedAny = false;
            int before;
            do {
                before = stack.getCount();
                int remaining = storePartialItemStack(inventory, stack);
                stack.setCount(remaining);
                insertedAny |= (remaining < before);
            } while (stack.getCount() > 0 && stack.getCount() < before);

            if (stack.getCount() == before && creative) { // couldn’t place more, but creative: delete
                stack.setCount(0);
                return true;
            }
            return insertedAny;
        } catch (Throwable t) {
            // Log if you like
            return false;
        }
    }

    /** Finds a slot that can merge with {@code stack}. Returns -1 if none. */
    public static int storeItemStack(Container inventory, ItemStack stack)
    {
        for (int i = 0; i < inventory.getContainerSize(); ++i) {
            ItemStack old = inventory.getItem(i);
            if (!old.isEmpty()
                    && old.isStackable()
                    && ItemStack.isSameItemSameTags(old, stack)
                    && old.getCount() < Math.min(old.getMaxStackSize(), inventory.getMaxStackSize())) {
                return i;
            }
        }
        return -1;
    }

    /** Tries to put as much of {@code stack} as possible into the container.
     *  Returns the remaining count that couldn’t be inserted. */
    public static int storePartialItemStack(Container inventory, ItemStack stack)
    {
        Objects.requireNonNull(stack);
        int remaining = stack.getCount();

        // Non-stackable: just place a copy in the first empty slot
        if (stack.getMaxStackSize() == 1) {
            int slot = getFirstEmptyStack(inventory);
            if (slot < 0) return remaining;

            if (inventory.getItem(slot).isEmpty()) {
                inventory.setItem(slot, stack.copy());
                return 0;
            }
            return remaining;
        }

        // First try to merge into existing stacks
        int slot = storeItemStack(inventory, stack);
        if (slot < 0) {
            // Then try an empty slot
            slot = getFirstEmptyStack(inventory);
        }
        if (slot < 0) {
            return remaining; // nowhere to go
        }

        ItemStack target = inventory.getItem(slot);
        if (target.isEmpty()) {
            // Create a zero-count target with same item/tags to merge into (then we’ll increase count)
            target = stack.copy();
            target.setCount(0);
            inventory.setItem(slot, target);
        }

        int maxPerSlot = Math.min(target.getMaxStackSize(), inventory.getMaxStackSize());
        int canMove = Math.min(remaining, maxPerSlot - target.getCount());
        if (canMove <= 0) return remaining;

        target.grow(canMove);
        target.setPopTime(5);
        remaining -= canMove;

        return remaining;
    }

    /** First empty slot or -1. */
    public static int getFirstEmptyStack(Container inventory)
    {
        for (int i = 0; i < inventory.getContainerSize(); ++i) {
            if (inventory.getItem(i).isEmpty()) return i;
        }
        return -1;
    }
}
