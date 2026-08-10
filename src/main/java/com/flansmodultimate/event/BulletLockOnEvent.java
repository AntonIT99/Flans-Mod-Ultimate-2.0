package com.flansmodultimate.event;

import com.flansmodultimate.common.entity.Bullet;
import lombok.Getter;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.entity.Entity;

@Getter
public class BulletLockOnEvent extends Event implements ICancellableEvent
{
    private final Bullet bullet;
    private final Entity lockedOnTo;

    public BulletLockOnEvent(Bullet bullet, Entity lockedOnTo)
    {
        this.bullet = bullet;
        this.lockedOnTo = lockedOnTo;
    }
}
