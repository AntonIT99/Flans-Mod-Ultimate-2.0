package com.flansmodultimate.client.sound;

import com.flansmodultimate.mixin.ChannelAccessor;
import com.mojang.blaze3d.audio.Channel;
import org.junit.jupiter.api.Test;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.SOFTLoopback;
import org.lwjgl.openal.SOFTSourceSpatialize;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class SoundSpatializationTest
{
    @Test
    void contentPackWorldSoundsUseSpatializationForPlayersAndNpcs()
    {
        // Selection depends on the sound, not whether a player, driveable or NPC emitted it.
        assertTrue(SoundSpatialization.isPositionalFlanSound(sound("flansmod:fighterengine", false, SoundInstance.Attenuation.LINEAR)));
        assertTrue(SoundSpatialization.isPositionalFlanSound(sound("flansmod:planemgshoot", false, SoundInstance.Attenuation.LINEAR)));
    }

    @Test
    void addonEngineClipsUseSpatializationEvenWithAnotherNamespace()
    {
        EntitySoundInstance sound = mock(EntitySoundInstance.class);
        when(sound.getLocation()).thenReturn(ResourceLocation.parse("addon:engine"));
        when(sound.getAttenuation()).thenReturn(SoundInstance.Attenuation.LINEAR);
        assertTrue(SoundSpatialization.isPositionalFlanSound(sound));
    }

    @Test
    void uiAndExplicitlyUnattenuatedSoundsKeepTheirPlaybackMode()
    {
        assertFalse(SoundSpatialization.isPositionalFlanSound(sound("flansmod:fighterengine", true, SoundInstance.Attenuation.LINEAR)));
        assertFalse(SoundSpatialization.isPositionalFlanSound(sound("flansmod:fighterengine", false, SoundInstance.Attenuation.NONE)));
        assertFalse(SoundSpatialization.isPositionalFlanSound(sound("minecraft:music.menu", true, SoundInstance.Attenuation.NONE)));
        assertFalse(SoundSpatialization.isPositionalFlanSound(sound("another_mod:engine", false, SoundInstance.Attenuation.LINEAR)));
    }

    @Test
    void stereoEngineFadesToSilenceInTheActualAudioBackend()
    {
        // Loopback renders into memory: no audio device or audible playback is needed.
        long device = SOFTLoopback.alcLoopbackOpenDeviceSOFT((ByteBuffer) null);
        assertNotEquals(0L, device);
        long context = ALC10.alcCreateContext(device,
            new int[]{ALC10.ALC_FREQUENCY, 48000, SOFTLoopback.ALC_FORMAT_CHANNELS_SOFT, SOFTLoopback.ALC_STEREO_SOFT, SOFTLoopback.ALC_FORMAT_TYPE_SOFT, SOFTLoopback.ALC_FLOAT_SOFT, 0});
        try
        {
            assertTrue(ALC10.alcMakeContextCurrent(context));
            assertTrue(AL.createCapabilities(ALC.createCapabilities(device)).AL_SOFT_source_spatialize);
            int source = AL10.alGenSources();
            int buffer = AL10.alGenBuffers();
            try
            {
                short[] stereo = new short[48000 * 2];
                for (int i = 0; i < stereo.length / 2; i++)
                {
                    short sample = (short) (10000 * Math.sin(2D * Math.PI * 440D * i / 48000D));
                    stereo[2 * i] = sample;
                    stereo[2 * i + 1] = sample;
                }
                AL10.alBufferData(buffer, AL10.AL_FORMAT_STEREO16, stereo, 48000);
                AL10.alSourcei(source, AL10.AL_BUFFER, buffer);
                AL10.alSourcei(source, AL10.AL_LOOPING, AL10.AL_TRUE);
                AL10.alDistanceModel(AL11.AL_LINEAR_DISTANCE_CLAMPED);
                AL10.alSourcef(source, AL10.AL_REFERENCE_DISTANCE, 0F);
                AL10.alSourcef(source, AL10.AL_MAX_DISTANCE, 50F);
                AL10.alSourcePlay(source);

                double unfadedNear = loudness(device, source, 1F);
                double unfadedFar = loudness(device, source, 49F);
                assertEquals(1D, unfadedFar / unfadedNear, 0.02D, "AUTO reproduces the stereo bug: no distance fade");

                Channel channel = mock(Channel.class, withSettings().extraInterfaces(ChannelAccessor.class));
                when(((ChannelAccessor) channel).flansmodultimateSource()).thenReturn(source);
                SoundSpatialization.apply(sound("another_mod:engine", false, SoundInstance.Attenuation.LINEAR), channel);
                SoundSpatialization.apply(sound("flansmod:menu", true, SoundInstance.Attenuation.NONE), channel);
                assertEquals(SOFTSourceSpatialize.AL_AUTO_SOFT, AL10.alGetSourcei(source, SOFTSourceSpatialize.AL_SOURCE_SPATIALIZE_SOFT));
                SoundSpatialization.apply(sound("flansmod:fighterengine", false, SoundInstance.Attenuation.LINEAR), channel);

                double near = loudness(device, source, 1F);
                assertTrue(near > 0.01D);
                assertEquals(25D / 49D, loudness(device, source, 25F) / near, 0.02D);
                assertEquals(1D / 49D, loudness(device, source, 49F) / near, 0.01D);
                assertEquals(0D, loudness(device, source, 50F), 0.0001D);
                assertEquals(AL10.AL_NO_ERROR, AL10.alGetError());
            }
            finally
            {
                AL10.alDeleteSources(source);
                AL10.alDeleteBuffers(buffer);
            }
        }
        finally
        {
            ALC10.alcMakeContextCurrent(0L);
            ALC10.alcDestroyContext(context);
            ALC10.alcCloseDevice(device);
        }
    }

    private static double loudness(long device, int source, float distance)
    {
        AL10.alSource3f(source, AL10.AL_POSITION, 0F, 0F, -distance);
        float[] samples = new float[9600 * 2];
        SOFTLoopback.alcRenderSamplesSOFT(device, samples, samples.length / 2);
        double energy = 0D;
        // Ignore the first half while OpenAL ramps to the new gain.
        for (int i = samples.length / 2; i < samples.length; i++)
            energy += samples[i] * samples[i];
        return Math.sqrt(energy / (samples.length / 2D));
    }

    private static SoundInstance sound(String id, boolean relative, SoundInstance.Attenuation attenuation)
    {
        SoundInstance sound = mock(SoundInstance.class);
        when(sound.getLocation()).thenReturn(ResourceLocation.parse(id));
        when(sound.isRelative()).thenReturn(relative);
        when(sound.getAttenuation()).thenReturn(attenuation);
        return sound;
    }
}
