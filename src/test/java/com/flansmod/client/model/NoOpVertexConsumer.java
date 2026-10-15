package com.flansmod.client.model;

import com.flansmodultimate.platform.render.TestVertexSink;

/** Vertex sink for rendering tests that only inspect pose transforms. */
final class NoOpVertexConsumer extends TestVertexSink
{
    static final NoOpVertexConsumer INSTANCE = new NoOpVertexConsumer();

    private NoOpVertexConsumer()
    {
        super(false);
    }

    @Override
    protected void accept(float x, float y, float z, float red, float green, float blue, float alpha, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ)
    {
        // Only the pose transforms are inspected.
    }
}
