package com.flansmod.client.tmt;

import com.flansmodultimate.platform.render.TestVertexSink;

import java.util.ArrayList;
import java.util.List;

/** Records every submitted vertex without creating an OpenGL context. */
class RecordingVertexConsumer extends TestVertexSink
{
    final List<float[]> vertices = new ArrayList<>();

    @Override
    protected void accept(float x, float y, float z, float red, float green, float blue, float alpha, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ)
    {
        vertices.add(new float[]{x, y, z, red, green, blue, alpha, u, v, overlay, light, normalX, normalY, normalZ});
    }
}
