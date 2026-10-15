package com.flansmodultimate.platform.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.jetbrains.annotations.NotNull;

/**
 * Version boundary for test vertex sinks: hands every vertex to {@link #accept} as one call, whatever shape the version's
 * {@link VertexConsumer} has. Minecraft 1.21 emits each vertex as an {@code addVertex} chain that the normal completes,
 * and has no packed fast path for a strict sink to protect.
 */
public abstract class TestVertexSink implements VertexConsumer
{
    private final float[] pending = new float[9];
    private int overlay;
    private int light;

    protected TestVertexSink()
    {
        this(true);
    }

    /**
     * @param strict
     *            whether per-element vertex calls fail the test; 1.21 has no other calls, so this only keeps the shared signature
     */
    // Part of the cross-version test signature; the Forge 1.20.1 sink uses the parameter.
    @SuppressWarnings("java:S1172")
    protected TestVertexSink(boolean strict)
    {}

    /** One vertex: position, colour channels from 0 to 1, texture coordinates, packed overlay and light, and normal. */
    // One argument per vertex attribute, as VertexConsumer#vertex.
    @SuppressWarnings("java:S107")
    protected abstract void accept(float x, float y, float z, float red, float green, float blue, float alpha, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ);

    @Override
    public @NotNull VertexConsumer addVertex(float x, float y, float z)
    {
        pending[0] = x;
        pending[1] = y;
        pending[2] = z;
        return this;
    }

    @Override
    public @NotNull VertexConsumer setColor(int red, int green, int blue, int alpha)
    {
        pending[3] = red / 255F;
        pending[4] = green / 255F;
        pending[5] = blue / 255F;
        pending[6] = alpha / 255F;
        return this;
    }

    @Override
    public @NotNull VertexConsumer setUv(float u, float v)
    {
        pending[7] = u;
        pending[8] = v;
        return this;
    }

    @Override
    public @NotNull VertexConsumer setUv1(int u, int v)
    {
        overlay = u | v << 16;
        return this;
    }

    @Override
    public @NotNull VertexConsumer setUv2(int u, int v)
    {
        light = u | v << 16;
        return this;
    }

    @Override
    public @NotNull VertexConsumer setNormal(float x, float y, float z)
    {
        accept(pending[0], pending[1], pending[2], pending[3], pending[4], pending[5], pending[6], pending[7], pending[8], overlay, light, x, y, z);
        return this;
    }
}
