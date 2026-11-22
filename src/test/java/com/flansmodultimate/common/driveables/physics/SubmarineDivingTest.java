package com.flansmodultimate.common.driveables.physics;

import com.flansmodultimate.common.types.EnumType;
import com.flansmodultimate.common.types.TypeFile;
import com.flansmodultimate.common.types.VehicleType;
import com.flansmodultimate.content.ContentPack;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SubmarineDivingTest
{
    private static final ContentPack PACK = new ContentPack("test", Path.of("build", "test-packs", "submarine"));

    @Test
    void heldControlsBuildTheRateAndReleasedItSettlesToHoldDepth()
    {
        float rate = 0F;
        for (int tick = 0; tick < 50; tick++)
            rate = SubmarineDiving.stepRate(rate, true, false, false, true);
        assertEquals(-0.5F, rate, 1E-4F);
        for (int tick = 0; tick < 60; tick++)
            rate = SubmarineDiving.stepRate(rate, false, false, false, true);
        assertEquals(0F, rate);
        for (int tick = 0; tick < 300; tick++)
            rate = SubmarineDiving.stepRate(rate, false, true, false, true);
        assertEquals(1F, rate);
        assertTrue(SubmarineDiving.stepRate(0.5F, false, false, true, true) < 0.4F);
    }

    @Test
    void outOfOxygenTheHullIsDrivenUp()
    {
        float rate = -1F;
        for (int tick = 0; tick < 200; tick++)
            rate = SubmarineDiving.stepRate(rate, true, false, true, false);
        assertEquals(1F, rate);
        assertTrue(SubmarineDiving.verticalVelocity(rate, 0.3F, 0.8F) > 0D);
        assertEquals(-0.15D, SubmarineDiving.verticalVelocity(-1F, 0.3F, 0.8F), 1E-6D);
        assertEquals(0.4D, SubmarineDiving.verticalVelocity(1F, 0.3F, 0.8F), 1E-6D);
    }

    @Test
    void oxygenIsUsedDeepAndRefilledNearTheSurface()
    {
        assertEquals(99F, SubmarineDiving.stepOxygen(100F, 10D, 9000F, false));
        assertEquals(100F, SubmarineDiving.stepOxygen(100F, 10D, 9000F, true));
        assertEquals(115F, SubmarineDiving.stepOxygen(100F, 1D, 9000F, false));
        assertEquals(9000F, SubmarineDiving.stepOxygen(8995F, 0D, 9000F, false));
        assertEquals(0F, SubmarineDiving.stepOxygen(0.5F, 10D, 9000F, false));
    }

    @Test
    void readsLabjacDivingKeys()
    {
        VehicleType defaults = new VehicleType();
        defaults.load(new TypeFile("subDefault", EnumType.VEHICLE, PACK, List.of("ShortName subDefault")));
        assertFalse(defaults.isCanDive());

        VehicleType sub = new VehicleType();
        sub.load(new TypeFile("subTest", EnumType.VEHICLE, PACK,
            List.of("ShortName subTest", "canDive true", "DiveSpeed 0.3", "SurfaceSpeed 0.8", "maxOxygen 9600", "unlimitedOxygen true", "MaxDepth 40")));
        assertTrue(sub.isCanDive());
        assertEquals(0.3F, sub.getDiveSpeed());
        assertEquals(0.8F, sub.getSurfaceSpeed());
        assertEquals(9600F, sub.getMaxOxygen());
        assertTrue(sub.isUnlimitedOxygen());
        assertEquals(40, sub.getMaxDepth());
    }
}
