package com.flansmodultimate.platform.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;

import net.minecraft.client.model.geom.ModelPart;

/** Version boundary for emitting vertices and immediate-mode quads. Client-only. */
public final class VertexPlatform
{
    private VertexPlatform() {}

    /** Entity-format vertex; position and normal are transformed by {@code pose}. */
    public static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z,
                              float red, float green, float blue, float alpha, float u, float v,
                              int packedOverlay, int packedLight, float normalX, float normalY, float normalZ)
    {
        consumer.vertex(pose.pose(), x, y, z)
            .color(red, green, blue, alpha)
            .uv(u, v)
            .overlayCoords(packedOverlay)
            .uv2(packedLight)
            .normal(pose.normal(), normalX, normalY, normalZ)
            .endVertex();
    }

    /** Entity-format vertex whose position and normal are already transformed. */
    public static void vertex(VertexConsumer consumer, float x, float y, float z,
                              float red, float green, float blue, float alpha, float u, float v,
                              int packedOverlay, int packedLight, float normalX, float normalY, float normalZ)
    {
        consumer.vertex(x, y, z, red, green, blue, alpha, u, v, packedOverlay, packedLight, normalX, normalY, normalZ);
    }

    /** Position-colour-normal vertex, as used by line render types. */
    public static void lineVertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z,
                                  float red, float green, float blue, float alpha,
                                  float normalX, float normalY, float normalZ)
    {
        consumer.vertex(pose.pose(), x, y, z)
            .color(red, green, blue, alpha)
            .normal(pose.normal(), normalX, normalY, normalZ)
            .endVertex();
    }

    /** Position-texture vertex. */
    public static void positionTexVertex(VertexConsumer consumer, Matrix4f pose, float x, float y, float z, float u, float v)
    {
        consumer.vertex(pose, x, y, z).uv(u, v).endVertex();
    }

    /** Starts an immediate-mode quad batch on the shared tesselator. */
    public static BufferBuilder beginQuads(VertexFormat format)
    {
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, format);
        return builder;
    }

    /** Draws a batch started with {@link #beginQuads} using the current shader. */
    public static void drawWithShader(BufferBuilder builder)
    {
        Tesselator.getInstance().end();
    }

    public static void renderModelPart(ModelPart part, PoseStack poseStack, VertexConsumer consumer, int packedLight,
                                       int packedOverlay, float red, float green, float blue, float alpha)
    {
        part.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
