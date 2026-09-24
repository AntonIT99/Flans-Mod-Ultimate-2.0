package com.flansmodultimate.platform.neoforge;

import com.flansmodultimate.platform.damage.MutableDamageContext;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.damagesource.DamageContainer;

public final class NeoForgeDamageContext implements MutableDamageContext
{
    private final LivingEntity entity;
    private final DamageContainer container;
    private boolean canceled;

    public NeoForgeDamageContext(LivingEntity entity, DamageContainer container)
    {
        this.entity = entity;
        this.container = container;
    }

    @Override
    public LivingEntity entity()
    {
        return entity;
    }

    @Override
    public DamageSource source()
    {
        return container.getSource();
    }

    @Override
    public float amount()
    {
        return container.getNewDamage();
    }

    @Override
    public void setAmount(float amount)
    {
        container.setNewDamage(amount);
    }

    @Override
    public void cancel()
    {
        canceled = true;
    }

    @Override
    public boolean isCanceled()
    {
        return canceled;
    }
}
