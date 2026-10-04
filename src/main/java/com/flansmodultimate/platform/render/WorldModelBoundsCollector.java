package com.flansmodultimate.platform.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.AABB;
import java.util.Optional;

/** Version-specific vertex collector for a conservative radius about a static model's origin. */
public final class WorldModelBoundsCollector implements VertexConsumer
{
    private double radiusSquared;
    private boolean valid = true;
    private double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
    private double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;

    /** Empty if no vertices were emitted or any vertex was invalid. */
    public Optional<AABB> bounds()
    {
        return valid && Double.isFinite(minX) ? Optional.of(new AABB(minX, minY, minZ, maxX, maxY, maxZ)) : Optional.empty();
    }

    public float radius()
    {
        return valid ? (float)Math.sqrt(radiusSquared) : Float.POSITIVE_INFINITY;
    }

    @Override public VertexConsumer addVertex(float x, float y, float z)
    {
        double squared = (double)x*x + (double)y*y + (double)z*z;
        valid &= Double.isFinite(squared);
        radiusSquared = Math.max(radiusSquared, squared);
        minX = Math.min(minX, x);
        minY = Math.min(minY, y);
        minZ = Math.min(minZ, z);
        maxX = Math.max(maxX, x);
        maxY = Math.max(maxY, y);
        maxZ = Math.max(maxZ, z);
        return this;
    }
    @Override public VertexConsumer setColor(int r, int g, int b, int a) { return this; }
    @Override public VertexConsumer setUv(float u, float v) { return this; }
    @Override public VertexConsumer setUv1(int u, int v) { return this; }
    @Override public VertexConsumer setUv2(int u, int v) { return this; }
    @Override public VertexConsumer setNormal(float x, float y, float z) { return this; }
}
