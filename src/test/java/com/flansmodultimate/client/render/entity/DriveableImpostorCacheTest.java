package com.flansmodultimate.client.render.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DriveableImpostorCacheTest
{
    private static final float EPSILON = 1.0E-5F;

    @Test
    void adaptivePartThresholdTransitionsTowardFarLod()
    {
        assertEquals(0.75F,
            DriveableImpostorCache.adaptivePartThreshold(0.75F, 2F, 96F, 32F), EPSILON);
        assertEquals(1.375F,
            DriveableImpostorCache.adaptivePartThreshold(0.75F, 2F, 64F, 32F), EPSILON);
        assertEquals(2F,
            DriveableImpostorCache.adaptivePartThreshold(0.75F, 2F, 32F, 32F), EPSILON);
    }

    @Test
    void adaptivePartThresholdRetainsExplicitOffAndInvalidRanges()
    {
        assertEquals(0F,
            DriveableImpostorCache.adaptivePartThreshold(0F, 2F, 32F, 32F), EPSILON);
        assertEquals(2F,
            DriveableImpostorCache.adaptivePartThreshold(2F, 1F, 32F, 32F), EPSILON);
        assertEquals(0.75F,
            DriveableImpostorCache.adaptivePartThreshold(0.75F, 2F, 32F, 0F), EPSILON);
    }

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
}
