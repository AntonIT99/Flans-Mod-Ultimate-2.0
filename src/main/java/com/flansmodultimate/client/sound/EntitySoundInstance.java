package com.flansmodultimate.client.sound;

import com.flansmodultimate.client.SoundHelper;
import com.flansmodultimate.common.driveables.DriveableControlPhysics;
import com.flansmodultimate.common.entity.Driveable;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

/**
 * A sound that follows the entity emitting it, and that the sound engine loops seamlessly when asked to.
 * <p>
 * A looping sound played this way never has to be sent again: it keeps playing until it is stopped,
 * with no gap where a repeat would otherwise have to be scheduled. That matters for continuous sounds
 * such as a running engine, where a repeat arriving even a few milliseconds late is clearly audible.
 */
public class EntitySoundInstance extends AbstractTickableSoundInstance
{
    private final Entity source;
    private float pitchRange;
    private boolean stopRequested;

    public EntitySoundInstance(SoundEvent soundEvent, Entity source, float range, boolean looping, float pitchRange)
    {
        super(soundEvent, SoundSource.PLAYERS, RandomSource.create());
        this.source = source;
        this.pitchRange = pitchRange;
        this.looping = looping;
        delay = 0;
        volume = SoundHelper.getVolumeFromRange(range, false);
        pitch = 1F;
        attenuation = Attenuation.LINEAR;
        followSource();
    }

    /** Asks the sound engine to stop this sound on the next client tick. */
    public void requestStop()
    {
        stopRequested = true;
    }

    /** True once the sound has nothing left to follow, so it can be forgotten. */
    public boolean isSourceGone()
    {
        return source.isRemoved() || !source.isAlive();
    }

    public boolean isSound(String sound)
    {
        return getLocation().getPath().equals(sound);
    }

    public void setPitchRange(float pitchRange)
    {
        this.pitchRange = pitchRange;
    }

    @Override
    public void tick()
    {
        if (stopRequested || isSourceGone())
        {
            stop();
            return;
        }

        followSource();
        if (source instanceof Driveable driveable)
            pitch = DriveableControlPhysics.engineSoundPitch(driveable.getThrottle(), pitchRange,
                driveable.getEngineSoundReverseSpeedRatio());
    }

    private void followSource()
    {
        x = source.getX();
        y = source.getY();
        z = source.getZ();
    }
}
