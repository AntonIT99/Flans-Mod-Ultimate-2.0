package com.flansmodultimate.common.entity;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.FlanDamageSources;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.platform.entity.SynchedDataDefinition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
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
        // The weapon is the arrow's pickup stack, not its firing weapon, so no enchantment hooks of the gun apply
        super(FlansMod.thrownGunEntity.get(), thrower, level, weapon.copy(), null);
        setWeapon(getPickupItemStackOrigin());
        this.throwDamage = throwDamage;
    }

    @Override
    protected void defineSynchedData(@NotNull SynchedEntityData.Builder builder)
    {
        super.defineSynchedData(builder);
        defineEntityData(new SynchedDataDefinition(builder));
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

    /** Also reached from vanilla loading and slot access; keeps the synced copy renderers read in step. */
    @Override
    protected void setPickupItemStack(@NotNull ItemStack pickupItemStack)
    {
        super.setPickupItemStack(pickupItemStack);
        setWeapon(getPickupItemStackOrigin());
    }

    @Override
    @NotNull
    protected ItemStack getDefaultPickupItem()
    {
        return ItemStack.EMPTY;
    }

    /** Minecraft 1.21 cannot encode the empty pickup stack of a weaponless entity, which has nothing to recover anyway. */
    @Override
    public boolean shouldBeSaved()
    {
        return super.shouldBeSaved() && !getPickupItemStackOrigin().isEmpty();
    }

    @Nullable
    public GunType getGunType()
    {
        return getWeapon().getItem() instanceof GunItem gunItem ? gunItem.getConfigType() : null;
    }

    @Override
    public void tick()
    {
        if (!level().isClientSide && getPickupItemStackOrigin().isEmpty())
        {
            discard();
            return;
        }

        // Once it has settled it no longer wounds anything walking into it
        if (inGroundTime > 4)
            dealtDamage = true;

        super.tick();
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

    /** The weapon is saved by AbstractArrow as its pickup item; its own "weapon" key is the firing weapon. */
    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        throwDamage = tag.getFloat(NBT_DAMAGE);
        dealtDamage = tag.getBoolean(NBT_DEALT_DAMAGE);
        life = tag.getInt(NBT_LIFE);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        tag.putFloat(NBT_DAMAGE, throwDamage);
        tag.putBoolean(NBT_DEALT_DAMAGE, dealtDamage);
        tag.putInt(NBT_LIFE, life);
    }
}
