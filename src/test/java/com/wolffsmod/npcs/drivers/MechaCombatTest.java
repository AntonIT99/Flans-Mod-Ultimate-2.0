package com.wolffsmod.npcs.drivers;

import com.flansmodultimate.api.driveables.ISeat;
import com.flansmodultimate.api.driveables.MechaWalking;
import com.flansmodultimate.api.driveables.WeaponTriggerState;
import com.flansmodultimate.api.driveables.WeaponTriggerState.Mode;
import com.flansmodultimate.api.equipment.MechaMeleeProperties;
import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;

import static com.flansmodultimate.api.driveables.DriveControl.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MechaCombatTest
{
    @Test
    void rangedArmTakesPriorityOverToolsForCombatAim()
    {
        ISeat seat = mock(ISeat.class);
        when(seat.getDriverWeaponOrigin(anyBoolean())).thenReturn(Optional.of(Vec3.ZERO));
        when(seat.getDriverMiningReach(false)).thenReturn(OptionalDouble.of(4D));
        assertTrue(NpcDriver.turretBank(seat));
        when(seat.getDriverMiningReach(false)).thenReturn(OptionalDouble.empty());
        assertFalse(NpcDriver.turretBank(seat));
        when(seat.getDriverMeleeProperties(true)).thenReturn(Optional.of(new MechaMeleeProperties(6F, 10, 8D)));
        assertFalse(NpcDriver.turretBank(seat));
    }

    @Test
    void eachSemiAutomaticBankRepressesWhileAutomaticAndSpinningWeaponsStayHeld()
    {
        boolean semi = false;
        boolean automatic = false;
        for (int tick = 0; tick < 12; tick++)
        {
            semi = MechaCombat.trigger(true, semi, new WeaponTriggerState(Mode.SEMIAUTOMATIC, false));
            automatic = MechaCombat.trigger(true, automatic, new WeaponTriggerState(Mode.HELD, false));
            assertEquals(tick % 2 == 0, semi);
            assertTrue(automatic);
        }
        assertFalse(MechaCombat.trigger(false, true, new WeaponTriggerState(Mode.BURST, true)), "Losing a safe shot releases even an active burst");
    }

    @Test
    void aBurstCompletesBeforeTheNextRisingEdge()
    {
        boolean held = MechaCombat.trigger(true, false, new WeaponTriggerState(Mode.BURST, false));
        assertTrue(held);
        for (int tick = 0; tick < 9; tick++)
        {
            held = MechaCombat.trigger(true, held, new WeaponTriggerState(Mode.BURST, true));
            assertTrue(held);
        }
        held = MechaCombat.trigger(true, held, new WeaponTriggerState(Mode.BURST, false));
        assertFalse(held);
        assertTrue(MechaCombat.trigger(true, held, new WeaponTriggerState(Mode.BURST, false)));
    }

    @Test
    void anInterruptedBurstCannotRestartUntilItFinishes()
    {
        WeaponTriggerState active = new WeaponTriggerState(Mode.BURST, true);
        boolean held = MechaCombat.trigger(false, true, active);
        assertFalse(held);
        held = MechaCombat.trigger(true, held, active);
        assertFalse(held);
        assertTrue(MechaCombat.trigger(true, held, new WeaponTriggerState(Mode.BURST, false)));
    }

    @Test
    void theShorterSwordSetsClosingDistanceAndHeightReducesHorizontalReach()
    {
        ISeat seat = mock(ISeat.class);
        when(seat.getDriverMeleeProperties(false)).thenReturn(Optional.of(new MechaMeleeProperties(6F, 10, 8D)));
        when(seat.getDriverMeleeProperties(true)).thenReturn(Optional.of(new MechaMeleeProperties(12F, 5, 3D)));
        assertEquals(3D, MechaCombat.meleeReach(seat));
        assertTrue(MechaCombat.approach(3D, 2D) < MechaCombat.approach(3D, 0D));
        assertEquals(0.25D, MechaCombat.approach(3D, 4D));
        when(seat.getDriverMeleeProperties(true)).thenReturn(Optional.empty());
        assertEquals(8D, MechaCombat.meleeReach(seat));
        when(seat.getDriverMeleeProperties(false)).thenReturn(Optional.empty());
        assertEquals(0D, MechaCombat.meleeReach(seat));
    }

    @Test
    void climbingUsesStepsJumpsAndRocketThrustBeforeStuckRecovery()
    {
        assertFalse(MechaCombat.ascend(0.5D, 1D, 2D, true, false, false));
        assertTrue(MechaCombat.ascend(1.5D, 1D, 2D, true, false, false));
        assertFalse(MechaCombat.ascend(5D, 1D, 2D, true, false, false));
        assertTrue(MechaCombat.ascend(5D, 1D, 2D, true, false, true));
        assertTrue(MechaCombat.ascend(5D, 1D, 2D, false, false, true));
        assertFalse(MechaCombat.ascend(1.5D, 1D, 2D, false, false, false));
        assertTrue(MechaCombat.ascend(0D, 1D, 2D, true, true, false));
        assertFalse(MechaCombat.ascend(0D, 1D, 2D, false, true, true));
    }

    @Test
    void sharedWalkingKeepsAllQuadrantsAndStopsAtReach()
    {
        assertEquals(Set.of(FORWARD), MechaWalking.toward(0D, 10D, 2D));
        assertEquals(Set.of(FORWARD, RIGHT), MechaWalking.toward(45D, 10D, 2D));
        assertEquals(Set.of(RIGHT), MechaWalking.toward(90D, 10D, 2D));
        assertEquals(Set.of(BACKWARD, LEFT), MechaWalking.toward(-135D, 10D, 2D));
        assertTrue(MechaWalking.toward(90D, 2D, 2D).isEmpty());
        for (int angle = -180; angle <= 180; angle++)
            assertEquals(MechaWalking.toward(angle, 10D, 2D), DriveSteering.mecha(angle, 10D, 2D));
    }
}
