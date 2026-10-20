package com.flansmodultimate.apocalyse.client.render;

import com.flansmodultimate.platform.render.TestVertexSink;
import com.mojang.blaze3d.vertex.PoseStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NukeDropModelTest
{
    @Test
    void fallingBombKeepsItsLegacyDimensions()
    {
        Bounds vertices = new Bounds();
        new NukeDropModel().renderBomb(new PoseStack(), vertices, 0, 0);

        assertEquals(120, vertices.count);
        assertEquals(-6F / 16F, vertices.minY, 1.0E-5F);
        assertEquals(10F / 16F, vertices.maxY, 1.0E-5F);
        assertTrue(vertices.maxRadius < 0.7F);
    }

    @Test
    void blastIsASphericalShellWithTheRendererSuppliedOpacity()
    {
        Bounds vertices = new Bounds();
        new NukeDropModel().renderBlast(new PoseStack(), vertices, 0, 0, 0.25F);

        assertTrue(vertices.count > 1000);
        assertEquals(-1F, vertices.minY, 1.0E-5F);
        assertEquals(1F, vertices.maxY, 1.0E-5F);
        assertEquals(1F, vertices.maxRadius, 1.0E-5F);
        assertEquals(0.25F, vertices.alpha);
    }

    private static class Bounds extends TestVertexSink
    {
        private int count;
        private float minY = Float.POSITIVE_INFINITY;
        private float maxY = Float.NEGATIVE_INFINITY;
        private float maxRadius;
        private float alpha;

        @Override
        protected void accept(float x, float y, float z, float red, float green, float blue, float opacity, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ)
        {
            count++;
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
            maxRadius = Math.max(maxRadius, (float) Math.sqrt(x * x + y * y + z * z));
            alpha = opacity;
            assertTrue(Float.isFinite(x) && Float.isFinite(y) && Float.isFinite(z));
        }
    }
}
