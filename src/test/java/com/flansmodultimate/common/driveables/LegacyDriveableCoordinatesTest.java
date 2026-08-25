package com.flansmodultimate.common.driveables;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LegacyDriveableCoordinatesTest
{
    private static final double EPSILON = 1.0E-6D;

    @Test
    void pitchRotatesConvertedLegacyForwardAroundLocalXAxis()
    {
        Vec3 legacyForward = LegacyDriveableCoordinates.toLocal(new Vec3(1D, 0D, 0D));
        Vec3 result = LegacyDriveableCoordinates.rotateTurretLocal(legacyForward, 0F, 30F);

        assertVector(result, 0D, -0.5D, -Math.sqrt(3D) / 2D);
    }

    @Test
    void yawIsAppliedAfterPitchInTheRenderedTurretOrder()
    {
        Vec3 legacyForward = LegacyDriveableCoordinates.toLocal(new Vec3(1D, 0D, 0D));
        Vec3 result = LegacyDriveableCoordinates.rotateTurretLocal(legacyForward, 90F, 30F);

        assertVector(result, -Math.sqrt(3D) / 2D, -0.5D, 0D);
    }

    @Test
    void pitchLeavesTheConvertedLegacyLateralAxisUnchanged()
    {
        Vec3 legacyLateral = LegacyDriveableCoordinates.toLocal(new Vec3(0D, 0D, 1D));
        Vec3 result = LegacyDriveableCoordinates.rotateBarrelPitchLocal(legacyLateral, 35F);

        assertVector(result, 1D, 0D, 0D);
    }

    @Test
    void modelPitchRotatesLegacyForwardLikeTheRenderer()
    {
        Vec3 legacyForward = LegacyDriveableCoordinates.toLocal(new Vec3(1D, 0D, 0D));
        Vec3 result = LegacyDriveableCoordinates.modelLocalToWorldDirection(legacyForward, 0F, 30F, 0F);

        assertVector(result, -Math.sqrt(3D) / 2D, 0.5D, 0D);
    }

    @Test
    void modelRollIsAppliedBeforePitchAndYaw()
    {
        Vec3 legacyUp = LegacyDriveableCoordinates.toLocal(new Vec3(0D, 1D, 0D));
        Vec3 result = LegacyDriveableCoordinates.modelLocalToWorldDirection(legacyUp, 0F, 0F, 90F);

        assertVector(result, 0D, 0D, -1D);
    }

    @Test
    void flatModelTransformPreservesTheEstablishedLateralSide()
    {
        Vec3 legacyLateral = LegacyDriveableCoordinates.toLocal(new Vec3(0D, 0D, 1D));
        Vec3 result = LegacyDriveableCoordinates.modelLocalToWorldDirection(legacyLateral, 0F, 0F, 0F);

        assertVector(result, 0D, 0D, 1D);
    }

    @Test
    void planeFacingCorrectionTurnsTypeFileAnchorsAroundHorizontally()
    {
        Vec3 configured = LegacyDriveableCoordinates.toLocal(new Vec3(2D, 3D, 5D));
        Vec3 result = LegacyDriveableCoordinates.applyPlaneModelFacing(configured);

        assertVector(result, -5D, 3D, 2D);
    }

    @Test
    void correctedPlaneForwardMatchesItsMovementFacing()
    {
        Vec3 configuredForward = LegacyDriveableCoordinates.applyPlaneModelFacing(
            LegacyDriveableCoordinates.toLocal(new Vec3(1D, 0D, 0D)));
        Vec3 result = LegacyDriveableCoordinates.modelLocalToWorldDirection(
            configuredForward, 0F, 0F, 0F);

        assertVector(result, 1D, 0D, 0D);
    }

    @Test
    void recoversLegacyWheelLongitudinalAndLateralCoordinates()
    {
        Vec3 local = LegacyDriveableCoordinates.toLocal(new Vec3(36D, 4D, -20D));

        assertEquals(36D, LegacyDriveableCoordinates.legacyForwardCoordinate(local), EPSILON);
        assertEquals(-20D, LegacyDriveableCoordinates.legacyRightCoordinate(local), EPSILON);
    }

    private static void assertVector(Vec3 actual, double x, double y, double z)
    {
        assertEquals(x, actual.x, EPSILON);
        assertEquals(y, actual.y, EPSILON);
        assertEquals(z, actual.z, EPSILON);
    }
}
