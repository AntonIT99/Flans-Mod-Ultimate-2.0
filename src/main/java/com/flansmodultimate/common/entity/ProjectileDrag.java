package com.flansmodultimate.common.entity;

import com.flansmodultimate.common.physics.ModPhysics;
import com.flansmodultimate.common.types.ShootableType;

import net.minecraft.world.entity.Entity;

/** Per-tick velocity retained by a projectile in each medium. */
public final class ProjectileDrag
{
    private ProjectileDrag() {}

    public static float factor(Entity entity, float air, float water)
    {
        if (entity.isInWater())
            return (float) ModPhysics.dragRetention(water, entity.level());
        if (entity.isInLava())
            return (float) ModPhysics.dragRetention(ShootableType.LAVA_DEFAULT_DRAG, entity.level());
        return (float) ModPhysics.dragRetention(air, entity.level());
    }
}
