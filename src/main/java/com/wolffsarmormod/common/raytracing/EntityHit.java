package com.wolffsarmormod.common.raytracing;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class EntityHit extends BulletHit
{
    private final Entity entity;
    public final Vec3 impact;

    public EntityHit(Entity entity, float intersectTime, Vec3 impact)
    {
        super(intersectTime);
        this.entity = entity;
        this.impact = impact;
    }

    @Override
    public Entity getEntity()
    {
        return entity;
    }
}
