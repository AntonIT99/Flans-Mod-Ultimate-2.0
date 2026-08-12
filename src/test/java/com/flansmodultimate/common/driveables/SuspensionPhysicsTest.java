package com.flansmodultimate.common.driveables;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SuspensionPhysicsTest
{
    private static final double EPSILON = 1.0E-6D;

    @Test
    void boundsStepResponseInsteadOfLaunchingVehicle()
    {
        double velocity = SuspensionPhysics.dampVerticalVelocity(-0.04D, 1D, 0.5F, 0.4D);

        assertTrue(velocity > 0D);
        assertTrue(velocity < 0.3D);
    }

    @Test
    void dampsStoredUpwardVelocityAtRest()
    {
        double velocity = SuspensionPhysics.dampVerticalVelocity(0.3D, 0D, 0.5F, 0D);

        assertTrue(velocity >= 0D);
        assertTrue(velocity < 0.1D);
    }

    @Test
    void limitsTerrainAngleAndPerTickRotation()
    {
        float target = SuspensionPhysics.terrainAngle(10D, 0.5D);
        float firstTick = SuspensionPhysics.smoothTerrainAngle(0F, target, 0.5F);

        assertEquals(35F, target, EPSILON);
        assertTrue(firstTick > 0F);
        assertTrue(firstTick <= 3F);
    }

    @Test
    void rejectsInvalidNumbersAtPhysicsBoundary()
    {
        assertEquals(0D, SuspensionPhysics.dampVerticalVelocity(Double.NaN, 1D, 0.5F, 1D), EPSILON);
        assertEquals(0F, SuspensionPhysics.terrainAngle(Double.POSITIVE_INFINITY, 1D), EPSILON);
    }
}
