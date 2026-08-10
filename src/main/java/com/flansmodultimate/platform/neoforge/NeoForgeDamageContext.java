package com.flansmodultimate.platform.neoforge;

import com.flansmodultimate.platform.damage.MutableDamageContext;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public record NeoForgeDamageContext(LivingIncomingDamageEvent event) implements MutableDamageContext
{
    @Override
    public LivingEntity entity()
    {
        return event.getEntity();
    }

    @Override
    public DamageSource source()
    {
        return event.getSource();
    }

    @Override
    public float amount()
    {
        return event.getAmount();
    }

    @Override
    public void setAmount(float amount)
    {
        event.setAmount(amount);
    }

    @Override
    public void cancel()
    {
        event.setCanceled(true);
    }

    @Override
    public boolean isCanceled()
    {
        return event.isCanceled();
    }
}
