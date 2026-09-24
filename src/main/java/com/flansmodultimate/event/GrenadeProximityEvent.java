package com.flansmodultimate.event;

import com.flansmodultimate.common.entity.Grenade;
import lombok.Getter;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

import net.minecraft.world.entity.Entity;

@Getter
public class GrenadeProximityEvent extends Event implements ICancellableEvent
{
    private final Grenade grenade;
    private final Entity trigger;

    public GrenadeProximityEvent(Grenade grenade, Entity trigger)
    {
        this.grenade = grenade;
        this.trigger = trigger;
    }
}
