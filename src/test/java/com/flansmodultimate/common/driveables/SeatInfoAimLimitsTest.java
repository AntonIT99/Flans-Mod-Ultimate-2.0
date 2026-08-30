package com.flansmodultimate.common.driveables;

import com.flansmod.common.vector.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SeatInfoAimLimitsTest
{
    private static final double EPSILON = 1.0E-4D;

    private static SeatInfo seat(float minYaw, float maxYaw)
    {
        return seat(minYaw, maxYaw, -10F, 40F);
    }

    private static SeatInfo seat(float minYaw, float maxYaw, float minPitch, float maxPitch)
    {
        return new SeatInfo(1, new Vector3f(), EnumDriveablePart.CORE, false,
            minYaw, maxYaw, minPitch, maxPitch, null, null);
    }

    @Test
    void rearArcTraversesThroughTheWrapBoundary()
    {
        // The SdKfz 251's rear MG: "Passenger 2 ... core 135 225 -10 40".
        SeatInfo rearMg = seat(135F, 225F);

        assertEquals(179F, rearMg.clampYaw(179F), EPSILON);
        assertEquals(180F, rearMg.clampYaw(180F), EPSILON);
        assertEquals(181F, rearMg.clampYaw(181F), EPSILON);
        assertEquals(224F, rearMg.clampYaw(224F), EPSILON);
    }

    @Test
    void rearArcStopsAtBothOfItsOwnEnds()
    {
        SeatInfo rearMg = seat(135F, 225F);

        assertEquals(135F, rearMg.clampYaw(120F), EPSILON);
        assertEquals(225F, rearMg.clampYaw(240F), EPSILON);
        // The same angles named from the other side of the wrap boundary.
        assertEquals(225F, rearMg.clampYaw(-120F), EPSILON);
        assertEquals(135F, rearMg.clampYaw(-240F), EPSILON);
    }

    @Test
    void forwardArcIsUnchanged()
    {
        // The SdKfz 251's front MG: "Passenger 1 ... core -60 60 -15 25".
        SeatInfo frontMg = seat(-60F, 60F);

        assertEquals(0F, frontMg.clampYaw(0F), EPSILON);
        assertEquals(45F, frontMg.clampYaw(45F), EPSILON);
        assertEquals(60F, frontMg.clampYaw(90F), EPSILON);
        assertEquals(-60F, frontMg.clampYaw(-90F), EPSILON);
        assertEquals(-60F, frontMg.clampYaw(180F), EPSILON);
    }

    @Test
    void fullTraverseStaysFree()
    {
        SeatInfo turret = seat(-360F, 360F);

        assertEquals(0F, turret.clampYaw(360F), EPSILON);
        assertEquals(-90F, turret.clampYaw(270F), EPSILON);
        assertEquals(179F, turret.clampYaw(179F), EPSILON);
    }

    @Test
    void depressionAndElevationAreReadTheGunnersWayRound()
    {
        // The SdKfz 251's front MG: "-15 25" is 15 degrees down, 25 degrees up.
        // Aim pitch is vanilla view space, where a positive angle points down.
        SeatInfo frontMg = seat(-60F, 60F, -15F, 25F);

        assertEquals(15F, frontMg.clampPitch(90F), EPSILON);
        assertEquals(-25F, frontMg.clampPitch(-90F), EPSILON);
        assertEquals(10F, frontMg.clampPitch(10F), EPSILON);
        assertEquals(-20F, frontMg.clampPitch(-20F), EPSILON);
    }

    @Test
    void rearGunKeepsItsWiderElevation()
    {
        // The rear MG: "-10 40" is 10 degrees down, 40 degrees up.
        SeatInfo rearMg = seat(135F, 225F, -10F, 40F);

        assertEquals(10F, rearMg.clampPitch(45F), EPSILON);
        assertEquals(-40F, rearMg.clampPitch(-45F), EPSILON);
    }

    @Test
    void tankTurretKeepsItsDepressionAndElevation()
    {
        // The Hellcat's driver: "-10 20", a 76mm with 10 down and 20 up.
        SeatInfo driver = seat(-360F, 360F, -10F, 20F);

        assertEquals(10F, driver.clampPitch(60F), EPSILON);
        assertEquals(-20F, driver.clampPitch(-60F), EPSILON);
    }
}
