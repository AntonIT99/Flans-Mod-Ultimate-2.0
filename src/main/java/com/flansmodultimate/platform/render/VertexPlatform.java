package com.flansmodultimate.platform.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
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
        consumer.addVertex(pose.pose(), x, y, z)
            .setColor(red, green, blue, alpha)
            .setUv(u, v)
            .setOverlay(packedOverlay)
            .setLight(packedLight)
            .setNormal(pose, normalX, normalY, normalZ);
    }

    /** Entity-format vertex whose position and normal are already transformed. */
    public static void vertex(VertexConsumer consumer, float x, float y, float z,
                              float red, float green, float blue, float alpha, float u, float v,
                              int packedOverlay, int packedLight, float normalX, float normalY, float normalZ)
    {
        consumer.addVertex(x, y, z)
            .setColor(red, green, blue, alpha)
            .setUv(u, v)
            .setOverlay(packedOverlay)
            .setLight(packedLight)
            .setNormal(normalX, normalY, normalZ);
    }

    /** Position-colour-normal vertex, as used by line render types. */
    public static void lineVertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z,
                                  float red, float green, float blue, float alpha,
                                  float normalX, float normalY, float normalZ)
    {
        consumer.addVertex(pose.pose(), x, y, z)
            .setColor(red, green, blue, alpha)
            .setNormal(pose, normalX, normalY, normalZ);
    }

    /** Position-texture vertex. */
    public static void positionTexVertex(VertexConsumer consumer, Matrix4f pose, float x, float y, float z, float u, float v)
    {
        consumer.addVertex(pose, x, y, z).setUv(u, v);
    }

    /** Starts an immediate-mode quad batch on the shared tesselator. */
    public static BufferBuilder beginQuads(VertexFormat format)
    {
        return Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, format);
    }

    /** Draws a batch started with {@link #beginQuads} using the current shader. */
    public static void drawWithShader(BufferBuilder builder)
    {
        BufferUploader.drawWithShader(builder.buildOrThrow());
    }

    public static void renderModelPart(ModelPart part, PoseStack poseStack, VertexConsumer consumer, int packedLight,
                                       int packedOverlay, float red, float green, float blue, float alpha)
    {
        int color = Math.round(alpha * 255F) << 24 | Math.round(red * 255F) << 16
            | Math.round(green * 255F) << 8 | Math.round(blue * 255F);
        part.render(poseStack, consumer, packedLight, packedOverlay, color);
    }
}
