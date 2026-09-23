package com.flansmodultimate.client.render.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DriveableLodPolicyTest
{
    private static final float EPSILON = 1.0E-5F;

    @Test
    void smallGroundVehiclesUseHalfDistancesButShipsDoNot()
    {
        assertEquals(0.5F, DriveableLodPolicy.distanceScale(3F, true, 0.5F), EPSILON);
        assertEquals(1F, DriveableLodPolicy.distanceScale(3F, false, 0.5F), EPSILON);
        assertEquals(20F, DriveableLodPolicy.distanceScale(60F, false, 0.5F), EPSILON);
    }

    @Test
    void earlyGroundDistanceFadesOutForLargeModels()
    {
        assertEquals(1F, DriveableLodPolicy.distanceScale(6F, true, 0.5F), EPSILON);
        assertEquals(2.25F, DriveableLodPolicy.distanceScale(9F, true, 0.5F), EPSILON);
        assertEquals(4F, DriveableLodPolicy.distanceScale(12F, true, 0.5F), EPSILON);
        assertEquals(1F, DriveableLodPolicy.distanceScale(3F, true, 1F), EPSILON);
    }

    @Test
    void wholeModelDetailReducesProgressivelyAndPreservesCloseGeometry()
    {
        assertEquals(0.75F, DriveableLodPolicy.partThreshold(0.75F, 2F, 2F, 12D, 0.5F), EPSILON);
        assertEquals(2.375F, DriveableLodPolicy.partThreshold(0.75F, 2F, 2F, 26D, 0.5F), EPSILON);
        assertEquals(4F, DriveableLodPolicy.partThreshold(0.75F, 2F, 2F, 40D, 0.5F), EPSILON);
        // The same distances leave a large ship at its near-detail threshold.
        assertEquals(0.75F, DriveableLodPolicy.partThreshold(0.75F, 2F, 2F, 100D, 20F), EPSILON);
        assertEquals(4F, DriveableLodPolicy.partThreshold(0.75F, 2F, 2F, 1600D, 20F), EPSILON);
    }

    @Test
    void disabledCullingAndUnknownBoundsRemainConservative()
    {
        assertEquals(0F, DriveableLodPolicy.partThreshold(0F, 2F, 2F, 100D, 1F));
        assertEquals(2F, DriveableLodPolicy.partThreshold(2F, 1F, 2F, 100D, 1F));
        float unknown = DriveableLodPolicy.distanceScale(Float.POSITIVE_INFINITY, true, 0.5F);
        assertEquals(0.75F, DriveableLodPolicy.partThreshold(0.75F, 2F, 2F, 100D, unknown));
    }

    @Test
    void groundImpostorPixelAllowanceGrowsWithDistance()
    {
        float limit = DriveableLodPolicy.qualityPixelLimit(128);
        assertEquals(32F, DriveableLodPolicy.impostorThreshold(32F, 32D, 32F, 64F, true, 128), EPSILON);
        assertEquals((32F + limit) / 2F, DriveableLodPolicy.impostorThreshold(32F, 48D, 32F, 64F, true, 128), EPSILON);
        assertEquals(limit, DriveableLodPolicy.impostorThreshold(32F, 64D, 32F, 64F, true, 128), EPSILON);
        assertEquals(32F, DriveableLodPolicy.impostorThreshold(32F, 64D, 32F, 64F, false, 128), EPSILON);
    }

    @Test
    void explicitPixelOnlyAndDistanceOnlyModesRemainAvailable()
    {
        assertEquals(32F, DriveableLodPolicy.impostorThreshold(32F, 100D, 32F, 0F, true, 128));
        assertEquals(0F, DriveableLodPolicy.impostorThreshold(0F, 100D, 32F, 64F, true, 128));
    }

    @Test
    void defaultQualityDoublesResolutionAndYawSamplesWithinLimits()
    {
        assertEquals(128, DriveableLodPolicy.resolution(64, 2));
        assertEquals(16, DriveableLodPolicy.yawAngles(8, 2));
        assertEquals(256, DriveableLodPolicy.resolution(256, 2));
        assertEquals(16, DriveableLodPolicy.yawAngles(16, 2));
        assertEquals(64, DriveableLodPolicy.resolution(64, 1));
    }

    @Test
    void imageQualityLimitsForcedDistanceSwitchesWithHysteresis()
    {
        assertTrue(DriveableImpostorCache.shouldUseImpostor(300F, 1000D, 32F, 64F, false));
        assertFalse(DriveableLodPolicy.withinImageQuality(300F, 128, false));
        float limit = DriveableLodPolicy.qualityPixelLimit(128);
        assertTrue(DriveableLodPolicy.withinImageQuality(limit, 128, false));
        assertFalse(DriveableLodPolicy.withinImageQuality(limit * 1.05F, 128, false));
        assertTrue(DriveableLodPolicy.withinImageQuality(limit * 1.05F, 128, true));
        assertFalse(DriveableLodPolicy.withinImageQuality(limit * 1.11F, 128, true));
        assertFalse(DriveableLodPolicy.withinImageQuality(Float.NaN, 128, false));
    }
}
