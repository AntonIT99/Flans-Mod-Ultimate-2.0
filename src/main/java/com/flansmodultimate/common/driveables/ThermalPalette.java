package com.flansmodultimate.common.driveables;

import java.util.Locale;

/** Colour scheme of a thermal sight, from the Labjac Edition's {@code ThermalVisionColor}. */
public enum ThermalPalette
{
    WHITE, GREEN, RED;

    /** Reads a palette name; anything unrecognised falls back to white-hot, the Labjac Edition default. */
    public static ThermalPalette parse(String value)
    {
        String name = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return switch (name)
        {
            case "green", "greenhot", "green_hot", "green-hot" -> GREEN;
            case "red", "redhot", "red_hot", "red-hot" -> RED;
            default -> WHITE;
        };
    }
}
