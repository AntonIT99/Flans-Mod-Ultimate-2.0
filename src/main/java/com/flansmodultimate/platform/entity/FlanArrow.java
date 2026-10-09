package com.flansmodultimate.platform.entity;

import org.jetbrains.annotations.NotNull;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Adapts arrow data, pickup storage, construction and persistence to the Minecraft version. */
public abstract class FlanArrow extends AbstractArrow
{
    protected FlanArrow(EntityType<? extends AbstractArrow> type, Level level)
    {
        super(type, level);
    }

    protected FlanArrow(EntityType<? extends AbstractArrow> type, LivingEntity owner, Level level, ItemStack pickup)
    {
        // The thrown stack is the pickup item, not the firing weapon used by vanilla enchantment hooks.
        super(type, owner, level, pickup.copy(), null);
        setWeapon(getPickupItemStackOrigin());
    }

    @Override
    protected final void defineSynchedData(SynchedEntityData.Builder builder)
    {
        super.defineSynchedData(builder);
        defineEntityData(new SynchedDataDefinition(builder));
    }

    protected abstract void defineEntityData(SynchedDataDefinition data);

    protected abstract ItemStack getWeapon();

    protected abstract void setWeapon(ItemStack stack);

    @Override
    protected void setPickupItemStack(@NotNull ItemStack stack)
    {
        super.setPickupItemStack(stack);
        setWeapon(getPickupItemStackOrigin());
    }

    @Override
    protected ItemStack getDefaultPickupItem()
    {
        return ItemStack.EMPTY;
    }

    /** Vanilla's 1.21 codec cannot save an empty pickup stack. */
    @Override
    public boolean shouldBeSaved()
    {
        return super.shouldBeSaved() && !getPickupItemStackOrigin().isEmpty();
    }

    protected boolean prepareProjectileTick()
    {
        if (!level().isClientSide && getPickupItemStackOrigin().isEmpty())
        {
            discard();
            return false;
        }
        return true;
    }
}
