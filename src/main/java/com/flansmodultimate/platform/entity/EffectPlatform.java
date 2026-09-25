package com.flansmodultimate.platform.entity;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;

/** Version boundary for looking up mob effects by their legacy numeric potion IDs. */
public final class EffectPlatform
{
    private EffectPlatform() {}

    /** An effect instance for the legacy 1-based potion ID, or null if no effect has that ID. */
    @Nullable
    public static MobEffectInstance legacyEffect(int legacyId, int duration, int amplifier, boolean ambient, boolean visible)
    {
        // The 1.21 built-in registry is indexed from zero, while legacy content uses 1-based IDs.
        return BuiltInRegistries.MOB_EFFECT.getHolder(legacyId - 1)
            .map(effect -> new MobEffectInstance(effect, duration, amplifier, ambient, visible))
            .orElse(null);
    }
}
