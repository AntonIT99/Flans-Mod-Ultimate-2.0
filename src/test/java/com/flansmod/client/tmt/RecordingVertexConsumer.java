package com.flansmod.client.tmt;

import com.mojang.blaze3d.vertex.VertexConsumer;

import java.util.ArrayList;
import java.util.List;

/** Records the packed submission path without creating an OpenGL context. */
class RecordingVertexConsumer implements VertexConsumer
{
    final List<float[]> vertices = new ArrayList<>();

    @Override
    public void vertex(float x, float y, float z, float red, float green, float blue, float alpha,
                       float u, float v, int overlay, int light, float nx, float ny, float nz)
    {
        vertices.add(new float[]{x, y, z, red, green, blue, alpha, u, v, overlay, light, nx, ny, nz});
    }

    @Override public VertexConsumer vertex(double x, double y, double z) { throw new AssertionError(); }
    @Override public VertexConsumer color(int r, int g, int b, int a) { throw new AssertionError(); }
    @Override public VertexConsumer uv(float u, float v) { throw new AssertionError(); }
    @Override public VertexConsumer overlayCoords(int u, int v) { throw new AssertionError(); }
    @Override public VertexConsumer uv2(int u, int v) { throw new AssertionError(); }
    @Override public VertexConsumer normal(float x, float y, float z) { throw new AssertionError(); }
    @Override public void endVertex() { throw new AssertionError(); }
    @Override public void defaultColor(int r, int g, int b, int a) { throw new AssertionError(); }
    @Override public void unsetDefaultColor() { throw new AssertionError(); }
}
