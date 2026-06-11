package com.flansmodultimate.common.entity;

import lombok.EqualsAndHashCode;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Vehicle extends Driveable
{
    public Vehicle(EntityType<?> entityType, Level level)
    {
        super(entityType, level);
    }
}
