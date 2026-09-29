package com.flansmodultimate.common.driveables;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlaneRiderRotationTest
{
    private static final float EPSILON = 1.0E-5F;

    @Test
    void levelPlaneLeavesRiderPoseUnchanged()
    {
        Vector3f up = PlaneRiderRotation.forAngles(35F, 35F, 0F, 0F)
            .transform(new Vector3f(0F, 1F, 0F));
        assertEquals(0F, up.x, EPSILON);
        assertEquals(1F, up.y, EPSILON);
        assertEquals(0F, up.z, EPSILON);
    }

    @Test
    void rollTiltsRiderHitboxWithAircraft()
    {
        Vector3f up = PlaneRiderRotation.forAngles(0F, 0F, 0F, 90F)
            .transform(new Vector3f(0F, 1F, 0F));
        assertEquals(-1F, up.x, EPSILON);
        assertEquals(0F, up.y, EPSILON);
        assertEquals(0F, up.z, EPSILON);
    }

    @Test
    void riderBodyHeadingChangesAxisOfRoll()
    {
        Vector3f up = PlaneRiderRotation.forAngles(90F, 0F, 0F, 90F)
            .transform(new Vector3f(0F, 1F, 0F));
        assertEquals(0F, up.x, EPSILON);
        assertEquals(0F, up.y, EPSILON);
        assertEquals(1F, up.z, EPSILON);
    }
}
