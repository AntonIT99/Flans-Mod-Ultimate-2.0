package com.wolffsmod.npcs.sim;

import com.flansmodultimate.api.driveables.MountAim;
import com.flansmodultimate.api.driveables.WeaponMount;
import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.*;

class NpcFiringPositionTest
{
    /** A turret that elevates 20 degrees and depresses 10, as a mount states it: pitch positive downwards. */
    private static final WeaponMount TURRET = new WeaponMount(0, true, true, new MountAim(-360F, -20F), new MountAim(360F, 10F), 2F, 1F, Vec3.ZERO, Vec3.ZERO, Optional.empty());

    @Test
    void aShotBeyondTheTurretsElevationGivesTheNearestPitchItReachesWithinTheTolerance()
    {
        assertEquals(14F, NpcVehicleState.reachablePitch(TURRET, true, new MountAim(0F, 30F), 4F));
        assertEquals(-24F, NpcVehicleState.reachablePitch(TURRET, true, new MountAim(0F, -30F), 4F));
        assertTrue(Float.isNaN(NpcVehicleState.reachablePitch(TURRET, true, new MountAim(0F, 12F), 4F)));
        // A turret without a seat to state its limits elevates freely.
        assertTrue(Float.isNaN(NpcVehicleState.reachablePitch(null, true, new MountAim(0F, 60F), 4F)));
    }

    @Test
    void aGunFixedToTheHullOnlyReachesLevelWithinTheTolerance()
    {
        assertEquals(4F, NpcVehicleState.reachablePitch(TURRET, false, new MountAim(0F, 6F), 4F));
        assertEquals(-4F, NpcVehicleState.reachablePitch(null, false, new MountAim(0F, -6F), 4F));
        assertTrue(Float.isNaN(NpcVehicleState.reachablePitch(null, false, new MountAim(0F, 3F), 4F)));
    }

    @Test
    void aVehicleBacksAwayFromATargetCloseBelowUntilItsGunDepressesOntoIt()
    {
        // One block down and two out, with the gun depressing ten degrees: it needs about 6.3 blocks and keeps a margin.
        double needed = 1D / Math.tan(Math.toRadians(10F - NpcFiringPosition.ELEVATION_MARGIN));
        OptionalDouble back = NpcFiringPosition.backDistance(2D, 1D, 10F, 64D);
        assertTrue(back.isPresent());
        assertEquals(needed + NpcFiringPosition.BACK_MARGIN - 2D, back.getAsDouble(), 1.0E-6D);
    }

    @Test
    void aVehicleBacksAwayFromATargetCloseAboveUntilItsGunRisesOntoIt()
    {
        double needed = 3D / Math.tan(Math.toRadians(20F - NpcFiringPosition.ELEVATION_MARGIN));
        OptionalDouble back = NpcFiringPosition.backDistance(1D, -3D, -20F, 64D);
        assertTrue(back.isPresent());
        assertEquals(needed + NpcFiringPosition.BACK_MARGIN - 1D, back.getAsDouble(), 1.0E-6D);
    }

    @Test
    void aVehicleDoesNotBackAwayWhereThatCannotHelp()
    {
        // A gun that cannot lower itself below level never reaches a target below, however far it backs away.
        assertTrue(NpcFiringPosition.backDistance(2D, 1D, 0F, 64D).isEmpty());
        // A shot short of elevation for its arc, not for the target's height, is not mended by backing away.
        assertTrue(NpcFiringPosition.backDistance(2D, 1D, -5F, 64D).isEmpty());
        assertTrue(NpcFiringPosition.backDistance(40D, 1D, 10F, 64D).isEmpty());
        // Nor is one it would only reach from beyond its firing range.
        assertTrue(NpcFiringPosition.backDistance(2D, 20D, 10F, 64D).isEmpty());
    }

    @Test
    void aVehicleBacksAwayFromATargetUnderItsBarrelUntilTheTargetLiesBeyondTheMuzzle()
    {
        // Two blocks from the turret, under a barrel reaching three: it backs off to a margin beyond the muzzle.
        OptionalDouble back = NpcFiringPosition.reachDistance(2D, 3D, 64D);
        assertTrue(back.isPresent());
        assertEquals(3D + NpcFiringPosition.BACK_MARGIN - 2D, back.getAsDouble(), 1.0E-6D);
        // A target already beyond the muzzle, or a gun whose rounds leave from its pivot, needs no room.
        assertTrue(NpcFiringPosition.reachDistance(3.5D, 3D, 64D).isEmpty());
        assertTrue(NpcFiringPosition.reachDistance(0D, 0D, 64D).isEmpty());
        // A barrel longer than the firing range is not backed away from.
        assertTrue(NpcFiringPosition.reachDistance(2D, 3D, 2.5D).isEmpty());
    }
}
