package com.flansmodultimate.common.driveables.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Derived fixed-wing performance, including the playability clamps. */
class AircraftPerformancePhysicsTest
{
    // Supermarine Spitfire Mk V: 2890 kg, 993 kW, 635 km/h, 22.48 m^2, 11.23 m span.
    private static final double MASS_KG = 2890D;
    private static final double POWER_KW = 993D;
    private static final double WING_AREA = 22.48D;
    private static final double SPAN_M = 11.23D;
    private static final double TERMINAL_MS = 635D / 3.6D;

    @Test
    void jetThrustIsAirspeedIndependent()
    {
        double slow = AircraftPerformancePhysics.thrustNewtons(79.6D, 0D, 10D, TERMINAL_MS);
        double fast = AircraftPerformancePhysics.thrustNewtons(79.6D, 0D, 300D, TERMINAL_MS);
        assertEquals(79_600D, slow, 1.0E-6D);
        assertEquals(slow, fast, 1.0E-6D);
    }

    @Test
    void theDefaultThrottleResponseMetersEnginePowerDirectly()
    {
        // Exponent one is the physical value: the lever is linear in power, so
        // half throttle really is half of the rated output.
        assertEquals(0.5D, AircraftPerformancePhysics.throttleThrustFactor(0.5D, 1D), 1.0E-9D);
        assertEquals(0.1D, AircraftPerformancePhysics.throttleThrustFactor(0.1D, 1D), 1.0E-9D);
        assertEquals(1D, AircraftPerformancePhysics.throttleThrustFactor(1D, 1D), 1.0E-9D);
    }

    @Test
    void anExponentOfThreeTradesPowerRealismForASpeedLinearLever()
    {
        // Half throttle then produces only an eighth of rated power, which is
        // what makes the resulting top-speed fraction come out linear.
        assertEquals(0.125D, AircraftPerformancePhysics.throttleThrustFactor(0.5D, 3D), 1.0E-9D);
        assertEquals(0.001D, AircraftPerformancePhysics.throttleThrustFactor(0.1D, 3D), 1.0E-9D);
        assertEquals(1D, AircraftPerformancePhysics.throttleThrustFactor(1D, 3D), 1.0E-9D);
    }

    @Test
    void equilibriumSpeedFollowsTheCubeRootOfTheThrottleFactor()
    {
        // Thrust balances drag at v = Vmax * cbrt(throttle^n). At n = 1 that is
        // the cube-root curve real aircraft follow; at n = 3 it collapses to a
        // straight line, which is the whole point of the arcade preset.
        for (double throttle : new double[] {0.1D, 0.25D, 0.5D, 0.75D})
        {
            assertEquals(Math.cbrt(throttle), equilibriumSpeedFraction(throttle, 1D), 1.0E-3D);
            assertEquals(throttle, equilibriumSpeedFraction(throttle, 3D), 1.0E-3D);
        }
        assertEquals(1D, equilibriumSpeedFraction(1D, 1D), 1.0E-3D);
        assertEquals(1D, equilibriumSpeedFraction(1D, 3D), 1.0E-3D);
    }

    /** Integrates a propeller aircraft to its steady speed, as a fraction of terminal speed. */
    private static double equilibriumSpeedFraction(double throttle, double responseExponent)
    {
        double referenceThrust = AircraftPerformancePhysics.thrustNewtons(0D, POWER_KW, TERMINAL_MS, TERMINAL_MS);
        double factor = AircraftPerformancePhysics.throttleThrustFactor(throttle, responseExponent);
        double speed = 0.001D;
        for (int tick = 0; tick < 200_000; tick++)
        {
            double thrust = AircraftPerformancePhysics.thrustNewtons(0D, POWER_KW, speed, TERMINAL_MS) * factor;
            double acceleration = AircraftPerformancePhysics.accelerationMs2(thrust, MASS_KG, speed,
                TERMINAL_MS, referenceThrust);
            speed = Math.max(0D, Math.min(TERMINAL_MS, speed + acceleration / 20D));
        }
        return speed / TERMINAL_MS;
    }

    @Test
    void degenerateThrottleResponsesFallBackToTheLinearPowerLever()
    {
        assertEquals(0.4D, AircraftPerformancePhysics.throttleThrustFactor(0.4D, 0D), 1.0E-9D);
        assertEquals(0.4D, AircraftPerformancePhysics.throttleThrustFactor(0.4D, -2D), 1.0E-9D);
        assertEquals(0.4D, AircraftPerformancePhysics.throttleThrustFactor(0.4D, Double.NaN), 1.0E-9D);
        // A wound-up lever never asks for more than full thrust.
        assertEquals(1D, AircraftPerformancePhysics.throttleThrustFactor(1.5D, 1D), 1.0E-9D);
        assertEquals(0D, AircraftPerformancePhysics.throttleThrustFactor(-0.5D, 1D), 1.0E-9D);
        assertEquals(0D, AircraftPerformancePhysics.throttleThrustFactor(Double.NaN, 1D), 1.0E-9D);
    }

    @Test
    void propellerThrustFallsWithAirspeed()
    {
        double slow = AircraftPerformancePhysics.thrustNewtons(0D, POWER_KW, 40D, TERMINAL_MS);
        double fast = AircraftPerformancePhysics.thrustNewtons(0D, POWER_KW, 160D, TERMINAL_MS);
        assertTrue(slow > fast, "a propeller loses thrust as the aircraft speeds up");
        assertTrue(Double.isFinite(AircraftPerformancePhysics.thrustNewtons(0D, POWER_KW, 0D, TERMINAL_MS)),
            "standstill must be launch limited, not infinite");
    }

    @Test
    void authoredThrustWinsOverPowerWhenBothArePresent()
    {
        assertEquals(79_600D,
            AircraftPerformancePhysics.thrustNewtons(79.6D, POWER_KW, 100D, TERMINAL_MS), 1.0E-6D);
    }

    @Test
    void accelerationBalancesToZeroAtTheAuthoredTopSpeed()
    {
        double reference = AircraftPerformancePhysics.thrustNewtons(0D, POWER_KW, TERMINAL_MS, TERMINAL_MS);
        assertEquals(0D, AircraftPerformancePhysics.accelerationMs2(
            reference, MASS_KG, TERMINAL_MS, TERMINAL_MS, reference), 1.0E-9D);
    }

    @Test
    void referenceSpeedRisesWithWingLoading()
    {
        double light = AircraftPerformancePhysics.referenceSpeedMs(2000D, WING_AREA);
        double heavy = AircraftPerformancePhysics.referenceSpeedMs(6000D, WING_AREA);
        assertTrue(heavy > light, "a more heavily loaded wing needs more speed to fly");
        // 128.6 kg/m2 of wing loading gives 38.3 m/s, about 138 km/h, which is
        // the right order for a Spitfire's clean stall.
        assertEquals(38.34D, AircraftPerformancePhysics.referenceSpeedMs(MASS_KG, WING_AREA), 0.05D);
    }

    @Test
    void referenceSpeedIsClampedSoNoAircraftBecomesUnflyable()
    {
        // A very heavily loaded, slow aircraft would otherwise stall above its own top speed.
        double unclamped = AircraftPerformancePhysics.referenceSpeedMs(20_000D, 10D);
        double clamped = AircraftPerformancePhysics.clampedReferenceSpeedMs(20_000D, 10D, 100D, 1D);
        assertTrue(clamped < unclamped);
        assertEquals(100D * VehiclePhysicsConstants.MAX_REFERENCE_SPEED_FRACTION, clamped, 1.0E-9D);
    }

    @Test
    void configurableReferenceSpeedScaleShortensTakeoffWithoutChangingAircraftRelationships()
    {
        double physical = AircraftPerformancePhysics.referenceSpeedMs(MASS_KG, WING_AREA);
        double arcade = AircraftPerformancePhysics.clampedReferenceSpeedMs(
            MASS_KG, WING_AREA, TERMINAL_MS, 0.5D);
        assertEquals(physical * 0.5D, arcade, 1.0E-9D);

        double heavier = AircraftPerformancePhysics.clampedReferenceSpeedMs(
            MASS_KG * 2D, WING_AREA, TERMINAL_MS, 0.5D);
        assertTrue(heavier > arcade);
    }

    @Test
    void liftReachesWeightAtTheReferenceSpeedAndFallsOffSmoothlyBelowIt()
    {
        double reference = 42D;
        assertEquals(1D, AircraftPerformancePhysics.liftFraction(reference, reference), 1.0E-9D);
        assertEquals(0.25D, AircraftPerformancePhysics.liftFraction(reference * 0.5D, reference), 1.0E-9D);
        assertEquals(0D, AircraftPerformancePhysics.liftFraction(0D, reference), 1.0E-9D);
        // No hard threshold: lift is continuous through the reference speed.
        assertTrue(AircraftPerformancePhysics.liftFraction(reference * 0.99D, reference) > 0.97D);
    }

    @Test
    void climbRateConstrainsExcessLiftWithoutSettingVerticalVelocity()
    {
        double constrained = AircraftPerformancePhysics.maxExcessLiftFraction(17D, TERMINAL_MS);
        double fastClimber = AircraftPerformancePhysics.maxExcessLiftFraction(60D, TERMINAL_MS);
        assertTrue(fastClimber > constrained, "a better climber may convert more excess lift");
        assertTrue(constrained > 0D && constrained <= 1D);
        // Absent climb rate leaves a permissive default rather than blocking climb.
        assertTrue(AircraftPerformancePhysics.maxExcessLiftFraction(0D, TERMINAL_MS) > 0D);
    }

    @Test
    void rollResponseFallsAsSpanAndMassRise()
    {
        float light = AircraftPerformancePhysics.rollInertiaFactor(9D, 1500D);
        float spitfire = AircraftPerformancePhysics.rollInertiaFactor(11.23D, MASS_KG);
        float bomber = AircraftPerformancePhysics.rollInertiaFactor(31D, 30_000D);
        assertTrue(light > spitfire);
        assertTrue(spitfire > bomber);
        assertTrue(bomber >= VehiclePhysicsConstants.MIN_ROLL_INERTIA_FACTOR,
            "control must never disappear entirely");
        assertTrue(light <= VehiclePhysicsConstants.MAX_ROLL_INERTIA_FACTOR);
    }

    @Test
    void controlAuthorityNeverCollapsesToZeroAtHighSpeedTheWayTheLegacyCurveDid()
    {
        double terminal = VehiclePhysicsUnits.kmhToBlocksPerTick(635D, 1D);
        assertEquals(0F, AircraftPerformancePhysics.normalizedControlAuthority(0D, terminal), 1.0E-6F);
        assertEquals(1F, AircraftPerformancePhysics.normalizedControlAuthority(terminal * 0.35D, terminal), 1.0E-6F);
        float atTopSpeed = AircraftPerformancePhysics.normalizedControlAuthority(terminal, terminal);
        assertTrue(atTopSpeed > 0.4F, "a fast aircraft must stay controllable, was " + atTopSpeed);
        assertTrue(atTopSpeed < 1F, "control surfaces still load up at speed");
    }

    @Test
    void degenerateInputsProduceZeroRatherThanNaN()
    {
        assertEquals(0D, AircraftPerformancePhysics.thrustNewtons(0D, 0D, 100D, TERMINAL_MS));
        assertEquals(0D, AircraftPerformancePhysics.accelerationMs2(1000D, 0D, 100D, TERMINAL_MS, 1000D));
        assertEquals(0D, AircraftPerformancePhysics.referenceSpeedMs(MASS_KG, 0D));
        assertEquals(0D, AircraftPerformancePhysics.liftFraction(100D, 0D));
        assertEquals(1F, AircraftPerformancePhysics.rollInertiaFactor(0D, MASS_KG));
        assertEquals(0F, AircraftPerformancePhysics.normalizedControlAuthority(10D, 0D));
    }

    @Test
    void inducedDragDominatesWellBelowTopSpeed()
    {
        double reference = AircraftPerformancePhysics.thrustNewtons(0D, POWER_KW, TERMINAL_MS, TERMINAL_MS);
        double cruise = 300D / 3.6D;
        double withSpan = AircraftPerformancePhysics.dragNewtons(cruise, MASS_KG, SPAN_M, TERMINAL_MS, reference);
        double parasiticOnly = AircraftPerformancePhysics.dragNewtons(cruise, MASS_KG, 0D, TERMINAL_MS, reference);
        assertTrue(withSpan > parasiticOnly * 1.5D,
            "the wing pays a large induced-drag bill at manoeuvring speed");
    }

    @Test
    void totalDragStillBalancesThrustAtTheAuthoredTopSpeed()
    {
        double reference = AircraftPerformancePhysics.thrustNewtons(0D, POWER_KW, TERMINAL_MS, TERMINAL_MS);
        assertEquals(reference,
            AircraftPerformancePhysics.dragNewtons(TERMINAL_MS, MASS_KG, SPAN_M, TERMINAL_MS, reference), 1.0E-6D);
        assertEquals(0D, AircraftPerformancePhysics.accelerationMs2(
            reference, MASS_KG, TERMINAL_MS, TERMINAL_MS, reference, SPAN_M, 1D), 1.0E-9D);
    }

    @Test
    void aCoastingAircraftAlwaysBleedsSpeed()
    {
        double reference = AircraftPerformancePhysics.thrustNewtons(0D, POWER_KW, TERMINAL_MS, TERMINAL_MS);
        double cruise = 300D / 3.6D;
        double coasting = AircraftPerformancePhysics.accelerationMs2(0D, MASS_KG, cruise,
            TERMINAL_MS, reference, SPAN_M, 0D);
        assertTrue(coasting <= -VehiclePhysicsConstants.MIN_AIRCRAFT_COAST_DECELERATION_MS2,
            "closing the throttle at 300 km/h must decelerate at least at the coasting floor");
        // A stationary aircraft is untouched by the floor, so it can still taxi.
        assertEquals(0D, AircraftPerformancePhysics.accelerationMs2(0D, MASS_KG, 0D,
            TERMINAL_MS, reference, SPAN_M, 0D), 1.0E-9D);
    }

    @Test
    void aVeryShortSpanAirframeKeepsAWellBehavedTopSpeed()
    {
        double reference = AircraftPerformancePhysics.thrustNewtons(0D, POWER_KW, TERMINAL_MS, TERMINAL_MS);
        assertEquals(reference,
            AircraftPerformancePhysics.dragNewtons(TERMINAL_MS, 12_000D, 3D, TERMINAL_MS, reference), 1.0E-6D);
    }
}
