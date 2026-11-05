package com.flansmodultimate.client.damage;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Vector3f;

import java.util.LinkedHashSet;
import java.util.Set;

/** Collects the actual drawn vertices once at detachment, including model and animation transforms. */
final class PieceGeometry implements VertexConsumer
{
    private final Set<Vector3f> points = new LinkedHashSet<>();
    private final Vector3f vertex = new Vector3f();
    private final Vector3f faceCentre = new Vector3f();
    private int faceVertices;

    Vector3f[] relativeTo(Vector3f pivot)
    {
        return points.stream().map(point -> new Vector3f(point).sub(pivot)).toArray(Vector3f[]::new);
    }

    @Override
    public VertexConsumer vertex(double x, double y, double z)
    {
        vertex.set((float) x, (float) y, (float) z);
        if (Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z))
            points.add(new Vector3f(vertex));
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
    {
        // Entity models emit quads (including triangles padded to four vertices). Sample their interiors too:
        // a long track can overhang both edges of a narrow block while its face still rests on that block.
        faceCentre.add(vertex);
        if (++faceVertices == 4)
        {
            if (faceCentre.isFinite())
                points.add(new Vector3f(faceCentre).mul(0.25F));
            faceCentre.zero();
            faceVertices = 0;
        }
    }

    @Override
    public void defaultColor(int red, int green, int blue, int alpha)
    {}

    @Override
    public void unsetDefaultColor()
    {}
}
