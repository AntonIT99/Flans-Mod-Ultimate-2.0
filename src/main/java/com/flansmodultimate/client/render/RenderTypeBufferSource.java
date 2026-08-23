package com.flansmodultimate.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.rendertype.RenderType;

/**
 * Minimal replacement for the removed {@code MultiBufferSource}. Legacy Flan
 * renderers only need to select a vertex consumer by render type; submission
 * and batching remain owned by Minecraft's 26.2 feature renderer.
 */
@FunctionalInterface
public interface RenderTypeBufferSource
{
    VertexConsumer getBuffer(RenderType renderType);
}
