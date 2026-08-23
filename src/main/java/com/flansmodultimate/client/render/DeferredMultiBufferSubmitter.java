package com.flansmodultimate.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Compatibility bridge for legacy render helpers which select several render
 * types through {@link MultiBufferSource}. The extraction renderer needs those
 * types up front, so a no-output pass discovers them before one deferred node
 * is submitted for each type.
 */
public final class DeferredMultiBufferSubmitter
{
    private DeferredMultiBufferSubmitter() { }

    public static void submit(PoseStack poseStack, SubmitNodeCollector collector,
                              BiConsumer<PoseStack, MultiBufferSource> renderer)
    {
        Set<RenderType> renderTypes = new LinkedHashSet<>();
        renderer.accept(poseStack, renderType -> {
            renderTypes.add(renderType);
            return DiscardingVertexConsumer.INSTANCE;
        });

        for (RenderType renderType : renderTypes)
        {
            collector.submitCustomGeometry(poseStack, renderType, (submittedPose, vertices) -> {
                PoseStack deferred = new PoseStack();
                deferred.last().set(submittedPose);
                renderer.accept(deferred, requestedType -> renderType.equals(requestedType)
                    ? vertices : DiscardingVertexConsumer.INSTANCE);
            });
        }
    }

    private enum DiscardingVertexConsumer implements VertexConsumer
    {
        INSTANCE;

        @Override public VertexConsumer addVertex(float x, float y, float z) { return this; }
        @Override public VertexConsumer setColor(int red, int green, int blue, int alpha) { return this; }
        @Override public VertexConsumer setColor(int color) { return this; }
        @Override public VertexConsumer setUv(float u, float v) { return this; }
        @Override public VertexConsumer setUv1(int u, int v) { return this; }
        @Override public VertexConsumer setUv2(int u, int v) { return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { return this; }
        @Override public VertexConsumer setLineWidth(float width) { return this; }
    }
}
