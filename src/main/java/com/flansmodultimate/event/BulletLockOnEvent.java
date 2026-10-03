package com.flansmodultimate.event;

import com.flansmodultimate.common.entity.Bullet;
import com.flansmodultimate.platform.event.FlanCancellableEvent;
import lombok.Getter;

import net.minecraft.world.entity.Entity;

@Getter
public class BulletLockOnEvent extends FlanCancellableEvent
{
    private final Bullet bullet;
    private final Entity lockedOnTo;

    public BulletLockOnEvent(Bullet bullet, Entity lockedOnTo)
    {
        this.bullet = bullet;
        this.lockedOnTo = lockedOnTo;
    }
}
