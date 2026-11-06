package com.flansmodultimate.platform.render;

import net.minecraft.world.phys.AABB;

import java.util.Optional;

/** Vertex collector for a conservative radius about a static model's origin. */
public final class WorldModelBoundsCollector extends PositionCollector
{
    private double radiusSquared;
    private boolean valid = true;
    private double minX = Double.POSITIVE_INFINITY;
    private double minY = Double.POSITIVE_INFINITY;
    private double minZ = Double.POSITIVE_INFINITY;
    private double maxX = Double.NEGATIVE_INFINITY;
    private double maxY = Double.NEGATIVE_INFINITY;
    private double maxZ = Double.NEGATIVE_INFINITY;

    /** Empty if no vertices were emitted or any vertex was invalid. */
    public Optional<AABB> bounds()
    {
        return valid && Double.isFinite(minX) ? Optional.of(new AABB(minX, minY, minZ, maxX, maxY, maxZ)) : Optional.empty();
    }

    public float radius()
    {
        return valid ? (float) Math.sqrt(radiusSquared) : Float.POSITIVE_INFINITY;
    }

    @Override
    public void vertexPosition(float x, float y, float z)
    {
        double squared = (double) x * x + (double) y * y + (double) z * z;
        valid &= Double.isFinite(squared);
        radiusSquared = Math.max(radiusSquared, squared);
        minX = Math.min(minX, x);
        minY = Math.min(minY, y);
        minZ = Math.min(minZ, z);
        maxX = Math.max(maxX, x);
        maxY = Math.max(maxY, y);
        maxZ = Math.max(maxZ, z);
    }
}
