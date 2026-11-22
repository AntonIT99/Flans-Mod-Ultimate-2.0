package com.flansmodultimate.common.guns;

import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.*;

class PassiveApsTest
{
    private static final Vec3 FLYING_EAST = new Vec3(2D, 0D, 0D);

    @Test
    void aMissileFlyingStraightAtTheVehicleThreatensIt()
    {
        assertTrue(PassiveAps.threatens(new Vec3(40D, 0D, 0D), FLYING_EAST, 3D));
    }

    @Test
    void aMissileInsideTheMinimumRangeIsTooCloseToJam()
    {
        assertFalse(PassiveAps.threatens(new Vec3(9D, 0D, 0D), FLYING_EAST, 3D));
    }

    @Test
    void aMissileFlyingAwayOrWideIsNoThreat()
    {
        assertFalse(PassiveAps.threatens(new Vec3(-40D, 0D, 0D), FLYING_EAST, 3D), "flying away");
        assertFalse(PassiveAps.threatens(new Vec3(40D, 0D, 30D), FLYING_EAST, 3D), "passing far to the side");
    }

    @Test
    void theInterceptReachesTheVehiclesExtentPlusTheMargin()
    {
        // 50 blocks out and 11 to the side: misses a small vehicle, not one whose hitboxes reach 8 blocks.
        Vec3 offset = new Vec3(50D, 0D, 11D);
        assertFalse(PassiveAps.threatens(offset, FLYING_EAST, 3D));
        assertTrue(PassiveAps.threatens(offset, FLYING_EAST, 8D));
    }

    @Test
    void onlyAllAspectSystemsCoverTheRear()
    {
        Vec3 forward = new Vec3(0D, 0D, 1D);
        assertTrue(PassiveAps.covers(forward, new Vec3(0.5D, 0D, 1D), false));
        assertFalse(PassiveAps.covers(forward, new Vec3(0D, 0D, -1D), false));
        assertTrue(PassiveAps.covers(forward, new Vec3(0D, 0D, -1D), true));
    }

    @Test
    void theCorkscrewKeepsTheSpeedAndTurnsTheMissile()
    {
        Vec3 velocity = new Vec3(0D, 0D, 3D);
        int seed = PassiveAps.seed(12, 34);
        Vec3 turned = PassiveAps.spiral(velocity, seed, 0);
        assertEquals(velocity.length(), turned.length(), 1.0E-9D);
        assertTrue(turned.normalize().dot(velocity.normalize()) < 0.9999D);
        assertEquals(turned, PassiveAps.spiral(velocity, seed, 0), "every side corkscrews the same way");
    }

    @Test
    void theSeedIsNeverZero()
    {
        assertNotEquals(0, PassiveAps.seed(0, 0));
    }
}
