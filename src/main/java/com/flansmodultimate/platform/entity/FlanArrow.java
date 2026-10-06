package com.flansmodultimate.platform.entity;

import com.flansmodultimate.platform.item.ItemStackData;
import org.jetbrains.annotations.NotNull;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
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
        super(type, owner, level);
        setWeapon(pickup.copy());
    }

    @Override
    protected final void defineSynchedData()
    {
        super.defineSynchedData();
        defineEntityData(new SynchedDataDefinition(entityData));
    }

    protected abstract void defineEntityData(SynchedDataDefinition data);

    protected abstract ItemStack getWeapon();

    protected abstract void setWeapon(ItemStack stack);

    private static final String NBT_WEAPON = "weapon";

    @Override
    protected ItemStack getPickupItem()
    {
        return getWeapon().copy();
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        if (tag.contains(NBT_WEAPON, Tag.TAG_COMPOUND))
            setWeapon(ItemStackData.parse(level().registryAccess(), tag.getCompound(NBT_WEAPON)));
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        ItemStack weapon = getWeapon();
        if (!weapon.isEmpty())
            tag.put(NBT_WEAPON, ItemStackData.save(weapon, level().registryAccess()));
    }

    protected boolean prepareProjectileTick()
    {
        return true;
    }
}
