package com.flansmodultimate.hooks.client;

import com.flansmodultimate.client.sound.SoundHelper;
import com.flansmodultimate.hooks.IClientSoundHooks;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class ClientSoundHooksImpl implements IClientSoundHooks
{
    public void playSound(@Nullable String sound, Vec3 pos, float range, boolean distort, boolean silenced, boolean cancellable, UUID instanceUUID, @Nullable Entity emitter)
    {
        SoundHelper.playSound(sound, pos, range, distort, silenced, cancellable, instanceUUID, emitter);
    }

    public void setLoopingEntitySound(Entity source, String channel, @Nullable String sound, float range, boolean varyPitch)
    {
        SoundHelper.setLoopingEntitySound(source, channel, sound, range, varyPitch);
    }

    public void playEntitySound(Entity source, @Nullable String sound, float range)
    {
        SoundHelper.playEntitySound(source, sound, range);
    }

    public void cancelSound(UUID instanceUUID)
    {
        SoundHelper.cancelSound(instanceUUID);
    }
}
