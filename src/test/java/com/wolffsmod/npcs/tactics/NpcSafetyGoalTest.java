package com.wolffsmod.npcs.tactics;

import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.*;

class NpcSafetyGoalTest
{
    @Test
    void aSoldierInThePathOfAnApproachingVehicleEscapesToTheNearestSide()
    {
        AABB soldier = new AABB(-0.3D, 0D, -0.3D, 0.3D, 1.8D, 0.3D);
        AABB vehicle = new AABB(-2D, 0D, -7D, 2D, 2D, -3D);
        Vec3 escape = NpcSafetyGoal.escape(Vec3.ZERO, soldier, vehicle, new Vec3(0D, 0D, 0.4D));
        assertNotNull(escape);
        assertEquals(0D, escape.z, 1.0E-9D, "escape is sideways, not farther along the vehicle's path");
        assertTrue(Math.abs(escape.x) > 2D);
        Vec3 right = new Vec3(1.5D, 0D, 0D);
        Vec3 nearest = NpcSafetyGoal.escape(right, soldier.move(right), vehicle, new Vec3(0D, 0D, 0.4D));
        assertNotNull(nearest);
        assertTrue(nearest.x > right.x, "someone already on the right does not cross in front of the vehicle");
    }

    @Test
    void stationaryRecedingAndNonIntersectingVehiclesDoNotCauseEvasion()
    {
        AABB soldier = new AABB(-0.3D, 0D, -0.3D, 0.3D, 1.8D, 0.3D);
        AABB vehicle = new AABB(-2D, 0D, -7D, 2D, 2D, -3D);
        assertNull(NpcSafetyGoal.escape(Vec3.ZERO, soldier, vehicle, Vec3.ZERO));
        assertNull(NpcSafetyGoal.escape(Vec3.ZERO, soldier, vehicle, new Vec3(0D, 0D, -0.4D)));
        assertNull(NpcSafetyGoal.escape(Vec3.ZERO, soldier, vehicle.move(10D, 0D, 0D), new Vec3(0D, 0D, 0.4D)));
    }
}
