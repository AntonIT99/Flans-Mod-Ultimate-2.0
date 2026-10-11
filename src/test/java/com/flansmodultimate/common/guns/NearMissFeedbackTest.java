package com.flansmodultimate.common.guns;

import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NearMissFeedbackTest
{
    @Test
    void fastRoundDetectsPlayerBetweenTickEndpoints()
    {
        Vec3 eye = new Vec3(50D, 2D, 0D);
        Vec3 closest = NearMissFeedback.closestPoint(Vec3.ZERO, new Vec3(100D, 0D, 0D), eye);
        assertEquals(new Vec3(50D, 0D, 0D), closest);
        assertEquals(2D, closest.distanceTo(eye));
    }

    @Test
    void stoppingImpactDoesNotExtendTheNearMissSegment()
    {
        Vec3 impact = new Vec3(5D, 0D, 0D);
        Vec3 eye = new Vec3(10D, 0D, 0D);
        assertEquals(impact, NearMissFeedback.closestPoint(Vec3.ZERO, impact, eye));
        assertEquals(0F, NearMissFeedback.intensity(40F, impact.distanceTo(eye), 3D, 1D));
        assertEquals(Vec3.ZERO, NearMissFeedback.closestPoint(Vec3.ZERO, Vec3.ZERO, eye));
    }

    @Test
    void heavierAndCloserShotsSuppressMoreAndMultipliersCanDisable()
    {
        float rifle = NearMissFeedback.intensity(5F, 1D, 3D, 1D);
        float cannon = NearMissFeedback.intensity(40F, 1D, 3D, 1D);
        assertTrue(cannon > rifle);
        assertTrue(NearMissFeedback.intensity(40F, 0D, 3D, 1D) > cannon);
        assertEquals(0F, NearMissFeedback.intensity(40F, 1D, 3D, 0D));
        assertEquals(0F, NearMissFeedback.intensity(40F, 1D, 0D, 1D));
        assertEquals(1F, NearMissFeedback.intensity(4000F, 0D, 3D, 10D));
        assertEquals(0F, NearMissFeedback.intensity(Float.NaN, 1D, 3D, 1D));
    }
}
