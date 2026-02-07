package com.flansmodultimate.event;

import com.flansmodultimate.common.entity.Grenade;
import lombok.Getter;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

import net.minecraft.world.entity.Entity;

@Cancelable
@Getter
public class GrenadeProximityEvent extends Event
{
    private final Grenade grenade;
    private final Entity trigger;

    public GrenadeProximityEvent(Grenade grenade, Entity trigger)
    {
        this.grenade = grenade;
        this.trigger = trigger;
    }
}
