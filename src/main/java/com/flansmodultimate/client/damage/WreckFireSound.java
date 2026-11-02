package com.flansmodultimate.client.damage;

import com.flansmodultimate.common.entity.Driveable;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

/**
 * One layer of the sound of a burning wreck: a loop that follows the wreck and swells and dies with its fire, rather
 * than a fresh burst every second or so.
 *
 * <p>
 * A vehicle fire is played as two layers, so that it reads as a hull full of burning fuel rather than as a campfire:
 * a roar that carries, and a crackle only heard close to. Both drop in pitch as the fire burns itself out.
 * </p>
 */
final class WreckFireSound extends AbstractTickableSoundInstance
{
    private static final float MIN_VOLUME = 0.3F;
    private static final float VOLUME_PER_FIRE = 1.4F;
    /** How much of the fire a crackle layer is worth beside the roar, and how far it carries. */
    private static final float CRACKLE_SHARE = 0.45F;
    /** The pitch a layer is played at when the fire is at its height, and when it is nearly out. */
    private static final float HOT_PITCH = 1F;
    private static final float COLD_PITCH = 0.7F;

    private final Driveable wreck;
    private final float gain;
    private final float basePitch;

    /** @return the roar that carries, which is what a wreck is heard by from a distance */
    static WreckFireSound roar(Driveable wreck)
    {
        return new WreckFireSound(wreck, SoundEvents.FIRE_AMBIENT, 1F, 0.8F);
    }

    /** @return the crackle of the fire itself, audible only near the wreck */
    static WreckFireSound crackle(Driveable wreck)
    {
        return new WreckFireSound(wreck, SoundEvents.CAMPFIRE_CRACKLE, CRACKLE_SHARE, 1.1F);
    }

    private WreckFireSound(Driveable wreck, SoundEvent sound, float gain, float basePitch)
    {
        super(sound, SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
        this.wreck = wreck;
        this.gain = gain;
        this.basePitch = basePitch;
        looping = true;
        delay = 0;
        volume = MIN_VOLUME * gain;
        pitch = basePitch;
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
        volume = (MIN_VOLUME + VOLUME_PER_FIRE * fire) * gain;
        // The fire falls away in pitch as well as in volume, so it dies down rather than simply growing quieter.
        pitch = basePitch * Mth.lerp(Mth.clamp(fire, 0F, 1F), COLD_PITCH, HOT_PITCH);
        follow();
    }

    private void follow()
    {
        x = wreck.getX();
        y = wreck.getY() + wreck.getBbHeight() * 0.5D;
        z = wreck.getZ();
    }
}
