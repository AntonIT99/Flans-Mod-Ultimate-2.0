package com.flansmodultimate.platform.render;

import com.mojang.blaze3d.vertex.PoseStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldModelBoundsCollectorTest
{
    @Test
    void radiusIncludesTheRenderedTransformAndOffCenterGeometry()
    {
        var bounds = new WorldModelBoundsCollector();
        var pose = new PoseStack();
        pose.translate(3, 4, 0);
        pose.scale(2, 2, 2);
        emit(bounds, pose, 0, 0, 0);
        assertEquals(5F, bounds.radius(), 1E-5F);
        emit(bounds, pose, 1, 0, 0);
        assertEquals((float)Math.sqrt(41), bounds.radius(), 1E-5F);
        emit(bounds, pose, -1, 0, 0);
        assertEquals((float)Math.sqrt(41), bounds.radius(), 1E-5F);
        var box = bounds.bounds().orElseThrow();
        assertEquals(1, box.minX, 1E-5);
        assertEquals(5, box.maxX, 1E-5);
        assertEquals(4, box.minY, 1E-5);
    }

    @Test
    void invalidVerticesKeepDetailRatherThanProducingSmallBounds()
    {
        var bounds = new WorldModelBoundsCollector();
        emit(bounds, new PoseStack(), Float.NaN, 0, 0);
        assertEquals(Float.POSITIVE_INFINITY, bounds.radius());
        assertTrue(bounds.bounds().isEmpty());
        assertTrue(new WorldModelBoundsCollector().bounds().isEmpty());
    }

    private static void emit(WorldModelBoundsCollector bounds, PoseStack pose, float x, float y, float z)
    {
        VertexPlatform.vertex(bounds, pose.last(), x, y, z, 1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 0);
    }
}
