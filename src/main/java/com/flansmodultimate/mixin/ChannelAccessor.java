package com.flansmodultimate.mixin;

import com.mojang.blaze3d.audio.Channel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes the audio source so Flan's positional stereo sounds can use OpenAL distance attenuation. */
@Mixin(Channel.class)
public interface ChannelAccessor
{
    @Accessor("source")
    int flansmodultimateSource();
}
