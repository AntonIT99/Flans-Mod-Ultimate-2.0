package com.flansmodultimate.common.entity;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Wheel extends Entity
{
    @Getter
    protected Driveable driveable;

    public Wheel(EntityType<?> pEntityType, Level pLevel)
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
