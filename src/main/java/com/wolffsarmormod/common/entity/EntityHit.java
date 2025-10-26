package com.wolffsarmormod.common.entity;

import com.wolffsarmormod.common.raytracing.BulletHit;

import net.minecraft.world.entity.Entity;

public class EntityHit extends BulletHit
{
    public Entity entity;

    public EntityHit(Entity e, float f)
    {
        super(f);
        entity = e;
    }

    @Override
    public Entity getEntity()
    {
        return entity;
    }
}
