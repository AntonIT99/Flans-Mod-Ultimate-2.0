package com.flansmod.client.model;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.jetbrains.annotations.NotNull;

/** Vertex sink for rendering tests that only inspect pose transforms. */
final class NoOpVertexConsumer implements VertexConsumer
{
    static final NoOpVertexConsumer INSTANCE = new NoOpVertexConsumer();

    private NoOpVertexConsumer() {}

    @Override
    public @NotNull VertexConsumer addVertex(float x, float y, float z)
    {
        return this;
    }

    @Override
    public @NotNull VertexConsumer setColor(int red, int green, int blue, int alpha)
    {
        return this;
    }

    @Override
    public @NotNull VertexConsumer setUv(float u, float v)
    {
        return this;
    }

    @Override
    public @NotNull VertexConsumer setUv1(int u, int v)
    {
        return this;
    }

    @Override
    public @NotNull VertexConsumer setUv2(int u, int v)
    {
        return this;
    }

    @Override
    public @NotNull VertexConsumer setNormal(float x, float y, float z)
    {
        return this;
    }
}
