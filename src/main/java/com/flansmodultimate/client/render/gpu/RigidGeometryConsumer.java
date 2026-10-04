package com.flansmodultimate.client.render.gpu;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/** Optional renderer-owned sink; normal vertex consumers retain the legacy path. */
public interface RigidGeometryConsumer extends VertexConsumer
{
    void submit(RigidGeometry geometry, PoseStack.Pose pose, int light, int overlay, float red, float green, float blue, float alpha);

    /** Keep size-culled geometry in the mesh layout without drawing it. Other sinks may discard it. */
    default void submit(RigidGeometry geometry, PoseStack.Pose pose, int light, int overlay,
                        float red, float green, float blue, float alpha, boolean visible)
    {
        if (visible) submit(geometry, pose, light, overlay, red, green, blue, alpha);
    }

    /**
     * A pose a part composes from its own transform for this submission, not an entry of the caller's stack.
     * Equal composed poses may share a palette entry with each other but never with a stack pose, so a turret
     * or barrel at exactly zero rotation keeps the mesh layout it has when rotated.
     */
    /**
     * Appends geometry to the previous palette entry when that entry came from exactly this stack pose
     * object and the caller knows nothing has changed the pose, light, overlay or tint since. Returns
     * false, appending nothing, when the caller must submit normally instead. The identity hash and
     * vertex count are the geometry's own, cached by the caller.
     */
    default boolean submitToLastPalette(RigidGeometry geometry, int identityHash, int vertexCount,
                                        PoseStack.Pose source, boolean visible)
    {
        return false;
    }

    /** {@link #submit(RigidGeometry, PoseStack.Pose, int, int, float, float, float, float, boolean)} with the
     *  geometry's identity hash and vertex count cached by the caller, which then need not touch the object. */
    default void submitCached(RigidGeometry geometry, int identityHash, int vertexCount, PoseStack.Pose pose, int light,
                              int overlay, float red, float green, float blue, float alpha, boolean visible)
    {
        submit(geometry, pose, light, overlay, red, green, blue, alpha, visible);
    }

    default void submitComposed(RigidGeometry geometry, PoseStack.Pose pose, int light, int overlay,
                                float red, float green, float blue, float alpha, boolean visible)
    {
        submit(geometry, pose, light, overlay, red, green, blue, alpha, visible);
    }
}
