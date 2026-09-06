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
    void invalidMassFallsBackSafely()
    {
        VehicleHealthScaler.Result noMass = VehicleHealthScaler.resolve(true, null,
            Map.of(EnumDriveablePart.CORE, box(100F)), 5D);
        assertFalse(noMass.enabled());
        assertEquals(100F, noMass.boxes().get(EnumDriveablePart.CORE).getHealth());
        assertFalse(noMass.warnings().isEmpty());
    }

    /**
     * Plenty of packs never author per-part health. The mass curve is still the better
     * number for them, so the split falls back to how big each hitbox is.
     */
    @Test
    void missingHealthWeightsFallBackToHitboxVolume()
    {
        EnumMap<EnumDriveablePart, CollisionBox> boxes = new EnumMap<>(EnumDriveablePart.class);
        boxes.put(EnumDriveablePart.CORE, box(0F, 2F, 1F, 1F));
        boxes.put(EnumDriveablePart.TURRET, box(0F, 1F, 1F, 1F));
        VehicleHealthScaler.Result result = VehicleHealthScaler.resolve(true, 1_000F, boxes, 5D);

        assertTrue(result.enabled());
        assertEquals(500F, result.totalHp(), 1.0E-3F);
        assertEquals(333.333F, result.allocations().get(EnumDriveablePart.CORE), 1.0E-2F);
        assertEquals(166.666F, result.allocations().get(EnumDriveablePart.TURRET), 1.0E-2F);
        assertFalse(result.warnings().isEmpty());
    }

    @Test
    void hitboxesWithoutHealthOrVolumeShareEvenly()
    {
        EnumMap<EnumDriveablePart, CollisionBox> boxes = new EnumMap<>(EnumDriveablePart.class);
        boxes.put(EnumDriveablePart.CORE, box(0F, 0F, 0F, 0F));
        boxes.put(EnumDriveablePart.TURRET, box(0F, 0F, 0F, 0F));
        VehicleHealthScaler.Result result = VehicleHealthScaler.resolve(true, 1_000F, boxes, 5D);

        assertTrue(result.enabled());
        assertEquals(250F, result.allocations().get(EnumDriveablePart.CORE), 1.0E-3F);
        assertEquals(250F, result.allocations().get(EnumDriveablePart.TURRET), 1.0E-3F);
    }

    /** With nothing to split the total between, the derived total still applies to the hull. */
    @Test
    void noHitboxesAtAllStillDerivesTheTotal()
    {
        VehicleHealthScaler.Result result = VehicleHealthScaler.resolve(true, 1_000F, Map.of(), 5D);
        assertTrue(result.enabled());
        assertEquals(500F, result.totalHp(), 1.0E-3F);
        assertTrue(result.boxes().isEmpty());
        assertFalse(result.warnings().isEmpty());
    }

    @Test
    void singleEntityUsesTheSameMassCurveWithoutPartWeights()
    {
        VehicleHealthScaler.SingleResult result = VehicleHealthScaler.resolveSingle(true, 1_000F, 20F, 5D);
        assertTrue(result.enabled());
        assertEquals(500F, result.health(), 1.0E-3F);

        VehicleHealthScaler.SingleResult fallback = VehicleHealthScaler.resolveSingle(true, null, 20F, 5D);
        assertFalse(fallback.enabled());
        assertEquals(20F, fallback.health());
        assertFalse(fallback.warnings().isEmpty());
    }

    private static void assertTotal(float mass, float expected)
    {
        VehicleHealthScaler.Result result = VehicleHealthScaler.resolve(true, mass,
            Map.of(EnumDriveablePart.CORE, box(1F)), 5D);
        assertEquals(expected, result.totalHp(), 1.5F);
    }

    private static CollisionBox box(float health)
    {
        return box(health, 1F, 1F, 1F);
    }

    private static CollisionBox box(float health, float width, float height, float depth)
    {
        return CollisionBox.inWorldUnits(health, 0F, 0F, 0F, width, height, depth, 5F, 0F);
    }
}
