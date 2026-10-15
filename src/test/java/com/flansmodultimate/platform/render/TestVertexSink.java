package com.flansmodultimate.platform.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.jetbrains.annotations.NotNull;

/**
 * Version boundary for test vertex sinks: hands every vertex to {@link #accept} as one call, whatever shape the version's
 * {@link VertexConsumer} has. Forge 1.20.1 emits Flan geometry through the packed {@code vertex} call; a strict sink
 * rejects the per-element calls, so a test notices when geometry leaves that fast path.
 */
public abstract class TestVertexSink implements VertexConsumer
{
    private final boolean strict;

    protected TestVertexSink()
    {
        this(true);
    }

    /**
     * @param strict
     *            whether per-element vertex calls fail the test instead of being ignored
     */
    protected TestVertexSink(boolean strict)
    {
        this.strict = strict;
    }

    /** One vertex: position, colour channels from 0 to 1, texture coordinates, packed overlay and light, and normal. */
    // One argument per vertex attribute, as VertexConsumer#vertex.
    @SuppressWarnings("java:S107")
    protected abstract void accept(float x, float y, float z, float red, float green, float blue, float alpha, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ);

    @Override
    public final void vertex(float x, float y, float z, float red, float green, float blue, float alpha, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ)
    {
        accept(x, y, z, red, green, blue, alpha, u, v, overlay, light, normalX, normalY, normalZ);
    }

    @Override
    public @NotNull VertexConsumer vertex(double x, double y, double z)
    {
        return element();
    }

    @Override
    public @NotNull VertexConsumer color(int red, int green, int blue, int alpha)
    {
        return element();
    }

    @Override
    public @NotNull VertexConsumer uv(float u, float v)
    {
        return element();
    }

    @Override
    public @NotNull VertexConsumer overlayCoords(int u, int v)
    {
        return element();
    }

    @Override
    public @NotNull VertexConsumer uv2(int u, int v)
    {
        return element();
    }

    @Override
    public @NotNull VertexConsumer normal(float x, float y, float z)
    {
        return element();
    }

    @Override
    public void endVertex()
    {
        element();
    }

    @Override
    public void defaultColor(int red, int green, int blue, int alpha)
    {
        element();
    }

    @Override
    public void unsetDefaultColor()
    {
        element();
    }

    private VertexConsumer element()
    {
        if (strict)
            throw new AssertionError("Expected the packed vertex call");
        return this;
    }
}
