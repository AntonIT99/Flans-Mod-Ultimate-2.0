package com.flansmodultimate.common.driveables.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** A gearbox cannot drive against the way the vehicle is still rolling. */
class DriveDirectionInterlockTest
{
    private static final int SHIFT = VehiclePhysicsConstants.DIRECTION_CHANGE_TICKS;
    private static final int CLUTCH = VehiclePhysicsConstants.CLUTCH_ENGAGE_TICKS;
    private static final double SHIFT_SPEED = VehiclePhysicsConstants.DIRECTION_CHANGE_SPEED_MS;

    @Test
    void pullingAwayInTheSelectedGearIsUnaffected()
    {
        DriveDirectionInterlock drivetrain = new DriveDirectionInterlock();
        assertEquals(1D, drivetrain.advance(1D, 0D), 1.0E-9D);
        assertFalse(drivetrain.isShifting());
    }

    @Test
    void reverseDemandWhileRollingForwardTransmitsNothing()
    {
        DriveDirectionInterlock drivetrain = new DriveDirectionInterlock();
        for (int tick = 0; tick < SHIFT * 10; tick++)
        {
            assertEquals(0D, drivetrain.advance(-1D, 5D), 1.0E-9D);
            assertTrue(drivetrain.isShifting());
        }
        assertEquals(1, drivetrain.getGear(), "reverse is never selected while still rolling forward");
    }

    @Test
    void reverseEngagesAfterAShiftPauseAndTheClutchTakesUpProgressively()
    {
        DriveDirectionInterlock drivetrain = new DriveDirectionInterlock();
        for (int tick = 0; tick < SHIFT; tick++)
            assertEquals(0D, drivetrain.advance(-1D, 0D), 1.0E-9D, "still in neutral on tick " + tick);
        assertEquals(-1, drivetrain.getGear());
        assertFalse(drivetrain.isShifting());

        double previous = 0D;
        for (int tick = 0; tick < CLUTCH; tick++)
        {
            double transmission = drivetrain.advance(-1D, 0D);
            assertTrue(transmission > previous, "take-up rises on every tick");
            previous = transmission;
        }
        assertEquals(1D, previous, 1.0E-9D);
        assertTrue(drivetrain.advance(-1D, 0D) <= 1D);
    }

    @Test
    void theShiftPauseRestartsIfTheVehicleIsStillRollingAgainstIt()
    {
        DriveDirectionInterlock drivetrain = new DriveDirectionInterlock();
        for (int tick = 0; tick < SHIFT - 1; tick++)
            drivetrain.advance(-1D, 0D);
        drivetrain.advance(-1D, SHIFT_SPEED * 4D);
        for (int tick = 0; tick < SHIFT - 1; tick++)
            drivetrain.advance(-1D, 0D);
        assertEquals(1, drivetrain.getGear(), "the pause counts from the moment the vehicle settled");
        drivetrain.advance(-1D, 0D);
        assertEquals(-1, drivetrain.getGear());
    }

    @Test
    void creepingBelowTheShiftSpeedOrRollingTheRequestedWayDoesNotBlockTheShift()
    {
        DriveDirectionInterlock creeping = new DriveDirectionInterlock();
        for (int tick = 0; tick < SHIFT; tick++)
            creeping.advance(-1D, SHIFT_SPEED * 0.5D);
        assertEquals(-1, creeping.getGear());

        DriveDirectionInterlock rollingBack = new DriveDirectionInterlock();
        for (int tick = 0; tick < SHIFT; tick++)
            rollingBack.advance(-1D, -3D);
        assertEquals(-1, rollingBack.getGear(), "already rolling backwards, reverse can be selected");
    }

    @Test
    void releasingDemandKeepsTheSelectedGear()
    {
        DriveDirectionInterlock drivetrain = engagedInReverse();
        drivetrain.advance(0D, 0D);
        assertFalse(drivetrain.isShifting(), "no demand is not a shift");
        assertEquals(1D, drivetrain.advance(-1D, 0D), 1.0E-9D, "reverse again needs no new shift");
        assertEquals(0D, drivetrain.advance(1D, 0D), 1.0E-9D, "but drive does");
    }

    @Test
    void degenerateInputsAreTreatedAsNoDemandOrStandstill()
    {
        DriveDirectionInterlock drivetrain = new DriveDirectionInterlock();
        drivetrain.advance(Double.NaN, 5D);
        assertFalse(drivetrain.isShifting());
        for (int tick = 0; tick < SHIFT; tick++)
            drivetrain.advance(-1D, Double.NaN);
        assertEquals(-1, drivetrain.getGear());
    }

    @Test
    void aTankAtFullSpeedStopsBeforeReverseEngagesAndThenBuildsReverseGradually()
    {
        // T-34-76 mod. 1940: 26.5 t, 500 PS, 55 km/h, 8 km/h in reverse.
        double powerW = VehiclePhysicsUnits.psToKw(500D) * VehiclePhysicsUnits.WATTS_PER_KILOWATT;
        double massKg = 26_500D;
        double terminal = VehiclePhysicsUnits.kmhToBlocksPerTick(55D);
        double reverse = -VehiclePhysicsUnits.kmhToBlocksPerTick(8D);
        DriveDirectionInterlock drivetrain = new DriveDirectionInterlock();
        double speed = terminal;
        int stoppedTick = -1;
        int reverseTick = -1;
        for (int tick = 0; tick < 20 * 60 && reverseTick < 0; tick++)
        {
            double transmission = drivetrain.advance(reverse,
                VehiclePhysicsUnits.blocksPerTickToMetresPerSecond(speed));
            double target = drivetrain.isShifting() ? 0D : reverse;
            double acceleration = GroundPropulsionPhysics.accelerationBlocksPerTickSquared(
                speed, powerW, massKg, terminal, 1D) * transmission;
            // Reverse demand while still rolling forwards works the brake.
            double deceleration = GroundPropulsionPhysics.decelerationBlocksPerTickSquared(
                speed, powerW, massKg, terminal, drivetrain.isShifting() ? 1D : 0D);
            speed = GroundPropulsionPhysics.approach(speed, target, acceleration, deceleration);
            assertTrue(speed >= 0D || drivetrain.getGear() < 0, "no reverse motion before reverse is selected");
            if (stoppedTick < 0 && speed <= 0D)
                stoppedTick = tick;
            if (reverseTick < 0 && speed <= reverse * 0.9D)
                reverseTick = tick;
        }
        assertTrue(stoppedTick > 0, "the tank must stop");
        assertTrue(reverseTick > stoppedTick, "and only then reach its reverse speed");
        // Before the interlock reverse speed arrived about four ticks after the
        // stop; it now takes the shift pause plus most of the clutch take-up.
        assertTrue(reverseTick - stoppedTick >= 10,
            "reverse must build, not snap; took " + (reverseTick - stoppedTick) + " ticks");
    }

    private static DriveDirectionInterlock engagedInReverse()
    {
        DriveDirectionInterlock drivetrain = new DriveDirectionInterlock();
        for (int tick = 0; tick < SHIFT + CLUTCH; tick++)
            drivetrain.advance(-1D, 0D);
        return drivetrain;
    }
}
