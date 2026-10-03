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

    @Override public VertexConsumer vertex(double x, double y, double z)
    {
        double squared = x*x + y*y + z*z;
        valid &= Double.isFinite(squared);
        radiusSquared = Math.max(radiusSquared, squared);
        return this;
    }
    @Override public VertexConsumer color(int r, int g, int b, int a) { return this; }
    @Override public VertexConsumer uv(float u, float v) { return this; }
    @Override public VertexConsumer overlayCoords(int u, int v) { return this; }
    @Override public VertexConsumer uv2(int u, int v) { return this; }
    @Override public VertexConsumer normal(float x, float y, float z) { return this; }
    @Override public void endVertex() {}
    @Override public void defaultColor(int r, int g, int b, int a) {}
    @Override public void unsetDefaultColor() {}
}
