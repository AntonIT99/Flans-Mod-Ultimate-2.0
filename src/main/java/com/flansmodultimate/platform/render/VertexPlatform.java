package com.flansmodultimate.platform.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;

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
        consumer.addVertex(pose.pose(), x, y, z).setColor(red, green, blue, alpha).setUv(u, v).setOverlay(packedOverlay).setLight(packedLight).setNormal(pose, normalX, normalY, normalZ);
    }

    /** Entity-format vertex whose position and normal are already transformed. */
    // One argument per vertex attribute, as VertexConsumer#vertex.
    @SuppressWarnings("java:S107")
    public static void vertex(VertexConsumer consumer, float x, float y, float z, float red, float green, float blue, float alpha, float u, float v, int packedOverlay, int packedLight, float normalX,
        float normalY, float normalZ)
    {
        consumer.addVertex(x, y, z).setColor(red, green, blue, alpha).setUv(u, v).setOverlay(packedOverlay).setLight(packedLight).setNormal(normalX, normalY, normalZ);
    }

    /** Position-only vertex, as used by position render types. */
    public static void position(VertexConsumer consumer, float x, float y, float z)
    {
        consumer.addVertex(x, y, z);
    }

    /** Position-colour-normal vertex, as used by line render types. */
    // One argument per vertex attribute, as VertexConsumer#vertex.
    @SuppressWarnings("java:S107")
    public static void lineVertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float red, float green, float blue, float alpha, float normalX, float normalY, float normalZ)
    {
        consumer.addVertex(pose.pose(), x, y, z).setColor(red, green, blue, alpha).setNormal(pose, normalX, normalY, normalZ);
    }

    // Mirrors the vanilla render call shape: pose, buffer, light, overlay and tint.
    @SuppressWarnings("java:S107")
    public static void renderModelPart(ModelPart part, PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha)
    {
        int color = Math.round(alpha * 255F) << 24 | Math.round(red * 255F) << 16 | Math.round(green * 255F) << 8 | Math.round(blue * 255F);
        part.render(poseStack, consumer, packedLight, packedOverlay, color);
    }

    /** Pushes the global model-view matrix; pair with {@link #popModelView}. Call {@link RenderSystem#applyModelViewMatrix} after changing it. */
    public static void pushModelView()
    {
        RenderSystem.getModelViewStack().pushMatrix();
    }

    public static void popModelView()
    {
        RenderSystem.getModelViewStack().popMatrix();
    }

    public static void resetModelView()
    {
        RenderSystem.getModelViewStack().identity();
    }

    /** The live top of the global model-view stack, which is itself the matrix since 1.20.5. Call {@link RenderSystem#applyModelViewMatrix} after changing it. */
    public static Matrix4f modelView()
    {
        return RenderSystem.getModelViewStack();
    }

    /** Sets the shader's inverse view-rotation matrix; Minecraft 1.21 removed that uniform, so there is nothing to set. */
    @SuppressWarnings("java:S1172") // Part of the cross-version platform signature; 1.20.1 uses the shader.
    public static void setInverseViewRotation(ShaderInstance shader)
    {
        // Minecraft 1.21 shaders have no inverse view-rotation uniform.
    }

    /** Multiplies the pose stack by the matrix. */
    public static void mulPose(PoseStack poseStack, Matrix4f matrix)
    {
        poseStack.mulPose(matrix);
    }

    /** An immediate buffer source backed by one growable buffer of the given initial capacity. */
    public static MultiBufferSource.BufferSource immediateBuffers(int capacity)
    {
        return MultiBufferSource.immediate(new ByteBufferBuilder(capacity));
    }

    /** Emits one model cube with a float colour, packed to ARGB by truncation. */
    // Mirrors the vanilla render call shape: pose, buffer, light, overlay and tint.
    @SuppressWarnings("java:S107")
    public static void compileCube(ModelPart.Cube cube, PoseStack.Pose pose, VertexConsumer consumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha)
    {
        int packedColor = ((int) (alpha * 255F) & 0xFF) << 24 | ((int) (red * 255F) & 0xFF) << 16 | ((int) (green * 255F) & 0xFF) << 8 | ((int) (blue * 255F) & 0xFF);
        cube.compile(pose, consumer, packedLight, packedOverlay, packedColor);
    }
}
