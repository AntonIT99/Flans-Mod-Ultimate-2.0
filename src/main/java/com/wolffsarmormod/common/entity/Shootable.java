package com.wolffsarmormod.common.entity;

import com.wolffsarmormod.common.types.ShootableType;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import org.apache.commons.lang3.StringUtils;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public abstract class Shootable extends Entity implements IEntityAdditionalSpawnData, IFlanEntity
{
    protected static final EntityDataAccessor<String> SHOOTABLE_TYPE = SynchedEntityData.defineId(Shootable.class, EntityDataSerializers.STRING);

    /** Client and Server side */
    protected String shortname = StringUtils.EMPTY;
    protected float fallSpeed;
    protected boolean hasLight;
    protected boolean trailParticles;
    protected String trailParticleType = StringUtils.EMPTY;

    protected Shootable(EntityType<?> entityType, Level level)
    {
        super(entityType, level);
    }

    protected Shootable(EntityType<?> entityType, Level level, ShootableType type)
    {
        this(entityType, level);
        shortname = type.getShortName();
        fallSpeed = type.getFallSpeed();
        hasLight = type.isHasLight();
        trailParticles = type.isTrailParticles();
        trailParticleType = type.getTrailParticleType();
        entityData.set(SHOOTABLE_TYPE, shortname);
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

    @Override
    protected void defineSynchedData()
    {
        entityData.define(SHOOTABLE_TYPE, StringUtils.EMPTY);
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buf)
    {
        buf.writeUtf(shortname);
        buf.writeFloat(fallSpeed);
        buf.writeBoolean(hasLight);
        buf.writeBoolean(trailParticles);
        buf.writeUtf(trailParticleType);
    }

    @Override
    public void readSpawnData(FriendlyByteBuf buf)
    {
        shortname = buf.readUtf();
        fallSpeed = buf.readFloat();
        hasLight = buf.readBoolean();
        trailParticles = buf.readBoolean();
        trailParticleType = buf.readUtf();
    }
}
