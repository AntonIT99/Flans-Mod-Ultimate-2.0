package com.flansmodultimate.common.entity;

import com.flansmod.common.vector.Vector3f;
import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AAGunBarrelGeometryTest
{
    private static final double EPSILON = 1.0E-5D;

    @Test
    void aSuggestedBarrelLineLandsOnTheMeasuredMuzzleAtRest()
    {
        Vec3 muzzle = new Vec3(1.25D, 1.4375D, -0.5D);

        Vector3f line = AAGunBarrelGeometry.legacyBarrelFor(muzzle, false);
        Vec3 landed = AAGunBarrelGeometry.legacyBarrelOffset(line.x, line.y, line.z, 0F, 0F);

        assertEquals(muzzle.x, landed.x, EPSILON);
        assertEquals(muzzle.y, landed.y, EPSILON);
        assertEquals(muzzle.z, landed.z, EPSILON);
    }

    @Test
    void theLineKeepsFollowingTheMeasuredMuzzleAsTheGunYaws()
    {
        Vec3 pivot = new Vec3(0D, 20D, 0D);
        Vec3 muzzle = new Vec3(24D, 1D, 3D);
        Vector3f line = AAGunBarrelGeometry.legacyBarrelFor(AAGunBarrelGeometry.modelBarrelOffset(pivot, muzzle, 0F, 0F), false);

        for (float yaw : new float[] { 37F, 90F, -140F })
        {
            Vec3 model = AAGunBarrelGeometry.modelBarrelOffset(pivot, muzzle, yaw, 0F);
            Vec3 legacy = AAGunBarrelGeometry.legacyBarrelOffset(line.x, line.y, line.z, yaw, 0F);
            assertEquals(model.x, legacy.x, EPSILON);
            assertEquals(model.y, legacy.y, EPSILON);
            assertEquals(model.z, legacy.z, EPSILON);
        }
    }

    @Test
    void aSentryLineLeavesRoomForTheSentryLift()
    {
        Vec3 muzzle = new Vec3(0.5D, 2D, 0D);

        Vector3f sentry = AAGunBarrelGeometry.legacyBarrelFor(muzzle, true);
        Vector3f plain = AAGunBarrelGeometry.legacyBarrelFor(muzzle, false);

        assertEquals(plain.y - 1.5F * 16F, sentry.y, 1.0E-3F);
    }
}
