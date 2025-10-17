package com.wolffsarmormod.common.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public abstract class Shootable extends Entity
{
    protected Shootable(EntityType<?> entityType, Level level)
    {
        super(entityType, level);
    }
}
