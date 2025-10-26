package com.wolffsarmormod.common.raytracing;

import net.minecraft.world.entity.Entity;

public abstract class BulletHit implements Comparable<BulletHit>
{
    /**
     * The time along the ray that the intersection happened. Between 0 and 1
     */
    public float intersectTime;

    protected BulletHit(float f)
    {
        intersectTime = f;
    }

    @Override
    public int compareTo(BulletHit other)
    {
        if (intersectTime < other.intersectTime)
            return -1;
        else if (intersectTime > other.intersectTime)
            return 1;
        return 0;
    }

    public abstract Entity getEntity();
}
