package com.wolffsmod.npcs.tactics;

import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.*;

class NpcSafetyGoalTest
{
    @Test
    void aLauncherRetreatsPastItsBlastMargin()
    {
        Vec3 tank = new Vec3(3D, 0D, 0D);
        Vec3 retreat = NpcSafetyGoal.blastRetreat(Vec3.ZERO, tank, 10D, point -> true);
        assertNotNull(retreat);
        assertTrue(retreat.distanceTo(tank) >= 11D);
        assertTrue(retreat.x < 0D);
    }

    @Test
    void anUnreachableDirectRetreatUsesASideRoute()
    {
        Vec3 tank = new Vec3(3D, 0D, 0D);
        Vec3 retreat = NpcSafetyGoal.blastRetreat(Vec3.ZERO, tank, 10D, point -> Math.abs(point.z) > 2D);
        assertNotNull(retreat);
        assertTrue(Math.abs(retreat.z) > 2D);
        assertTrue(retreat.distanceTo(tank) >= 11D);
    }

    @Test
    void aBlockedLongRetreatCanMakeProgressInShorterSteps()
    {
        Vec3 tank = new Vec3(3D, 0D, 0D);
        Vec3 retreat = NpcSafetyGoal.blastRetreat(Vec3.ZERO, tank, 10D, point -> point.length() <= 4D);
        assertNotNull(retreat);
        assertTrue(retreat.length() <= 4D);
        assertTrue(retreat.distanceTo(tank) > tank.length() + 0.5D);
        assertNull(NpcSafetyGoal.blastRetreat(Vec3.ZERO, tank, 10D, point -> false), "no retreat may bypass movement or path restrictions");
    }

    @Test
    void anOverlappingTankStillAllowsARetreatDirection()
    {
        Vec3 retreat = NpcSafetyGoal.blastRetreat(Vec3.ZERO, Vec3.ZERO, 10D, point -> true);
        assertNotNull(retreat);
        assertTrue(retreat.length() >= 11D);
    }

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
