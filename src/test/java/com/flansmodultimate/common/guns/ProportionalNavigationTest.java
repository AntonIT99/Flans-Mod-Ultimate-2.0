package com.flansmodultimate.common.guns;

import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.*;

class ProportionalNavigationTest
{
    @Test
    void leadsCrossingTargetWithoutChangingSpeedAndRespectsTurnBudget()
    {
        Vec3 velocity = new Vec3(3, 0, 0);
        Vec3 steered = ProportionalNavigation.steer(velocity, new Vec3(100, 0, 0), new Vec3(0, 1, 0), 4, 0.01);
        assertTrue(steered.y > 0D);
        assertEquals(velocity.length(), steered.length(), 1E-10D);
        assertTrue(Math.acos(steered.normalize().dot(velocity.normalize())) <= 0.0100001D);
    }

    @Test
    void invalidGeometryAndRecedingTargetsKeepCurrentVelocity()
    {
        Vec3 velocity = new Vec3(3, 0, 0);
        assertEquals(velocity, ProportionalNavigation.steer(velocity, Vec3.ZERO, Vec3.ZERO, 4, 1));
        assertEquals(velocity, ProportionalNavigation.steer(velocity, new Vec3(100, 0, 0), new Vec3(4, 1, 0), 4, 1));
        assertEquals(velocity, ProportionalNavigation.steer(velocity, new Vec3(100, 0, 0), Vec3.ZERO, Double.NaN, 1));
        assertEquals(Vec3.ZERO, ProportionalNavigation.steer(Vec3.ZERO, new Vec3(100, 0, 0), Vec3.ZERO, 4, 1));
    }

    @Test
    void steadyCollisionCourseNeedsNoCorrection()
    {
        Vec3 velocity = new Vec3(3, 0, 0);
        assertEquals(velocity, ProportionalNavigation.steer(velocity, new Vec3(100, 0, 0), Vec3.ZERO, 4, 1));
    }
}
