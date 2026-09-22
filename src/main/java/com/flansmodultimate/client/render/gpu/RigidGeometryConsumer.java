package com.flansmodultimate.client.render.gpu;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/** Optional renderer-owned sink; normal vertex consumers retain the legacy path. */
public interface RigidGeometryConsumer extends VertexConsumer
{
    void submit(RigidGeometry geometry, PoseStack.Pose pose, int light, int overlay,
                float red, float green, float blue, float alpha);
}
