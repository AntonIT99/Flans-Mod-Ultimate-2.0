package com.flansmodultimate.client.render.effects;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VehicleScreenShakeTest
{
    @AfterEach
    void reset()
    {
        VehicleScreenShake.reset();
    }

    @Test
    void idleCameraIsUntouched()
    {
        assertEquals(0F, VehicleScreenShake.pitchOffset(1000L));
        assertEquals(0F, VehicleScreenShake.rollOffset(1000L));
    }

    @Test
    void kickLooksUpPeaksMidwayAndEnds()
    {
        VehicleScreenShake.add(1F, 0.2F, 1000L);
        float start = VehicleScreenShake.pitchOffset(1000L);
        float middle = VehicleScreenShake.pitchOffset(1100L);
        assertEquals(-0.08F, start, 1e-5F);
        assertTrue(middle < start, "the kick should be strongest halfway through");
        assertEquals(-0.2F, middle, 1e-5F);
        assertEquals(0F, VehicleScreenShake.pitchOffset(1200L));
        assertEquals(0F, VehicleScreenShake.rollOffset(1200L));
    }

    @Test
    void weakerKickDoesNotCutAStrongerOneShort()
    {
        VehicleScreenShake.add(2F, 0.5F, 0L);
        VehicleScreenShake.add(0.25F, 0.08F, 100L);
        assertEquals(VehicleScreenShakeTest.expectedPitch(2F, 0.2F), VehicleScreenShake.pitchOffset(100L), 1e-5F);
    }

    @Test
    void strongerKickRestartsTheShake()
    {
        VehicleScreenShake.add(0.25F, 0.08F, 0L);
        VehicleScreenShake.add(2F, 0.5F, 50L);
        assertEquals(-0.16F, VehicleScreenShake.pitchOffset(50L), 1e-5F);
    }

    @Test
    void weakerKickStartsOnceThePreviousOneHasEnded()
    {
        VehicleScreenShake.add(2F, 0.1F, 0L);
        VehicleScreenShake.add(0.5F, 0.1F, 150L);
        assertEquals(-0.04F, VehicleScreenShake.pitchOffset(150L), 1e-5F);
    }

    @Test
    void durationIsCapped()
    {
        VehicleScreenShake.add(1F, 60F, 0L);
        assertNotEquals(0F, VehicleScreenShake.pitchOffset(VehicleScreenShake.MAX_DURATION_MS - 1));
        assertEquals(0F, VehicleScreenShake.pitchOffset(VehicleScreenShake.MAX_DURATION_MS));
    }

    @Test
    void invalidKicksAreIgnored()
    {
        VehicleScreenShake.add(Float.NaN, 1F, 0L);
        VehicleScreenShake.add(1F, Float.NaN, 0L);
        VehicleScreenShake.add(0F, 1F, 0L);
        VehicleScreenShake.add(1F, -1F, 0L);
        assertEquals(0F, VehicleScreenShake.pitchOffset(10L));
    }

    private static float expectedPitch(float intensity, float progress)
    {
        float pulse = (float) Math.sin(progress * Math.PI);
        return -intensity * (0.08F + 0.32F * pulse) * (1F - progress);
    }
}
