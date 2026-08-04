package com.flansmodultimate.common.driveables;

import java.util.Locale;

public enum EnumPlaneMode
{
    PLANE,
    VTOL,
    HELI,
    SIXDOF;

    public static EnumPlaneMode parse(String value)
    {
        if (value == null)
            return PLANE;
        String normalized = value.trim().replace("-", "").replace("_", "").toUpperCase(Locale.ROOT);
        if (normalized.equals("HELICOPTER"))
            return HELI;
        if (normalized.equals("6DOF"))
            return SIXDOF;
        for (EnumPlaneMode mode : values())
        {
            if (mode.name().equals(normalized))
                return mode;
        }
        return PLANE;
    }
}
