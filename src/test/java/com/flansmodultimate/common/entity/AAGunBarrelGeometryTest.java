package com.flansmodultimate.common.entity;

import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.util.ModUtils;
import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AAGunBarrelGeometryTest
{
    private static final double EPSILON = 1.0E-5D;
    // ModUtils aims through Mth.sin, which reads a lookup table.
    private static final double AIM_EPSILON = 1.0E-3D;

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
    void aBarrelLaidAlongTheAimFollowsItAsTheGunElevates()
    {
        double length = 3D;
        Vector3f line = AAGunBarrelGeometry.legacyBarrelFor(new Vec3(0D, 0D, length), false);

        for (float[] aim : new float[][] { { 0F, -45F }, { 37F, -70F }, { -140F, 20F } })
        {
            Vec3 expected = ModUtils.getDirectionFromPitchAndYaw(aim[1], aim[0]).scale(length);
            Vec3 legacy = AAGunBarrelGeometry.legacyBarrelOffset(line.x, line.y, line.z, aim[0], aim[1]);
            assertEquals(expected.x, legacy.x, AIM_EPSILON);
            assertEquals(expected.y, legacy.y, AIM_EPSILON);
            assertEquals(expected.z, legacy.z, AIM_EPSILON);
        }
    }

    @Test
    void aForwardBarrelRisesWhenTheGunAimsUp()
    {
        // Flak 88 line: Barrel 0 88 40 0.
        Vec3 rest = AAGunBarrelGeometry.legacyBarrelOffset(88D, 40D, 0D, 0F, 0F);
        Vec3 raised = AAGunBarrelGeometry.legacyBarrelOffset(88D, 40D, 0D, 0F, -60F);

        assertTrue(raised.y > rest.y + 3D);
        assertEquals(rest.length(), raised.length(), EPSILON);
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
