package com.flansmodultimate.event;

import lombok.Getter;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

import net.minecraft.world.entity.Entity;

public class GunFiredEvent extends Event implements ICancellableEvent
{
    @Getter
    private final Entity shooter;

    public GunFiredEvent(Entity shooter) {
        this.shooter = shooter;
    }
}
