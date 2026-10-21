package com.flansmodultimate.client.damage;

import com.flansmodultimate.common.entity.Driveable;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * The crackle of a burning wreck: one looping sound that follows the wreck and swells and dies with its fire, rather
 * than a fresh burst every second or so.
 */
final class WreckFireSound extends AbstractTickableSoundInstance
{
    private static final float MIN_VOLUME = 0.3F;
    private static final float VOLUME_PER_FIRE = 1.4F;

    private final Driveable wreck;

    WreckFireSound(Driveable wreck)
    {
        super(SoundEvents.FIRE_AMBIENT, SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
        this.wreck = wreck;
        looping = true;
        delay = 0;
        volume = MIN_VOLUME;
        pitch = 0.8F + random.nextFloat() * 0.2F;
        follow();
    }

    @Override
    public void tick()
    {
        float fire = DriveableDamageEffects.wreckFireStrength(wreck);
        if (wreck.isRemoved() || fire <= 0F)
        {
            stop();
            return;
        }
        volume = MIN_VOLUME + VOLUME_PER_FIRE * fire;
        follow();
    }

    private void follow()
    {
        x = wreck.getX();
        y = wreck.getY() + wreck.getBbHeight() * 0.5D;
        z = wreck.getZ();
    }
}
