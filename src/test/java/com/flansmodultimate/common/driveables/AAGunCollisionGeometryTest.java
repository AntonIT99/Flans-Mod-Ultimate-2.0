package com.flansmodultimate.common.driveables;

import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.*;

class AAGunCollisionGeometryTest
{
    private static DriveableHullGeometry box(float yaw)
    {
        DriveableHullGeometry geometry = new DriveableHullGeometry(DriveableCollisionProfile.aaGun(8F, 9F));
        geometry.update(0D, 0D, 0D, yaw, 0F, 0F, 0F, 0F, Vec3.ZERO, Vec3.ZERO,
            part -> true, false);
        return geometry;
    }

    @Test
    void squareFootprintTurnsWithYawForHitsAndStanding()
    {
        DriveableHullGeometry straight = box(0F);
        DriveableHullGeometry diagonal = box(45F);
        Vec3 above = new Vec3(5D, 11D, 0D);
        Vec3 below = new Vec3(5D, -1D, 0D);

        assertNull(straight.clipSegment(above, below, 0D));
        Vec3 hit = diagonal.clipSegment(above, below, 0D);
        assertNotNull(hit);
        assertEquals(9D, hit.y, 1.0E-6D);

        assertEquals(-3D, straight.clip(1, 4.8D, 10D, -0.2D, 5.2D, 11.8D, 0.2D, -3D), 1.0E-6D);
        assertEquals(-1D, diagonal.clip(1, 4.8D, 10D, -0.2D, 5.2D, 11.8D, 0.2D, -3D), 1.0E-6D);
    }
}
