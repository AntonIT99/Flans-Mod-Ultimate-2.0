package com.flansmodultimate.common.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModPhysicsTest
{
    @Test
    void neutralFactorsPreserveAuthoredPhysics()
    {
        assertEquals(0.024525D, ModPhysics.gravity(0.024525D, 1D));
        assertEquals(0.8D, ModPhysics.dragRetention(0.8D, 1D));
        assertEquals(12D, ModPhysics.dragForce(12D, 1D));
    }

    @Test
    void zeroRemovesGravityAndDragWithoutChangingThrust()
    {
        assertEquals(0D, ModPhysics.gravity(0.05D, 0D));
        assertEquals(1D, ModPhysics.dragRetention(0.6D, 0D));
        assertEquals(0D, ModPhysics.dragForce(12D, 0D));
    }

    @Test
    void strongerDragCompoundsVelocityLoss()
    {
        assertEquals(0.64D, ModPhysics.dragRetention(0.8D, 2D), 1E-12D);
        assertEquals(24D, ModPhysics.dragForce(12D, 2D));
    }
}
