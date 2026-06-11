package com.flansmodultimate.common.entity;

import lombok.EqualsAndHashCode;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Mecha extends Driveable
{
    public Mecha(EntityType<?> entityType, Level level)
    {
        super(entityType, level);
    }

    //TODO: uncomment
    /** Light Level */
    public int lightLevel()
    {
        int level = 0;
        /*for (MechaItemType type : getUpgradeTypes())
        {
            level = Math.max(level, type.lightLevel);
        }*/
        return level;
    }

    //TODO: uncomment
    /** Force Darkness */
    public boolean forceDark()
    {
        /*for (MechaItemType type : getUpgradeTypes())
        {
            if (type.forceDark)
                return true;
        }*/
        return false;
    }
}
