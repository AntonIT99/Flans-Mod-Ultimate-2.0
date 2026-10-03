package com.flansmodultimate.event;

import com.flansmodultimate.common.entity.Grenade;
import com.flansmodultimate.platform.event.FlanCancellableEvent;
import lombok.Getter;

import net.minecraft.world.entity.Entity;

@Getter
public class GrenadeProximityEvent extends FlanCancellableEvent
{
    private final Grenade grenade;
    private final Entity trigger;

    public GrenadeProximityEvent(Grenade grenade, Entity trigger)
    {
        this.grenade = grenade;
        this.trigger = trigger;
    }
}
