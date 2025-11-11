package com.flansmodultimate.common.driveables;

import com.flansmodultimate.common.entity.Driveable;
import lombok.Getter;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class Seat extends Entity
{
    @Getter
    protected Driveable driveable;

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
