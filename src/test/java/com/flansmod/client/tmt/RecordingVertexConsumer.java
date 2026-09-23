package com.flansmod.client.tmt;

import com.mojang.blaze3d.vertex.VertexConsumer;

import java.util.ArrayList;
import java.util.List;

/** Records the 1.21 chained vertex submission path without an OpenGL context. */
class RecordingVertexConsumer implements VertexConsumer
{
    final List<float[]> vertices = new ArrayList<>();
    private final float[] pending = new float[14];

    @Override public VertexConsumer addVertex(float x, float y, float z)
    {
        pending[0] = x; pending[1] = y; pending[2] = z;
        return this;
    }

    @Override public VertexConsumer setColor(int r, int g, int b, int a)
    {
        pending[3] = r / 255F; pending[4] = g / 255F;
        pending[5] = b / 255F; pending[6] = a / 255F;
        return this;
    }

    @Override public VertexConsumer setUv(float u, float v)
    {
        pending[7] = u; pending[8] = v;
        return this;
    }

    @Override public VertexConsumer setUv1(int u, int v)
    {
        pending[9] = u | v << 16;
        return this;
    }

    @Override public VertexConsumer setUv2(int u, int v)
    {
        pending[10] = u | v << 16;
        return this;
    }

    @Override public VertexConsumer setNormal(float x, float y, float z)
    {
        pending[11] = x; pending[12] = y; pending[13] = z;
        vertices.add(pending.clone());
        return this;
    }
}
