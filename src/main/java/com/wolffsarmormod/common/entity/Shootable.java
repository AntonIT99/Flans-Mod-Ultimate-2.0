package com.wolffsarmormod.common.entity;

import com.wolffsarmormod.common.types.ShootableType;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;

public abstract class Shootable extends Entity implements IEntityAdditionalSpawnData, IFlanEntity
{
    public static final float DEFAULT_HITBOX_SIZE = 0.5F;

    protected static final EntityDataAccessor<String> SHOOTABLE_TYPE = SynchedEntityData.defineId(Shootable.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Float> HITBOX_SIZE = SynchedEntityData.defineId(Shootable.class, EntityDataSerializers.FLOAT);

    /** Synced between Client and Server */
    protected String shortname = StringUtils.EMPTY;

    protected Shootable(EntityType<?> entityType, Level level)
    {
        super(entityType, level);
    }

    protected Shootable(EntityType<?> entityType, Level level, ShootableType type)
    {
        this(entityType, level);
        setShortName(type.getShortName());
        setHitboxSize(type.getHitBoxSize());
    }

    public String getShortName()
    {
        shortname = entityData.get(SHOOTABLE_TYPE);
        return shortname;
    }

    public void setShortName(String s)
    {
        shortname = s;
        entityData.set(SHOOTABLE_TYPE, shortname);
    }

    public float getHitboxSize()
    {
        return entityData.get(HITBOX_SIZE);
    }

    public void setHitboxSize(float hitboxSize)
    {
        hitboxSize = Math.max(0.01F, hitboxSize);
        entityData.set(HITBOX_SIZE, hitboxSize);
        if (!level().isClientSide)
            refreshDimensions();
    }

    @Override
    @NotNull
    public EntityDimensions getDimensions(@NotNull Pose pose)
    {
        float hitboxSize = getHitboxSize();
        return EntityDimensions.fixed(hitboxSize, hitboxSize);
    }

    @Override
    protected void defineSynchedData()
    {
        entityData.define(SHOOTABLE_TYPE, StringUtils.EMPTY);
        entityData.define(HITBOX_SIZE, DEFAULT_HITBOX_SIZE);
    }

    @Override
    public void onSyncedDataUpdated(@NotNull EntityDataAccessor<?> key)
    {
        super.onSyncedDataUpdated(key);
        if (HITBOX_SIZE.equals(key))
            refreshDimensions();
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buf)
    {
        buf.writeUtf(shortname);
        buf.writeFloat(getHitboxSize());
    }

    @Override
    public void readSpawnData(FriendlyByteBuf buf)
    {
        shortname = buf.readUtf();
        setHitboxSize(buf.readFloat());
    }
}
