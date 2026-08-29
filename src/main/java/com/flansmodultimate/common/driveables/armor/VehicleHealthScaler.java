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
        if (massKg == null || !Float.isFinite(massKg) || massKg <= 0F)
        {
            warnings.add("UseRealisticVehicleHealth requires a valid RealMassKg; retaining authored hitbox health");
            return legacy(true, original, warnings);
        }
        if (!Double.isFinite(healthScale) || healthScale <= 0D)
        {
            warnings.add("realisticVehicleHealthScale must be finite and greater than zero; retaining authored hitbox health");
            return legacy(true, original, warnings);
        }

        double weightTotal = 0D;
        for (CollisionBox box : original.values())
        {
            if (box != null && box.getHealth() > 0F)
                weightTotal += box.getHealth();
        }
        if (!(weightTotal > 0D) || !Double.isFinite(weightTotal))
        {
            warnings.add("UseRealisticVehicleHealth found no positive hitbox health weights; retaining authored health");
            return legacy(true, original, warnings);
        }

        double total = healthScale * Math.pow(massKg, 2D / 3D);
        if (!Double.isFinite(total) || total <= 0D)
        {
            warnings.add("Realistic vehicle health calculation was invalid; retaining authored health");
            return legacy(true, original, warnings);
        }

        EnumMap<EnumDriveablePart, CollisionBox> scaled = new EnumMap<>(EnumDriveablePart.class);
        EnumMap<EnumDriveablePart, Float> allocations = new EnumMap<>(EnumDriveablePart.class);
        for (Map.Entry<EnumDriveablePart, CollisionBox> entry : original.entrySet())
        {
            CollisionBox box = entry.getValue();
            if (box == null)
                continue;
            float hp = box.getHealth() <= 0F ? 0F : (float) (total * box.getHealth() / weightTotal);
            scaled.put(entry.getKey(), copyWithHealth(box, hp));
            allocations.put(entry.getKey(), hp);
        }
        return new Result(true, true, (float) total, scaled, allocations, warnings);
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
