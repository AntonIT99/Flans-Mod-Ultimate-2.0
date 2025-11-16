package com.flansmodultimate.common.entity;

import com.flansmodultimate.common.types.InfoType;
import lombok.EqualsAndHashCode;
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

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class AAGun extends Entity implements IEntityAdditionalSpawnData, IFlanEntity<InfoType>
{
    protected static final EntityDataAccessor<String> AA_TYPE = SynchedEntityData.defineId(AAGun.class, EntityDataSerializers.STRING);

    @Getter
    protected InfoType configType;
    protected String shortname = StringUtils.EMPTY;

    public AAGun(EntityType<?> entityType, Level level)
    {
        super(entityType, level);
    }

    public AAGun(EntityType<?> entityType, Level level, InfoType infoType)
    {
        super(entityType, level);
        shortname = infoType.getShortName();
    }

    public String getShortName()
    {
        shortname = entityData.get(AA_TYPE);
        return shortname;
    }

    public void setShortName(String s)
    {
        shortname = s;
        entityData.set(AA_TYPE, shortname);
    }

    @Override
    protected void defineSynchedData()
    {
        entityData.define(AA_TYPE, StringUtils.EMPTY);
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
}
