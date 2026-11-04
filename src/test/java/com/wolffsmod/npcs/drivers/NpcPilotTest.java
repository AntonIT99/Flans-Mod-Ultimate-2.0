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
    void aPlaneDivingOnAGroundTargetKeepsItsDiveWhereLevelFlightWouldPullUp()
    {
        // The default attack height, sinking as a 40-degree dive does: level flight reads the ground as rushing up.
        assertTrue(NpcPilot.tooLow(24D, -0.6D, false));
        assertFalse(NpcPilot.tooLow(24D, -0.6D, true));
    }

    @Test
    void anAircraftPullsUpOffTheHardFloorHoweverItIsFlying()
    {
        assertTrue(NpcPilot.tooLow(6.5D, 0D, true));
        assertTrue(NpcPilot.tooLow(6.5D, 0D, false));
        assertFalse(NpcPilot.tooLow(40D, 0D, false));
        // Even in a dive, a sink that would reach the floor within the pull-out pulls up.
        assertTrue(NpcPilot.tooLow(14D, -1D, true));
    }

    @Test
    void aHoveringRotorcraftBarelyNosesUpOnAGroundTargetAndLaysItsGunsOnAnAircraftAbove()
    {
        // A target well below is covered by the forward tilt, airborne or not.
        assertEquals(-15F, NpcPilot.aimTilt(-40D, 20D, false), 1.0E-3F);
        assertEquals(-15F, NpcPilot.aimTilt(-40D, 20D, true), 1.0E-3F);
        // One well above: a target on the ground gets the hover nose-up, an aircraft as far as the attack tilt.
        assertEquals(5F, NpcPilot.aimTilt(40D, 20D, false), 1.0E-3F);
        assertEquals(30F, NpcPilot.aimTilt(40D, 20D, true), 1.0E-3F);
        // Within what its nose reaches, it lays the angle the shot actually needs.
        assertEquals(26.565F, NpcPilot.aimTilt(10D, 20D, true), 1.0E-2F);
    }

    @Test
    void aRotorcraftHoldsItsHoverHeightUnlessItFightsAnAircraftHigherUp()
    {
        double hover = NpcPilot.Settings.DEFAULTS.hoverHeight();
        assertEquals(64D + hover, NpcPilot.hoverAltitude(64D, hover, 200D, false));
        assertEquals(64D + hover, NpcPilot.hoverAltitude(64D, hover, 70D, true));
        assertEquals(200D, NpcPilot.hoverAltitude(64D, hover, 200D, true));
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

    @Test
    void aPlaneOnABombRunHasFlownOverItsReleasePointOnceItIsCloseBehindIt()
    {
        // Heading 0 flies toward +z.
        Vec3 position = new Vec3(0D, 80D, 0D);
        assertFalse(NpcPilot.overflown(position, 0F, new Vec3(0D, 64D, 10D), 24D));
        assertTrue(NpcPilot.overflown(position, 0F, new Vec3(0D, 64D, -10D), 24D));
        // A point far behind is one it turned away from, not one it just passed over.
        assertFalse(NpcPilot.overflown(position, 0F, new Vec3(0D, 64D, -100D), 24D));
    }
}
