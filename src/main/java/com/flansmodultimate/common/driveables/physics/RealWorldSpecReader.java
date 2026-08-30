package com.flansmodultimate.common.driveables.physics;

import com.flansmodultimate.common.types.TypeFile;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Reads the optional {@code Real*} and {@code DriveType} keys out of a legacy
 * type file.
 *
 * <p>Two properties matter here. First, the reader touches no key that already
 * existed, so legacy parsing is bit-for-bit unchanged. Second, it never logs
 * directly: malformed values are collected as warning strings and handed back to
 * the caller. That keeps the class free of any dependency on the mod class and
 * its registries, which is what makes it unit testable, and it means a malformed
 * optional value can never abort content pack loading.
 */
public final class RealWorldSpecReader
{
    // Common keys.
    public static final String KEY_MASS = "RealMassKg";
    public static final String KEY_MAX_SPEED = "RealMaxSpeedKmh";
    public static final String KEY_ENGINE_POWER = "RealEnginePowerKw";
    public static final String KEY_ENGINE_POWER_HP = "RealEnginePowerHp";
    public static final String KEY_ENGINE_POWER_PS = "RealEnginePowerPS";
    public static final String KEY_ENGINE_THRUST = "RealEngineThrustKn";
    // Aircraft keys.
    public static final String KEY_WING_SPAN = "RealWingSpanM";
    public static final String KEY_WING_AREA = "RealWingAreaM2";
    public static final String KEY_CLIMB_RATE = "RealClimbRateMs";
    // Ground keys.
    public static final String KEY_DRIVE_TYPE = "DriveType";
    public static final String KEY_MAX_REVERSE_SPEED = "RealMaxReverseSpeedKmh";
    // Marine keys.
    public static final String KEY_DRAFT = "RealDraftM";

    private RealWorldSpecReader() {}

    /**
     * The parsed specification plus any warnings the caller should surface.
     * Warnings are plain sentences without a file prefix; the caller adds that.
     */
    public record Result(RealWorldVehicleSpec spec, List<String> warnings)
    {
        public Result
        {
            warnings = List.copyOf(warnings);
        }

        public boolean hasWarnings()
        {
            return !warnings.isEmpty();
        }
    }

    public static Result read(TypeFile file)
    {
        List<String> warnings = new ArrayList<>();
        if (file == null)
            return new Result(RealWorldVehicleSpec.EMPTY, warnings);

        Float massKg = readPositive(file, KEY_MASS, warnings, "kilograms");
        Float maxSpeedKmh = readPositive(file, KEY_MAX_SPEED, warnings, "km/h");
        Float enginePowerKw = readEnginePowerKw(file, warnings);
        Float engineThrustKn = readPositive(file, KEY_ENGINE_THRUST, warnings, "kilonewtons");

        RealWorldVehicleSpec.Aircraft aircraft = new RealWorldVehicleSpec.Aircraft(
            readPositive(file, KEY_WING_SPAN, warnings, "metres"),
            readPositive(file, KEY_WING_AREA, warnings, "square metres"),
            readPositive(file, KEY_CLIMB_RATE, warnings, "metres per second"));

        RealWorldVehicleSpec.Ground ground = new RealWorldVehicleSpec.Ground(
            readDriveType(file, warnings),
            readPositive(file, KEY_MAX_REVERSE_SPEED, warnings, "km/h"));

        RealWorldVehicleSpec.Marine marine = new RealWorldVehicleSpec.Marine(
            readPositive(file, KEY_DRAFT, warnings, "metres"));

        RealWorldVehicleSpec spec = new RealWorldVehicleSpec(
            massKg, maxSpeedKmh, enginePowerKw, engineThrustKn, aircraft, ground, marine);

        if (VehiclePhysicsUnits.isUsablePositive(enginePowerKw) && VehiclePhysicsUnits.isUsablePositive(engineThrustKn))
            warnings.add("Both " + KEY_ENGINE_POWER + " and " + KEY_ENGINE_THRUST
                + " are set; thrust is used for aircraft and power for everything else");

        return new Result(spec, warnings);
    }

    @Nullable
    private static Float readPositive(TypeFile file, String key, List<String> warnings, String unit)
    {
        String raw = firstToken(file, key);
        if (raw == null)
            return null;
        float value;
        try
        {
            value = Float.parseFloat(raw);
        }
        catch (NumberFormatException ex)
        {
            warnings.add(key + " must be a number in " + unit + " but was '" + raw + "'; ignoring it");
            return null;
        }
        if (!VehiclePhysicsUnits.isUsablePositive(value))
        {
            warnings.add(key + " must be a finite value greater than zero in " + unit
                + " but was '" + raw + "'; ignoring it");
            return null;
        }
        return value;
    }

    /**
     * Engine power accepts any of three units a reference book might list it in.
     * {@code RealEnginePowerHp} (mechanical/imperial horsepower) and
     * {@code RealEnginePowerPS} (metric horsepower / Pferdestärke) are both
     * converted to kilowatts so the rest of the system only ever handles one
     * unit internally. Following the same last-declared-wins convention the
     * legacy alias readers use elsewhere in this codebase, each key overrides
     * the ones before it in the priority order below when more than one is
     * present: {@code RealEnginePowerKw} < {@code RealEnginePowerHp} <
     * {@code RealEnginePowerPS}.
     */
    @Nullable
    private static Float readEnginePowerKw(TypeFile file, List<String> warnings)
    {
        Float kw = readPositive(file, KEY_ENGINE_POWER, warnings, "kilowatts");
        Float hp = readPositive(file, KEY_ENGINE_POWER_HP, warnings, "horsepower");
        if (hp != null)
            kw = (float) VehiclePhysicsUnits.hpToKw(hp);
        Float ps = readPositive(file, KEY_ENGINE_POWER_PS, warnings, "metric horsepower (PS)");
        if (ps != null)
            kw = (float) VehiclePhysicsUnits.psToKw(ps);
        return kw;
    }

    @Nullable
    private static EnumDriveType readDriveType(TypeFile file, List<String> warnings)
    {
        String raw = firstToken(file, KEY_DRIVE_TYPE);
        if (raw == null)
            return null;
        EnumDriveType parsed = EnumDriveType.parse(raw);
        if (parsed == null)
        {
            warnings.add(KEY_DRIVE_TYPE + " '" + raw + "' is not one of "
                + String.join(", ", names()) + "; falling back to the legacy drive layout");
            return null;
        }
        return parsed;
    }

    private static String[] names()
    {
        EnumDriveType[] values = EnumDriveType.values();
        String[] names = new String[values.length];
        for (int i = 0; i < values.length; i++)
            names[i] = values[i].name();
        return names;
    }

    /**
     * Returns the first whitespace-separated token of the last non-blank line
     * declared for {@code key}, or {@code null} when the key is absent or present
     * without a value. Legacy files commonly leave optional keys present and
     * empty, and that must mean "not specified", never "specified as zero".
     */
    @Nullable
    private static String firstToken(TypeFile file, String key)
    {
        if (!file.hasConfigLine(key))
            return null;
        List<String> lines = file.getConfigLines(key);
        if (lines == null)
            return null;
        for (int i = lines.size() - 1; i >= 0; i--)
        {
            String token = firstToken(lines.get(i));
            if (token != null)
                return token;
        }
        return null;
    }

    @Nullable
    private static String firstToken(@Nullable String line)
    {
        if (line == null)
            return null;
        String value = line.trim();
        int comment = value.indexOf("//");
        if (comment >= 0)
            value = value.substring(0, comment).trim();
        if (value.startsWith("="))
            value = value.substring(1).trim();
        if (value.isEmpty())
            return null;
        String[] split = value.split("\\s+");
        String token = split[0].trim();
        return token.isEmpty() ? null : token;
    }

    /** Lower-cased key list, used by the debug command to echo what a pack declared. */
    public static List<String> allKeys()
    {
        return List.of(KEY_MASS, KEY_MAX_SPEED, KEY_ENGINE_POWER, KEY_ENGINE_POWER_HP, KEY_ENGINE_POWER_PS,
            KEY_ENGINE_THRUST, KEY_WING_SPAN, KEY_WING_AREA, KEY_CLIMB_RATE,
            KEY_DRIVE_TYPE, KEY_MAX_REVERSE_SPEED, KEY_DRAFT);
    }

    /** Normalised form used when matching keys case-insensitively. */
    public static String normalizeKey(String key)
    {
        return key.toLowerCase(Locale.ROOT);
    }
}
