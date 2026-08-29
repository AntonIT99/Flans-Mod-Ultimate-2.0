package com.flansmodultimate.common.driveables.armor;

import com.flansmodultimate.common.driveables.CollisionBox;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VehicleHealthScalerTest
{
    @Test
    void requestedMassPointsMatchTheFivePointScale()
    {
        assertTotal(1_000F, 500F);
        assertTotal(3_000F, 1_040F);
        assertTotal(10_000F, 2_321F);
        assertTotal(20_000F, 3_684F);
        assertTotal(60_000F, 7_663F);
    }

    @Test
    void positiveLegacyHealthBecomesRelativeWeightsAndZeroStaysSpecial()
    {
        EnumMap<EnumDriveablePart, CollisionBox> boxes = new EnumMap<>(EnumDriveablePart.class);
        boxes.put(EnumDriveablePart.CORE, box(100F));
        boxes.put(EnumDriveablePart.TURRET, box(50F));
        boxes.put(EnumDriveablePart.LEFT_TRACK, box(25F));
        boxes.put(EnumDriveablePart.RIGHT_TRACK, box(25F));
        boxes.put(EnumDriveablePart.BARREL, box(0F));
        VehicleHealthScaler.Result result = VehicleHealthScaler.resolve(true, 1_000F, boxes, 5D);
        assertTrue(result.enabled());
        assertEquals(250F, result.allocations().get(EnumDriveablePart.CORE), 1.0E-3F);
        assertEquals(125F, result.allocations().get(EnumDriveablePart.TURRET), 1.0E-3F);
        assertEquals(62.5F, result.allocations().get(EnumDriveablePart.LEFT_TRACK), 1.0E-3F);
        assertEquals(62.5F, result.allocations().get(EnumDriveablePart.RIGHT_TRACK), 1.0E-3F);
        assertEquals(0F, result.boxes().get(EnumDriveablePart.BARREL).getHealth());
    }

    @Test
    void invalidMassAndMissingWeightsFallBackSafely()
    {
        VehicleHealthScaler.Result noMass = VehicleHealthScaler.resolve(true, null,
            Map.of(EnumDriveablePart.CORE, box(100F)), 5D);
        assertFalse(noMass.enabled());
        assertEquals(100F, noMass.boxes().get(EnumDriveablePart.CORE).getHealth());
        assertFalse(noMass.warnings().isEmpty());

        VehicleHealthScaler.Result noWeights = VehicleHealthScaler.resolve(true, 1_000F,
            Map.of(EnumDriveablePart.CORE, box(0F)), 5D);
        assertFalse(noWeights.enabled());
    }

    private static void assertTotal(float mass, float expected)
    {
        VehicleHealthScaler.Result result = VehicleHealthScaler.resolve(true, mass,
            Map.of(EnumDriveablePart.CORE, box(1F)), 5D);
        assertEquals(expected, result.totalHp(), 1.5F);
    }

    private static CollisionBox box(float health)
    {
        return CollisionBox.inWorldUnits(health, 0F, 0F, 0F, 1F, 1F, 1F, 5F, 0F);
    }
}
