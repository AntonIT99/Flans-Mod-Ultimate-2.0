package com.wolffsarmormod.common.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class Vehicle extends Driveable
{
    public Vehicle(EntityType<?> entityType, Level level)
    {
        super(entityType, level);
    }
}
