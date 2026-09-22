package com.flansmodultimate.mixin;

import com.mojang.blaze3d.vertex.BufferBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.renderer.MultiBufferSource;

import java.util.Set;

/** Read-only use: detect buffered work queued by custom renderers outside our consumer. */
@Mixin(MultiBufferSource.BufferSource.class)
public interface BufferSourceAccessor
{
    @Accessor("startedBuffers")
    Set<BufferBuilder> flansmodultimate$startedBuffers();
}
