package com.flansmodultimate.common.driveables;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MechaPhysicsTest
{
    private static final double EPSILON = 1.0E-6D;

    @Test
    void forwardAndStrafeUseTheMechaAimBasis()
    {
        assertVector(MechaPhysics.movementIntent(0F, 1F, 0F), -1D, 0D, 0D);
        assertVector(MechaPhysics.movementIntent(0F, 0F, 1F), 0D, 0D, 1D);
        assertVector(MechaPhysics.movementIntent(90F, 1F, 0F), 0D, 0D, -1D);
    }

    @Test
    void diagonalInputIsNormalizedWithoutReducingCardinalSpeed()
    {
        Vec3 diagonal = MechaPhysics.movementIntent(0F, 1F, 1F);

        assertEquals(1D, diagonal.length(), EPSILON);
        assertEquals(-Math.sqrt(0.5D), diagonal.x, EPSILON);
        assertEquals(Math.sqrt(0.5D), diagonal.z, EPSILON);
    }

    @Test
    void configuredSpeedUsesTheLegacyPerTickConversion()
    {
        assertEquals(0.43D, MechaPhysics.movementSpeed(2F, 1F, 1F), EPSILON);
        assertEquals(0.645D, MechaPhysics.movementSpeed(2F, 1.5F, 1F), EPSILON);
    }

    @Test
    void rotateSpeedIsDegreesPerTickAndUsesTheShortestTurn()
    {
        assertEquals(20F, MechaPhysics.approachYaw(0F, 90F, 20F), EPSILON);
        assertEquals(-170F, MechaPhysics.approachYaw(170F, -170F, 40F), EPSILON);
        assertEquals(170F, MechaPhysics.approachYaw(-170F, 170F, 20F), EPSILON);
    }

    @Test
    void movementDirectionConvertsBackToEntityYaw()
    {
        assertEquals(0F, MechaPhysics.movementYaw(new Vec3(-1D, 0D, 0D), 45F), EPSILON);
        assertEquals(90F, MechaPhysics.movementYaw(new Vec3(0D, 0D, -1D), 45F), EPSILON);
        assertEquals(-90F, MechaPhysics.movementYaw(new Vec3(0D, 0D, 1D), 45F), EPSILON);
    }

    @Test
    void throttleRepresentsWalkingEffortInEveryDirection()
    {
        assertEquals(1F, MechaPhysics.throttle(0F, 1F), EPSILON);
        assertEquals(1F, MechaPhysics.throttle(-1F, 0F), EPSILON);
        assertEquals(1F, MechaPhysics.throttle(1F, 1F), EPSILON);
    }

    private static void assertVector(Vec3 actual, double x, double y, double z)
    {
        assertEquals(x, actual.x, EPSILON);
        assertEquals(y, actual.y, EPSILON);
        assertEquals(z, actual.z, EPSILON);
    }
}
