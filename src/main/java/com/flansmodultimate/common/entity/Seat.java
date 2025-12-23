package com.flansmodultimate.common.entity;

import com.flansmodultimate.common.driveables.SeatInfo;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

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

    @Getter
    protected SeatInfo seatInfo;

    public Seat(EntityType<?> pEntityType, Level pLevel)
    {
        super(pEntityType, pLevel);
    }

    @Override
    protected void defineSynchedData()
    {

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
