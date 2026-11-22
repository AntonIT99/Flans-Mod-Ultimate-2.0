package com.flansmodultimate.common.driveables.physics;

import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HoverHoldTest
{
    @Test
    void aHelicopterBelowItsHeldHeightClimbsAtMostTheCappedRate()
    {
        Vec3 held = Vec3.ZERO;
        for (int tick = 0; tick < 200; tick++)
            held = HoverHold.hold(new Vec3(0D, held.y, 0D), 50D);
        assertEquals(HoverHold.MAX_VERTICAL_SPEED, held.y, 1.0E-6D);
    }

    @Test
    void aHelicopterAboveItsHeldHeightSinksBackTowardsIt()
    {
        Vec3 held = HoverHold.hold(new Vec3(0D, 0.1D, 0D), -2D);
        assertTrue(held.y < 0.1D);
        assertEquals(0.1D + (-0.16D - 0.1D) * HoverHold.VERTICAL_RESPONSE, held.y, 1.0E-9D);
    }

    @Test
    void driftIsDampedEachTick()
    {
        Vec3 held = HoverHold.hold(new Vec3(1D, 0D, -2D), 0D);
        assertEquals(HoverHold.DRIFT_RETENTION, held.x, 1.0E-9D);
        assertEquals(-2D * HoverHold.DRIFT_RETENTION, held.z, 1.0E-9D);
        assertEquals(0D, held.y, 1.0E-9D);
    }

    @Test
    void anUnknownHeightLeavesTheVelocityAlone()
    {
        Vec3 velocity = new Vec3(0.3D, -0.4D, 0.2D);
        assertSame(velocity, HoverHold.hold(velocity, Double.NaN));
    }
}
