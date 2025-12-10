package com.flansmodultimate.common.entity;

import com.flansmodultimate.common.driveables.Seat;
import com.flansmodultimate.common.guns.ShootingHelper;
import com.flansmodultimate.common.raytracing.hits.BulletHit;
import com.flansmodultimate.common.raytracing.hits.DriveableHit;
import com.flansmodultimate.common.types.BulletType;
import com.flansmodultimate.common.types.DriveableType;
import com.flansmodultimate.common.types.InfoType;
import lombok.Getter;
import lombok.Setter;
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
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.List;

public abstract class Driveable extends Entity implements IEntityAdditionalSpawnData, IFlanEntity<DriveableType>
{
    protected static final EntityDataAccessor<String> DRIVEABLE_TYPE = SynchedEntityData.defineId(Driveable.class, EntityDataSerializers.STRING);

    @Getter
    protected DriveableType configType;
    protected String shortname = StringUtils.EMPTY;

    @Getter
    protected Seat[] seats;

    protected boolean isShowedPosition = false;

    /** Flares */
    protected int flareDelay = 0;
    @Getter
    protected int ticksFlareUsing = 0;
    @Getter
    protected boolean varFlare;

    @Setter
    protected Entity lastAtkEntity;

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

    public void setEntityMarker(int tick)
    {
        isShowedPosition = true;
        tickCount = tick;
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

    /** Used to stop self collision */
    public boolean isPartOfThis(Entity entity)
    {
        for (Seat seat : seats)
        {
            if (seat == null)
                continue;
            if (entity == seat)
                return true;
            if (seat.getRiddenByEntity() == entity)
                return true;
        }
        return entity == this;
    }

    /**
     * Called if the bullet actually hit the part returned by the raytrace
     */
    public ShootingHelper.HitData bulletHit(BulletType bulletType, DriveableHit hit, ShootingHelper.HitData hitData)
    {
        //TODO: implement
        return hitData;
    }

    /**
     * Attack method called by bullets hitting the plane. Does advanced raytracing to detect which part of the plane is hit
     */
    public List<BulletHit> attackFromBullet(Vec3 origin, Vec3 motion)
    {
        //TODO: implement
        return Collections.emptyList();
    }
}
