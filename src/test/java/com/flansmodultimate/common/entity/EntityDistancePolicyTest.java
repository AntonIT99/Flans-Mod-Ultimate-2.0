package com.flansmodultimate.common.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityDistancePolicyTest
{
    @Test
    void preservesProportionalityAndMinecraftScaling()
    {
        double normal = EntityDistancePolicy.proportionalDistance(2D, 1.5D, 1D);
        assertEquals(192D, normal);
        assertEquals(normal * 2D, EntityDistancePolicy.proportionalDistance(4D, 1.5D, 1D));
        assertEquals(normal * 0.5D, EntityDistancePolicy.proportionalDistance(2D, 1.5D, 0.5D));
        assertEquals(normal * 2D, EntityDistancePolicy.proportionalDistance(2D, 3D, 1D));
    }

    @Test
    void malformedGeometryAndSettingsCannotProduceAnUnboundedDistance()
    {
        assertEquals(64D, EntityDistancePolicy.proportionalDistance(Double.NaN, 1D, 1D));
        assertEquals(64D, EntityDistancePolicy.proportionalDistance(1D, Double.POSITIVE_INFINITY, 1D));
        assertEquals(64D, EntityDistancePolicy.proportionalDistance(1D, 1D, Double.NaN));
        assertEquals(64D, EntityDistancePolicy.proportionalDistance(1D, 1D, -1D));
        assertEquals(64D, EntityDistancePolicy.proportionalDistance(Double.MAX_VALUE, 1D, 4D));
        assertTrue(Double.isFinite(EntityDistancePolicy.proportionalDistance(1D, 1D, Double.POSITIVE_INFINITY)));
    }
}
