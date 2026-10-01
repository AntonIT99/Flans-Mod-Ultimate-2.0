package com.flansmodultimate.client.distant;

import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.driveables.LegacyDriveableCoordinates;
import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DistantProxyShapesTest
{
    private static final int COLOR = 0xFF336699;
    /** A fuselage 12 blocks long, 1.5 wide and 1.5 tall, centred on the origin. */
    private static final AABB FUSELAGE = new AABB(-6D, -0.75D, -0.75D, 6D, 0.75D, 0.75D);

    @Test
    void aCompactPartIsOneBoxAndKeepsItsBoundsWhenUnturned()
    {
        List<DistantBox> boxes = new ArrayList<>();
        AABB hull = new AABB(-1D, 0D, -1D, 1D, 1.5D, 1D);
        DistantProxyShapes.appendPart(boxes, EnumDriveablePart.CORE, hull, (part, local) -> local, COLOR);

        assertEquals(1, boxes.size());
        DistantBox box = boxes.get(0);
        assertEquals(-1F, box.minX(), 1.0E-6F);
        assertEquals(0F, box.minY(), 1.0E-6F);
        assertEquals(-1F, box.minZ(), 1.0E-6F);
        assertEquals(1F, box.maxX(), 1.0E-6F);
        assertEquals(1.5F, box.maxY(), 1.0E-6F);
        assertEquals(1F, box.maxZ(), 1.0E-6F);
        assertEquals(COLOR, box.argb());
    }

    @Test
    void aLongPartIsCutAlongItsLengthSoATurnedOneStaysSlim()
    {
        List<DistantBox> unturned = new ArrayList<>();
        DistantProxyShapes.appendPart(unturned, EnumDriveablePart.CORE, FUSELAGE, (part, local) -> local, COLOR);
        assertEquals(DistantProxyShapes.MAX_SEGMENTS, unturned.size());

        // Turned 45 degrees, one box around the whole fuselage would cover a 9 x 9 square
        List<DistantBox> turned = new ArrayList<>();
        DistantProxyShapes.appendPart(turned, EnumDriveablePart.CORE, FUSELAGE,
            (part, local) -> LegacyDriveableCoordinates.modelLocalToWorldDirection(local, 45F, 0F, 0F), COLOR);
        assertEquals(unturned.size(), turned.size());

        double covered = 0D;
        for (DistantBox box : turned)
            covered += (box.maxX() - box.minX()) * (box.maxZ() - box.minZ());
        double singleBox = 9.0D * 9.0D;
        assertTrue(covered < singleBox / 3D, "the pieces cover " + covered + " square blocks");
    }

    @Test
    void theBoxCountOfAPartDoesNotDependOnItsOrientation()
    {
        for (float yaw = 0F; yaw < 360F; yaw += 15F)
        {
            float turn = yaw;
            List<DistantBox> boxes = new ArrayList<>();
            DistantProxyShapes.appendPart(boxes, EnumDriveablePart.CORE, FUSELAGE,
                (part, local) -> LegacyDriveableCoordinates.modelLocalToWorldDirection(local, turn, 20F, 35F), COLOR);
            assertEquals(DistantProxyShapes.MAX_SEGMENTS, boxes.size());
        }
    }

    @Test
    void anEmptyPartAddsNothingAndAFullShapeStopsGrowing()
    {
        List<DistantBox> boxes = new ArrayList<>();
        DistantProxyShapes.appendPart(boxes, EnumDriveablePart.CORE, new AABB(0D, 0D, 0D, 0D, 0D, 0D), (part, local) -> local, COLOR);
        assertTrue(boxes.isEmpty());

        for (int i = 0; i < 40; i++)
            DistantProxyShapes.appendPart(boxes, EnumDriveablePart.CORE, FUSELAGE, (part, local) -> local, COLOR);
        assertEquals(DistantProxyShapes.MAX_BOXES, boxes.size());
    }

    @Test
    void segmentsFollowTheRatioOfLengthToWidth()
    {
        assertEquals(1, DistantProxyShapes.segmentsFor(2D, 2D));
        assertEquals(3, DistantProxyShapes.segmentsFor(6D, 2D));
        // Thin parts are not cut finer than the shortest worthwhile piece
        assertEquals(4, DistantProxyShapes.segmentsFor(3D, 0.1D));
        assertEquals(DistantProxyShapes.MAX_SEGMENTS, DistantProxyShapes.segmentsFor(100D, 1D));
    }
}
