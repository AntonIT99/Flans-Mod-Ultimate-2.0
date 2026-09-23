package com.flansmodultimate.client.render.entity;

import org.joml.Quaternionf;
import org.junit.jupiter.api.Test;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

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

        assertView(0, 1, new Vec3(0D, 0D, 10D), identity);
        assertView(2, 1, new Vec3(-10D, 0D, 0D), identity);
        assertView(4, 1, new Vec3(0D, 0D, -10D), identity);
        assertView(6, 1, new Vec3(10D, 0D, 0D), identity);
    }

    @Test
    void atlasViewAccountsForVehicleRotationAndCameraElevation()
    {
        Quaternionf quarterTurn = new Quaternionf().rotateY(90F * Mth.DEG_TO_RAD);

        assertView(0, 1, new Vec3(10D, 0D, 0D), quarterTurn);
        assertView(2, 1, new Vec3(0D, 0D, 10D), quarterTurn);
        assertView(0, 2, new Vec3(0D, 10D, 10D), new Quaternionf());
        assertView(0, 0, new Vec3(0D, -10D, 10D), new Quaternionf());
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
