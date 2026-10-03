package com.flansmodultimate.event;

import lombok.Getter;
import lombok.Setter;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

@Getter
public class GunReloadEvent extends Event implements ICancellableEvent
{
    private final Entity entity;
    private final ItemStack gunStack;
    /**
     * Set to false to load the gun's default ammunition without taking any from the inventory,
     * as the 1.7.10 event allowed. The common config gunDevMode does the same for every reload.
     */
    @Setter
    private boolean needsAmmo = true;

    public GunReloadEvent(Entity entity, ItemStack gunStack)
    {
        this.entity = entity;
        this.gunStack = gunStack;
    }
}
