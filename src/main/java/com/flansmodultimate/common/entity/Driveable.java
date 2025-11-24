package com.flansmodultimate.common.entity;

import com.flansmodultimate.common.raytracing.DriveableHit;
import com.flansmodultimate.common.types.BulletType;
import com.flansmodultimate.common.types.DriveableType;
import com.flansmodultimate.common.types.InfoType;
import lombok.Getter;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public abstract class Driveable extends Entity implements IEntityAdditionalSpawnData, IFlanEntity<DriveableType>
{
    protected static final EntityDataAccessor<String> DRIVEABLE_TYPE = SynchedEntityData.defineId(Driveable.class, EntityDataSerializers.STRING);

    @Getter
    protected DriveableType configType;
    protected String shortname = StringUtils.EMPTY;

    //Flares
    protected int flareDelay = 0;
    @Getter
    protected int ticksFlareUsing = 0;
    @Getter
    protected boolean varFlare;

    protected Driveable(EntityType<?> entityType, Level level)
    {
        super(entityType, level);
    }

    protected Driveable(EntityType<?> entityType, Level level, InfoType infoType)
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
    protected void readAdditionalSaveData(@NotNull CompoundTag pCompound)
    {

    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag pCompound)
    {

    }

    /**
     * Called if the bullet actually hit the part returned by the raytrace
     */
    public float bulletHit(BulletType bulletType, float damage, DriveableHit hit, float penetratingPower)
    {
        //TODO implement
        return 0F;
    }
}
