package com.wolffsmod.npcs.client;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.AABB;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlanPreviewFramingTest
{
    @Test
    void shipsWideWingsAndTallMechasFitAtEveryRotation()
    {
        for (AABB box : List.of(new AABB(-2, -1, -30, 2, 5, 40),
            new AABB(-22, 0, -5, 22, 3, 8), new AABB(-3, 0, -2, 3, 24, 2)))
        {
            for (int angle = 0; angle < 360; angle += 15)
            {
                Quaternionf rotation = new Quaternionf().rotationX((float)Math.toRadians(20))
                    .rotateY((float)Math.toRadians(angle));
                float scale = FlanPreviewFraming.pixelsPerBlock(box, rotation, 260, 162);
                assertTrue(scale > 0 && scale <= 30);
                var center = box.getCenter();
                for (int i = 0; i < 8; i++)
                {
                    Vector3f corner = new Vector3f((float)(((i & 1) == 0 ? box.minX : box.maxX) - center.x),
                        (float)(((i & 2) == 0 ? box.minY : box.maxY) - center.y),
                        (float)(((i & 4) == 0 ? box.minZ : box.maxZ) - center.z));
                    rotation.transform(corner);
                    assertTrue(Math.abs(corner.x * scale) <= 104.001F, "Horizontal margin at " + angle);
                    assertTrue(Math.abs(corner.y * scale) <= 64.801F, "Vertical margin at " + angle);
                }
            }
        }
    }

    @Test
    void offCenterGeometryAndDegenerateBoundsRemainStable()
    {
        AABB box = new AABB(-1, 0, -2, 1, 3, 2);
        var rotation = new Quaternionf().rotationY(1);
        assertEquals(FlanPreviewFraming.pixelsPerBlock(box, rotation, 260, 162),
            FlanPreviewFraming.pixelsPerBlock(box.move(40, -12, 70), rotation, 260, 162));
        assertEquals(30, FlanPreviewFraming.pixelsPerBlock(new AABB(0, 0, 0, 0, 0, 0), rotation, 260, 162));
    }
}
