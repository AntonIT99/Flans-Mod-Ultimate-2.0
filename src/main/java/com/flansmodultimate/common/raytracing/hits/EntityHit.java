package com.flansmodultimate.common.raytracing.hits;

import lombok.Getter;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Getter
public class EntityHit extends BulletHit
{
    private final Entity entity;
    private final Vec3 impact;

    public EntityHit(Entity entity, float intersectTime, Vec3 impact)
    {
        super(intersectTime);
        this.entity = entity;
        this.impact = impact;
    }
}
