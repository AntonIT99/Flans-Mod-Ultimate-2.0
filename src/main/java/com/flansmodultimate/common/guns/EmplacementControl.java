package com.flansmodultimate.common.guns;

import com.flansmodultimate.common.item.ShootableItem;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Optional;
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
    private long aimAt = Long.MIN_VALUE;
    @Nullable
    private UUID aimOperator;
    @Nullable
    private Vec3 aimDirection;

    /** Keeps automated aim independent of the operator's look controller. Renew every server tick. */
    public void aim(long now, Entity operator, Vec3 direction)
    {
        aimAt = now;
        aimOperator = operator.getUUID();
        aimDirection = direction;
    }

    public Optional<Vec3> aim(long now, Entity operator)
    {
        return aimAt >= now - 1 && operator.isAlive() && operator.getUUID().equals(aimOperator) ? Optional.ofNullable(aimDirection) : Optional.empty();
    }

    public void clearAim()
    {
        aimDirection = null;
        aimOperator = null;
    }

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
