package com.flansmodultimate.common.entity;

import com.flansmodultimate.api.DriveableAttitude;
import com.flansmodultimate.common.driveables.LegacyDriveableCoordinates;
import com.flansmodultimate.common.raytracing.RotatedAxes;
import org.junit.jupiter.api.Test;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The attitude the API reports, built as {@link Driveable#getFrontVector} and {@link Driveable#getRoofVector} build it,
 * against the rotations the plane physics apply for each control, so that an automated pilot reads its keys right.
 */
class DriveableAttitudeTest
{
    private static DriveableAttitude attitude(RotatedAxes axes, boolean plane)
    {
        return new DriveableAttitude(world(new Vec3(1D, 0D, 0D), axes, plane), world(new Vec3(0D, 1D, 0D), axes, plane));
    }

    private static Vec3 world(Vec3 legacy, RotatedAxes axes, boolean plane)
    {
        Vec3 local = LegacyDriveableCoordinates.toLocal(legacy);
        if (plane)
            local = LegacyDriveableCoordinates.applyPlaneModelFacing(local);
        return LegacyDriveableCoordinates.modelLocalToWorldDirection(local, axes.getYaw(), axes.getPitch(), axes.getRoll()).normalize();
    }

    @Test
    void aLevelDriveableHeadsWhereItsModelIsDrawnFacing()
    {
        DriveableAttitude plane = attitude(new RotatedAxes(30F, 0F, 0F), true);
        assertEquals(LegacyDriveableCoordinates.renderedForwardYaw(30F, true), plane.heading(), 1.0E-3F);
        assertEquals(0F, plane.pitch(), 1.0E-3F);
        assertEquals(0F, plane.bank(), 1.0E-3F);
        DriveableAttitude vehicle = attitude(new RotatedAxes(30F, 0F, 0F), false);
        assertEquals(LegacyDriveableCoordinates.renderedForwardYaw(30F, false), vehicle.heading(), 1.0E-3F);
    }

    @Test
    void aPlanesSimulationPitchIsNegativeNoseUp()
    {
        assertEquals(10F, attitude(new RotatedAxes(30F, -10F, 0F), true).pitch(), 1.0E-3F);
    }

    @Test
    void thePitchUpRollRightAndYawRightControlsTurnThePlaneThatWay()
    {
        // Each control turns the axes as the plane physics do with a positive rate: pitch and yaw as given, roll negated.
        RotatedAxes pitchedUp = new RotatedAxes(30F, 0F, 0F);
        pitchedUp.rotateLocalPitch(5F);
        assertEquals(5F, attitude(pitchedUp, true).pitch(), 1.0E-3F);

        RotatedAxes rolledRight = new RotatedAxes(30F, 0F, 0F);
        rolledRight.rotateLocalRoll(-5F);
        assertEquals(5F, attitude(rolledRight, true).bank(), 1.0E-3F);

        RotatedAxes yawedRight = new RotatedAxes(30F, 0F, 0F);
        float heading = attitude(yawedRight, true).heading();
        yawedRight.rotateLocalYaw(5F);
        assertEquals(5F, Mth.wrapDegrees(attitude(yawedRight, true).heading() - heading), 1.0E-3F);
    }

    @Test
    void aBankedPlaneReadsItsBankWhateverItsHeading()
    {
        for (float yaw = -180F; yaw < 180F; yaw += 45F)
        {
            RotatedAxes axes = new RotatedAxes(yaw, -10F, 0F);
            axes.rotateLocalRoll(-30F);
            DriveableAttitude attitude = attitude(axes, true);
            assertEquals(30F, attitude.bank(), 0.5F, "yaw " + yaw);
            assertEquals(10F, attitude.pitch(), 0.5F, "yaw " + yaw);
        }
    }
}
