package com.wolffsarmormod.common.entity;

import com.wolffsarmormod.common.raytracing.DriveableHit;
import com.wolffsarmormod.common.types.BulletType;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class Driveable extends Entity
{
    public Driveable(EntityType<?> entityType, Level level)
    {
        super(entityType, level);
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
