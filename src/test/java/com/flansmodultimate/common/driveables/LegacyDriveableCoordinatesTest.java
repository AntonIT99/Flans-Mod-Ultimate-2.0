package com.flansmodultimate.common.driveables;

import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void correctedPlaneForwardFollowsTheRenderedNosePitch()
    {
        Vec3 configuredForward = LegacyDriveableCoordinates.applyPlaneModelFacing(
            LegacyDriveableCoordinates.toLocal(new Vec3(1D, 0D, 0D)));
        Vec3 result = LegacyDriveableCoordinates.modelLocalToWorldDirection(
            configuredForward, 0F, -30F, 0F);

        assertVector(result, Math.sqrt(3D) / 2D, 0.5D, 0D);
    }

    @Test
    void planeForwardViewUsesTheRenderedNoseYaw()
    {
        assertEquals(-90F, LegacyDriveableCoordinates.planeForwardYaw(0F), EPSILON);
        assertEquals(90F, LegacyDriveableCoordinates.planeForwardYaw(180F), EPSILON);
        assertEquals(-80F, LegacyDriveableCoordinates.planeForwardYaw(370F), EPSILON);
    }

    @Test
    void everyDriveableEntityYawUsesItsRenderedForwardConvention()
    {
        assertEquals(-90F, LegacyDriveableCoordinates.renderedForwardYaw(0F, true), EPSILON);
        assertEquals(90F, LegacyDriveableCoordinates.renderedForwardYaw(0F, false), EPSILON);
        assertEquals(35F, LegacyDriveableCoordinates.driveableYawFromRenderedForward(-55F, true), EPSILON);
        assertEquals(35F, LegacyDriveableCoordinates.driveableYawFromRenderedForward(125F, false), EPSILON);
        assertEquals(20F, LegacyDriveableCoordinates.renderedForwardPitch(20F, true), EPSILON);
        assertEquals(-20F, LegacyDriveableCoordinates.renderedForwardPitch(20F, false), EPSILON);
    }

    @Test
    void alignedEntityYawRoundTripsWithoutChangingSimulationYaw()
    {
        for (boolean planeFacing : new boolean[] { false, true })
        {
            float simulationYaw = 173F;
            float entityYaw = LegacyDriveableCoordinates.renderedForwardYaw(simulationYaw, planeFacing);
            assertEquals(simulationYaw,
                LegacyDriveableCoordinates.driveableYawFromRenderedForward(entityYaw, planeFacing), EPSILON);
        }
    }

    @Test
    void vanillaEntityVectorMatchesRenderedForwardForEveryDriveableBasis()
    {
        float yaw = 35F;
        float pitch = 20F;
        for (boolean planeFacing : new boolean[] { false, true })
        {
            Vec3 modelForward = LegacyDriveableCoordinates.toLocal(new Vec3(1D, 0D, 0D));
            if (planeFacing)
                modelForward = LegacyDriveableCoordinates.applyPlaneModelFacing(modelForward);
            Vec3 renderedForward = LegacyDriveableCoordinates.modelLocalToWorldDirection(
                modelForward, yaw, pitch, 0F);
            Vec3 entityForward = vanillaViewVector(
                LegacyDriveableCoordinates.renderedForwardPitch(pitch, planeFacing),
                LegacyDriveableCoordinates.renderedForwardYaw(yaw, planeFacing));

            assertVector(entityForward, renderedForward.x, renderedForward.y, renderedForward.z);
        }
    }

    @Test
    void recoversLegacyWheelLongitudinalAndLateralCoordinates()
    {
        Vec3 local = LegacyDriveableCoordinates.toLocal(new Vec3(36D, 4D, -20D));

        assertEquals(36D, LegacyDriveableCoordinates.legacyForwardCoordinate(local), EPSILON);
        assertEquals(-20D, LegacyDriveableCoordinates.legacyRightCoordinate(local), EPSILON);
    }

    @Test
    void planeWheelFrontRemainsAheadOfTailInRenderedFacing()
    {
        Vec3 front = LegacyDriveableCoordinates.applyPlaneModelFacing(
            LegacyDriveableCoordinates.toLocal(new Vec3(42D, -5D, 0D)));
        Vec3 tail = LegacyDriveableCoordinates.applyPlaneModelFacing(
            LegacyDriveableCoordinates.toLocal(new Vec3(-94D, 18D, 0D)));
        Vec3 renderedFront = LegacyDriveableCoordinates.modelLocalToWorldDirection(front, 0F, 0F, 0F);
        Vec3 renderedTail = LegacyDriveableCoordinates.modelLocalToWorldDirection(tail, 0F, 0F, 0F);
        Vec3 renderedForward = LegacyDriveableCoordinates.modelLocalToWorldDirection(
            LegacyDriveableCoordinates.applyPlaneModelFacing(
                LegacyDriveableCoordinates.toLocal(new Vec3(1D, 0D, 0D))), 0F, 0F, 0F);

        assertTrue(renderedFront.subtract(renderedTail).dot(renderedForward) > 0D);
    }

    @Test
    void vehicleWheelFrontRemainsAheadWithoutPlaneCorrection()
    {
        Vec3 front = LegacyDriveableCoordinates.toLocal(new Vec3(22D, -10D, 0D));
        Vec3 rear = LegacyDriveableCoordinates.toLocal(new Vec3(-36D, -10D, 0D));
        Vec3 renderedFront = LegacyDriveableCoordinates.modelLocalToWorldDirection(front, 0F, 0F, 0F);
        Vec3 renderedRear = LegacyDriveableCoordinates.modelLocalToWorldDirection(rear, 0F, 0F, 0F);
        Vec3 renderedForward = LegacyDriveableCoordinates.modelLocalToWorldDirection(
            LegacyDriveableCoordinates.toLocal(new Vec3(1D, 0D, 0D)), 0F, 0F, 0F);

        assertTrue(renderedFront.subtract(renderedRear).dot(renderedForward) > 0D);
    }

    private static void assertVector(Vec3 actual, double x, double y, double z)
    {
        assertEquals(x, actual.x, EPSILON);
        assertEquals(y, actual.y, EPSILON);
        assertEquals(z, actual.z, EPSILON);
    }

    private static Vec3 vanillaViewVector(float pitch, float yaw)
    {
        double pitchRadians = Math.toRadians(pitch);
        double yawRadians = Math.toRadians(-yaw);
        return new Vec3(Math.sin(yawRadians) * Math.cos(pitchRadians),
            -Math.sin(pitchRadians), Math.cos(yawRadians) * Math.cos(pitchRadians));
    }
}
