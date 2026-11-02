package com.flansmodultimate.common.driveables.collision;

import com.flansmodultimate.common.driveables.EnumDriveablePart;
import org.junit.jupiter.api.Test;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PartBodyMovementTest
{
    @Test
    void aSolidBodyWithNoRemainingPartsDoesNotFallBackToTheVanillaBox()
    {
        Entity owner = mock(Entity.class);
        when(owner.position()).thenReturn(Vec3.ZERO);
        PartBodyCollisionHelper helper = new PartBodyCollisionHelper(owner, () -> true);
        helper.tick(Map.of(), 0, Vec3.ZERO, 0F, 0F, 0F, 1F, 0F, 0F, Vec3.ZERO, Vec3.ZERO, part -> false);
        Vec3 motion = new Vec3(1D, -1D, 2D);
        assertEquals(motion, helper.collideMovement(motion));
    }

    private static DriveableHullGeometry hull(float yaw, boolean intact)
    {
        var boxes = Map.of(EnumDriveablePart.CORE, CollisionBox.inWorldUnits(100F, -1F, 0F, -2F, 2F, 1F, 4F, 0F, 0F));
        var hull = new DriveableHullGeometry(PartBodyCollisionHelper.profile(boxes, 1F));
        PartBodyCollisionHelper.pose(hull, Vec3.ZERO, yaw, 0F, 0F, 1F, 0F, 0F, Vec3.ZERO, Vec3.ZERO, part -> intact, false);
        return hull;
    }

    @Test
    void theNoseStopsAtAWallAndSlidesAlongIt()
    {
        Vec3 result = PartBodyMovement.collide(hull(0F, true), List.of(new AABB(-10D, -1D, 3D, 10D, 4D, 4D)), Vec3.ZERO, new Vec3(0.5D, 0D, 2D), 0D, true);
        assertEquals(0.5D, result.x, 1.0E-6D);
        assertEquals(1D, result.z, 1.0E-6D);
    }

    @Test
    void contactUnderAnOuterPartSupportsTheWholeBody()
    {
        Vec3 result = PartBodyMovement.collide(hull(0F, true), List.of(new AABB(-0.5D, -2D, 1.5D, 0.5D, -0.5D, 2.5D)), Vec3.ZERO, new Vec3(0D, -2D, 0D), 0D, false);
        assertEquals(-0.5D, result.y, 1.0E-6D);
    }

    @Test
    void yawAndRepeatedMovementOffsetsFollowThePartGeometry()
    {
        List<AABB> wall = List.of(new AABB(3D, -1D, -10D, 4D, 4D, 10D));
        assertEquals(1D, PartBodyMovement.collide(hull(90F, true), wall, Vec3.ZERO, new Vec3(2D, 0D, 0D), 0D, true).x, 1.0E-6D);
        assertEquals(0.5D, PartBodyMovement.collide(hull(90F, true), wall, new Vec3(0.5D, 0D, 0D), new Vec3(2D, 0D, 0D), 0D, true).x, 1.0E-6D);
        assertEquals(2D, PartBodyMovement.collide(hull(0F, true), wall, Vec3.ZERO, new Vec3(2D, 0D, 0D), 0D, true).x, 1.0E-6D);
    }

    @Test
    void missingPartsLeaveNoCollisionAndEmptySpaceAroundTheHullStaysEmpty()
    {
        Vec3 motion = new Vec3(0D, 0D, 2D);
        assertEquals(motion, PartBodyMovement.collide(hull(0F, false), List.of(new AABB(-1D, 0D, 3D, 1D, 2D, 4D)), Vec3.ZERO, motion, 0D, true));
        assertEquals(motion, PartBodyMovement.collide(hull(0F, true), List.of(new AABB(1.5D, 0D, 2D, 2.5D, 2D, 3D)), Vec3.ZERO, motion, 0D, true));
    }

    @Test
    void aGroundedBodyStepsOntoALedgeButCannotStepThroughACeiling()
    {
        AABB ledge = new AABB(-10D, -1D, 2.5D, 10D, 0.5D, 5D);
        Vec3 motion = new Vec3(0D, -0.1D, 1D);
        Vec3 stepped = PartBodyMovement.collide(hull(0F, true), List.of(ledge), Vec3.ZERO, motion, 0.6D, true);
        assertEquals(1D, stepped.z, 1.0E-6D);
        assertEquals(0.5D, stepped.y, 1.0E-6D);
        AABB ceiling = new AABB(-10D, 1.1D, -10D, 10D, 2D, 10D);
        Vec3 stopped = PartBodyMovement.collide(hull(0F, true), List.of(ledge, ceiling), Vec3.ZERO, motion, 0.6D, true);
        assertEquals(0.5D, stopped.z, 1.0E-6D);
    }
}
