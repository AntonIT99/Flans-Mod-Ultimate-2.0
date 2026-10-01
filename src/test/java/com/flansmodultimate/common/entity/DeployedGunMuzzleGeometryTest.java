package com.flansmodultimate.common.entity;

import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeployedGunMuzzleGeometryTest
{
    @Test
    void modelMuzzleUsesTheSamePitchScaleAndYawAsRendering()
    {
        Vec3 offset = DeployedGunMuzzleGeometry.modelMuzzleOffset(
            new Vec3(0D, 6D, 0D), new Vec3(0D, 6D, 20D), 1F, 180F, 30F);

        assertEquals(0D, offset.x, 1.0E-6D);
        assertEquals(1D, offset.y, 1.0E-6D);
        assertEquals(20D * Math.cos(Math.toRadians(30D)) / 16D, offset.z, 1.0E-6D);
    }

    @Test
    void cameraHeightClearsLowMuzzlesAndCapsTallModels()
    {
        assertEquals(1D, DeployedGunMuzzleGeometry.recommendedCameraHeight(new Vec3(0D, 6D, 0D), 1F));
        assertEquals(1.25D, DeployedGunMuzzleGeometry.recommendedCameraHeight(new Vec3(0D, 12D, 0D), 1F));
        assertEquals(1.75D, DeployedGunMuzzleGeometry.recommendedCameraHeight(new Vec3(0D, 40D, 0D), 1F));
    }
}
