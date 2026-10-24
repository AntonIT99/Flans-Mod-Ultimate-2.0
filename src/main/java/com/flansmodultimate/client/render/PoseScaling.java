package com.flansmodultimate.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PoseScaling
{
    /**
     * {@link PoseStack#scale} that also handles reflections. Vanilla corrects the normal matrix with
     * Mth.fastInvCubeRoot, which only works for positive input, so a scale with an odd number of negative factors
     * leaves normals scaled by about 1e25 and the model lit wrongly.
     */
    public static void scale(PoseStack poseStack, float x, float y, float z)
    {
        PoseStack.Pose pose = poseStack.last();
        pose.pose().scale(x, y, z);
        float magnitude = (float) Math.cbrt(Math.abs((double) x * y * z));
        pose.normal().scale(magnitude / x, magnitude / y, magnitude / z);
    }
}
