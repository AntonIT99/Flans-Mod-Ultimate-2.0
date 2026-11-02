package com.flansmodultimate.client.render.layer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArmorLodPolicyTest
{
    private static final float EPSILON = 1.0E-5F;

    @Test
    void uniformsKeepTheirNearThresholdWithinInfantryRange()
    {
        assertEquals(0.5F, ArmorLodPolicy.partThreshold(0.5F, 1.5F, 1F, 0D), EPSILON);
        assertEquals(0.5F, ArmorLodPolicy.partThreshold(0.5F, 1.5F, 1F, ArmorLodPolicy.NEAR_DISTANCE), EPSILON);
    }

    @Test
    void detailReducesSmoothlyAndStopsAtTheFarThreshold()
    {
        float middle = ArmorLodPolicy.partThreshold(0.5F, 1.5F, 1F, (ArmorLodPolicy.NEAR_DISTANCE + ArmorLodPolicy.FAR_DISTANCE) / 2D);
        assertEquals(1F, middle, EPSILON);
        assertEquals(1.5F, ArmorLodPolicy.partThreshold(0.5F, 1.5F, 1F, ArmorLodPolicy.FAR_DISTANCE), EPSILON);
        // Past the far distance nothing more is dropped.
        assertEquals(1.5F, ArmorLodPolicy.partThreshold(0.5F, 1.5F, 1F, 4096D), EPSILON);
        // The blend is monotonic, so no row of boxes reappears as the camera backs away.
        float previous = 0F;
        for (double distance = 0D; distance <= 80D; distance += 2D)
        {
            float threshold = ArmorLodPolicy.partThreshold(0.5F, 1.5F, 1F, distance);
            assertTrue(threshold >= previous, "threshold fell at " + distance);
            previous = threshold;
        }
    }

    @Test
    void strengthMultipliesOnlyTheFarThreshold()
    {
        assertEquals(0.5F, ArmorLodPolicy.partThreshold(0.5F, 1.5F, 4F, 0D), EPSILON);
        assertEquals(6F, ArmorLodPolicy.partThreshold(0.5F, 1.5F, 4F, ArmorLodPolicy.FAR_DISTANCE), EPSILON);
    }

    @Test
    void disabledCullingAndUnusableThresholdsStayConservative()
    {
        assertEquals(0F, ArmorLodPolicy.partThreshold(0F, 1.5F, 2F, 100D));
        assertEquals(2F, ArmorLodPolicy.partThreshold(2F, 1.5F, 1F, 100D));
        assertEquals(0.5F, ArmorLodPolicy.partThreshold(0.5F, 1.5F, 1F, Double.NaN));
        assertEquals(0.5F, ArmorLodPolicy.partThreshold(0.5F, 1.5F, 1F, Double.POSITIVE_INFINITY));
    }
}
