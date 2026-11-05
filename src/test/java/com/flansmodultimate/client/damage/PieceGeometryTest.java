package com.flansmodultimate.client.damage;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.*;

class PieceGeometryTest
{
    @Test
    void aLongFaceRemainsSupportedWhenItsVerticesOverhangANarrowBlock()
    {
        PieceGeometry geometry = new PieceGeometry();
        geometry.vertex(-2D, 0D, -0.2D).endVertex();
        geometry.vertex(2D, 0D, -0.2D).endVertex();
        geometry.vertex(2D, 0D, 0.2D).endVertex();
        geometry.vertex(-2D, 0D, 0.2D).endVertex();
        Vector3f[] points = geometry.relativeTo(new Vector3f());
        Quaternionf rotation = new Quaternionf();
        Vec3 pivot = new Vec3(0D, 1D, 0D);
        PieceGrounding.Surface block = (point, ceiling) -> Math.abs(point.x) < 0.5D && ceiling >= 0D ? 0D : Double.NaN;
        assertEquals(0D, PieceGrounding.supportHeight(points, rotation, pivot, rotation, pivot, block), 1.0E-6D,
            "The face must rest on the block even when none of its vertices is above it");
    }

    @Test
    void elevatedScreenContactsTheFloorAtItsDrawnVerticesAfterRotating()
    {
        PieceGeometry geometry = new PieceGeometry();
        Vector3f pivot = new Vector3f(2F, 1F, 3F);
        for (int x = -1; x <= 1; x += 2)
        {
            for (int y = 2; y <= 3; y++)
            {
                geometry.vertex(pivot.x + x, pivot.y + y, pivot.z);
                geometry.vertex(pivot.x + x, pivot.y + y, pivot.z);
            }
        }
        Vector3f[] points = geometry.relativeTo(pivot);
        assertEquals(4, points.length, "Shared face vertices should be sampled only once");
        Quaternionf rotation = new Quaternionf().rotateZ(0.7F);
        Vec3 initial = new Vec3(0D, 4D, 0D);
        double support = PieceGrounding.supportHeight(points, rotation, initial, rotation, initial, (point, ceiling) -> ceiling >= 0D ? 0D : Double.NaN);
        double lowest = Double.POSITIVE_INFINITY;
        for (Vector3f point : points)
            lowest = Math.min(lowest, support + rotation.transform(new Vector3f(point)).y);
        assertEquals(0D, lowest, 1.0E-6D, "Visible geometry must touch the floor even when it is offset from the damage-box pivot");
        assertTrue(support < 0D, "An elevated screen needs to fall below the old damage-box pivot to land");
    }
}
