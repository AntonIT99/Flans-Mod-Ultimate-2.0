package com.flansmodultimate.platform.render;

import com.mojang.blaze3d.vertex.VertexConsumer;

/** Version-specific vertex collector for a conservative radius about a static model's origin. */
public final class WorldModelBoundsCollector implements VertexConsumer
{
    private double radiusSquared;
    private boolean valid = true;

    public float radius()
    {
        return valid ? (float)Math.sqrt(radiusSquared) : Float.POSITIVE_INFINITY;
    }

    @Override public VertexConsumer addVertex(float x, float y, float z)
    {
        double squared = (double)x*x + (double)y*y + (double)z*z;
        valid &= Double.isFinite(squared);
        radiusSquared = Math.max(radiusSquared, squared);
        return this;
    }
    @Override public VertexConsumer setColor(int r, int g, int b, int a) { return this; }
    @Override public VertexConsumer setUv(float u, float v) { return this; }
    @Override public VertexConsumer setUv1(int u, int v) { return this; }
    @Override public VertexConsumer setUv2(int u, int v) { return this; }
    @Override public VertexConsumer setNormal(float x, float y, float z) { return this; }
}
