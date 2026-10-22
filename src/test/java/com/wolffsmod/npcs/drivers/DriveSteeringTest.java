package com.wolffsmod.npcs.drivers;

import com.flansmodultimate.api.DriveControl;
import com.wolffsmod.npcs.drivers.DriveSteering.Decision;
import com.wolffsmod.npcs.drivers.DriveSteering.Situation;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static com.flansmodultimate.api.DriveControl.*;
import static org.junit.jupiter.api.Assertions.*;

class DriveSteeringTest
{
    private static final double ARRIVAL = 2.5D;

    private static Decision wheeled(float off, double distance, double speed)
    {
        return DriveSteering.vehicle(new Situation(off, distance, ARRIVAL, speed, false, false));
    }

    private static Decision tracked(float off, double speed)
    {
        return DriveSteering.vehicle(new Situation(off, 50D, ARRIVAL, speed, true, false));
    }

    private static Set<DriveControl> controls(DriveControl... controls)
    {
        Set<DriveControl> set = EnumSet.noneOf(DriveControl.class);
        set.addAll(Set.of(controls));
        return set;
    }

    @Test
    void aVehicleDrivesStraightAtAGoalAheadAndSteersTowardOneAside()
    {
        assertEquals(controls(FORWARD), wheeled(2F, 50D, 0.3D).controls());
        assertEquals(controls(FORWARD, RIGHT), wheeled(30F, 50D, 0.3D).controls());
        assertEquals(controls(FORWARD, LEFT), tracked(-30F, 0.3D).controls());
    }

    @Test
    void aTrackedVehicleTurnsOnTheSpotWhileAWheeledOneHasToRollRound()
    {
        assertEquals(controls(RIGHT), tracked(90F, 0D).controls());
        assertEquals(controls(FORWARD, RIGHT), wheeled(90F, 50D, 0D).controls());
        // Too fast for so sharp a turn, a wheeled vehicle brakes into it.
        assertEquals(controls(RIGHT, BRAKE), wheeled(90F, 50D, 0.6D).controls());
    }

    @Test
    void steeringTurnsAWheeledVehicleRollingBackwardTheOtherWayRound()
    {
        assertEquals(controls(FORWARD, LEFT), wheeled(30F, 50D, -0.2D).controls());
        assertEquals(controls(FORWARD, RIGHT), tracked(30F, -0.2D).controls());
    }

    @Test
    void aGoalCloseBehindIsReachedInReverseAndAFarOneByTurningRound()
    {
        Decision close = wheeled(170F, 8D, 0D);
        assertTrue(close.reversing());
        // Its rear points 10 degrees right of the goal, so the hull turns left; once rolling back, steering turns the other way.
        assertEquals(controls(BACKWARD, LEFT), close.controls());
        assertEquals(controls(BACKWARD, RIGHT), DriveSteering.vehicle(new Situation(170F, 8D, ARRIVAL, -0.1D, false, true)).controls());
        Decision far = wheeled(170F, 40D, 0D);
        assertFalse(far.reversing());
        assertEquals(controls(FORWARD, RIGHT), far.controls());
        // Once in reverse it stays in reverse until the goal is clearly ahead.
        assertTrue(DriveSteering.vehicle(new Situation(100F, 8D, ARRIVAL, -0.1D, false, true)).reversing());
        assertFalse(DriveSteering.vehicle(new Situation(80F, 8D, ARRIVAL, -0.1D, false, true)).reversing());
    }

    @Test
    void aVehicleStopsAcceleratingNearItsGoalAndBrakesThere()
    {
        // 20 ticks at 0.5 blocks a tick is 10 blocks: it rolls out from there, and brakes from 5 blocks out.
        assertEquals(controls(), wheeled(0F, ARRIVAL + 8D, 0.5D).controls());
        assertEquals(controls(BRAKE), wheeled(0F, ARRIVAL + 4D, 0.5D).controls());
        assertEquals(controls(BRAKE), wheeled(0F, ARRIVAL, 0.5D).controls());
        assertEquals(controls(), wheeled(0F, ARRIVAL, 0D).controls());
    }

    @Test
    void aStuckVehicleBacksAwayTurningItsFrontTowardTheGoal()
    {
        assertEquals(controls(BACKWARD, RIGHT), DriveSteering.recovery(30F, true, 0D, true));
        assertEquals(controls(BACKWARD, LEFT), DriveSteering.recovery(30F, true, -0.2D, false));
        assertEquals(controls(FORWARD, RIGHT), DriveSteering.recovery(30F, false, 0D, false));
    }

    @Test
    void aMechaStepsTowardItsGoalWithoutTurning()
    {
        assertEquals(controls(FORWARD), DriveSteering.mecha(10F, 20D, ARRIVAL));
        assertEquals(controls(RIGHT), DriveSteering.mecha(90F, 20D, ARRIVAL));
        assertEquals(controls(FORWARD, LEFT), DriveSteering.mecha(-45F, 20D, ARRIVAL));
        assertEquals(controls(BACKWARD, LEFT), DriveSteering.mecha(-135F, 20D, ARRIVAL));
        assertEquals(controls(), DriveSteering.mecha(90F, ARRIVAL, ARRIVAL));
    }

    @Test
    void aVehicleKeepsItsDistanceToADriveableAhead()
    {
        Set<DriveControl> driving = controls(FORWARD, RIGHT);
        // Far ahead, or drawing away within the distance: it drives on.
        assertEquals(driving, DriveSteering.keepDistance(driving, DriveSteering.Traffic.NONE, 6, 0.4D));
        assertEquals(driving, DriveSteering.keepDistance(driving, new DriveSteering.Traffic(8D, 0.2D), 6, 0.4D));
        assertEquals(driving, DriveSteering.keepDistance(driving, new DriveSteering.Traffic(5D, -0.1D), 6, 0.4D));
        // Closing in within the distance, it stops accelerating; within half of it, it brakes and waits.
        assertEquals(controls(RIGHT), DriveSteering.keepDistance(driving, new DriveSteering.Traffic(5D, 0.1D), 6, 0.4D));
        assertEquals(controls(RIGHT, BRAKE), DriveSteering.keepDistance(driving, new DriveSteering.Traffic(2D, -0.1D), 6, 0.4D));
        assertEquals(controls(RIGHT), DriveSteering.keepDistance(driving, new DriveSteering.Traffic(2D, 0D), 6, 0D));
        // Reversing, or braking already, it has nothing to hold back.
        assertEquals(controls(BACKWARD), DriveSteering.keepDistance(controls(BACKWARD), new DriveSteering.Traffic(2D, 0.3D), 6, -0.3D));
    }
}
