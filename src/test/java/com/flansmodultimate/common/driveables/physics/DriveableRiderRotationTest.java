package com.flansmodultimate.common.driveables.physics;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DriveableRiderRotationTest
{
    private static final float EPSILON = 1.0E-5F;

    @Test
    void levelDriveableLeavesRiderPoseUnchanged()
    {
        Vector3f up = DriveableRiderRotation.forAngles(35F, 35F, 0F, 0F).transform(new Vector3f(0F, 1F, 0F));
        assertEquals(0F, up.x, EPSILON);
        assertEquals(1F, up.y, EPSILON);
        assertEquals(0F, up.z, EPSILON);
    }

    @Test
    void rollTiltsRiderHitboxWithDriveable()
    {
        Vector3f up = DriveableRiderRotation.forAngles(0F, 90F, 0F, 90F).transform(new Vector3f(0F, 1F, 0F));
        assertEquals(-1F, up.x, EPSILON);
        assertEquals(0F, up.y, EPSILON);
        assertEquals(0F, up.z, EPSILON);
    }

    @Test
    void riderBodyHeadingChangesAxisOfRoll()
    {
        Vector3f up = DriveableRiderRotation.forAngles(90F, 90F, 0F, 90F).transform(new Vector3f(0F, 1F, 0F));
        assertEquals(0F, up.x, EPSILON);
        assertEquals(0F, up.y, EPSILON);
        assertEquals(1F, up.z, EPSILON);
    }
}
