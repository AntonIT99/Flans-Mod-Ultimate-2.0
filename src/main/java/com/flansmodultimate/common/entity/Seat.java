package com.flansmodultimate.common.entity;

import lombok.Getter;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class Seat extends Entity
{
    @Getter
    protected Driveable driveable;

    /** The entity that is riding this entity */
    @Getter
    protected Entity riddenByEntity;

    public Seat(EntityType<?> pEntityType, Level pLevel)
    {
        super(pEntityType, pLevel);
    }

    @Override
    protected void defineSynchedData()
    {

    }

    @Override
    protected void readAdditionalSaveData(CompoundTag pCompound)
    {

    }

    @Override
    protected void addAdditionalSaveData(CompoundTag pCompound)
    {

    }
}
