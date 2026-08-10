package com.flansmodultimate.event;

import lombok.Getter;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

@Getter
public class GunReloadEvent extends Event implements ICancellableEvent
{
    private final Entity entity;
    private final ItemStack gunStack;

    public GunReloadEvent(Entity entity, ItemStack gunStack)
    {
        this.entity = entity;
        this.gunStack = gunStack;
    }
}
