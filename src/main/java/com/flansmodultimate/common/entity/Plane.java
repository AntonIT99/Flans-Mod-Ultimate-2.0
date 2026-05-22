package com.flansmodultimate.common.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class Plane extends Driveable
{
    public Plane(EntityType<?> entityType, Level level)
    {
        super(entityType, level);
    }
}
