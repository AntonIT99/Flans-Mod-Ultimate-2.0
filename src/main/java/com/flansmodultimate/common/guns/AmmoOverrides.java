package com.flansmodultimate.common.guns;

import com.flansmodultimate.common.types.BulletType;
import com.flansmodultimate.common.types.TypeFile;
import com.flansmodultimate.util.ResourceUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reader and lookup table for the per-ammunition overrides a weapon may declare.
 *
 * <p>The reader collects warnings instead of logging, so a malformed optional line can
 * never abort content loading and the whole class stays unit testable.
 *
 * <p>Recognised keys, all repeatable and all keyed by the ammunition's
 * <em>original</em> short name as written in its own definition:
 *
 * <pre>
 * AmmoMass             &lt;ammoShortName&gt; &lt;grams&gt;
 * AmmoMuzzleVelocity   &lt;ammoShortName&gt; &lt;metresPerSecond&gt;
 * AmmoExplosiveMass    &lt;ammoShortName&gt; &lt;kgTntEquivalent&gt;
 * AmmoPenetrationAt100m&lt;ammoShortName&gt; &lt;millimetres&gt;
 * AddRoundForAmmo      &lt;ammoShortName&gt; &lt;name&gt; &lt;count&gt; &lt;massG&gt; &lt;explKg&gt; &lt;mps&gt; &lt;mm&gt;
 * </pre>
 */
public final class AmmoOverrides
{
    public static final String KEY_MASS = "AmmoMass";
    public static final String KEY_MUZZLE_VELOCITY = "AmmoMuzzleVelocity";
    public static final String KEY_EXPLOSIVE_MASS = "AmmoExplosiveMass";
    public static final String KEY_PENETRATION = "AmmoPenetrationAt100m";
    public static final String KEY_ADD_ROUND = "AddRoundForAmmo";

    public static final AmmoOverrides EMPTY = new AmmoOverrides(Map.of());

    private final Map<String, AmmoOverride> byAmmo;

    private AmmoOverrides(Map<String, AmmoOverride> byAmmo)
    {
        this.byAmmo = Collections.unmodifiableMap(byAmmo);
    }

    public boolean isEmpty()
    {
        return byAmmo.isEmpty();
    }

    /** The declared keys, for debug output and tests. */
    public Map<String, AmmoOverride> asMap()
    {
        return byAmmo;
    }

    /**
     * @param ammoOriginalShortName the ammunition's own {@code ShortName}, matched case-insensitively
     * @return the override for that ammunition, or null when this weapon declares none for it
     */
    @Nullable
    public AmmoOverride get(@Nullable String ammoOriginalShortName)
    {
        if (ammoOriginalShortName == null || byAmmo.isEmpty())
            return null;
        return byAmmo.get(key(ammoOriginalShortName));
    }

    public static String key(String ammoShortName)
    {
        return ResourceUtils.sanitize(ammoShortName);
    }

    public record Result(AmmoOverrides overrides, List<String> warnings)
    {
        public Result
        {
            warnings = List.copyOf(warnings);
        }
    }

    /** Mutable accumulator, one per ammunition short name. */
    private static final class Builder
    {
        private Float mass;
        private Float speed;
        private Float explosiveMass;
        private Float penetration;
        private final List<BulletType.RoundEntry> rounds = new ArrayList<>();

        private AmmoOverride build()
        {
            return new AmmoOverride(mass, speed, explosiveMass, penetration, rounds);
        }
    }

    public static Result read(@Nullable TypeFile file)
    {
        List<String> warnings = new ArrayList<>();
        if (file == null)
            return new Result(EMPTY, warnings);

        Map<String, Builder> builders = new LinkedHashMap<>();
        readScalar(file, KEY_MASS, "grams", builders, warnings, (b, v) -> b.mass = v);
        // Authored in m/s for consistency with MuzzleVelocity; stored in blocks per tick.
        readScalar(file, KEY_MUZZLE_VELOCITY, "metres per second", builders, warnings,
            (b, v) -> b.speed = v / 20F);
        readScalar(file, KEY_EXPLOSIVE_MASS, "kilograms TNT equivalent", builders, warnings,
            (b, v) -> b.explosiveMass = v);
        readScalar(file, KEY_PENETRATION, "millimetres", builders, warnings, (b, v) -> b.penetration = v);
        readRounds(file, builders, warnings);

        Map<String, AmmoOverride> result = new LinkedHashMap<>();
        for (Map.Entry<String, Builder> entry : builders.entrySet())
        {
            AmmoOverride override = entry.getValue().build();
            if (!override.isEmpty())
                result.put(entry.getKey(), override);
        }
        return new Result(result.isEmpty() ? EMPTY : new AmmoOverrides(result), warnings);
    }

    private interface Assign
    {
        void accept(Builder builder, float value);
    }

    private static void readScalar(TypeFile file, String key, String unit, Map<String, Builder> builders,
                                   List<String> warnings, Assign assign)
    {
        for (String[] values : lines(file, key))
        {
            if (values.length < 2)
            {
                warnings.add(key + " requires <ammoShortName> <" + unit + ">; ignoring '"
                    + String.join(" ", values) + "'");
                continue;
            }
            Float value = parseNonNegative(values[1], key, unit, warnings);
            if (value == null)
                continue;
            assign.accept(builders.computeIfAbsent(key(values[0]), k -> new Builder()), value);
        }
    }

    private static void readRounds(TypeFile file, Map<String, Builder> builders, List<String> warnings)
    {
        for (String[] values : lines(file, KEY_ADD_ROUND))
        {
            if (values.length < 4)
            {
                warnings.add(KEY_ADD_ROUND + " requires <ammoShortName> <name> <count> <massG>"
                    + " [explosiveMassKg] [muzzleVelocityMps] [penetrationMm]; ignoring '"
                    + String.join(" ", values) + "'");
                continue;
            }
            int count;
            try
            {
                count = Integer.parseInt(values[2]);
            }
            catch (NumberFormatException ex)
            {
                warnings.add(KEY_ADD_ROUND + " count must be a whole number but was '" + values[2]
                    + "'; ignoring the line");
                continue;
            }
            if (count <= 0)
            {
                warnings.add(KEY_ADD_ROUND + " count must be greater than zero but was '" + values[2]
                    + "'; ignoring the line");
                continue;
            }
            Float mass = parseNonNegative(values[3], KEY_ADD_ROUND, "mass in grams", warnings);
            if (mass == null)
                continue;
            float explosiveMass = optional(values, 4, KEY_ADD_ROUND, "explosive mass in kg TNT equivalent", warnings);
            float speed = optional(values, 5, KEY_ADD_ROUND, "muzzle velocity in m/s", warnings) / 20F;
            float penetration = optional(values, 6, KEY_ADD_ROUND, "penetration at 100 m in millimetres", warnings);
            builders.computeIfAbsent(key(values[0]), k -> new Builder()).rounds.add(
                new BulletType.RoundEntry(values[1], count,
                    new BulletType.RoundStats(mass, explosiveMass, speed, penetration)));
        }
    }

    private static float optional(String[] values, int index, String key, String unit, List<String> warnings)
    {
        if (index >= values.length)
            return 0F;
        Float value = parseNonNegative(values[index], key, unit, warnings);
        return value == null ? 0F : value;
    }

    @Nullable
    private static Float parseNonNegative(String raw, String key, String unit, List<String> warnings)
    {
        float value;
        try
        {
            value = Float.parseFloat(raw);
        }
        catch (NumberFormatException ex)
        {
            warnings.add(key + " " + unit + " must be numeric but was '" + raw + "'; ignoring it");
            return null;
        }
        if (!Float.isFinite(value) || value < 0F)
        {
            warnings.add(key + " " + unit + " must be finite and non-negative but was '" + raw
                + "'; ignoring it");
            return null;
        }
        return value;
    }

    @NotNull
    private static List<String[]> lines(TypeFile file, String key)
    {
        List<String> raw = file.getConfigLines(key);
        if (raw == null)
            return List.of();
        List<String> cleaned = new ArrayList<>();
        for (String line : raw)
        {
            if (line == null)
                continue;
            String value = line.trim();
            int comment = value.indexOf("//");
            if (comment >= 0)
                value = value.substring(0, comment).trim();
            if (value.startsWith("="))
                value = value.substring(1).trim();
            if (!value.isEmpty())
                cleaned.add(value);
        }
        List<String[]> out = new ArrayList<>(cleaned.size());
        for (String line : cleaned)
        {
            String[] split = line.split("\\s+");
            // Some readers hand back the key as the first token; drop it when present.
            if (split.length > 0 && split[0].equalsIgnoreCase(key))
                split = java.util.Arrays.copyOfRange(split, 1, split.length);
            if (split.length > 0)
                out.add(split);
        }
        return out;
    }
}
