package com.flansmodultimate.common.driveables.collision;

import com.flansmodultimate.api.driveables.MountAim;
import com.flansmodultimate.api.parts.PartBodyPose;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.driveables.parts.PartBody;
import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class PartBodyCollisionHelperTest
{
    private static final Vec3 ORIGIN = new Vec3(10D, 64D, 10D);
    private static final Vec3 PIVOT = new Vec3(0D, 1D, 0.5D);
    private static final Vec3 OFFSET = new Vec3(0D, 0.25D, 0D);

    /** A hull, a pod on its right side only, and a turret box ahead of an off-centre pivot. */
    private static Map<EnumDriveablePart, CollisionBox> boxes()
    {
        Map<EnumDriveablePart, CollisionBox> boxes = new EnumMap<>(EnumDriveablePart.class);
        boxes.put(EnumDriveablePart.CORE, CollisionBox.inWorldUnits(100F, -1F, 0F, -2F, 2F, 1F, 4F, 2F, 0F));
        boxes.put(EnumDriveablePart.RIGHT_SKIRT, CollisionBox.inWorldUnits(50F, 2F, 0F, -0.5F, 1F, 1F, 1F, 2F, 0F));
        boxes.put(EnumDriveablePart.TURRET, CollisionBox.inWorldUnits(40F, -0.25F, 1F, -3F, 0.5F, 0.5F, 1F, 2F, 0F));
        return boxes;
    }

    @Test
    void solidBoxesStandExactlyWhereRoundsHitThem()
    {
        Map<EnumDriveablePart, CollisionBox> boxes = boxes();
        PartBody body = new PartBody(null, boxes, PIVOT, OFFSET);
        for (float yaw : new float[]{0F, 37F, -120F})
        {
            for (float scale : new float[]{1F, 1.5F, 0.4F})
            {
                // Level, then pitched and banked as a plane in flight.
                for (float[] tilt : new float[][]{{0F, 0F}, {-25F, 40F}})
                {
                    PartBodyPose pose = new PartBodyPose(ORIGIN, yaw, tilt[0], tilt[1], scale, new MountAim(55F, 0F));
                    body.setPose(pose);
                    DriveableHullGeometry geometry = new DriveableHullGeometry(PartBodyCollisionHelper.profile(boxes, scale));
                    PartBodyCollisionHelper.pose(geometry, ORIGIN, yaw, tilt[0], tilt[1], scale, 55F, 0F, PIVOT, OFFSET, part -> true, false);
                    List<PartBody.Outline> outlines = body.outlines();
                    assertEquals(3, outlines.size());
                    for (PartBody.Outline outline : outlines)
                        assertTrue(matchesAShape(geometry, outline), "yaw " + yaw + " pitch " + tilt[0] + " roll " + tilt[1] + " scale " + scale);
                }
            }
        }
    }

    @Test
    void anEntityIsStoppedByAndRestsOnTheDeckButNotWhereAPartIsGone()
    {
        Map<EnumDriveablePart, CollisionBox> boxes = boxes();
        DriveableHullGeometry geometry = new DriveableHullGeometry(PartBodyCollisionHelper.profile(boxes, 1F));
        PartBodyCollisionHelper.pose(geometry, ORIGIN, 0F, 0F, 0F, 1F, 0F, 0F, PIVOT, OFFSET, part -> true, false);
        // Falling onto the hull, whose top is one block above the origin.
        assertEquals(-0.5D, geometry.clip(1, 9.7D, 65.5D, 9.7D, 10.3D, 67.3D, 10.3D, -2D), 1.0E-6D);
        // Standing on it counts from the tick after the boxes were first posed, when there is a pose to be carried from.
        assertEquals(-1, geometry.findSupport(9.7D, 65D, 9.7D, 10.3D, 66.8D, 10.3D));
        PartBodyCollisionHelper.pose(geometry, ORIGIN, 0F, 0F, 0F, 1F, 0F, 0F, PIVOT, OFFSET, part -> true, true);
        assertTrue(geometry.findSupport(9.7D, 65D, 9.7D, 10.3D, 66.8D, 10.3D) >= 0);
        // The pod is on the model's right, which is west while it faces south.
        assertTrue(geometry.intersects(7.2D, 64.2D, 9.8D, 7.8D, 64.8D, 10.2D));
        assertFalse(geometry.intersects(12.2D, 64.2D, 9.8D, 12.8D, 64.8D, 10.2D));
        PartBodyCollisionHelper.pose(geometry, ORIGIN, 0F, 0F, 0F, 1F, 0F, 0F, PIVOT, OFFSET, part -> part != EnumDriveablePart.RIGHT_SKIRT, true);
        assertFalse(geometry.intersects(7.2D, 64.2D, 9.8D, 7.8D, 64.8D, 10.2D));
    }

    /** Whether one posed shape has all eight of the outline's corners, whatever their order. */
    private static boolean matchesAShape(DriveableHullGeometry geometry, PartBody.Outline outline)
    {
        for (int shape = 0; shape < 3; shape++)
        {
            double[] vertices = geometry.copyCurrentVertices(shape);
            assertNotNull(vertices);
            boolean all = true;
            for (Vec3 corner : outline.corners())
                all &= hasVertex(vertices, ORIGIN.add(corner));
            if (all)
                return true;
        }
        return false;
    }

    private static boolean hasVertex(double[] vertices, Vec3 point)
    {
        for (int index = 0; index < vertices.length; index += 3)
        {
            if (point.distanceToSqr(vertices[index], vertices[index + 1], vertices[index + 2]) < 1.0E-8D)
                return true;
        }
        return false;
    }
}
