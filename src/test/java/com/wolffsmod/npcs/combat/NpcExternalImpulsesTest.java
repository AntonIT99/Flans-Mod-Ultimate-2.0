package com.wolffsmod.npcs.combat;

import com.flansmodultimate.common.driveables.physics.VehicleImpulsePhysics;
import com.flansmodultimate.common.types.AAGunType;
import com.flansmodultimate.common.types.DriveableType;
import com.wolffsmod.npcs.combat.NpcCombatSettings.Knockback;
import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NpcExternalImpulsesTest
{
    @Test
    void aaGunsRetainOnlyTheirMassShareOfAnOutsideBlastExactlyOnce()
    {
        AAGunType gun = mock(AAGunType.class);
        when(gun.getImpulseMass()).thenReturn(new VehicleImpulsePhysics.ImpulseMass(1000F, VehicleImpulsePhysics.MassSource.FALLBACK));
        NpcExternalImpulses tracker = new NpcExternalImpulses();
        Vec3 own = new Vec3(0.2D, -0.1D, 0D);
        tracker.settle(own);
        Vec3 result = tracker.absorb(own.add(1D, 1D, 0D), Knockback.MODEL, gun);
        assertEquals(0.3D, result.x, 1E-9D);
        assertEquals(0D, result.y, 1E-9D);
        assertEquals(result, tracker.absorb(result, Knockback.MODEL, gun));
    }

    @Test
    void heavyDriveablesDiscardSmallPushesWithoutChangingTheirOwnMovement()
    {
        DriveableType tank = mock(DriveableType.class);
        when(tank.getImpulseMass()).thenReturn(new VehicleImpulsePhysics.ImpulseMass(64000F, VehicleImpulsePhysics.MassSource.REAL_MASS));
        NpcExternalImpulses tracker = new NpcExternalImpulses();
        Vec3 own = new Vec3(0.4D, -0.08D, 0D);
        tracker.settle(own);
        assertEquals(own, tracker.absorb(own.add(1D, 0.2D, 0D), Knockback.MODEL, tank));
        Vec3 nextMovement = new Vec3(0.8D, 0.1D, 0D);
        tracker.settle(nextMovement);
        assertEquals(nextMovement, tracker.absorb(nextMovement, Knockback.MODEL, tank));
    }

    @Test
    void normalAndInfantryKeepPushesWhileImmunityPreservesOnlyOwnMovement()
    {
        NpcExternalImpulses tracker = new NpcExternalImpulses();
        Vec3 own = new Vec3(0.2D, -0.1D, 0D);
        Vec3 pushed = own.add(1D, 0.5D, 0D);
        tracker.settle(own);
        assertEquals(pushed, tracker.absorb(pushed, Knockback.NORMAL, mock(AAGunType.class)));
        tracker.settle(own);
        assertEquals(pushed, tracker.absorb(pushed, Knockback.MODEL, null));
        tracker.settle(own);
        assertEquals(own, tracker.absorb(pushed, Knockback.IMMUNE, null));
    }

    @Test
    void firstTickPreservesLoadedVelocity()
    {
        Vec3 loaded = new Vec3(0.2D, -0.1D, 0D);
        assertSame(loaded, new NpcExternalImpulses().absorb(loaded, Knockback.IMMUNE, null));
    }
}
