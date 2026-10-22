package com.wolffsmod.npcs.drivers;

import com.wolffsmod.npcs.sim.MountMotion;
import org.junit.jupiter.api.Test;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.*;

class NpcPilotTest
{
    private static final double RADIUS = 48D;

    private static float offCentre(double distance)
    {
        Vec3 position = new Vec3(distance, 64D, 0D);
        float bearing = MountMotion.yaw(Vec3.ZERO.subtract(position));
        return Mth.wrapDegrees(NpcPilot.circle(position, new Vec3(0D, 64D, 0D), RADIUS) - bearing);
    }

    @Test
    void aCirclingPlaneTurnsInFromOutsideFliesRoundOnTheRadiusAndTurnsOutFromInside()
    {
        assertEquals(30F, offCentre(RADIUS + 100D), 1.0E-3F);
        assertEquals(90F, offCentre(RADIUS), 1.0E-3F);
        assertEquals(150F, offCentre(RADIUS - 40D), 1.0E-3F);
    }

    @Test
    void anIdleAircraftLandsAfterItsDelayUnlessThatIsZero()
    {
        assertFalse(NpcPilot.Settings.landsAfter(600, 600));
        assertTrue(NpcPilot.Settings.landsAfter(601, 600));
        assertFalse(NpcPilot.Settings.landsAfter(100000, 0));
    }

    @Test
    void theDefaultSettingsFlyAsBefore()
    {
        NpcPilot.Settings settings = NpcPilot.Settings.DEFAULTS;
        assertEquals(32D, settings.cruiseHeight());
        assertEquals(24D, settings.attackHeight());
        assertEquals(RADIUS, settings.circleRadius());
        assertEquals(600, settings.planeIdle());
        assertEquals(16D, settings.hoverHeight());
        assertEquals(200, settings.rotorIdle());
        assertTrue(settings.shortOfFuel(0.14D));
        assertFalse(settings.shortOfFuel(0.15D));
    }
}
