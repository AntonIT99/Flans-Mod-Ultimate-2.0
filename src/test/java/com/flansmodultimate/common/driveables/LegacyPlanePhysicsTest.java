package com.flansmodultimate.common.driveables;

import com.flansmodultimate.common.driveables.physics.VehiclePhysicsConstants;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LegacyPlanePhysicsTest
{
    private static final float EPSILON = 1.0E-6F;

    @Test
    void flapInputAccumulatesAndReturnsLikeLegacy()
    {
        float flap = 0F;
        for (int tick = 0; tick < 100; tick++)
            flap = LegacyPlanePhysics.flap(flap, 1F);
        assertEquals(9F, flap, 0.001F);
        assertEquals(8.1F, LegacyPlanePhysics.flap(flap, 0F), 0.001F);
    }

    @Test
    void mouseAndKeyboardUseTheSameStickAuthorityAndCanBeCombined()
    {
        assertEquals(1F, LegacyPlanePhysics.combinedControlInput(
            LegacyPlanePhysics.MAX_FLAP_ANGLE, 0F), EPSILON);
        assertEquals(1F, LegacyPlanePhysics.combinedControlInput(0F, 1F), EPSILON);
        assertEquals(0F, LegacyPlanePhysics.combinedControlInput(
            LegacyPlanePhysics.MAX_FLAP_ANGLE, -1F), EPSILON,
            "an opposite key must remain fully effective in mouse mode");
        assertEquals(1F, LegacyPlanePhysics.combinedControlInput(
            LegacyPlanePhysics.MAX_FLAP_ANGLE * 0.5F, 1F), EPSILON,
            "combined inputs stay bounded to one stick");

        float mouseFlap = 0F;
        float keyboardFlap = 0F;
        for (int tick = 0; tick < 100; tick++)
        {
            mouseFlap = LegacyPlanePhysics.flap(mouseFlap,
                LegacyPlanePhysics.combinedControlInput(LegacyPlanePhysics.MAX_FLAP_ANGLE, 0F));
            keyboardFlap = LegacyPlanePhysics.flap(keyboardFlap,
                LegacyPlanePhysics.combinedControlInput(0F, 1F));
        }
        assertEquals(keyboardFlap, mouseFlap, EPSILON,
            "mouse roll must not exceed the equivalent key-bound roll");
    }

    @Test
    void fixedWingControlAuthorityDependsOnAirspeed()
    {
        var stopped = LegacyPlanePhysics.controlRates(EnumPlaneMode.PLANE, 0F, 1F, 1F,
            0F, 10F, 10F, 2F, 2F, 3F, 3F, 4F, 4F);
        var flying = LegacyPlanePhysics.controlRates(EnumPlaneMode.PLANE, 1F, 1F, 1F,
            0F, 10F, 10F, 2F, 2F, 3F, 3F, 4F, 4F);
        assertEquals(0F, stopped.pitch(), EPSILON);
        assertEquals(3.75F, flying.pitch(), EPSILON);
        assertEquals(5F, flying.roll(), EPSILON);
    }

    @Test
    void legacyFixedWingControlsReachFullAuthorityAtLowSpeed()
    {
        var half = LegacyPlanePhysics.controlRates(EnumPlaneMode.PLANE,
            LegacyPlanePhysics.FULL_CONTROL_AUTHORITY_SPEED * 0.5F, 0F, 1F,
            0F, 10F, 10F, 1F, 1F, 1F, 1F, 1F, 1F);
        var full = LegacyPlanePhysics.controlRates(EnumPlaneMode.PLANE,
            LegacyPlanePhysics.FULL_CONTROL_AUTHORITY_SPEED, 0F, 1F,
            0F, 10F, 10F, 1F, 1F, 1F, 1F, 1F, 1F);
        assertEquals(full.yaw() * 0.5F, half.yaw(), EPSILON);
        assertEquals(full.pitch() * 0.5F, half.pitch(), EPSILON);
        assertEquals(full.roll() * 0.5F, half.roll(), EPSILON);
        assertEquals(1.25F, full.pitch(), EPSILON);
        assertEquals(1.25F, full.roll(), EPSILON);
    }

    @Test
    void legacyPowerIsConvertedToPerTickThrustOnce()
    {
        assertEquals(0.09F, LegacyPlanePhysics.thrust(1F, 8F, 0F, 0F, 1F, false), EPSILON);
        assertEquals(0.015F, LegacyPlanePhysics.thrust(-1F, 8F, 0.5F, 0F, 1F, false), EPSILON);
        assertEquals(0.95F, LegacyPlanePhysics.drag(1F), EPSILON);
    }

    @Test
    void angularMomentumApproachesControlInsteadOfSnapping()
    {
        assertEquals(1F, LegacyPlanePhysics.approachMomentum(0F, 8F), EPSILON);
        assertEquals(7F, LegacyPlanePhysics.approachMomentum(8F, 0F), EPSILON);
    }

    @Test
    void wheelSupportReleasesOnlyForARealFixedWingTakeoff()
    {
        assertTrue(LegacyPlanePhysics.isLiftingOff(EnumPlaneMode.PLANE, 0.6D, 0.5F, 0.15D, 0.04D));
        assertFalse(LegacyPlanePhysics.isLiftingOff(EnumPlaneMode.PLANE, 0.4D, 0.5F, 0.15D, 0.04D));
        assertFalse(LegacyPlanePhysics.isLiftingOff(EnumPlaneMode.PLANE, 0.6D, 0.5F, -0.15D, 0.04D));
        assertFalse(LegacyPlanePhysics.isLiftingOff(EnumPlaneMode.HELI, 0.6D, 0.5F, 0.15D, 0.04D));
    }

    @Test
    void aLandedPlaneReturnsGraduallyToItsRestingAttitude()
    {
        assertEquals(9.75F, LegacyPlanePhysics.landingAttitudeAngle(10F, 0F), EPSILON);
        assertEquals(-9.75F, LegacyPlanePhysics.landingAttitudeAngle(-10F, 0F), EPSILON);
        assertEquals(2F, LegacyPlanePhysics.landingAttitudeAngle(2.1F, 2F), EPSILON,
            "the recovery must stop at the resting angle rather than overshoot");
    }

    @Test
    void verticalDescentDoesNotCountAsForwardAirspeed()
    {
        assertEquals(0D, LegacyPlanePhysics.forwardAirspeed(0D, -1D, 0D, 1D, 0D, 0D), EPSILON);
        assertEquals(1D, LegacyPlanePhysics.forwardAirspeed(1D, -4D, 0D, 1D, 0D, 0D), EPSILON);
        assertEquals(1D, LegacyPlanePhysics.forwardAirspeed(-1D, 0D, 0D, 2D, 0D, 0D), EPSILON,
            "reverse airflow still has dynamic pressure over the wing");
    }

    @Test
    void passiveRollLevelingIsSlowProportionalAndYieldsToThePilot()
    {
        assertEquals(LegacyPlanePhysics.PASSIVE_ROLL_LEVEL_MAX_DEG_PER_TICK,
            LegacyPlanePhysics.passiveRollLevelingRate(45F, false, 1F, 1F), EPSILON);
        assertEquals(0.03F, LegacyPlanePhysics.passiveRollLevelingRate(3F, false, 1F, 1F), EPSILON,
            "small banks ease gently rather than using the capped rate");
        assertEquals(-LegacyPlanePhysics.PASSIVE_ROLL_LEVEL_MAX_DEG_PER_TICK,
            LegacyPlanePhysics.passiveRollLevelingRate(-45F, false, 1F, 1F), EPSILON);
        assertEquals(0F, LegacyPlanePhysics.passiveRollLevelingRate(45F, true, 1F, 1F), EPSILON);
        assertEquals(0F, LegacyPlanePhysics.passiveRollLevelingRate(45F, false, 0F, 1F), EPSILON);
    }

    @Test
    void passiveRollLevelingFollowsLiftingSurfaceRollResponse()
    {
        float slow = LegacyPlanePhysics.passiveRollLevelingRate(45F, false, 1F,
            VehiclePhysicsConstants.MIN_ROLL_INERTIA_FACTOR);
        float fallback = LegacyPlanePhysics.passiveRollLevelingRate(45F, false, 1F, 1F);
        float agile = LegacyPlanePhysics.passiveRollLevelingRate(45F, false, 1F,
            VehiclePhysicsConstants.MAX_ROLL_INERTIA_FACTOR);
        assertTrue(slow < fallback);
        assertTrue(fallback < agile);
    }

    @Test
    void bladesStandStillOnStandby()
    {
        assertEquals(0F, LegacyPlanePhysics.propellerStep(0F), EPSILON);
        assertEquals(0F, LegacyPlanePhysics.rotorStep(0F), EPSILON);
    }

    @Test
    void propellerSpinsUpSharplyOffIdleAndIgnoresThrottleSign()
    {
        // 1.5 radians per tick at full throttle, on a throttle^0.4 curve.
        assertEquals(85.9437F, LegacyPlanePhysics.propellerStep(1F), 1.0E-3F);
        assertEquals(65.1331F, LegacyPlanePhysics.propellerStep(0.5F), 1.0E-3F);
        assertEquals(LegacyPlanePhysics.propellerStep(0.5F), LegacyPlanePhysics.propellerStep(-0.5F), EPSILON);
        // A quarter throttle already turns the blades more than half as fast.
        assertTrue(LegacyPlanePhysics.propellerStep(0.25F) > LegacyPlanePhysics.propellerStep(1F) * 0.5F);
    }

    @Test
    void activeEngineKeepsPropellersAtAVisualIdle()
    {
        assertEquals(0F, LegacyPlanePhysics.engineAnimationThrottle(false, 0.7F, 0.08F), EPSILON);
        assertEquals(0.08F, LegacyPlanePhysics.engineAnimationThrottle(true, 0F, 0.08F), EPSILON);
        assertEquals(-0.08F, LegacyPlanePhysics.engineAnimationThrottle(true, -0.03F, 0.08F), EPSILON);
        assertEquals(0.4F, LegacyPlanePhysics.engineAnimationThrottle(true, 0.4F, 0.08F), EPSILON);
    }

    @Test
    void rotorTracksThrottleLinearlyAndFollowsItsSign()
    {
        assertEquals(65.4809F, LegacyPlanePhysics.rotorStep(1F), 1.0E-3F);
        assertEquals(LegacyPlanePhysics.rotorStep(1F) * 0.5F, LegacyPlanePhysics.rotorStep(0.5F), 1.0E-3F);
        assertEquals(-LegacyPlanePhysics.rotorStep(0.5F), LegacyPlanePhysics.rotorStep(-0.5F), EPSILON);
    }

    @Test
    void theDerivedControlRatesRollFasterThanTheyPitchOrYaw()
    {
        // Full stick deflection on all three axes, at full authority, with the
        // default pack modifiers of one.
        float deflection = LegacyPlanePhysics.MAX_FLAP_ANGLE;
        LegacyPlanePhysics.ControlRates rates = LegacyPlanePhysics.derivedControlRates(
            1F, deflection, deflection, deflection, 1F, 1F, 1F, 1F, 1F, 1F);
        assertTrue(rates.roll() > rates.pitch(), "a real aircraft rolls faster than it pitches");
        assertTrue(rates.pitch() > rates.yaw(), "and yaws slowest of all, on the smallest surface");
        // Five degrees per tick is 100 degrees per second, which is where a
        // wartime fighter actually sits; the unscaled model gave half of it.
        assertEquals(5F, rates.roll(), 1.0E-4F);
    }

    @Test
    void derivedControlAuthorityStillScalesTheWholeSetLinearly()
    {
        float deflection = LegacyPlanePhysics.MAX_FLAP_ANGLE;
        LegacyPlanePhysics.ControlRates full = LegacyPlanePhysics.derivedControlRates(
            1F, deflection, deflection, deflection, 1F, 1F, 1F, 1F, 1F, 1F);
        LegacyPlanePhysics.ControlRates half = LegacyPlanePhysics.derivedControlRates(
            0.5F, deflection, deflection, deflection, 1F, 1F, 1F, 1F, 1F, 1F);
        assertEquals(full.roll() * 0.5F, half.roll(), 1.0E-5F);
        assertEquals(full.pitch() * 0.5F, half.pitch(), 1.0E-5F);
        assertEquals(full.yaw() * 0.5F, half.yaw(), 1.0E-5F);
        LegacyPlanePhysics.ControlRates none = LegacyPlanePhysics.derivedControlRates(
            0F, deflection, deflection, deflection, 1F, 1F, 1F, 1F, 1F, 1F);
        assertEquals(0F, none.roll(), 1.0E-6F);
    }

    @Test
    void turbulenceKicksAreAnEighthOfADegreeEitherWay()
    {
        assertEquals(-0.125F, LegacyPlanePhysics.turbulenceKick(0F), EPSILON);
        assertEquals(0F, LegacyPlanePhysics.turbulenceKick(0.5F), EPSILON);
        assertEquals(0.125F, LegacyPlanePhysics.turbulenceKick(1F), EPSILON);
        assertEquals(-0.125F, LegacyPlanePhysics.turbulenceKick(Float.NaN), EPSILON);
    }
}
