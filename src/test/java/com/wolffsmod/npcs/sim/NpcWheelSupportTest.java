package com.wolffsmod.npcs.sim;

import com.flansmodultimate.api.driveables.WheelSupport;
import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.*;

class NpcWheelSupportTest
{
    /** The hanging wheels lie towards +X of the carried ones. */
    private static final Vec3 OVER_EAST = new Vec3(3D, 0D, 0D);

    @Test
    void halfTheFootprintOverTheEdgeTipsTheHullAndThreeQuartersOnTheGroundHoldsIt()
    {
        assertTrue(new WheelSupport(2, 4, OVER_EAST).overhanging(NpcWheelSupport.HOLD_SHARE));
        assertTrue(new WheelSupport(1, 2, OVER_EAST).overhanging(NpcWheelSupport.HOLD_SHARE));
        assertTrue(new WheelSupport(3, 6, OVER_EAST).overhanging(NpcWheelSupport.HOLD_SHARE));
        assertFalse(new WheelSupport(3, 4, OVER_EAST).overhanging(NpcWheelSupport.HOLD_SHARE));
        assertFalse(new WheelSupport(4, 6, OVER_EAST).overhanging(NpcWheelSupport.HOLD_SHARE));
        assertFalse(WheelSupport.UNKNOWN.overhanging(NpcWheelSupport.HOLD_SHARE));
    }

    @Test
    void theHullIsPushedOverTheWayItHangsTheHarderTheMoreOfItHangs()
    {
        Vec3 half = NpcWheelSupport.toppled(new WheelSupport(2, 4, OVER_EAST), Vec3.ZERO, 0F);
        Vec3 quarter = NpcWheelSupport.toppled(new WheelSupport(1, 4, OVER_EAST), Vec3.ZERO, 0F);
        assertTrue(half.x > 0.02D, "half the footprint hanging pushes harder than a mob's ground friction holds: " + half.x);
        assertTrue(quarter.x > half.x);
        assertEquals(0D, half.z, 1.0E-9D);
        assertEquals(0D, half.y);
    }

    @Test
    void withEveryWheelHangingItSlidesOffAlongItsNoseAndDrops()
    {
        // Facing 90 degrees, the nose points to -X.
        Vec3 motion = NpcWheelSupport.toppled(new WheelSupport(0, 4, Vec3.ZERO), new Vec3(0D, 0.05D, 0D), 90F);
        assertTrue(motion.x < 0D);
        assertEquals(0D, motion.z, 1.0E-6D);
        assertTrue(motion.y < 0D);
    }

    @Test
    void slidingAloneNeverExceedsTheToppleSpeed()
    {
        Vec3 motion = new Vec3(0.29D, 0D, 0D);
        for (int tick = 0; tick < 50; tick++)
            motion = NpcWheelSupport.toppled(new WheelSupport(0, 4, OVER_EAST), motion, 0F);
        assertEquals(0.3D, motion.horizontalDistance(), 1.0E-9D);
    }
}
