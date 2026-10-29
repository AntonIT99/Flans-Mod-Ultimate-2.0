package com.flansmodultimate.client.sound;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.mixin.ChannelAccessor;
import com.mojang.blaze3d.audio.Channel;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.SOFTSourceSpatialize;

import net.minecraft.client.resources.sounds.SoundInstance;

/** Enables the same positional fade for mono and stereo Flan audio, including NPC and player engines. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SoundSpatialization
{
    /** Called by the sound-source events on the audio thread, with the OpenAL context current. */
    public static void apply(SoundInstance sound, Channel channel)
    {
        if (!isPositionalFlanSound(sound) || !AL.getCapabilities().AL_SOFT_source_spatialize)
            return;

        // OpenAL's default AUTO mode ignores position and distance for stereo buffers. Explicitly opt
        // Flan world sounds into spatialization. Minecraft creates a fresh source for each channel.
        AL10.alSourcei(((ChannelAccessor) channel).flansmodultimateSource(), SOFTSourceSpatialize.AL_SOURCE_SPATIALIZE_SOFT, AL10.AL_TRUE);
    }

    static boolean isPositionalFlanSound(SoundInstance sound)
    {
        return !sound.isRelative() && sound.getAttenuation() == SoundInstance.Attenuation.LINEAR
            && (FlansMod.FLANSMOD_ID.equals(sound.getLocation().getNamespace()) || sound instanceof EntitySoundInstance);
    }
}
