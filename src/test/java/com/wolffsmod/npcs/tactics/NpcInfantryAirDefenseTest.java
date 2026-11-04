package com.wolffsmod.npcs.tactics;

import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.*;

class NpcInfantryAirDefenseTest
{
    @Test
    void anInboundAttackAndAHoveringGunshipAreDangerousButAnUnrelatedFlyoverIsNot()
    {
        Vec3 aircraft = new Vec3(0D, 30D, -50D);
        assertTrue(NpcInfantryAirDefense.attackRun(Vec3.ZERO, aircraft, new Vec3(0D, 0D, 1D), true));
        assertTrue(NpcInfantryAirDefense.attackRun(Vec3.ZERO, aircraft, new Vec3(0D, -0.2D, 1D), false), "a visible diving approach does not require reading a Mob target");
        assertTrue(NpcInfantryAirDefense.attackRun(Vec3.ZERO, aircraft, Vec3.ZERO, true));
        assertFalse(NpcInfantryAirDefense.attackRun(Vec3.ZERO, aircraft, new Vec3(0D, 0D, 1D), false));
        assertFalse(NpcInfantryAirDefense.attackRun(Vec3.ZERO, aircraft, new Vec3(0D, 0D, -1D), true));
        assertFalse(NpcInfantryAirDefense.attackRun(Vec3.ZERO, aircraft.add(50D, 0D, 0D), new Vec3(0D, 0D, 1D), true));
        assertFalse(NpcInfantryAirDefense.attackRun(Vec3.ZERO, aircraft.add(0D, 100D, 0D), new Vec3(0D, 0D, 1D), true));
    }

    @Test
    void dangerKeepsItsSnapshotThroughThePassWithoutFollowingAnUnseenAircraft()
    {
        var memory = new NpcInfantryAirDefense.Memory();
        Vec3 aircraft = new Vec3(0D, 30D, -50D);
        memory.remember(Vec3.ZERO, aircraft, new Vec3(0D, 0D, 1D), 10L);
        var snapshot = memory.getSources();
        memory.remember(Vec3.ZERO, new Vec3(0D, 30D, -20D), new Vec3(0D, 0D, 1D), 40L);
        assertSame(snapshot, memory.getSources(), "a moving aircraft cannot keep restarting the incremental cover search");
        assertTrue(memory.active(139L));
        assertFalse(memory.active(140L));
        memory.remember(Vec3.ZERO, new Vec3(0D, 30D, -10D), new Vec3(0D, 0D, 1D), 80L);
        assertNotEquals(snapshot, memory.getSources(), "a prolonged attack may refresh its observed corridor after the search has had time to finish");
        memory.remember(Vec3.ZERO, new Vec3(30D, 30D, 0D), new Vec3(-1D, 0D, 0D), 150L);
        assertNotEquals(snapshot, memory.getSources());
    }

    @Test
    void escapeCrossesTheApproachCorridorByTheNearestSide()
    {
        Vec3 right = new Vec3(3D, 0D, 0D);
        Vec3 side = NpcInfantryAirDefense.sideways(right, new Vec3(0D, 30D, -50D), new Vec3(0D, 0D, 1D));
        assertEquals(1D, side.x, 1.0E-9D);
        assertEquals(0D, side.z, 1.0E-9D);
        Vec3 opposite = NpcInfantryAirDefense.sideways(right.scale(-1D), new Vec3(0D, 30D, -50D), new Vec3(0D, 0D, 1D));
        assertEquals(-1D, opposite.x, 1.0E-9D);
        assertEquals(0D, opposite.z, 1.0E-9D);
    }

    @Test
    void firingRequiresAnInterceptWithinAShortFlightTime()
    {
        Vec3 offset = new Vec3(0D, 25D, 60D);
        assertTrue(NpcInfantryAirDefense.plausibleShot(offset, new Vec3(1D, 0D, 0D), 5D, 0.01D, 0.99D));
        assertFalse(NpcInfantryAirDefense.plausibleShot(offset, new Vec3(0D, 0D, 5D), 2D, 0.01D, 0.99D));
        assertFalse(NpcInfantryAirDefense.plausibleShot(new Vec3(0D, 100D, 150D), Vec3.ZERO, 1D, 0.05D, 0.99D));
        assertFalse(NpcInfantryAirDefense.plausibleShot(offset, Vec3.ZERO, 0D, 0.01D, 0.99D));
    }
}
