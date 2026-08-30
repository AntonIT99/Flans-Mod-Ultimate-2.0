package com.flansmodultimate.common.driveables.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The propulsion model must be stable at 20 Hz, must not snap to top speed, and
 * must settle exactly at the authored top speed rather than somewhere near it.
 */
class GroundPropulsionPhysicsTest
{
    // A Humvee: 2400 kg, 140 kW, 113 km/h.
    private static final double POWER_W = 140_000D;
    private static final double MASS_KG = 2400D;
    private static final double TERMINAL_MS = 113D / 3.6D;

    @Test
    void accelerationIsZeroExactlyAtTheAuthoredTopSpeed()
    {
        assertEquals(0D, GroundPropulsionPhysics.accelerationMs2(
            TERMINAL_MS, POWER_W, MASS_KG, TERMINAL_MS, 1D), 1.0E-9D);
    }

    @Test
    void accelerationFallsMonotonicallyWithSpeed()
    {
        double previous = Double.MAX_VALUE;
        for (double speed = 1D; speed <= TERMINAL_MS; speed += 1D)
        {
            double acceleration = GroundPropulsionPhysics.accelerationMs2(speed, POWER_W, MASS_KG, TERMINAL_MS, 1D);
            assertTrue(acceleration <= previous + 1.0E-9D,
                "acceleration must not rise with speed (at " + speed + " m/s)");
            previous = acceleration;
        }
    }

    @Test
    void accelerationBeyondTopSpeedIsNegativeSoTheVehicleIsPulledBack()
    {
        assertTrue(GroundPropulsionPhysics.accelerationMs2(
            TERMINAL_MS * 1.5D, POWER_W, MASS_KG, TERMINAL_MS, 1D) < 0D);
    }

    @Test
    void standstillForceIsLaunchLimitedRatherThanInfinite()
    {
        double atRest = GroundPropulsionPhysics.accelerationMs2(0D, POWER_W, MASS_KG, TERMINAL_MS, 1D);
        assertTrue(Double.isFinite(atRest));
        assertTrue(atRest > 0D);
        assertTrue(atRest <= VehiclePhysicsConstants.MAX_DERIVED_ACCELERATION_MS2);
    }

    @Test
    void powerToWeightDrivesRelativeDifferencesBetweenVehicles()
    {
        // Same top speed, four times the power-to-weight: the sports car must
        // out-accelerate the truck at every speed.
        double truck = GroundPropulsionPhysics.accelerationMs2(10D, 100_000D, 8000D, TERMINAL_MS, 1D);
        double car = GroundPropulsionPhysics.accelerationMs2(10D, 200_000D, 1400D, TERMINAL_MS, 1D);
        assertTrue(car > truck * 4D, "expected the lighter, more powerful vehicle to pull far harder");
    }

    @Test
    void integrationConvergesOnTopSpeedWithoutSnappingOrOvershooting()
    {
        double terminalBlocksPerTick = VehiclePhysicsUnits.kmhToBlocksPerTick(113D, 1D);
        double speed = 0D;
        int ticksToHalf = -1;
        for (int tick = 0; tick < 2000; tick++)
        {
            double acceleration = GroundPropulsionPhysics.accelerationBlocksPerTickSquared(
                speed, POWER_W, MASS_KG, terminalBlocksPerTick, 1D);
            double deceleration = GroundPropulsionPhysics.decelerationBlocksPerTickSquared(
                speed, POWER_W, MASS_KG, terminalBlocksPerTick, false);
            speed = GroundPropulsionPhysics.approach(speed, terminalBlocksPerTick, acceleration, deceleration);
            assertTrue(speed <= terminalBlocksPerTick + 1.0E-9D, "must never overshoot the authored top speed");
            if (ticksToHalf < 0 && speed >= terminalBlocksPerTick * 0.5D)
                ticksToHalf = tick;
        }
        assertTrue(ticksToHalf > 10, "must not snap to speed; took only " + ticksToHalf + " ticks to reach half");
        assertEquals(terminalBlocksPerTick, speed, terminalBlocksPerTick * 0.02D);
    }

    @Test
    void hellcatScaleRuntimeIntegrationReachesAuthoredSpeedWithoutLegacyDrag()
    {
        double terminal = VehiclePhysicsUnits.kmhToBlocksPerTick(89D, 1D);
        double powerW = VehiclePhysicsUnits.hpToKw(400D) * VehiclePhysicsUnits.WATTS_PER_KILOWATT;
        double massKg = 17_036D;
        double speed = 0D;
        double oldDoubleDraggedSpeed = 0D;
        for (int tick = 0; tick < 4000; tick++)
        {
            speed = integrateTick(speed, terminal, powerW, massKg,
                GroundPropulsionPhysics.postIntegrationHorizontalDrag(true, 1F));
            oldDoubleDraggedSpeed = integrateTick(oldDoubleDraggedSpeed, terminal, powerW, massKg,
                GroundPropulsionPhysics.postIntegrationHorizontalDrag(false, 1F));
        }

        assertEquals(89D, VehiclePhysicsUnits.blocksPerTickToKmh(speed), 1D);
        assertTrue(VehiclePhysicsUnits.blocksPerTickToKmh(oldDoubleDraggedSpeed) < 45D,
            "the regression fixture must reproduce the much lower legacy-drag equilibrium");
    }

    @Test
    void postIntegrationDragIsExclusiveToLegacyGroundPropulsion()
    {
        assertEquals(1D, GroundPropulsionPhysics.postIntegrationHorizontalDrag(true, 1F), 1.0E-9D);
        assertEquals(1D, GroundPropulsionPhysics.postIntegrationHorizontalDrag(true, 12F), 1.0E-9D);
        assertEquals(0.98D, GroundPropulsionPhysics.postIntegrationHorizontalDrag(false, 1F), 1.0E-9D);
        assertEquals(0.97D, GroundPropulsionPhysics.postIntegrationHorizontalDrag(false, 2F), 1.0E-9D);
        assertEquals(0.75D, GroundPropulsionPhysics.postIntegrationHorizontalDrag(false, 100F), 1.0E-9D);
    }

    @Test
    void coastingDecaysTowardZeroAndBrakingIsFaster()
    {
        double terminal = VehiclePhysicsUnits.kmhToBlocksPerTick(113D, 1D);
        double coasting = terminal;
        double braked = terminal;
        for (int tick = 0; tick < 40; tick++)
        {
            coasting = GroundPropulsionPhysics.approach(coasting, 0D, 0D,
                GroundPropulsionPhysics.decelerationBlocksPerTickSquared(coasting, POWER_W, MASS_KG, terminal, false));
            braked = GroundPropulsionPhysics.approach(braked, 0D, 0D,
                GroundPropulsionPhysics.decelerationBlocksPerTickSquared(braked, POWER_W, MASS_KG, terminal, true));
        }
        assertTrue(braked < coasting, "braking must shed speed faster than coasting");
        assertTrue(coasting < terminal);
        assertTrue(braked >= 0D);
    }

    @Test
    void approachNeverOvershootsInEitherDirection()
    {
        assertEquals(1D, GroundPropulsionPhysics.approach(0D, 1D, 5D, 5D));
        assertEquals(-1D, GroundPropulsionPhysics.approach(0D, -1D, 5D, 5D));
        assertEquals(0.5D, GroundPropulsionPhysics.approach(0D, 1D, 0.5D, 5D));
    }

    @Test
    void reversingThroughZeroUsesTheDecelerationBudget()
    {
        // Demanding reverse while moving forward is braking, not accelerating.
        assertEquals(0.9D, GroundPropulsionPhysics.approach(1D, -1D, 5D, 0.1D), 1.0E-9D);
    }

    @Test
    void degenerateInputsProduceZeroRatherThanNaN()
    {
        assertEquals(0D, GroundPropulsionPhysics.accelerationMs2(10D, 0D, MASS_KG, TERMINAL_MS, 1D));
        assertEquals(0D, GroundPropulsionPhysics.accelerationMs2(10D, POWER_W, 0D, TERMINAL_MS, 1D));
        assertEquals(0D, GroundPropulsionPhysics.accelerationMs2(10D, POWER_W, MASS_KG, 0D, 1D));
        assertEquals(0D, GroundPropulsionPhysics.accelerationMs2(Double.NaN, POWER_W, MASS_KG, TERMINAL_MS, 1D),
            VehiclePhysicsConstants.MAX_DERIVED_ACCELERATION_MS2);
        assertEquals(0D, GroundPropulsionPhysics.approach(Double.NaN, 1D, 1D, 1D));
    }

    @Test
    void aDamagedOrLowGripDriveLayoutReducesLaunchForceButNotTopSpeed()
    {
        // A heavy vehicle, so the launch limit binds before the acceleration
        // ceiling does and the traction factor is actually visible.
        double truckPower = 100_000D;
        double truckMass = 8000D;
        double gripped = GroundPropulsionPhysics.accelerationMs2(0.5D, truckPower, truckMass, TERMINAL_MS, 1D);
        double slipping = GroundPropulsionPhysics.accelerationMs2(0.5D, truckPower, truckMass, TERMINAL_MS, 0.5D);
        assertTrue(slipping < gripped, "lost grip must cost launch force");
        // Halving traction halves the tractive force; the two differ only by the
        // resistance term, which is unaffected by grip.
        assertEquals(gripped * 0.5D, slipping, 1.0E-3D);
        // At the top speed both still balance to zero: traction shapes launch, not terminal speed.
        assertEquals(0D, GroundPropulsionPhysics.accelerationMs2(TERMINAL_MS, truckPower, truckMass, TERMINAL_MS, 0.5D),
            1.0E-9D);
    }

    @Test
    void theAccelerationCeilingStandsInForTheGearingAndGripWeDoNotSimulate()
    {
        // A light, powerful vehicle would otherwise launch at several g, which no
        // real drivetrain or tyre could deliver.
        assertEquals(VehiclePhysicsConstants.MAX_DERIVED_ACCELERATION_MS2,
            GroundPropulsionPhysics.accelerationMs2(0.5D, POWER_W, MASS_KG, TERMINAL_MS, 1D), 1.0E-9D);
    }

    private static double integrateTick(double speed, double terminal, double powerW,
                                        double massKg, double postIntegrationDrag)
    {
        double acceleration = GroundPropulsionPhysics.accelerationBlocksPerTickSquared(
            speed, powerW, massKg, terminal, 1.1D);
        double deceleration = GroundPropulsionPhysics.decelerationBlocksPerTickSquared(
            speed, powerW, massKg, terminal, false);
        return GroundPropulsionPhysics.approach(speed, terminal, acceleration, deceleration)
            * postIntegrationDrag;
    }
}
