package com.wolffsmod.npcs.tactics;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NpcSafetyGoalTest
{
    @Test
    void coverToFightFromLeadsTheShortlistAndTheRestKeepItsOrder()
    {
        BlockPos nearest = new BlockPos(1, 64, 0);
        BlockPos second = new BlockPos(2, 64, 0);
        BlockPos third = new BlockPos(3, 64, 0);
        List<BlockPos> shortlist = List.of(nearest, second, third);
        assertSame(shortlist, NpcSafetyGoal.leading(shortlist, 0));
        assertEquals(List.of(third, nearest, second), NpcSafetyGoal.leading(shortlist, 2));
    }

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
    void aFlankingSoldierGoesRoundTowardTheRearAndClosesIn()
    {
        Vec3 front = new Vec3(0D, 0D, 1D);
        // Off the tank's front-right, the step leading away from its front goes further right, toward the rear.
        Vec3 offset = new Vec3(20D, 0D, 20D);
        Vec3 step = NpcSafetyGoal.flankOffset(offset, front, 12D, true);
        assertEquals(12D, step.length(), 1.0E-9D);
        assertTrue(step.normalize().dot(front) < offset.normalize().dot(front));
        assertEquals(step.x > 0D, NpcSafetyGoal.flankOffset(offset, front, 12D, false).x > 0D, "the tie-break only applies straight ahead");
    }

    @Test
    void soldiersStraightAheadOfATankSplitToBothSides()
    {
        Vec3 front = new Vec3(0D, 0D, 1D);
        Vec3 ahead = new Vec3(0D, 0D, 30D);
        Vec3 left = NpcSafetyGoal.flankOffset(ahead, front, 30D, true);
        Vec3 right = NpcSafetyGoal.flankOffset(ahead, front, 30D, false);
        assertTrue(left.x * right.x < 0D);
        assertTrue(left.normalize().dot(front) < 0.5D);
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
