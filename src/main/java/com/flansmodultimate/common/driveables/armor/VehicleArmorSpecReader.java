package com.flansmodultimate.common.driveables.armor;

import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.types.TypeFile;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

/** Deterministic reader for optional semantic and per-part armour keys. */
public final class VehicleArmorSpecReader
{
    public static final String ARMOR_FRONT = "ArmorFrontMm";
    public static final String ARMOR_REAR = "ArmorRearMm";
    public static final String ARMOR_SIDE = "ArmorSideMm";
    public static final String ARMOR_TOP = "ArmorTopMm";
    public static final String ARMOR_BOTTOM = "ArmorBottomMm";
    public static final String TURRET_ARMOR_FRONT = "TurretArmorFrontMm";
    public static final String TURRET_ARMOR_REAR = "TurretArmorRearMm";
    public static final String TURRET_ARMOR_SIDE = "TurretArmorSideMm";
    public static final String TURRET_ARMOR_TOP = "TurretArmorTopMm";
    public static final String TURRET_ARMOR_BOTTOM = "TurretArmorBottomMm";
    public static final String PART_ARMOR = "PartArmorMm";

    private static final float MAX_SLOPE_DEG = 89F;

    private VehicleArmorSpecReader() {}

    public record Result(VehicleArmorSpec spec, List<String> warnings)
    {
        public Result
        {
            warnings = List.copyOf(warnings);
        }
    }

    public static Result read(@Nullable TypeFile file)
    {
        List<String> warnings = new ArrayList<>();
        if (file == null)
            return new Result(VehicleArmorSpec.EMPTY, warnings);

        EnumMap<EnumArmorFacing, ArmorPlate> hull = new EnumMap<>(EnumArmorFacing.class);
        EnumMap<EnumArmorFacing, ArmorPlate> turret = new EnumMap<>(EnumArmorFacing.class);
        readFacing(file, ARMOR_FRONT, hull, EnumArmorFacing.FRONT, warnings);
        readFacing(file, ARMOR_REAR, hull, EnumArmorFacing.REAR, warnings);
        readSides(file, ARMOR_SIDE, hull, warnings);
        readFacing(file, ARMOR_TOP, hull, EnumArmorFacing.TOP, warnings);
        readFacing(file, ARMOR_BOTTOM, hull, EnumArmorFacing.BOTTOM, warnings);
        readFacing(file, TURRET_ARMOR_FRONT, turret, EnumArmorFacing.FRONT, warnings);
        readFacing(file, TURRET_ARMOR_REAR, turret, EnumArmorFacing.REAR, warnings);
        readSides(file, TURRET_ARMOR_SIDE, turret, warnings);
        readFacing(file, TURRET_ARMOR_TOP, turret, EnumArmorFacing.TOP, warnings);
        readFacing(file, TURRET_ARMOR_BOTTOM, turret, EnumArmorFacing.BOTTOM, warnings);

        EnumMap<EnumDriveablePart, ArmorPlate> parts = new EnumMap<>(EnumDriveablePart.class);
        List<String> lines = file.getConfigLines(PART_ARMOR);
        if (lines != null)
        {
            for (String line : lines)
            {
                String[] values = tokens(line);
                if (values.length == 0)
                    continue;
                if (values.length < 2)
                {
                    warnings.add(PART_ARMOR + " requires <part> <millimetres>; ignoring '" + clean(line) + "'");
                    continue;
                }
                EnumDriveablePart part = EnumDriveablePart.getPart(values[0]);
                if (part == null)
                {
                    warnings.add(PART_ARMOR + " names unknown part '" + values[0] + "'; ignoring it");
                    continue;
                }
                Float thickness = parseThickness(values[1], PART_ARMOR + " " + values[0], warnings);
                if (thickness != null)
                    parts.put(part, new ArmorPlate(thickness, 0F));
            }
        }
        return new Result(new VehicleArmorSpec(hull, turret, parts), warnings);
    }

    private static void readSides(TypeFile file, String key, EnumMap<EnumArmorFacing, ArmorPlate> target,
                                  List<String> warnings)
    {
        ArmorPlate plate = readLastPlate(file, key, warnings);
        if (plate != null)
        {
            target.put(EnumArmorFacing.LEFT, plate);
            target.put(EnumArmorFacing.RIGHT, plate);
        }
    }

    private static void readFacing(TypeFile file, String key, EnumMap<EnumArmorFacing, ArmorPlate> target,
                                   EnumArmorFacing facing, List<String> warnings)
    {
        ArmorPlate plate = readLastPlate(file, key, warnings);
        if (plate != null)
            target.put(facing, plate);
    }

    @Nullable
    private static ArmorPlate readLastPlate(TypeFile file, String key, List<String> warnings)
    {
        List<String> lines = file.getConfigLines(key);
        if (lines == null)
            return null;
        for (int index = lines.size() - 1; index >= 0; index--)
        {
            String line = lines.get(index);
            String[] values = tokens(line);
            if (values.length == 0)
                continue;
            Float thickness = parseThickness(values[0], key, warnings);
            if (thickness == null)
                return null;
            float slope = 0F;
            if (values.length > 1)
            {
                try
                {
                    slope = Float.parseFloat(values[1]);
                }
                catch (NumberFormatException ex)
                {
                    warnings.add(key + " slope must be a number in degrees but was '" + values[1] + "'; ignoring the line");
                    return null;
                }
                if (!Float.isFinite(slope) || slope < 0F || slope > MAX_SLOPE_DEG)
                {
                    warnings.add(key + " slope must be finite and between 0 and " + MAX_SLOPE_DEG
                        + " degrees but was '" + values[1] + "'; ignoring the line");
                    return null;
                }
            }
            return new ArmorPlate(thickness, slope);
        }
        return null;
    }

    @Nullable
    private static Float parseThickness(String raw, String key, List<String> warnings)
    {
        float value;
        try
        {
            value = Float.parseFloat(raw);
        }
        catch (NumberFormatException ex)
        {
            warnings.add(key + " thickness must be a number in millimetres but was '" + raw + "'; ignoring it");
            return null;
        }
        if (!Float.isFinite(value) || value < 0F)
        {
            warnings.add(key + " thickness must be a finite non-negative value in millimetres but was '"
                + raw + "'; ignoring it");
            return null;
        }
        return value;
    }

    private static String[] tokens(@Nullable String line)
    {
        String value = clean(line);
        return value.isEmpty() ? new String[0] : value.split("\\s+");
    }

    private static String clean(@Nullable String line)
    {
        if (line == null)
            return "";
        String value = line.trim();
        int comment = value.indexOf("//");
        if (comment >= 0)
            value = value.substring(0, comment).trim();
        if (value.startsWith("="))
            value = value.substring(1).trim();
        return value;
    }
}
