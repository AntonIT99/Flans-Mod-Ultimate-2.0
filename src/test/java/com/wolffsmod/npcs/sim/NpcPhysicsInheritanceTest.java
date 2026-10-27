package com.wolffsmod.npcs.sim;

import com.flansmodultimate.api.driveables.DriveableKind;
import com.flansmodultimate.api.driveables.IDriveableType;
import com.wolffsmod.npcs.sim.NpcTuning.Flight;
import com.wolffsmod.npcs.sim.NpcTuning.Value;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NpcPhysicsInheritanceTest
{
    @Test
    void anInheritedWalkingSpeedFollowsTheSquareAMobWalksBy()
    {
        // Walking speed 5 settles at 0.135 blocks a tick; four times that speed takes twice the speed value.
        assertEquals(5D, NpcMobility.walkingSpeedFor(NpcMobility.naturalSpeed(5)), 1.0E-9D);
        assertEquals(10D, NpcMobility.walkingSpeedFor(4D * NpcMobility.naturalSpeed(5)), 1.0E-9D);
        assertEquals(0D, NpcMobility.walkingSpeedFor(0D));
        assertEquals(0D, NpcMobility.walkingSpeedFor(Double.NaN));
        // A legacy MoveSpeed 1 mecha walks 0.215 blocks a tick, which the former linear mapping turned into walking speed 5: 0.135.
        double mecha = 4.3D / 20D;
        assertEquals(mecha, NpcMobility.naturalSpeed(1) * Math.pow(NpcMobility.walkingSpeedFor(mecha), 2D), 1.0E-9D);
    }

    @Test
    void aMechaJumpsAsHighAsItsDefinitionUnderAMobsGravity()
    {
        // A mob's own jump of 0.42 clears 1.25 blocks, which a mecha with a JumpHeight of 1.05 clears too.
        assertEquals(0.42F, NpcMobility.jumpPower(1.05D), 1.0E-4F);
        assertTrue(NpcMobility.jumpPower(4D) > NpcMobility.jumpPower(1D));
        assertEquals(NpcMobility.jumpPower(-0.2D), NpcMobility.jumpPower(-5D));
    }

    @Test
    void theGradeIsTheAngleOfTheRiseOverTheRunAndNothingWhileStanding()
    {
        assertEquals(45D, NpcVehicleState.grade(0.2D, 0.2D), 1.0E-9D);
        assertTrue(NpcVehicleState.grade(-0.1D, 0.2D) < 0D);
        assertEquals(0D, NpcVehicleState.grade(0.5D, 0D));
    }

    @Test
    void aPlaneFlyingItsModelBuildsUpToItsTopSpeedGainsInADiveAndNeverStalls()
    {
        double level = NpcFlight.modelSpeed(0.5D, 0.3D, 1.5D, 0F);
        assertEquals(0.5D + 1D * NpcFlight.THRUST_RESPONSE, level, 1.0E-9D);
        assertTrue(NpcFlight.modelSpeed(0.5D, 0.3D, 1.5D, 30F) > level);
        assertTrue(NpcFlight.modelSpeed(0.5D, 0.3D, 1.5D, -30F) < level);
        assertEquals(0.3D, NpcFlight.modelSpeed(0.3D, 0.3D, 1.5D, -45F), 1.0E-9D);
        assertEquals(1.5D, NpcFlight.modelSpeed(1.5D, 0.3D, 1.5D, 45F), 1.0E-9D);
        // A least flying speed above the top speed cannot hold it above its top speed.
        assertEquals(1D, NpcFlight.modelSpeed(0.5D, 2D, 1D, 0F), 1.0E-9D);
        double speed = 0.3D;
        for (int tick = 0; tick < 2000; tick++)
            speed = NpcFlight.modelSpeed(speed, 0.3D, 1.5D, 0F);
        assertEquals(1.5D, speed, 1.0E-6D);
    }

    @Test
    void aPlaneFlyingItsModelTurnsTighterTheSlowerItFliesUnlessItsTurnRateIsSet()
    {
        assertTrue(NpcFlight.coordinatedTurn(0.5D) > NpcFlight.coordinatedTurn(1.5D));
        // 45 degrees of bank at one block a tick under a gravity of 0.098: about 5.6 degrees a tick.
        assertEquals(5.615F, NpcFlight.coordinatedTurn(1D), 1.0E-2F);
        assertEquals(NpcFlight.MAX_COORDINATED_TURN, NpcFlight.coordinatedTurn(0.01D));
        assertEquals(NpcFlight.coordinatedTurn(1D), NpcFlight.planeTurn(0, 1D, 16D, true));
        assertEquals(NpcFlight.planeTurn(0.3D, 16D), NpcFlight.planeTurn(0, 0.3D, 16D, false));
        assertEquals(1.5F, NpcFlight.planeTurn(30, 1D, 16D, true));
        assertEquals(0.25F, NpcFlight.authority(0.01D));
        assertEquals(3F, NpcFlight.authority(10D));
        assertEquals(1.5F, NpcFlight.authority(1.5D));
    }

    @Test
    void anAircraftClimbsAtItsSetRateItsModelsOrItsDefault()
    {
        NpcTuning tuning = new NpcTuning();
        assertEquals(NpcFlight.PLANE_CLIMB, NpcFlight.maxClimb(tuning, null, Flight.PLANE));
        assertEquals(NpcFlight.HELICOPTER_CLIMB, NpcFlight.maxClimb(tuning, null, Flight.HELICOPTER));
        tuning.set(Value.CLIMB_RATE, 100);
        assertEquals(0.5D, NpcFlight.maxClimb(tuning, null, Flight.PLANE), 1.0E-9D);
        assertEquals(0.5D, NpcFlight.climb(0D, 30D, Flight.PLANE, false, 0.5D), 1.0E-9D);
        assertEquals(NpcFlight.climb(0D, 30D, Flight.PLANE, false), NpcFlight.climb(0D, 30D, Flight.PLANE, false, NpcFlight.PLANE_CLIMB));
    }

    @Test
    void automaticFlightFollowsTheAircraftModelAndLeavesEverythingElseOnTheGround()
    {
        assertEquals(Flight.NORMAL, NpcFlight.automatic(null));
        IDriveableType plane = mock(IDriveableType.class);
        when(plane.getDriveableKind()).thenReturn(DriveableKind.PLANE);
        assertEquals(Flight.PLANE, NpcFlight.automatic(plane));
        when(plane.isHelicopter()).thenReturn(true);
        assertEquals(Flight.HELICOPTER, NpcFlight.automatic(plane));
        when(plane.isShip()).thenReturn(true);
        assertEquals(Flight.NORMAL, NpcFlight.automatic(plane));
        IDriveableType tank = mock(IDriveableType.class);
        when(tank.getDriveableKind()).thenReturn(DriveableKind.VEHICLE);
        assertEquals(Flight.NORMAL, NpcFlight.automatic(tank));
        // Saves hold the index, so Automatic comes after every earlier flight type.
        assertEquals(3, Flight.AUTO.ordinal());
    }

    @Test
    void aSubmarineWithADiveRateDivesNoFasterThanItAndStillSettlesGently()
    {
        assertEquals(-0.05D, NpcSubmarine.dive(0D, -10D, 0.05D), 1.0E-9D);
        assertEquals(0.05D, NpcSubmarine.dive(0D, 10D, 0.05D), 1.0E-9D);
        assertEquals(-0.08D, NpcSubmarine.dive(0D, -1D, 0.5D), 1.0E-9D);
        assertEquals(0.045D, NpcSubmarine.dive(0.1D, 0.2D, 0.5D), 1.0E-9D);
    }
}
