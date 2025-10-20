package com.wolffsarmormod.common.item;

import com.wolffsarmormod.common.types.GrenadeType;
import lombok.Getter;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class GrenadeItem extends ShootableItem
{
    @Getter
    protected final GrenadeType configType;

    public GrenadeItem(GrenadeType configType)
    {
        super(configType);
        this.configType = configType;
    }

    public void throwGrenade(Level level, LivingEntity thrower)
    {
        //TODO: implement EntityGrenade
        /*EntityGrenade grenade = getGrenade(world, thrower);
        world.spawnEntity(grenade);*/
    }
}
