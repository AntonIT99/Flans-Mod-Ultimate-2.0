package com.wolffsmod.npcs.sim;

import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.AABB;

import static org.junit.jupiter.api.Assertions.*;

class NpcVehicleStateFollowTest
{
    /** A turret turning 3 degrees a tick, whose states every second tick reach the client one tick late now and then. */
    @Test
    void aSteadyTurnStaysSteadyWhenStatesArriveUnevenly()
    {
        float rate = 3F;
        int[] arrivals = {0, 2, 5, 6, 8, 11, 12, 14, 16, 19, 20, 22};
        float target = 0F;
        float yaw = 0F;
        int next = 0;
        for (int tick = 0; tick < 24; tick++)
        {
            if (next < arrivals.length && arrivals[next] == tick)
            {
                target = next * 2 * rate;
                next++;
            }
            float before = yaw;
            yaw = NpcVehicleState.follow(yaw, target, rate);
            // Once the first late state has given it a tick of lag to ride on.
            if (tick >= 5)
                assertEquals(rate, yaw - before, 1.0E-4F, "tick " + tick);
        }
    }

    @Test
    void aTurretThatStopsSettlesOnTheLastStateAndFarTargetsAreCaughtUpWith()
    {
        float yaw = NpcVehicleState.follow(10F, 12F, 3F);
        assertEquals(12F, yaw, 1.0E-4F);
        // Lagging well behind, it closes a third of the gap a tick.
        assertEquals(30F, NpcVehicleState.follow(0F, 90F, 3F), 1.0E-4F);
        // Across the seam at half a turn, the short way round.
        assertEquals(181F, NpcVehicleState.follow(179F, -178F, 2F), 1.0E-4F);
    }

    @Test
    void entitiesOnTheDeckAreNotRunOver()
    {
        AABB npc = new AABB(0D, 64D, 0D, 3D, 66D, 3D);
        assertTrue(NpcRunOver.onTop(npc, 66D));
        assertTrue(NpcRunOver.onTop(npc, 65.8D));
        assertFalse(NpcRunOver.onTop(npc, 64D));
    }
}
