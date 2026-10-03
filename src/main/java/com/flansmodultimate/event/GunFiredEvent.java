package com.flansmodultimate.event;

import com.flansmodultimate.platform.event.FlanCancellableEvent;
import lombok.Getter;

import net.minecraft.world.entity.Entity;

public class GunFiredEvent extends FlanCancellableEvent
{
    @Getter
    private final Entity shooter;

    public GunFiredEvent(Entity shooter) {
        this.shooter = shooter;
    }
}
