package com.flansmodultimate.client.render.entity;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

class DriveableImpostorCacheTest
{
    @Test
    void impostorActivatesByProjectedSizeOrMaximumDistance()
    {
        assertTrue(DriveableImpostorCache.shouldUseImpostor(31F, 80D, 32F, 128F, false));
        assertTrue(DriveableImpostorCache.shouldUseImpostor(96F, 128D, 32F, 128F, false));
        assertFalse(DriveableImpostorCache.shouldUseImpostor(96F, 127D, 32F, 128F, false));
    }

    @Test
    void impostorHysteresisPreventsBoundaryFlapping()
    {
        assertTrue(DriveableImpostorCache.shouldUseImpostor(38F, 110D, 32F, 128F, true));
        assertFalse(DriveableImpostorCache.shouldUseImpostor(39F, 100D, 32F, 128F, true));
        assertFalse(DriveableImpostorCache.shouldUseImpostor(1F, 1_000D, 0F, 0F, true));
    }

    @Test
    void atlasViewFollowsCameraPositionAroundVehicle()
    {
        Quaternionf identity = new Quaternionf();

        assertView(0, 4, new Vec3(0D, 0D, 10D), identity);
        assertView(2, 4, new Vec3(-10D, 0D, 0D), identity);
        assertView(4, 4, new Vec3(0D, 0D, -10D), identity);
        assertView(6, 4, new Vec3(10D, 0D, 0D), identity);
    }

    @Test
    void atlasViewAccountsForVehicleRotationAndCameraElevation()
    {
        Quaternionf quarterTurn = new Quaternionf().rotateY(90F * Mth.DEG_TO_RAD);

        assertView(0, 4, new Vec3(10D, 0D, 0D), quarterTurn);
        assertView(2, 4, new Vec3(0D, 0D, 10D), quarterTurn);
        assertView(0, 6, new Vec3(0D, 10D, 10D), new Quaternionf());
        assertView(0, 2, new Vec3(0D, -10D, 10D), new Quaternionf());
        assertView(0, 8, new Vec3(0D, 10D, 0D), new Quaternionf());
        assertView(0, 0, new Vec3(0D, -10D, 0D), new Quaternionf());
    }

    @Test
    void denserViewsBoundAngularErrorAcrossTheWholeSphere()
    {
        for (int yawAngles : new int[]{32, 64})
            for (int yaw = -180; yaw <= 180; yaw++)
                for (int pitch = -90; pitch <= 90; pitch += 3)
                {
                    Vector3f camera = new Vector3f(0F, 0F, 100F)
                        .rotateX(-pitch * Mth.DEG_TO_RAD).rotateY(-yaw * Mth.DEG_TO_RAD);
                    var view = select(camera, new Quaternionf(), 0F, 0F, 0F, yawAngles);
                    // Azimuth has no effect exactly at a pole.
                    if (Math.abs(pitch) < 90)
                        assertTrue(Math.abs(Mth.wrapDegrees(360F * view.yawIndex() / yawAngles - yaw))
                            <= 180F / yawAngles + 1E-4F);
                    assertTrue(Math.abs(DriveableImpostorCache.capturePitch(view.pitchIndex()) - pitch)
                        <= 11.25F + 1E-4F);
                }
    }

    @Test
    void billboardRetainsAircraftBank()
    {
        Quaternionf bank = new Quaternionf().rotateZ(45F * Mth.DEG_TO_RAD);
        Quaternionf original = new Quaternionf(bank);
        var view = select(new Vector3f(0F, 0F, 10F), bank, 0F, 0F, 0F, 32);
        Vector3f right = new Vector3f(1F, 0F, 0F).rotate(view.billboardRotation(bank));
        assertVector(new Vector3f(1F, 1F, 0F).normalize(), right);
        assertEquals(original, bank);
    }

    @Test
    void billboardReconstructsCapturedProjectionForRotatedOffCenterModels()
    {
        Quaternionf rotation = new Quaternionf().rotateY(0.7F).rotateZ(-0.4F).rotateX(1.1F);
        Vector3f center = new Vector3f(3F, -2F, 1F);
        Vector3f modelPoint = new Vector3f(0.7F, 1.3F, -0.9F);
        for (int pitchIndex = 0; pitchIndex < 9; pitchIndex++)
            for (int yawIndex = 0; yawIndex < 32; yawIndex++)
            {
                Quaternionf capture = new Quaternionf()
                    .rotateX(DriveableImpostorCache.capturePitch(pitchIndex) * Mth.DEG_TO_RAD)
                    .rotateY(360F * yawIndex / 32 * Mth.DEG_TO_RAD);
                Vector3f direction = new Vector3f(0F, 0F, 1F).rotate(new Quaternionf(capture).conjugate());
                Vector3f camera = new Vector3f(direction).mul(100F).add(center).rotate(rotation);
                var view = select(camera, rotation, center.x(), center.y(), center.z(), 32);
                Quaternionf billboard = view.billboardRotation(rotation);
                Vector3f normal = new Vector3f(0F, 0F, 1F).rotate(billboard);
                assertVector(new Vector3f(direction).rotate(rotation), normal);

                // Project geometry onto the camera plane and compare with the captured quad.
                Vector3f expected = new Vector3f(modelPoint).rotate(rotation);
                expected.sub(new Vector3f(normal).mul(expected.dot(normal)));
                Quaternionf selectedCapture = new Quaternionf()
                    .rotateX(DriveableImpostorCache.capturePitch(view.pitchIndex()) * Mth.DEG_TO_RAD)
                    .rotateY(360F * view.yawIndex() / 32 * Mth.DEG_TO_RAD);
                Vector3f imagePoint = new Vector3f(modelPoint).rotate(selectedCapture);
                imagePoint.z = 0F;
                assertVector(expected, imagePoint.rotate(billboard));
            }
    }

    @Test
    void atlasPagesNeverOverlapViewsIncludingPartialYawPages()
    {
        for (int yawAngles : new int[]{4, 5, 8, 17, 32, 64})
        {
            var occupied = new HashSet<DriveableImpostorCache.AtlasCell>();
            for (int pitch = 0; pitch < 9; pitch++)
                for (int yaw = 0; yaw < yawAngles; yaw++)
                {
                    var cell = DriveableImpostorCache.atlasCell(yaw, pitch, yawAngles);
                    assertTrue(occupied.add(cell));
                    assertTrue(cell.column() >= 0 && cell.column() < 4);
                    assertTrue(cell.row() >= 0 && cell.row() < 3);
                    assertTrue(cell.pageIndex() >= 0 && cell.pageIndex() < ((yawAngles + 3) / 4) * 3);
                }
            assertEquals(yawAngles * 9, occupied.size());
        }
    }

    @Test
    void invalidCameraOffsetKeepsANeutralView()
    {
        assertView(0, 4, Vec3.ZERO, new Quaternionf());
        assertView(0, 4, new Vec3(Double.NaN, 0D, 0D), new Quaternionf());
    }

    private static DriveableImpostorCache.ViewSelection select(Vector3f camera, Quaternionf rotation,
        float centerX, float centerY, float centerZ, int yawAngles)
    {
        return DriveableImpostorCache.selectView(new Vec3(camera.x(), camera.y(), camera.z()),
            rotation, centerX, centerY, centerZ, yawAngles);
    }

    private static void assertVector(Vector3f expected, Vector3f actual)
    {
        assertEquals(expected.x(), actual.x(), 2E-5F);
        assertEquals(expected.y(), actual.y(), 2E-5F);
        assertEquals(expected.z(), actual.z(), 2E-5F);
    }

    private static void assertView(int expectedYaw, int expectedPitch, Vec3 cameraOffset,
                                   Quaternionf entityRotation)
    {
        DriveableImpostorCache.ViewSelection selection = DriveableImpostorCache.selectView(
            cameraOffset, entityRotation, 0F, 0F, 0F, 8);
        assertEquals(expectedYaw, selection.yawIndex());
        assertEquals(expectedPitch, selection.pitchIndex());
    }
}
