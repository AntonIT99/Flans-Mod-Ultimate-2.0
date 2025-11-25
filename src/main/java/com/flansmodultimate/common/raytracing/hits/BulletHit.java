package com.flansmodultimate.common.raytracing.hits;

import lombok.Getter;

import net.minecraft.world.entity.Entity;

import java.util.Objects;

@Getter
public abstract class BulletHit implements Comparable<BulletHit>
{
    /** The time along the ray that the intersection happened. Between 0 and 1 */
    protected final float intersectTime;

    protected BulletHit(float intersectTime)
    {
        this.intersectTime = intersectTime;
    }

    public abstract Entity getEntity();

    @Override
    public int compareTo(BulletHit other)
    {
        if (intersectTime < other.intersectTime)
            return -1;
        else if (intersectTime > other.intersectTime)
            return 1;
        return 0;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (!(o instanceof BulletHit other))
            return false;
        return Float.compare(intersectTime, other.intersectTime) == 0 && getEntity() == other.getEntity();
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(intersectTime, getEntity());
    }
}
