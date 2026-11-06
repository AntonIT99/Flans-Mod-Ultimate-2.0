package com.flansmodultimate.platform.render;

import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * A {@link VertexConsumer} that only reads vertex positions. Subclasses implement {@link #vertexPosition}, which receives each
 * vertex once in emission order, so they stay independent of the version-specific vertex element methods.
 */
public abstract class PositionCollector implements VertexConsumer
{
    /** Called once per emitted vertex, in emission order. */
    public abstract void vertexPosition(float x, float y, float z);

    @Override
    public final VertexConsumer vertex(double x, double y, double z)
    {
        vertexPosition((float) x, (float) y, (float) z);
        return this;
    }

    @Override
    public final VertexConsumer color(int red, int green, int blue, int alpha)
    {
        return this;
    }

    @Override
    public final VertexConsumer uv(float u, float v)
    {
        return this;
    }

    @Override
    public final VertexConsumer overlayCoords(int u, int v)
    {
        return this;
    }

    @Override
    public final VertexConsumer uv2(int u, int v)
    {
        return this;
    }

    @Override
    public final VertexConsumer normal(float x, float y, float z)
    {
        return this;
    }

    @Override
    public final void endVertex()
    {
        // Only positions are collected.
    }

    @Override
    public final void defaultColor(int red, int green, int blue, int alpha)
    {
        // Only positions are collected.
    }

    @Override
    public final void unsetDefaultColor()
    {
        // Only positions are collected.
    }
}
