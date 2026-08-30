package com.flansmodultimate.common.driveables;

import com.flansmod.common.vector.Vector3f;
import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyDriveableCoordinatesTest
{
    private static final double EPSILON = 1.0E-6D;

    /**
     * Mirrors what {@code Driveable.getPassengerShootOrigin} composes for a
     * ground vehicle: the seat basis conversion, its lateral mirror, the muzzle
     * height, then the shared model transform. The vehicle model correction is
     * applied separately by the entity and is not part of this.
     */
    private static Vec3 passengerMuzzleOffset(Vector3f gunOrigin, float yaw, float pitch, float roll)
    {
        Vec3 local = LegacyDriveableCoordinates.toLocal(gunOrigin);
        local = new Vec3(-local.x, local.y + 1.35D, local.z);
        return LegacyDriveableCoordinates.modelLocalToWorldDirection(local, yaw, pitch, roll);
    }

    @Test
    void passengerGunOriginTakesTheSameLateralMirrorAsItsSeat()
    {
        // The Hellcat's turret MG: "GunOrigin 1 6 18 -11", in blocks.
        Vector3f gunOrigin = new Vector3f(6F / 16F, 18F / 16F, -11F / 16F);

        // Authored Z is -11px, and the mirror is what puts the muzzle on the
        // +Z side of the hull where the modelled gun actually sits.
        assertVector(passengerMuzzleOffset(gunOrigin, 0F, 0F, 0F), -0.375D, 2.475D, 0.6875D);
        assertVector(passengerMuzzleOffset(gunOrigin, 90F, 0F, 0F), -0.6875D, 2.475D, -0.375D);
    }

    @Test
    void passengerMuzzleRotatesRigidlyWithTheHull()
    {
        Vector3f gunOrigin = new Vector3f(6F / 16F, 18F / 16F, -11F / 16F);
        Vec3 local = LegacyDriveableCoordinates.toLocal(gunOrigin);
        double reach = new Vec3(-local.x, local.y + 1.35D, local.z).length();

        // Whatever the hull is doing, the muzzle stays the same distance out.
        assertEquals(reach, passengerMuzzleOffset(gunOrigin, 0F, 12F, 0F).length(), EPSILON);
        assertEquals(reach, passengerMuzzleOffset(gunOrigin, 0F, 0F, 15F).length(), EPSILON);
        assertEquals(reach, passengerMuzzleOffset(gunOrigin, 37F, 12F, 15F).length(), EPSILON);

        // Hull pitch is rotation about the lateral axis, so it cannot move the
        // muzzle sideways; hull roll is about the longitudinal axis, so it
        // cannot move it fore and aft.
        assertEquals(0.6875D, passengerMuzzleOffset(gunOrigin, 0F, 12F, 0F).z, EPSILON);
        assertEquals(-0.375D, passengerMuzzleOffset(gunOrigin, 0F, 0F, 15F).x, EPSILON);
    }

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

    @Test
    void mountedCameraBasisMatchesTheRenderedDriveableForEveryBasis()
    {
        float yaw = 35F;
        float pitch = 20F;
        float roll = 47F;
        for (boolean planeFacing : new boolean[] { false, true })
        {
            LegacyDriveableCoordinates.ViewAngles view =
                LegacyDriveableCoordinates.mountedViewAngles(yaw, pitch, roll, 0F, 0F, planeFacing);

            assertVector(vanillaViewVector(view.pitch(), view.yaw()), renderedNose(yaw, pitch, roll, planeFacing));
            assertVector(vanillaUpVector(view.pitch(), view.yaw(), view.roll()), renderedUp(yaw, pitch, roll));
        }
    }

    @Test
    void mountedCameraRollKeepsTheHorizonLevelWhileTheDriveableIsUpright()
    {
        for (boolean planeFacing : new boolean[] { false, true })
        {
            assertEquals(0F,
                LegacyDriveableCoordinates.mountedViewAngles(35F, 20F, 0F, 0F, 0F, planeFacing).roll(), EPSILON);
        }
    }

    @Test
    void mirroredDriveableBasesRollTheViewTheOppositeWay()
    {
        assertEquals(-25F, LegacyDriveableCoordinates.renderedViewRoll(25F, true), EPSILON);
        assertEquals(25F, LegacyDriveableCoordinates.renderedViewRoll(25F, false), EPSILON);
    }

    @Test
    void seatAimTurnsTheViewAroundTheDriveableAxesNotTheWorldAxes()
    {
        // Level driveable: aiming is a plain offset from the rendered forward.
        for (boolean planeFacing : new boolean[] { false, true })
        {
            LegacyDriveableCoordinates.ViewAngles level =
                LegacyDriveableCoordinates.mountedViewAngles(35F, 0F, 0F, 40F, 15F, planeFacing);

            assertEquals(LegacyDriveableCoordinates.renderedForwardYaw(35F, planeFacing) + 40F, level.yaw(), 1.0E-4D);
            assertEquals(15F, level.pitch(), 1.0E-4D);
            assertEquals(0F, level.roll(), 1.0E-4D);
        }

        // Rolled onto a wingtip, looking along that wing is looking straight
        // down, which the additive angles this replaced could not express.
        // Vanilla rejects an exactly vertical view, hence the 89.9 limit.
        LegacyDriveableCoordinates.ViewAngles banked =
            LegacyDriveableCoordinates.mountedViewAngles(0F, 0F, 90F, 90F, 0F, true);

        assertEquals(89.9F, Math.abs(banked.pitch()), 1.0E-4D);
    }

    @Test
    void aimYawPansTheViewAlongScreenRightWhateverTheDriveableBank()
    {
        // What a rider expects from a mouse: pushing it right moves the view
        // towards the right hand side of the screen, and nowhere else, even
        // when the cockpit is rolled and pitched away from the horizon.
        for (float roll : new float[] { 0F, 45F, 90F, 145F, 180F, -70F })
        {
            LegacyDriveableCoordinates.ViewAngles before =
                LegacyDriveableCoordinates.mountedViewAngles(35F, 20F, roll, 0F, 0F, true);
            LegacyDriveableCoordinates.ViewAngles after =
                LegacyDriveableCoordinates.mountedViewAngles(35F, 20F, roll, 2F, 0F, true);

            Vec3 panned = vanillaViewVector(after.pitch(), after.yaw())
                .subtract(vanillaViewVector(before.pitch(), before.yaw()));
            Vec3 screenUp = vanillaUpVector(before.pitch(), before.yaw(), before.roll());
            Vec3 screenRight = vanillaViewVector(before.pitch(), before.yaw()).cross(screenUp);

            assertTrue(panned.dot(screenRight) > 0.02D,
                "aim yaw should pan right on screen at roll " + roll);
            assertEquals(0D, panned.dot(screenUp), 1.0E-3D,
                "aim yaw should not tilt the view up or down at roll " + roll);
        }
    }

    @Test
    void aimPitchTiltsTheViewAlongScreenUpWhateverTheDriveableBank()
    {
        for (float roll : new float[] { 0F, 45F, 90F, 145F, 180F, -70F })
        {
            LegacyDriveableCoordinates.ViewAngles before =
                LegacyDriveableCoordinates.mountedViewAngles(35F, 20F, roll, 0F, 0F, true);
            LegacyDriveableCoordinates.ViewAngles after =
                LegacyDriveableCoordinates.mountedViewAngles(35F, 20F, roll, 0F, 2F, true);

            Vec3 tilted = vanillaViewVector(after.pitch(), after.yaw())
                .subtract(vanillaViewVector(before.pitch(), before.yaw()));
            Vec3 screenUp = vanillaUpVector(before.pitch(), before.yaw(), before.roll());
            Vec3 screenRight = vanillaViewVector(before.pitch(), before.yaw()).cross(screenUp);

            // Vanilla pitch grows downwards, so a positive aim pitch looks down.
            assertTrue(tilted.dot(screenUp) < -0.02D,
                "aim pitch should tilt down on screen at roll " + roll);
            assertEquals(0D, tilted.dot(screenRight), 1.0E-3D,
                "aim pitch should not pan the view sideways at roll " + roll);
        }
    }

    /** World direction of the rendered nose, in the basis the renderer uses. */
    private static Vec3 renderedNose(float yaw, float pitch, float roll, boolean planeFacing)
    {
        Vec3 modelForward = LegacyDriveableCoordinates.toLocal(new Vec3(1D, 0D, 0D));
        if (planeFacing)
            modelForward = LegacyDriveableCoordinates.applyPlaneModelFacing(modelForward);
        return LegacyDriveableCoordinates.modelLocalToWorldDirection(modelForward, yaw, pitch, roll);
    }

    /** World direction of the rendered model's up axis, shared by every basis. */
    private static Vec3 renderedUp(float yaw, float pitch, float roll)
    {
        return LegacyDriveableCoordinates.modelLocalToWorldDirection(
            LegacyDriveableCoordinates.toLocal(new Vec3(0D, 1D, 0D)), yaw, pitch, roll);
    }

    /** Screen-up direction of a vanilla camera at these angles, in world space. */
    private static Vec3 vanillaUpVector(float pitch, float yaw, float roll)
    {
        double pitchRadians = Math.toRadians(pitch);
        double yawRadians = Math.toRadians(yaw);
        double rollRadians = Math.toRadians(roll);
        Vec3 unrolledUp = new Vec3(-Math.sin(pitchRadians) * Math.sin(yawRadians), Math.cos(pitchRadians),
            Math.sin(pitchRadians) * Math.cos(yawRadians));
        Vec3 screenRight = new Vec3(-Math.cos(yawRadians), 0D, -Math.sin(yawRadians));
        return unrolledUp.scale(Math.cos(rollRadians)).add(screenRight.scale(Math.sin(rollRadians)));
    }

    private static void assertVector(Vec3 actual, Vec3 expected)
    {
        assertVector(actual, expected.x, expected.y, expected.z);
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
