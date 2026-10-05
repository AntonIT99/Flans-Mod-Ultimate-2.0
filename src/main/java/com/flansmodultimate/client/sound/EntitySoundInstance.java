package com.flansmodultimate.client.sound;

import com.flansmodultimate.common.driveables.DriveableControlPhysics;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.types.PlaneType;
import com.flansmodultimate.config.ModClientConfig;

import lombok.Setter;
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
    private final Entity emitter;
    private final float basePitch;
    @Setter
    private boolean varyPitch;
    private boolean stopRequested;

    public EntitySoundInstance(SoundEvent soundEvent, Entity source, float range, boolean looping, boolean varyPitch)
    {
        this(soundEvent, source, SoundHelper.getVolumeFromRange(range, false), 1F, RandomSource.create());
        this.varyPitch = varyPitch;
        this.looping = looping;
    }

    public EntitySoundInstance(SoundEvent soundEvent, Entity emitter, float volume, float pitch, RandomSource random)
    {
        super(soundEvent, SoundSource.PLAYERS, random);
        this.emitter = emitter;
        basePitch = pitch;
        this.volume = volume;
        this.pitch = pitch;
        relative = false;
        delay = 0;
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
        return emitter.isRemoved() || !emitter.isAlive();
    }

    public boolean isSound(String sound)
    {
        return getLocation().getPath().equals(sound);
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
        if (varyPitch && emitter instanceof Driveable driveable && driveable.getConfigType() != null)
            pitch = DriveableControlPhysics.engineSoundPitch(driveable.getThrottle(),
                driveable.getConfigType().getEngineSoundPitchCurve(
                    driveable.getConfigType() instanceof PlaneType ? ModClientConfig.defaultPlaneEnginePitch() : ModClientConfig.defaultVehicleEnginePitch()),
                driveable.getEngineSoundReverseSpeedRatio());
        else
            pitch = basePitch;
    }

    private void followSource()
    {
        x = emitter.getX();
        y = emitter.getY();
        z = emitter.getZ();
    }
}
