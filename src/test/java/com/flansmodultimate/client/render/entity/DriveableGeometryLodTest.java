package com.flansmodultimate.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DriveableGeometryLodTest
{
    @Test
    void sizeBoundHandlesUniformScaleAndShearedLegacyTransforms()
    {
        PoseStack pose = new PoseStack();
        pose.mulPose(Axis.YP.rotationDegrees(38));
        pose.scale(-3, 3, 3);
        assertEquals(3, DriveableRenderer.modelScaleBound(pose), 1E-5F);
        pose.last().pose().identity().m10(2F).m21(0.7F);
        float bound = DriveableRenderer.modelScaleBound(pose);
        for (int x = -5; x <= 5; x++)
            for (int y = -5; y <= 5; y++)
                for (int z = -5; z <= 5; z++)
                {
                    if (x == 0 && y == 0 && z == 0)
                        continue;
                    Vector3f direction = new Vector3f(x, y, z).normalize();
                    pose.last().pose().transformDirection(direction);
                    assertTrue(direction.length() <= bound + 1E-5F);
                }
    }
}
