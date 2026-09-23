package com.flansmodultimate.config;

import java.util.Locale;

/** Defines how a category property combines with values that were applied earlier. */
public enum CategoryPropertyMode
{
    APPEND,
    REPLACE,
    IF_ABSENT;

    public static CategoryPropertyMode fromConfigValue(String value)
    {
        if (value == null)
            return APPEND;

        return switch (value.trim().toLowerCase(Locale.ROOT))
        {
            case "replace" -> REPLACE;
            case "ifabsent" -> IF_ABSENT;
            default -> APPEND;
        };
    }
}
