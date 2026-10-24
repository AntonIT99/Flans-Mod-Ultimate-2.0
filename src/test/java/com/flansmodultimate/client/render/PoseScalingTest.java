package com.flansmodultimate.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PoseScalingTest
{
    @Test
    void reflectionsKeepUnitNormalsFacingTheMirroredWay()
    {
        PoseStack pose = new PoseStack();
        PoseScaling.scale(pose, -1F, 1F, 1F);
        Vector3f mirrored = pose.last().normal().transform(new Vector3f(1F, 0F, 0F));
        assertEquals(-1F, mirrored.x, 1.0E-6F);
        assertEquals(1F, mirrored.length(), 1.0E-6F);

        PoseScaling.scale(pose, 0.625F, -0.625F, 0.625F);
        Vector3f up = pose.last().normal().transform(new Vector3f(0F, 1F, 0F));
        assertEquals(-1F, up.y, 1.0E-6F);
        assertEquals(1F, up.length(), 1.0E-6F);
    }

    @Test
    void positiveScalesMatchVanilla()
    {
        PoseStack vanilla = new PoseStack();
        vanilla.scale(2F, 0.5F, 1F);
        PoseStack safe = new PoseStack();
        PoseScaling.scale(safe, 2F, 0.5F, 1F);
        assertEquals(vanilla.last().pose(), safe.last().pose());
        Vector3f expected = vanilla.last().normal().transform(new Vector3f(1F, 1F, 1F));
        Vector3f actual = safe.last().normal().transform(new Vector3f(1F, 1F, 1F));
        assertEquals(expected.x, actual.x, 1.0E-4F);
        assertEquals(expected.y, actual.y, 1.0E-4F);
        assertEquals(expected.z, actual.z, 1.0E-4F);
    }
}
