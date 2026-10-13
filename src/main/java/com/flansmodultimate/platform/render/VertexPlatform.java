package com.flansmodultimate.platform.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import org.joml.Matrix4f;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;

/** Version boundary for emitting vertices, immediate buffers, and the model-view matrix. Client-only. */
public final class VertexPlatform
{
    private VertexPlatform()
    {}

    /** Entity-format vertex; position and normal are transformed by {@code pose}. */
    // One argument per vertex attribute, as VertexConsumer#vertex.
    @SuppressWarnings("java:S107")
    public static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float red, float green, float blue, float alpha, float u, float v, int packedOverlay,
        int packedLight, float normalX, float normalY, float normalZ)
    {
        consumer.vertex(pose.pose(), x, y, z).color(red, green, blue, alpha).uv(u, v).overlayCoords(packedOverlay).uv2(packedLight).normal(pose.normal(), normalX, normalY, normalZ).endVertex();
    }

    /** Entity-format vertex whose position and normal are already transformed. */
    // One argument per vertex attribute, as VertexConsumer#vertex.
    @SuppressWarnings("java:S107")
    public static void vertex(VertexConsumer consumer, float x, float y, float z, float red, float green, float blue, float alpha, float u, float v, int packedOverlay, int packedLight, float normalX,
        float normalY, float normalZ)
    {
        consumer.vertex(x, y, z, red, green, blue, alpha, u, v, packedOverlay, packedLight, normalX, normalY, normalZ);
    }

    /** Position-colour-normal vertex, as used by line render types. */
    // One argument per vertex attribute, as VertexConsumer#vertex.
    @SuppressWarnings("java:S107")
    public static void lineVertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float red, float green, float blue, float alpha, float normalX, float normalY, float normalZ)
    {
        consumer.vertex(pose.pose(), x, y, z).color(red, green, blue, alpha).normal(pose.normal(), normalX, normalY, normalZ).endVertex();
    }

    // Mirrors the vanilla render call shape: pose, buffer, light, overlay and tint.
    @SuppressWarnings("java:S107")
    public static void renderModelPart(ModelPart part, PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha)
    {
        part.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    /** Pushes the global model-view matrix; pair with {@link #popModelView}. Call {@link RenderSystem#applyModelViewMatrix} after changing it. */
    public static void pushModelView()
    {
        RenderSystem.getModelViewStack().pushPose();
    }

    public static void popModelView()
    {
        RenderSystem.getModelViewStack().popPose();
    }

    public static void resetModelView()
    {
        RenderSystem.getModelViewStack().setIdentity();
    }

    /** The live top of the global model-view stack. Call {@link RenderSystem#applyModelViewMatrix} after changing it. */
    public static Matrix4f modelView()
    {
        return RenderSystem.getModelViewStack().last().pose();
    }

    /** Multiplies the pose stack by the matrix. */
    public static void mulPose(PoseStack poseStack, Matrix4f matrix)
    {
        poseStack.mulPoseMatrix(matrix);
    }

    /** An immediate buffer source backed by one growable buffer of the given initial capacity. */
    public static MultiBufferSource.BufferSource immediateBuffers(int capacity)
    {
        return MultiBufferSource.immediate(new BufferBuilder(capacity));
    }

    /** Emits one model cube with a float colour. */
    // Mirrors the vanilla render call shape: pose, buffer, light, overlay and tint.
    @SuppressWarnings("java:S107")
    public static void compileCube(ModelPart.Cube cube, PoseStack.Pose pose, VertexConsumer consumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha)
    {
        cube.compile(pose, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
