package com.flansmodultimate.common.guns;

import com.flansmodultimate.common.item.ShootableItem;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/** Server-only automated intent and ownership of temporary, infinite-ammunition magazines. */
public final class EmplacementControl
{
    private final Map<ItemStack, UUID> virtual = new WeakHashMap<>();
    private long intentAt = Long.MIN_VALUE;
    private boolean firing;
    @Nullable
    private UUID firingOperator;

    public void firing(long now, @Nullable Entity operator, boolean value)
    {
        intentAt = now;
        firing = value;
        firingOperator = operator == null ? null : operator.getUUID();
    }

    public boolean firing(long now, @Nullable Entity operator)
    {
        return firing && intentAt >= now - 1 && operator != null && operator.isAlive() && operator.getUUID().equals(firingOperator);
    }

    public ItemStack supplied(ItemStack stack, Entity operator, boolean temporary)
    {
        virtual.keySet().removeIf(round -> !ShootableItem.hasRoundsLeft(round));
        if (temporary && !stack.isEmpty())
            virtual.put(stack, operator.getUUID());
        return stack;
    }

    public boolean isVirtual(ItemStack stack)
    {
        return virtual.containsKey(stack);
    }

    public ItemStack retain(ItemStack stack, @Nullable Entity operator)
    {
        UUID owner = virtual.get(stack);
        if (owner == null || operator != null && operator.isAlive() && owner.equals(operator.getUUID()))
            return stack;
        virtual.remove(stack);
        return ItemStack.EMPTY;
    }

    public ItemStack finite(ItemStack stack)
    {
        if (!isVirtual(stack))
            return stack;
        virtual.remove(stack);
        return ItemStack.EMPTY;
    }
}
