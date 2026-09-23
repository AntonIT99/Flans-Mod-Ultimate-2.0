package com.flansmodultimate.common.driveables.armor;

import com.flansmodultimate.common.driveables.CollisionBox;
import com.flansmodultimate.common.driveables.EnumDriveablePart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Pure mass^(2/3) health derivation that preserves authored HP only as relative weights. */
public final class VehicleHealthScaler
{
    private VehicleHealthScaler() {}

    /** Mass-normalized health for a single entity with no per-part allocation. */
    public record SingleResult(boolean requested, boolean enabled, float health, List<String> warnings)
    {
        public SingleResult
        {
            warnings = List.copyOf(warnings);
        }
    }

    public record Result(boolean requested, boolean enabled, float totalHp,
                         Map<EnumDriveablePart, CollisionBox> boxes,
                         Map<EnumDriveablePart, Float> allocations,
                         List<String> warnings)
    {
        public Result
        {
            boxes = immutableBoxes(boxes);
            EnumMap<EnumDriveablePart, Float> allocationCopy = new EnumMap<>(EnumDriveablePart.class);
            if (allocations != null)
                allocationCopy.putAll(allocations);
            allocations = Collections.unmodifiableMap(allocationCopy);
            warnings = List.copyOf(warnings);
        }
    }

    public static Result resolve(boolean requested, Float massKg,
                                 Map<EnumDriveablePart, CollisionBox> authoredBoxes, double healthScale)
    {
        Map<EnumDriveablePart, CollisionBox> original = immutableBoxes(authoredBoxes);
        if (!requested)
            return legacy(false, original, List.of());

        List<String> warnings = new ArrayList<>();
        Float totalHp = calculateTotalHp(massKg, healthScale, warnings,
            "authored hitbox health", "Realistic vehicle health");
        if (totalHp == null)
            return legacy(true, original, warnings);

        Map<EnumDriveablePart, Double> weights = resolveWeights(original, warnings);
        if (weights.isEmpty())
        {
            // Nothing to split the total between, but the mass curve is still the honest
            // total for this hull, so the flag keeps affecting the number it is about.
            warnings.add("UseRealisticVehicleHealth found no hitboxes to distribute health over; applying the derived total only");
            return new Result(true, true, totalHp, original, Map.of(), warnings);
        }
        double weightTotal = weights.values().stream().mapToDouble(Double::doubleValue).sum();

        EnumMap<EnumDriveablePart, CollisionBox> scaled = new EnumMap<>(EnumDriveablePart.class);
        EnumMap<EnumDriveablePart, Float> allocations = new EnumMap<>(EnumDriveablePart.class);
        for (Map.Entry<EnumDriveablePart, CollisionBox> entry : original.entrySet())
        {
            CollisionBox box = entry.getValue();
            if (box == null)
                continue;
            double weight = weights.getOrDefault(entry.getKey(), 0D);
            float hp = weight <= 0D ? 0F : (float) (totalHp * weight / weightTotal);
            scaled.put(entry.getKey(), copyWithHealth(box, hp));
            allocations.put(entry.getKey(), hp);
        }
        return new Result(true, true, totalHp, scaled, allocations, warnings);
    }

    /**
     * Applies the same mass^(2/3) health curve to a single-health entity such as
     * an AA gun. Invalid inputs retain the authored legacy health.
     */
    public static SingleResult resolveSingle(boolean requested, Float massKg, float authoredHealth,
                                             double healthScale)
    {
        if (!requested)
            return new SingleResult(false, false, authoredHealth, List.of());

        List<String> warnings = new ArrayList<>();
        Float totalHp = calculateTotalHp(massKg, healthScale, warnings,
            "authored health", "Realistic health");
        if (totalHp == null)
            return new SingleResult(true, false, authoredHealth, warnings);
        return new SingleResult(true, true, totalHp, warnings);
    }

    private static Float calculateTotalHp(Float massKg, double healthScale, List<String> warnings,
                                          String fallbackDescription, String calculationDescription)
    {
        if (massKg == null || !Float.isFinite(massKg) || massKg <= 0F)
        {
            warnings.add("UseRealisticVehicleHealth requires a valid RealMassKg; retaining "
                + fallbackDescription);
            return null;
        }
        if (!Double.isFinite(healthScale) || healthScale <= 0D)
        {
            warnings.add("realisticVehicleHealthScale must be finite and greater than zero; retaining "
                + fallbackDescription);
            return null;
        }

        double total = healthScale * Math.pow(massKg, 2D / 3D);
        if (!Double.isFinite(total) || total <= 0D)
        {
            warnings.add(calculationDescription + " calculation was invalid; retaining " + fallbackDescription);
            return null;
        }
        return (float) total;
    }

    private static Result legacy(boolean requested, Map<EnumDriveablePart, CollisionBox> boxes,
                                 List<String> warnings)
    {
        EnumMap<EnumDriveablePart, Float> allocations = new EnumMap<>(EnumDriveablePart.class);
        float total = 0F;
        for (Map.Entry<EnumDriveablePart, CollisionBox> entry : boxes.entrySet())
        {
            float hp = entry.getValue() == null ? 0F : entry.getValue().getHealth();
            allocations.put(entry.getKey(), hp);
            if (hp > 0F)
                total += hp;
        }
        return new Result(requested, false, total, boxes, allocations, warnings);
    }

    /**
     * How the derived total is split between hitboxes. Authored health is the intent of
     * the pack, so it wins; packs that never bothered with per-part health still get a
     * usable split from hitbox volume, and boxes with no size at all share equally.
     */
    private static Map<EnumDriveablePart, Double> resolveWeights(Map<EnumDriveablePart, CollisionBox> boxes,
                                                                 List<String> warnings)
    {
        EnumMap<EnumDriveablePart, Double> weights = new EnumMap<>(EnumDriveablePart.class);
        for (Map.Entry<EnumDriveablePart, CollisionBox> entry : boxes.entrySet())
        {
            CollisionBox box = entry.getValue();
            if (box != null && box.getHealth() > 0F && Float.isFinite(box.getHealth()))
                weights.put(entry.getKey(), (double) box.getHealth());
        }
        if (!weights.isEmpty())
            return weights;

        for (Map.Entry<EnumDriveablePart, CollisionBox> entry : boxes.entrySet())
        {
            CollisionBox box = entry.getValue();
            if (box == null)
                continue;
            double volume = (double) box.getWidth() * box.getHeight() * box.getDepth();
            if (volume > 0D && Double.isFinite(volume))
                weights.put(entry.getKey(), volume);
        }
        if (!weights.isEmpty())
        {
            warnings.add("UseRealisticVehicleHealth found no positive hitbox health weights; distributing health by hitbox volume");
            return weights;
        }

        for (Map.Entry<EnumDriveablePart, CollisionBox> entry : boxes.entrySet())
        {
            if (entry.getValue() != null)
                weights.put(entry.getKey(), 1D);
        }
        if (!weights.isEmpty())
            warnings.add("UseRealisticVehicleHealth found no hitbox health or volume weights; splitting health evenly between parts");
        return weights;
    }

    private static CollisionBox copyWithHealth(CollisionBox box, float health)
    {
        return CollisionBox.inWorldUnits(health, box.getX(), box.getY(), box.getZ(), box.getWidth(),
            box.getHeight(), box.getDepth(), box.getPenetrationResistance(), box.getCrewDamageMultiplier());
    }

    private static Map<EnumDriveablePart, CollisionBox> immutableBoxes(
        Map<EnumDriveablePart, CollisionBox> source)
    {
        EnumMap<EnumDriveablePart, CollisionBox> copy = new EnumMap<>(EnumDriveablePart.class);
        if (source != null)
            copy.putAll(source);
        return Collections.unmodifiableMap(copy);
    }
}
