package com.wolffsmod.npcs.sim;

import com.wolffsmod.npcs.model.FlanModelKind;
import com.wolffsmod.npcs.sim.NpcVehicleState.BombPlan;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NpcBombRunTest
{
    @Test
    void aBomberFliesItsRunLevel()
    {
        BombPlan plan = NpcVehicleState.bombPlan(true, false, true);
        assertTrue(plan.run());
        assertFalse(plan.marksDrop());
    }

    @Test
    void bombsBesideTheGunsDoNotMakeTheRunSoTheGunsStillBear()
    {
        BombPlan plan = NpcVehicleState.bombPlan(false, true, true);
        assertFalse(plan.run());
        assertTrue(plan.marksDrop());
    }

    @Test
    void aPlaneWithNoBombsForTheTargetFollowsNoDrop()
    {
        assertFalse(NpcVehicleState.bombPlan(false, true, false).marksDrop());
        assertFalse(NpcVehicleState.bombPlan(false, false, true).marksDrop());
        assertFalse(NpcVehicleState.bombPlan(false, false, false).run());
    }

    @Test
    void anAircraftLayingItsGunsByFlyingGetsMoreRoomThanATurret()
    {
        assertEquals(7F, NpcVehicleState.firingTolerance(4F, FlanModelKind.PLANE, false));
        assertEquals(4F, NpcVehicleState.firingTolerance(4F, FlanModelKind.PLANE, true));
        assertEquals(4F, NpcVehicleState.firingTolerance(4F, FlanModelKind.VEHICLE, false));
        assertEquals(4F, NpcVehicleState.firingTolerance(4F, null, false));
    }
}
