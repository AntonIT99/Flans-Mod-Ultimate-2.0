package com.flansmodultimate.common.driveables.armor;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VehicleExplosionDamageBudgetTest
{
    @Test
    void onePartKeepsItsFullDamage()
    {
        assertEquals(List.of(100F), VehicleExplosionDamageBudget.allocate(List.of(100F)));
    }

    @Test
    void nearbyPartsTakeReducedDamageWithoutExceedingTheVehicleBudget()
    {
        List<Float> allocated = VehicleExplosionDamageBudget.allocate(List.of(100F, 100F, 100F, 100F, 100F));

        assertEquals(100F, allocated.get(0));
        assertEquals(200D, allocated.stream().mapToDouble(Float::doubleValue).sum(), 1.0E-4D);
        assertTrue(allocated.subList(1, allocated.size()).stream().allMatch(damage -> damage < 100F));
    }

    @Test
    void coveredPrimaryDoesNotPreventVisibleSecondaryDamage()
    {
        assertEquals(List.of(0F, 50F), VehicleExplosionDamageBudget.allocate(List.of(0F, 100F)));
    }

    @Test
    void destroyedSecondaryDoesNotConsumeTheRemainingBudget()
    {
        List<Float> raw = List.of(100F, 100F, 100F, 100F, 100F);
        double budget = VehicleExplosionDamageBudget.maximumTotal(raw) - raw.get(0);
        float firstSecondary = VehicleExplosionDamageBudget.allocateSecondaries(raw.subList(1, 5), budget).get(0);

        // Another part is destroyed by the first hit; only the two intact parts remain.
        List<Float> survivors = VehicleExplosionDamageBudget.allocateSecondaries(
            List.of(100F, 100F), budget - firstSecondary);
        assertEquals(25F, firstSecondary);
        assertEquals(List.of(37.5F, 37.5F), survivors);
    }
}
