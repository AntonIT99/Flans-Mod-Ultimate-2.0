package com.flansmodultimate.common.driveables.armor;

import com.flansmodultimate.common.driveables.EnumDriveablePart;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** Optional armour authored by a content pack. Map absence is distinct from an explicit zero plate. */
public record VehicleArmorSpec(
    Map<EnumArmorFacing, ArmorPlate> hull,
    Map<EnumArmorFacing, ArmorPlate> turret,
    Map<EnumDriveablePart, ArmorPlate> partOverrides)
{
    public static final VehicleArmorSpec EMPTY = new VehicleArmorSpec(Map.of(), Map.of(), Map.of());

    public VehicleArmorSpec
    {
        hull = immutableFacingMap(hull);
        turret = immutableFacingMap(turret);
        EnumMap<EnumDriveablePart, ArmorPlate> parts = new EnumMap<>(EnumDriveablePart.class);
        if (partOverrides != null)
            parts.putAll(partOverrides);
        partOverrides = Collections.unmodifiableMap(parts);
    }

    private static Map<EnumArmorFacing, ArmorPlate> immutableFacingMap(
        @Nullable Map<EnumArmorFacing, ArmorPlate> source)
    {
        EnumMap<EnumArmorFacing, ArmorPlate> result = new EnumMap<>(EnumArmorFacing.class);
        if (source != null)
            result.putAll(source);
        return Collections.unmodifiableMap(result);
    }

    public boolean isEmpty()
    {
        return hull.isEmpty() && turret.isEmpty() && partOverrides.isEmpty();
    }
}
