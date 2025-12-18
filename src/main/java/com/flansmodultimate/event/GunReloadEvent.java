package com.flansmodultimate.event;

import lombok.Getter;
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

    public GunReloadEvent(Entity entity, ItemStack gunStack)
    {
        this.entity = entity;
        this.gunStack = gunStack;
    }
}
