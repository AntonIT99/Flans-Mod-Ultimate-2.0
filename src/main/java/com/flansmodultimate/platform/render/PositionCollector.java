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
    public final VertexConsumer addVertex(float x, float y, float z)
    {
        vertexPosition(x, y, z);
        return this;
    }

    @Override
    public final VertexConsumer setColor(int red, int green, int blue, int alpha)
    {
        return this;
    }

    @Override
    public final VertexConsumer setUv(float u, float v)
    {
        return this;
    }

    @Override
    public final VertexConsumer setUv1(int u, int v)
    {
        return this;
    }

    @Override
    public final VertexConsumer setUv2(int u, int v)
    {
        return this;
    }

    @Override
    public final VertexConsumer setNormal(float x, float y, float z)
    {
        return this;
    }
}
