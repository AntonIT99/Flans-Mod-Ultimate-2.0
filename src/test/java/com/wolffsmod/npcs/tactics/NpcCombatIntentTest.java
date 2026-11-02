package com.wolffsmod.npcs.tactics;

import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.*;

class NpcCombatIntentTest
{
    @Test
    void aBoundStopsAtItsArrivalDistanceAndAircraftHeightDoesNotChangeProgress()
    {
        NpcCombatIntent intent = new NpcCombatIntent(new Vec3(10D, 50D, 0D), 2D);
        assertEquals(new Vec3(6D, 0D, 0D), intent.step(Vec3.ZERO, 6D));
        assertEquals(new Vec3(8D, 0D, 0D), intent.step(Vec3.ZERO, 20D));
        assertTrue(intent.reached(new Vec3(9D, 0D, 0D)));
        assertEquals(6D, intent.progress(Vec3.ZERO, new Vec3(6D, 100D, 0D)));
        assertTrue(intent.progress(Vec3.ZERO, new Vec3(-3D, 0D, 0D)) < 0D);
    }

    @Test
    void defendersKeepCoverInsideTheAssignedAreaOnceTheyHaveReachedIt()
    {
        NpcCombatIntent intent = new NpcCombatIntent(new Vec3(3D, 0D, 0D), 2D, Vec3.ZERO, 6D);
        assertTrue(intent.permits(new Vec3(20D, 0D, 0D), new Vec3(10D, 0D, 0D)), "approach may span several bounds");
        assertTrue(intent.permits(new Vec3(3D, 0D, 0D), new Vec3(0D, 0D, 5D)));
        assertFalse(intent.permits(new Vec3(3D, 0D, 0D), new Vec3(7D, 0D, 0D)));
    }
}
