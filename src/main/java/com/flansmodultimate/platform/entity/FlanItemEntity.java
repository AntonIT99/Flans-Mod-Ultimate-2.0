package com.flansmodultimate.platform.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;

/** Keeps loader-specific spawn packet construction out of gameplay entities. */
public abstract class FlanItemEntity extends ItemEntity
{
    protected FlanItemEntity(EntityType<? extends ItemEntity> type, Level level)
    {
        super(type, level);
    }

}
