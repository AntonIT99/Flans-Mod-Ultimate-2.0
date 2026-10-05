package com.flansmodultimate.common.entity;

import com.flansmodultimate.FlansModEntities;
import com.flansmodultimate.common.FlanDamageSources;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.physics.ModPhysics;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.common.types.ShootableType;
import com.flansmodultimate.platform.entity.FlanArrow;
import com.flansmodultimate.platform.entity.SynchedDataDefinition;
import lombok.EqualsAndHashCode;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A gun item thrown with {@code SecondaryFunction Throw}, such as a javelin or a pilum. It flies and
 * sticks like a trident, hits once, and carries the thrown stack so picking it up returns the very
 * same weapon.
 */
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ThrownGun extends FlanArrow
{
    /** Ticks a recoverable weapon stays stuck before despawning, the lifespan of a dropped item */
    public static final int RECOVERABLE_LIFESPAN = 6000;
    /** Gravity AbstractArrow applies on its own each tick, which this projectile replaces. */
    private static final double VANILLA_ARROW_GRAVITY = 0.05D;

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
        super(FlansModEntities.thrownGunEntity.get(), thrower, level, weapon);
        this.throwDamage = throwDamage;
    }

    @Override
    protected void defineEntityData(SynchedDataDefinition data)
    {
        data.define(DATA_WEAPON, ItemStack.EMPTY);
    }

    @Override
    public ItemStack getWeapon()
    {
        return entityData.get(DATA_WEAPON);
    }

    @Override
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
        if (!prepareProjectileTick())
            return;

        // Once it has settled it no longer wounds anything walking into it
        if (inGroundTime > 4)
            dealtDamage = true;

        super.tick();
        // AbstractArrow applies fixed air drag and the vanilla arrow gravity of 0.05
        // internally. Undo that gravity and apply the gravity shared by every Flan's
        // projectile instead, with this mod's dimension factors; water drag uses
        // getWaterInertia below.
        if (!inGround)
        {
            boolean gravity = !isNoGravity();
            double ratio = isInWater() ? 1D : ProjectileDrag.factor(this,
                ShootableType.AIR_DEFAULT_DRAG, ShootableType.WATER_DEFAULT_DRAG)
                / ShootableType.AIR_DEFAULT_DRAG;
            Vec3 motion = getDeltaMovement().add(0D, gravity ? VANILLA_ARROW_GRAVITY : 0D, 0D).scale(ratio)
                .add(0D, gravity ? -ModPhysics.gravity(ShootableType.FALL_SPEED_COEFFICIENT, level()) : 0D, 0D);
            setDeltaMovement(motion);
        }
    }

    @Override
    protected float getWaterInertia()
    {
        return ProjectileDrag.factor(this, ShootableType.AIR_DEFAULT_DRAG, ShootableType.WATER_DEFAULT_DRAG);
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
