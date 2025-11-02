package com.wolffsarmormod.common.entity;

import com.wolffsarmormod.common.raytracing.DriveableHit;
import com.wolffsarmormod.common.types.BulletType;
import com.wolffsarmormod.common.types.InfoType;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import org.apache.commons.lang3.StringUtils;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class Driveable extends Entity implements IEntityAdditionalSpawnData, IFlanEntity
{
    protected static final EntityDataAccessor<String> DRIVEABLE_TYPE = SynchedEntityData.defineId(Driveable.class, EntityDataSerializers.STRING);

    /** Client and Server side */
    protected String shortname = StringUtils.EMPTY;

    public Driveable(EntityType<?> entityType, Level level)
    {
        super(entityType, level);
    }

    public Driveable(EntityType<?> entityType, Level level, InfoType infoType)
    {
        super(entityType, level);
        shortname = infoType.getShortName();
    }


    public String getShortName()
    {
        shortname = entityData.get(DRIVEABLE_TYPE);
        return shortname;
    }

    public void setShortName(String s)
    {
        shortname = s;
        entityData.set(DRIVEABLE_TYPE, shortname);
    }

    @Override
    protected void defineSynchedData()
    {
        entityData.define(DRIVEABLE_TYPE, StringUtils.EMPTY);
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buf)
    {
        buf.writeUtf(shortname);
    }

    @Override
    public void readSpawnData(FriendlyByteBuf buf)
    {
        shortname = buf.readUtf();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag pCompound)
    {

    }

    @Override
    protected void addAdditionalSaveData(CompoundTag pCompound)
    {

    }

    /**
     * Called if the bullet actually hit the part returned by the raytrace
     *
     * @param penetratingPower
     */
    public float bulletHit(BulletType bulletType, float damage, DriveableHit hit, float penetratingPower)
    {
        //TODO implement
        return 0F;
    }
}
