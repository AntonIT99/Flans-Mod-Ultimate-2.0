package com.wolffsmod.npcs.sim;

import com.flansmodultimate.api.driveables.DriveableKind;
import com.flansmodultimate.api.driveables.IAAGunType;
import com.flansmodultimate.common.driveables.EnumWeaponType;
import com.flansmodultimate.common.types.DriveableType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NpcVehicleRecoilTest
{
    @Test
    void machineGunBurstsDoNotRestartTheMainGunsRecoil()
    {
        DriveableType tank = mock(DriveableType.class);
        when(tank.getDriveableKind()).thenReturn(DriveableKind.VEHICLE);
        when(tank.weaponType(false)).thenReturn(EnumWeaponType.SHELL);
        when(tank.weaponType(true)).thenReturn(EnumWeaponType.GUN);
        when(tank.hasBarrelRecoil(anyBoolean())).thenCallRealMethod();
        NpcVehicleState state = new NpcVehicleState();
        state.fired(0, tank, false);
        for (int shot = 0; shot < 20; shot++)
            state.fired(shot, tank, true);
        assertEquals(1, state.getShots());
        assertEquals(0, state.getBarrel());
        state.fired(1, tank, false);
        assertEquals(2, state.getShots());
        assertEquals(1, state.getBarrel());
    }

    @Test
    void shellRecoilFollowsTheBankRatherThanItsPrimaryOrSecondaryPosition()
    {
        DriveableType plane = mock(DriveableType.class);
        when(plane.getDriveableKind()).thenReturn(DriveableKind.PLANE);
        when(plane.hasBarrelRecoil(anyBoolean())).thenCallRealMethod();
        NpcVehicleState state = new NpcVehicleState();
        for (EnumWeaponType weapon : EnumWeaponType.values())
        {
            when(plane.weaponType(false)).thenReturn(weapon);
            when(plane.weaponType(true)).thenReturn(weapon);
            int before = state.getShots();
            state.fired(0, plane, false);
            state.fired(0, plane, true);
            assertEquals(before + (weapon == EnumWeaponType.SHELL ? 2 : 0), state.getShots(), weapon.name());
        }
    }

    @Test
    void aaBarrelsAndMechaFiringAnimationsStillAdvance()
    {
        NpcVehicleState state = new NpcVehicleState();
        state.fired(3, mock(IAAGunType.class), false);
        assertEquals(1, state.getShots());
        assertEquals(3, state.getBarrel());
        DriveableType mecha = mock(DriveableType.class);
        when(mecha.getDriveableKind()).thenReturn(DriveableKind.MECHA);
        state.fired(0, mecha, true);
        assertEquals(2, state.getShots());
    }
}
