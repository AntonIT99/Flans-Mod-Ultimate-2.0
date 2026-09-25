package com.flansmodultimate.common.entity;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.FlanDamageSources;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.platform.entity.SynchedDataDefinition;
import com.flansmodultimate.platform.item.ItemStackData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A gun item thrown with {@code SecondaryFunction Throw}, such as a javelin or a pilum. It flies and
 * sticks like a trident, hits once, and carries the thrown stack so picking it up returns the very
 * same weapon.
 */
public class ThrownGun extends AbstractArrow
{
    /** Ticks a recoverable weapon stays stuck before despawning, the lifespan of a dropped item */
    public static final int RECOVERABLE_LIFESPAN = 6000;

    protected static final String NBT_WEAPON = "weapon";
    protected static final String NBT_DAMAGE = "throw_damage";
    protected static final String NBT_DEALT_DAMAGE = "dealt_damage";
    protected static final String NBT_LIFE = "life";

    private static final EntityDataAccessor<ItemStack> DATA_WEAPON = SynchedEntityData.defineId(ThrownGun.class, EntityDataSerializers.ITEM_STACK);

    protected float throwDamage;
    protected boolean dealtDamage;
    protected int life;

    public ThrownGun(EntityType<? extends ThrownGun> entityType, Level level)
    {
        super(entityType, level);
    }

    public ThrownGun(Level level, LivingEntity thrower, ItemStack weapon, float throwDamage)
    {
        super(FlansMod.thrownGunEntity.get(), thrower, level);
        setWeapon(weapon.copy());
        this.throwDamage = throwDamage;
    }

    @Override
    protected void defineSynchedData()
    {
        super.defineSynchedData();
        defineEntityData(new SynchedDataDefinition(entityData));
    }

    protected void defineEntityData(SynchedDataDefinition data)
    {
        data.define(DATA_WEAPON, ItemStack.EMPTY);
    }

    public ItemStack getWeapon()
    {
        return entityData.get(DATA_WEAPON);
    }

    protected void setWeapon(ItemStack weapon)
    {
        entityData.set(DATA_WEAPON, weapon);
    }

    @Nullable
    public GunType getGunType()
    {
        return getWeapon().getItem() instanceof GunItem gunItem ? gunItem.getConfigType() : null;
    }

    @Override
    public void tick()
    {
        // Once it has settled it no longer wounds anything walking into it
        if (inGroundTime > 4)
            dealtDamage = true;

        super.tick();
    }

    @Override
    @NotNull
    protected ItemStack getPickupItem()
    {
        return getWeapon().copy();
    }

    @Override
    @Nullable
    protected EntityHitResult findHitEntity(@NotNull Vec3 startVec, @NotNull Vec3 endVec)
    {
        return dealtDamage ? null : super.findHitEntity(startVec, endVec);
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult result)
    {
        Entity target = result.getEntity();
        Entity owner = getOwner();
        DamageSource source = FlanDamageSources.createDamageSource(level(), this, owner == null ? this : owner, FlanDamageSources.SHOOTABLE);
        dealtDamage = true;

        if (target.hurt(source, throwDamage) && target instanceof LivingEntity living)
            doPostHurtEffects(living);

        // Drop off the target rather than bouncing back towards the thrower
        setDeltaMovement(getDeltaMovement().multiply(-0.01D, -0.1D, -0.01D));
        playSound(SoundEvents.TRIDENT_HIT, 1F, 1F);
    }

    @Override
    @NotNull
    protected SoundEvent getDefaultHitGroundSoundEvent()
    {
        return SoundEvents.TRIDENT_HIT_GROUND;
    }

    @Override
    protected void tickDespawn()
    {
        if (pickup != Pickup.ALLOWED)
        {
            super.tickDespawn();
            return;
        }

        // A weapon worth recovering lasts as long as the same item would on the ground
        life++;
        if (life >= RECOVERABLE_LIFESPAN)
            discard();
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        if (tag.contains(NBT_WEAPON, Tag.TAG_COMPOUND))
            setWeapon(ItemStackData.parse(level().registryAccess(), tag.getCompound(NBT_WEAPON)));
        throwDamage = tag.getFloat(NBT_DAMAGE);
        dealtDamage = tag.getBoolean(NBT_DEALT_DAMAGE);
        life = tag.getInt(NBT_LIFE);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        ItemStack weapon = getWeapon();
        if (!weapon.isEmpty())
            tag.put(NBT_WEAPON, ItemStackData.save(weapon, level().registryAccess()));
        tag.putFloat(NBT_DAMAGE, throwDamage);
        tag.putBoolean(NBT_DEALT_DAMAGE, dealtDamage);
        tag.putInt(NBT_LIFE, life);
    }
}
