package com.flansmodultimate.client.render.item;

import com.flansmodultimate.client.ModClient;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Adapts the legacy immediate item renderers to Minecraft 26.1's deferred
 * extraction/submission renderer. The discovery pass records the render types
 * requested by a renderer; each deferred callback then replays only its pass.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LegacyItemRenderBridge
{
    private static final VertexConsumer DISCARDING_CONSUMER = new VertexConsumer()
    {
        @Override public VertexConsumer addVertex(float x, float y, float z) { return this; }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) { return this; }
        @Override public VertexConsumer setColor(int color) { return this; }
        @Override public VertexConsumer setUv(float u, float v) { return this; }
        @Override public VertexConsumer setUv1(int u, int v) { return this; }
        @Override public VertexConsumer setUv2(int u, int v) { return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { return this; }
        @Override public VertexConsumer setLineWidth(float width) { return this; }
    };

    public static void submit(ItemStack stack, ItemDisplayContext context, @Nullable LivingEntity owner,
                              PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay)
    {
        ICustomItemRenderer renderer = CustomItemRenderers.get(stack.getItem());
        if (renderer == null)
            return;

        Set<RenderType> renderTypes = new LinkedHashSet<>();
        MultiBufferSource discoveryBuffers = renderType -> {
            renderTypes.add(renderType);
            return DISCARDING_CONSUMER;
        };
        withOwner(owner, () -> renderer.renderItem(stack, context, new PoseStack(), discoveryBuffers, light, overlay));

        for (RenderType renderType : renderTypes)
        {
            collector.submitCustomGeometry(poseStack, renderType, (submittedPose, vertexConsumer) -> {
                PoseStack deferredPose = new PoseStack();
                deferredPose.last().set(submittedPose);
                MultiBufferSource passBuffers = requestedType -> requestedType.equals(renderType)
                    ? vertexConsumer : DISCARDING_CONSUMER;
                withOwner(owner, () -> renderer.renderItem(stack, context, deferredPose, passBuffers, light, overlay));
            });
        }
    }

    private static void withOwner(@Nullable LivingEntity owner, Runnable action)
    {
        LivingEntity previous = ModClient.entityRenderContext.get();
        if (owner == null)
            ModClient.entityRenderContext.remove();
        else
            ModClient.entityRenderContext.set(owner);
        try
        {
            action.run();
        }
        finally
        {
            if (previous == null)
                ModClient.entityRenderContext.remove();
            else
                ModClient.entityRenderContext.set(previous);
        }
    }
}
