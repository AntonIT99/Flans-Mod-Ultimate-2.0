package com.flansmodultimate.event;

import lombok.Getter;
import lombok.Setter;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

@Cancelable
@Getter
public class GunReloadEvent extends Event
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
