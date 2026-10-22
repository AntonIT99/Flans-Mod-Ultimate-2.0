package com.flansmodultimate.platform;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

/** Version boundary for vanilla sounds that later versions expose only as registry holders. */
public final class PlatformSounds
{
    private PlatformSounds()
    {}

    public static SoundEvent tridentThrow()
    {
        return SoundEvents.TRIDENT_THROW;
    }

    public static SoundEvent genericExplode()
    {
        return SoundEvents.GENERIC_EXPLODE;
    }
}
