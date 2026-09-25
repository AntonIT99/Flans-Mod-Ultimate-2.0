package com.flansmodultimate.platform.entity;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

/** Version boundary for looking up mob effects by their legacy numeric potion IDs. */
public final class EffectPlatform
{
    private EffectPlatform() {}

    /** An effect instance for the legacy 1-based potion ID, or null if no effect has that ID. */
    @Nullable
    public static MobEffectInstance legacyEffect(int legacyId, int duration, int amplifier, boolean ambient, boolean visible)
    {
        MobEffect effect = MobEffect.byId(legacyId);
        return effect == null ? null : new MobEffectInstance(effect, duration, amplifier, ambient, visible);
    }
}
