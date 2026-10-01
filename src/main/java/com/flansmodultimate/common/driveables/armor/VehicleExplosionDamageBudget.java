package com.flansmodultimate.common.driveables.armor;

import java.util.ArrayList;
import java.util.List;

/** Limits the combined HP damage of one blast after each exposed part resolves its own armour. */
public final class VehicleExplosionDamageBudget
{
    private static final float SECONDARY_PART_SCALE = 0.5F;
    private static final float MAX_TOTAL_SCALE = 2F;

    private VehicleExplosionDamageBudget() {}

    /** Input order is nearest part first. Its full hit is preserved; other parts share the remaining budget. */
    public static List<Float> allocate(List<Float> rawDamage)
    {
        if (rawDamage.isEmpty())
            return List.of();

        float primary = validDamage(rawDamage.get(0));
        List<Float> allocated = new ArrayList<>(rawDamage.size());
        allocated.add(primary);
        allocated.addAll(allocateSecondaries(rawDamage.subList(1, rawDamage.size()),
            maximumTotal(rawDamage) - primary));
        return List.copyOf(allocated);
    }

    public static double maximumTotal(List<Float> rawDamage)
    {
        double strongest = 0D;
        for (Float damage : rawDamage)
            strongest = Math.max(strongest, validDamage(damage));
        return strongest * MAX_TOTAL_SCALE;
    }

    /** Recalculate after a part disappears so it cannot consume another part's share of the cap. */
    public static List<Float> allocateSecondaries(List<Float> rawDamage, double remainingBudget)
    {
        double secondaryTotal = 0D;
        List<Float> weighted = new ArrayList<>(rawDamage.size());
        for (Float damage : rawDamage)
        {
            float reduced = validDamage(damage) * SECONDARY_PART_SCALE;
            weighted.add(reduced);
            secondaryTotal += reduced;
        }

        double budget = Double.isFinite(remainingBudget) ? Math.max(0D, remainingBudget) : 0D;
        double secondaryFactor = secondaryTotal > budget && secondaryTotal > 0D
            ? budget / secondaryTotal : 1D;
        for (int index = 0; index < weighted.size(); index++)
            weighted.set(index, (float) (weighted.get(index) * secondaryFactor));
        return List.copyOf(weighted);
    }

    private static float validDamage(Float damage)
    {
        return damage != null && Float.isFinite(damage) ? Math.max(0F, damage) : 0F;
    }
}
