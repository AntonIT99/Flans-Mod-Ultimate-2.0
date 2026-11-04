package com.wolffsmod.npcs.drivers;

import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.*;

class NpcDriverTest
{
    @Test
    void aBombIsLetGoAheadOfTheTargetByAsFarAsItIsThrownForward()
    {
        Vec3 release = NpcDriver.releasePoint(new Vec3(5D, 90D, 0D), new Vec3(5D, 64D, 30D), new Vec3(0D, 64D, 0D));
        assertEquals(0D, release.x, 1.0E-9D);
        assertEquals(-30D, release.z, 1.0E-9D);
        assertEquals(64D, release.y, 1.0E-9D);
    }

    @Test
    void anAircraftClimbsAboveTheReachOfItsBombsBlastBeforeLettingThemGo()
    {
        assertEquals(64D + 12D + 4D, NpcDriver.bombAltitude(64D, 12D), 1.0E-9D);
        assertEquals(Double.NEGATIVE_INFINITY, NpcDriver.bombAltitude(64D, 0D));
        assertTrue(NpcDriver.belowOwnBlast(70D, 64D, 12D));
        assertFalse(NpcDriver.belowOwnBlast(80D, 64D, 12D));
        // Without a standoff it does not keep clear of its blast at all.
        assertFalse(NpcDriver.belowOwnBlast(65D, 64D, 0D));
    }
}
