package com.flansmodultimate.common.driveables;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DriveableControlPhysicsTest
{
    private static final float EPSILON = 1.0E-6F;

    @Test
    void engineToggleIsAValidatedEdgeTriggeredIntent()
    {
        assertEquals(DriveableInput.TOGGLE_ENGINE, DriveableInput.sanitize(DriveableInput.TOGGLE_ENGINE));
        assertTrue(DriveableInput.isDown(DriveableInput.EDGE_TRIGGERED_MASK, DriveableInput.TOGGLE_ENGINE));
        assertFalse(DriveableInput.isDown(DriveableInput.CONTINUOUS_MASK, DriveableInput.TOGGLE_ENGINE));
    }

    @Test
    void throttleIsNormalizedIndependentlyOfConfiguredPropulsion()
    {
        assertEquals(1F, DriveableControlPhysics.normalizedThrottle(8F, 0F), EPSILON);
        assertEquals(0F, DriveableControlPhysics.normalizedThrottle(-0.5F, 0F), EPSILON);
        assertEquals(-1F, DriveableControlPhysics.normalizedThrottle(-2F, 0.4F), EPSILON);
    }

    @Test
    void hudThrottleAlwaysSpansTheNormalizedMinusOneHundredToOneHundredRange()
    {
        assertEquals(100, DriveableControlPhysics.throttlePercent(1F));
        assertEquals(68, DriveableControlPhysics.throttlePercent(0.68F));
        assertEquals(0, DriveableControlPhysics.throttlePercent(0F));
        assertEquals(-68, DriveableControlPhysics.throttlePercent(-0.68F));
        assertEquals(-100, DriveableControlPhysics.throttlePercent(-1F));
        assertEquals(100, DriveableControlPhysics.throttlePercent(4F));
        assertEquals(-100, DriveableControlPhysics.throttlePercent(-4F));
        assertEquals(0, DriveableControlPhysics.throttlePercent(Float.NaN));
    }

    @Test
    void enginePitchUsesThrottleMagnitudeAcrossTheConfiguredRange()
    {
        assertEquals(0.6F, DriveableControlPhysics.engineSoundPitch(0F, 0.8F, 0.4F), EPSILON);
        assertEquals(1F, DriveableControlPhysics.engineSoundPitch(0.5F, 0.8F, 0.4F), EPSILON);
        assertEquals(1.4F, DriveableControlPhysics.engineSoundPitch(1F, 0.8F, 0.4F), EPSILON);
        assertEquals(0.92F, DriveableControlPhysics.engineSoundPitch(-1F, 0.8F, 0.4F), EPSILON);
        assertEquals(0.76F, DriveableControlPhysics.engineSoundPitch(-0.5F, 0.8F, 0.4F), EPSILON);
        assertEquals(1F, DriveableControlPhysics.engineSoundPitch(1F, 0F, 0.4F), EPSILON);
    }

    @Test
    void brakingBleedsThrottleTowardNeutralWithoutCrossingIt()
    {
        assertEquals(0.92F, DriveableControlPhysics.brakedThrottle(1F, 0.08F), EPSILON);
        assertEquals(-0.12F, DriveableControlPhysics.brakedThrottle(-0.2F, 0.08F), EPSILON);
        assertEquals(0F, DriveableControlPhysics.brakedThrottle(0.04F, 0.08F), EPSILON);
        assertEquals(0F, DriveableControlPhysics.brakedThrottle(-0.04F, 0.08F), EPSILON);
    }

    @Test
    void appliesDirectionalAndWaterPropulsionAfterNormalizingInput()
    {
        assertEquals(0.6F, DriveableControlPhysics.directionalPropulsion(1F, 0.6F, 0.4F, 0.2F, false), EPSILON);
        assertEquals(0.2F, DriveableControlPhysics.directionalPropulsion(1F, 0.6F, 0.4F, 0.2F, true), EPSILON);
        assertEquals(-0.2F, DriveableControlPhysics.directionalPropulsion(-0.5F, 0.6F, 0.4F, 0.2F, false), EPSILON);
    }

    @Test
    void heldLegacySteeringConvergesInsteadOfJumpingToTwentyDegrees()
    {
        float control = 0F;
        for (int tick = 0; tick < 100; tick++)
            control = DriveableControlPhysics.dampedControl(control, 1F, 1F);

        assertEquals(9F, control, 0.001F);
        assertEquals(8.1F, DriveableControlPhysics.dampedControl(control, 0F, 1F), 0.001F);
    }

    @Test
    void oneSurvivingTrackConvertsThrottleToHalfSpeedRotation()
    {
        assertEquals(-10F, DriveableControlPhysics.singleTrackTurnControl(1F, 0F, false,
            true, false), EPSILON);
        assertEquals(10F, DriveableControlPhysics.singleTrackTurnControl(1F, 0F, false,
            false, true), EPSILON);
        assertEquals(10F, DriveableControlPhysics.singleTrackTurnControl(-1F, 0F, false,
            true, false), EPSILON);
        assertEquals(-10F, DriveableControlPhysics.singleTrackTurnControl(-1F, 0F, false,
            false, true), EPSILON);
    }

    @Test
    void steeringKeepsItsDirectionWithEitherSurvivingTrack()
    {
        assertEquals(4.5F, DriveableControlPhysics.singleTrackTurnControl(0.8F, 9F, true,
            true, false), EPSILON);
        assertEquals(4.5F, DriveableControlPhysics.singleTrackTurnControl(0.8F, 9F, true,
            false, true), EPSILON);
        assertEquals(-4.5F, DriveableControlPhysics.singleTrackTurnControl(-0.8F, -9F, true,
            true, false), EPSILON);
    }

    @Test
    void zeroOrTwoSurvivingTracksDoNotUseTheDamagedTrackControl()
    {
        assertEquals(0F, DriveableControlPhysics.singleTrackTurnControl(1F, 9F, true,
            true, true), EPSILON);
        assertEquals(0F, DriveableControlPhysics.singleTrackTurnControl(1F, 9F, true,
            false, false), EPSILON);
    }

    @Test
    void damageReducesAccelerationAndMaximumControlThrottle()
    {
        assertEquals(1F, DriveableControlPhysics.damagedThrottleLimit(0F), EPSILON);
        assertEquals(0.2F, DriveableControlPhysics.damagedThrottleLimit(0.8F), EPSILON);
        assertEquals(0.1F, DriveableControlPhysics.damagedAccelerationMultiplier(1F), EPSILON);
    }

    @Test
    void fuelLoadsPreserveLegacyPerTickScaling()
    {
        assertEquals(8F, DriveableControlPhysics.vehicleFuelLoad(1F, 4), EPSILON);
        assertEquals(3.6F, DriveableControlPhysics.aircraftFuelLoad(1F, 8F, 1F), EPSILON);
    }

    @Test
    void vehicleThrottleLeverLatchesUntilAPedalTakesControlBack()
    {
        boolean fixed = DriveableControlPhysics.fixedVehicleThrottle(false, true, false,
            DriveableInput.THROTTLE_INCREASE);
        assertTrue(fixed);
        assertTrue(DriveableControlPhysics.fixedVehicleThrottle(fixed, true, false, 0));
        assertFalse(DriveableControlPhysics.fixedVehicleThrottle(fixed, true, false, DriveableInput.FORWARD));
        assertFalse(DriveableControlPhysics.fixedVehicleThrottle(fixed, true, true, 0));
        assertFalse(DriveableControlPhysics.fixedVehicleThrottle(fixed, false, false, 0));
    }
}
