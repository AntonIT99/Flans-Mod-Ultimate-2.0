package com.flansmodultimate.event;

import lombok.Getter;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

import net.minecraft.world.entity.Entity;

@Cancelable
public class GunFiredEvent extends Event
{
    @Getter
    private final Entity shooter;

    public GunFiredEvent(Entity shooter) {
        this.shooter = shooter;
    }
}
