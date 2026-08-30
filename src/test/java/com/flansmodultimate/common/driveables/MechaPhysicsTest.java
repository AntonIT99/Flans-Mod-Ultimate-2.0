package com.flansmodultimate.common.driveables;

import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MechaPhysicsTest
{
    private static final double EPSILON = 1.0E-6D;

    @Test
    void forwardAndStrafeUseTheMechaAimBasis()
    {
        assertVector(MechaPhysics.movementIntent(0F, 1F, 0F), 0D, 0D, 1D);
        assertVector(MechaPhysics.movementIntent(0F, -1F, 0F), 0D, 0D, -1D);
        assertVector(MechaPhysics.movementIntent(0F, 0F, 1F), -1D, 0D, 0D);
        assertVector(MechaPhysics.movementIntent(0F, 0F, -1F), 1D, 0D, 0D);
        assertVector(MechaPhysics.movementIntent(90F, 1F, 0F), -1D, 0D, 0D);
    }

    @Test
    void driverMovementUsesTheMountedCameraOffset()
    {
        assertEquals(90F, MechaPhysics.driverMovementYaw(0F), EPSILON);
        assertEquals(-170F, MechaPhysics.driverMovementYaw(100F), EPSILON);
    }

    @Test
    void worldSpaceTorsoTargetsAreStableWhenPacketsAreRetried()
    {
        assertEquals(30F, MechaPhysics.relativeAimYaw(10F, 40F), EPSILON);
        assertEquals(0F, MechaPhysics.relativeAimYaw(40F, 40F), EPSILON);
        assertEquals(20F, MechaPhysics.relativeAimYaw(170F, -170F), EPSILON);
    }

    @Test
    void clientMouseAimIsConsumedAsTheTorsoTurns()
    {
        assertEquals(0F, MechaPhysics.consumeParentYaw(30F, 10F, 40F), EPSILON);
        assertEquals(5F, MechaPhysics.consumeParentYaw(30F, 10F, 35F), EPSILON);
        assertEquals(-10F, MechaPhysics.consumeParentYaw(10F, 170F, -170F), EPSILON);
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
        assertEquals(-90F, MechaPhysics.movementYaw(new Vec3(0D, 0D, 1D), 45F), EPSILON);
        assertEquals(0F, MechaPhysics.movementYaw(new Vec3(-1D, 0D, 0D), 45F), EPSILON);
        assertEquals(-180F, MechaPhysics.movementYaw(new Vec3(1D, 0D, 0D), 45F), EPSILON);
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
