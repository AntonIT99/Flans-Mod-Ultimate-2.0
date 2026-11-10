package com.flansmodultimate.common.eras;

import java.util.List;
import java.util.Locale;

/**
 * A named span of history, such as WW2. Spans may overlap. {@code first} is {@link ServiceDate#OPEN_LOW} for an era with
 * no start and {@code last} is {@link ServiceDate#OPEN_HIGH} for one that has not ended. The future is not an era of a
 * catalog: it is the built-in value {@link #FUTURE_ID}, always later than every era.
 */
public record Era(String id, List<String> aliases, int first, int last)
{
    public static final String FUTURE_ID = "Future";
    private static final List<String> FUTURE_NAMES = List.of("future", "scifi");

    public Era
    {
        aliases = List.copyOf(aliases);
        if (first > last)
            throw new IllegalArgumentException("Era '" + id + "' ends before it starts");
    }

    public boolean contains(int month)
    {
        return first <= month && month <= last;
    }

    /** Whether the normalized name is this era's id or one of its aliases. */
    public boolean isNamed(String normalized)
    {
        return normalize(id).equals(normalized) || aliases.stream().anyMatch(alias -> normalize(alias).equals(normalized));
    }

    /** Era names compare without case, spaces, underscores or hyphens, so Cold War, cold_war and ColdWar are one era. */
    public static String normalize(String name)
    {
        return name.toLowerCase(Locale.ROOT).replaceAll("[\\s_-]", "");
    }

    /** Whether a name means the built-in future rather than an era of the catalog. */
    public static boolean isFutureName(String name)
    {
        return FUTURE_NAMES.contains(normalize(name));
    }
}
