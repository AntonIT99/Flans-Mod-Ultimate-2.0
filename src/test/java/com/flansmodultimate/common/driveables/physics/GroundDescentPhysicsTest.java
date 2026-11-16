package com.flansmodultimate.common.driveables.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GroundDescentPhysicsTest
{
    @Test
    void aGentleDescentAndEveryClimbKeepFullSpeed()
    {
        assertEquals(1F, GroundSlopePhysics.descentFactor(0F, 1F));
        assertEquals(1F, GroundSlopePhysics.descentFactor(-VehiclePhysicsConstants.DESCENT_FALLOFF_START_DEG, 1F));
        assertEquals(1F, GroundSlopePhysics.descentFactor(40F, 1F));
        // Reversing with the nose down is a climb.
        assertEquals(1F, GroundSlopePhysics.descentFactor(-40F, -1F));
        assertEquals(1F, GroundSlopePhysics.descentFactor(-40F, 0F));
        assertEquals(1F, GroundSlopePhysics.descentFactor(Float.NaN, 1F));
    }

    @Test
    void aSteepDescentIsTakenSlowerDownToAFloor()
    {
        float moderate = GroundSlopePhysics.descentFactor(-30F, 1F);
        float steep = GroundSlopePhysics.descentFactor(-40F, 1F);
        assertTrue(moderate < 1F);
        assertTrue(steep < moderate);
        assertEquals(VehiclePhysicsConstants.DESCENT_MIN_SPEED, GroundSlopePhysics.descentFactor(-45F, 1F), 1.0E-6F);
        assertEquals(VehiclePhysicsConstants.DESCENT_MIN_SPEED, GroundSlopePhysics.descentFactor(-80F, 1F));
        // Reversing down a slope has the nose up.
        assertEquals(steep, GroundSlopePhysics.descentFactor(40F, -1F));
    }

    @Test
    void theCrawlAngleIsWherePropulsionReachesItsFloor()
    {
        float crawl = GroundSlopePhysics.crawlAngle(0.03F, EnumDriveType.RWD);
        assertTrue(crawl > SuspensionPhysics.DEFAULT_TERRAIN_ANGLE);
        assertEquals(VehiclePhysicsConstants.SLOPE_CRAWL_PROPULSION, GroundSlopePhysics.propulsionFactor(crawl, 1F, 0.03F, EnumDriveType.RWD), 1.0E-5F);
        assertTrue(GroundSlopePhysics.propulsionFactor(crawl - 1F, 1F, 0.03F, EnumDriveType.RWD) > VehiclePhysicsConstants.SLOPE_CRAWL_PROPULSION);
        assertTrue(GroundSlopePhysics.crawlAngle(0.06F, EnumDriveType.AWD) > GroundSlopePhysics.crawlAngle(0.015F, EnumDriveType.RWD));
        assertEquals(0F, GroundSlopePhysics.crawlAngle(0F, EnumDriveType.RWD));
        assertEquals(0F, GroundSlopePhysics.crawlAngle(0.03F, null));
    }

    @Test
    void onlyACompleteGroundProfileFollowsSteeperTerrain()
    {
        assertEquals(SuspensionPhysics.DEFAULT_TERRAIN_ANGLE, ResolvedVehiclePhysics.legacy(EnumVehicleCategory.GROUND, EnumDriveType.AWD).terrainAngleLimitDegrees());
        assertEquals(SuspensionPhysics.DEFAULT_TERRAIN_ANGLE, ResolvedVehiclePhysics.legacy(EnumVehicleCategory.AIRCRAFT, EnumDriveType.RWD).terrainAngleLimitDegrees());
    }

    @Test
    void theSupportAngleKeepsToTheLimitItIsGiven()
    {
        assertEquals(35F, SuspensionPhysics.supportAngle(2D, 2D, 0D, false));
        assertEquals(50F, SuspensionPhysics.supportAngle(2D, 2D, 0D, false, 50F));
        assertEquals(-50F, SuspensionPhysics.supportAngle(2D, 2D, 0D, true, 50F));
        assertEquals(35F, SuspensionPhysics.supportAngle(2D, 2D, 0D, false, -1F));
        // Below the limit, the angle is the terrain's own.
        assertEquals(SuspensionPhysics.supportAngle(0.5D, 3D, 0D, false), SuspensionPhysics.supportAngle(0.5D, 3D, 0D, false, 50F));
    }
}
