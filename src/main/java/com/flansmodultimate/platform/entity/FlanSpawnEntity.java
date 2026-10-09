package com.flansmodultimate.platform.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** Keeps loader-specific spawn packet construction out of gameplay entities. */
public abstract class FlanSpawnEntity extends FlanEntity
{
    protected FlanSpawnEntity(EntityType<?> type, Level level)
    {
        super(type, level);
    }

}
