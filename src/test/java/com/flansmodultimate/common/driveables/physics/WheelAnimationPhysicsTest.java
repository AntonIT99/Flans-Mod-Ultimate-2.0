package com.flansmodultimate.common.driveables.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WheelAnimationPhysicsTest
{
    @Test
    void angularSpeedScalesWithAbsoluteVehicleSpeed()
    {
        assertEquals(6F, WheelAnimationPhysics.angularStepDegrees(0.1D, 0D, 1D, 0D), 1.0E-5F);
        assertEquals(18F, WheelAnimationPhysics.angularStepDegrees(0.3D, 0D, 1D, 0D), 1.0E-5F);
    }

    @Test
    void rotationReversesOnlyForActualBackwardMotion()
    {
        assertEquals(-12F, WheelAnimationPhysics.angularStepDegrees(-0.2D, 0D, 1D, 0D), 1.0E-5F);
        assertEquals(12F, WheelAnimationPhysics.angularStepDegrees(0.2D, 0D, 1D, 0D), 1.0E-5F);
    }

    @Test
    void stoppedVehicleDoesNotRotateWheels()
    {
        assertEquals(0F, WheelAnimationPhysics.angularStepDegrees(0D, 0D, 1D, 0D), 0F);
    }
}
