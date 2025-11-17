package com.flansmodultimate.event;

import com.flansmodultimate.common.entity.Bullet;
import lombok.Getter;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

import net.minecraft.world.entity.Entity;

@Cancelable
@Getter
public class BulletLockOnEvent extends Event
{
    private final Bullet bullet;
    private final Entity lockedOnTo;

    public BulletLockOnEvent(Bullet bullet, Entity lockedOnTo)
    {
        this.bullet = bullet;
        this.lockedOnTo = lockedOnTo;
    }
}
