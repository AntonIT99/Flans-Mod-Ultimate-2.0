package com.flansmodultimate.platform.forge;

import com.flansmodultimate.platform.damage.MutableDamageContext;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/** Forge {@link LivingHurtEvent} viewed as a loader-neutral mutable damage context. */
public final class ForgeDamageContext implements MutableDamageContext
{
    private final LivingHurtEvent event;

    public ForgeDamageContext(LivingHurtEvent event)
    {
        this.event = event;
    }

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
