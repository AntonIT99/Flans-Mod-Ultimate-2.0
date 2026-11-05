package com.flansmodultimate.client.damage;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Vector3f;

import java.util.LinkedHashSet;
import java.util.Set;

/** Collects the actual drawn vertices once at detachment, including model and animation transforms. */
final class PieceGeometry implements VertexConsumer
{
    private final Set<Vector3f> points = new LinkedHashSet<>();

    Vector3f[] relativeTo(Vector3f pivot)
    {
        return points.stream().map(point -> new Vector3f(point).sub(pivot)).toArray(Vector3f[]::new);
    }

    @Override
    public VertexConsumer vertex(double x, double y, double z)
    {
        if (Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z))
            points.add(new Vector3f((float) x, (float) y, (float) z));
        return this;
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha)
    {
        return this;
    }

    @Override
    public VertexConsumer uv(float u, float v)
    {
        return this;
    }

    @Override
    public VertexConsumer overlayCoords(int u, int v)
    {
        return this;
    }

    @Override
    public VertexConsumer uv2(int u, int v)
    {
        return this;
    }

    @Override
    public VertexConsumer normal(float x, float y, float z)
    {
        return this;
    }

    @Override
    public void endVertex()
    {}

    @Override
    public void defaultColor(int red, int green, int blue, int alpha)
    {}

    @Override
    public void unsetDefaultColor()
    {}
}
