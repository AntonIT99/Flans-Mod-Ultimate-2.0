package com.wolffsmod.npcs.drivers;

import com.wolffsmod.npcs.sim.MountMotion;
import org.junit.jupiter.api.Test;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.*;

class NpcPilotTest
{
    private static float offCentre(double distance)
    {
        Vec3 position = new Vec3(distance, 64D, 0D);
        float bearing = MountMotion.yaw(Vec3.ZERO.subtract(position));
        return Mth.wrapDegrees(NpcPilot.circle(position, new Vec3(0D, 64D, 0D)) - bearing);
    }

    @Test
    void aCirclingPlaneTurnsInFromOutsideFliesRoundOnTheRadiusAndTurnsOutFromInside()
    {
        assertEquals(30F, offCentre(NpcPilot.LOITER_RADIUS + 100D), 1.0E-3F);
        assertEquals(90F, offCentre(NpcPilot.LOITER_RADIUS), 1.0E-3F);
        assertEquals(150F, offCentre(NpcPilot.LOITER_RADIUS - 40D), 1.0E-3F);
    }
}
