package com.flansmodultimate.common.guns;

import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.*;

class DepthChargePhysicsTest
{
    @Test
    void activationRequiresWaterAndStrictlyExceedsTheAuthoredDepth()
    {
        assertFalse(DepthChargePhysics.shouldDetonate(false, 0D, 63, 20F));
        assertFalse(DepthChargePhysics.shouldDetonate(true, 43D, 63, 20F));
        assertTrue(DepthChargePhysics.shouldDetonate(true, 42.99D, 63, 20F));
        assertFalse(DepthChargePhysics.shouldDetonate(true, 64D, 63, 20F));
    }

    @Test
    void customSeaLevelsWorkAboveAndBelowZero()
    {
        assertTrue(DepthChargePhysics.shouldDetonate(true, 100D, 128, 20F));
        assertTrue(DepthChargePhysics.shouldDetonate(true, -85D, -64, 20F));
        assertFalse(DepthChargePhysics.shouldDetonate(true, -84D, -64, 20F));
        assertFalse(DepthChargePhysics.shouldDetonate(true, Double.NaN, 63, 20F));
        assertFalse(DepthChargePhysics.shouldDetonate(true, 0D, 63, Float.NaN));
    }

    @Test
    void waterStopsHorizontalTravelAndTheChargeEventuallySinksToItsFuseDepth()
    {
        double y = 62D;
        Vec3 velocity = new Vec3(5D, 0.4D, -3D);
        int ticks = 0;
        while (!DepthChargePhysics.shouldDetonate(true, y, 63, 20F) && ticks < 300)
        {
            velocity = DepthChargePhysics.velocity(velocity, true, 0.024525D, 0.8F);
            assertEquals(0D, velocity.x);
            assertEquals(0D, velocity.z);
            y += velocity.y;
            ticks++;
        }
        assertTrue(ticks < 300);
        assertTrue(y < 43D);
        assertTrue(velocity.y < 0D);
    }

    @Test
    void airborneReleaseKeepsHorizontalTravelAndLosesUpwardSpeed()
    {
        Vec3 ascending = DepthChargePhysics.velocity(new Vec3(2D, 1D, -2D), false, 0.025D, 1F);
        assertEquals(2D, ascending.x);
        assertEquals(-2D, ascending.z);
        assertEquals(0.8775D, ascending.y, 1E-9D);
        Vec3 descending = DepthChargePhysics.velocity(new Vec3(2D, -1D, -2D), false, 0.025D, 1F);
        assertEquals(-1.025D, descending.y, 1E-9D);
    }

    @Test
    void hundredMetreSettingOutlivesOrdinaryBulletsButReachesDepthWithinTheChargeLifetime()
    {
        double y = 62D;
        Vec3 velocity = Vec3.ZERO;
        int ticks = 0;
        while (!DepthChargePhysics.shouldDetonate(true, y, 63, 100F) && ticks < 2400)
        {
            velocity = DepthChargePhysics.velocity(velocity, true, 0.024525D, 0.8F);
            y += velocity.y;
            ticks++;
        }
        assertTrue(ticks > 600);
        assertTrue(ticks < 2400);
        assertTrue(y < -37D);
    }
}
