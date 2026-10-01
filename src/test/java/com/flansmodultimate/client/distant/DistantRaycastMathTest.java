package com.flansmodultimate.client.distant;

import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DistantRaycastMathTest
{
    private static final Vec3 EAST = new Vec3(1D, 0D, 0D);

    @Test
    void aRayEntersTheReportedColumnAtItsWidenedFace()
    {
        // A level ray at y 70 meets a column from 60 to 80 at x 100; the column is widened by half a block
        double distance = DistantRaycastMath.entryDistance(new Vec3(0.5D, 70D, 0.5D), EAST, 100, 60, 80, 0);
        assertEquals(99D, distance, 1.0E-9D);
    }

    @Test
    void aDescendingRayEntersThroughTheTop()
    {
        Vec3 direction = new Vec3(1D, -1D, 0D).normalize();
        // Starting 100 blocks above a column topped at y 0 and 100 blocks short of its centre, the ray meets its top
        double distance = DistantRaycastMath.entryDistance(new Vec3(10.5D, 100D, 0.5D), direction, 110, -20, 0, 0);
        assertEquals(100D * Math.sqrt(2D), distance, 1.0E-6D);
    }

    @Test
    void aColumnBesideTheRayFallsBackToItsClosestApproach()
    {
        // Ten blocks to the side, beyond the widening: the distance along the ray to abreast of it
        double distance = DistantRaycastMath.entryDistance(new Vec3(0.5D, 70D, 0.5D), EAST, 50, 60, 80, 10);
        assertEquals(50D, distance, 1.0E-9D);
    }

    @Test
    void aColumnAroundTheObserverIsAtNoDistance()
    {
        double distance = DistantRaycastMath.entryDistance(new Vec3(5.5D, 70D, 5.5D), EAST, 5, 60, 80, 5);
        assertEquals(0D, distance, 1.0E-9D);
    }

    @Test
    void aColumnBehindTheObserverIsNeverANegativeDistance()
    {
        assertTrue(DistantRaycastMath.entryDistance(new Vec3(0.5D, 70D, 0.5D), EAST, -50, 60, 80, 30) >= 0D);
    }
}
