package com.flansmodultimate.platform.damage;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Loader-neutral view of a mutable damage event. Business logic should depend on
 * this contract instead of a Forge or NeoForge event type.
 */
public interface MutableDamageContext
{
    LivingEntity entity();

    DamageSource source();

    float amount();

    void setAmount(float amount);

    void cancel();

    boolean isCanceled();
}
