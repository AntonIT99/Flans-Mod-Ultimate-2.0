package com.flansmodultimate.common.entity;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.driveables.DriveablePosition;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/** Non-persistent hit and suspension proxy owned by a {@link Driveable}. */
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Wheel extends Entity
{
    private static final EntityDataAccessor<Integer> DATA_PARENT_ID = SynchedEntityData.defineId(Wheel.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_WHEEL_INDEX = SynchedEntityData.defineId(Wheel.class, EntityDataSerializers.INT);
    private static final int MAX_ORPHAN_TICKS = 100;

    @Getter @Nullable
    protected Driveable driveable;
    private int orphanTicks;

    public Wheel(EntityType<?> entityType, Level level)
    {
        super(entityType, level);
        noPhysics = true;
    }

    public Wheel(Level level, Driveable parent, int wheelIndex)
    {
        super(FlansMod.wheelEntity.get(), level);
        noPhysics = true;
        bind(parent, wheelIndex);
    }

    public void bind(@NotNull Driveable parent, int wheelIndex)
    {
        driveable = parent;
        entityData.set(DATA_PARENT_ID, parent.getId());
        entityData.set(DATA_WHEEL_INDEX, wheelIndex);
        parent.registerWheelProxy(this);
        snapToParent();
    }

    public int getParentId()
    {
        return entityData.get(DATA_PARENT_ID);
    }

    public int getWheelIndex()
    {
        return entityData.get(DATA_WHEEL_INDEX);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder)
    {
        builder.define(DATA_PARENT_ID, -1);
        builder.define(DATA_WHEEL_INDEX, -1);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull ValueInput input)
    {
    }

    @Override
    protected void addAdditionalSaveData(@NotNull ValueOutput output)
    {
    }

    @Override
    public void tick()
    {
        super.tick();
        if (!resolveParent())
        {
            if (++orphanTicks > MAX_ORPHAN_TICKS)
                discard();
            return;
        }

        orphanTicks = 0;
        if (driveable == null || driveable.isRemoved())
        {
            discard();
            return;
        }
        driveable.registerWheelProxy(this);
        snapToParent();
    }

    private boolean resolveParent()
    {
        if (driveable != null && !driveable.isRemoved() && driveable.getId() == getParentId())
            return true;
        Entity candidate = level().getEntity(getParentId());
        if (candidate instanceof Driveable parent)
        {
            driveable = parent;
            return true;
        }
        return false;
    }

    private void snapToParent()
    {
        if (driveable == null)
            return;
        Vec3 position = driveable.getWheelWorldPosition(getWheelIndex());
        setPos(position.x, position.y, position.z);
        setDeltaMovement(driveable.getDeltaMovement());
        setYRot(driveable.getYaw());
    }

    @Override
    public boolean isPickable()
    {
        return isAlive();
    }

    @Override
    public boolean hurtServer(@NotNull ServerLevel serverLevel, @NotNull DamageSource source, float amount)
    {
        if (driveable == null || level().isClientSide())
            return driveable != null;
        DriveablePosition definition = driveable.getConfigType() == null ? null : driveable.getConfigType().getWheelPosition(getWheelIndex());
        EnumDriveablePart part = definition == null ? EnumDriveablePart.CORE : definition.getPart();
        return driveable.damagePart(part, amount, source);
    }
}
