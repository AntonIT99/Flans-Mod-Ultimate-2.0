package com.flansmodultimate.common.driveables;

import java.util.Locale;

public enum EnumMechaToolType
{
    PICKAXE,
    AXE,
    SHOVEL,
    SHEARS,
    SWORD;

    public static EnumMechaToolType parse(String value)
    {
        if (value != null)
        {
            try
            {
                return valueOf(value.trim().toUpperCase(Locale.ROOT));
            }
            catch (IllegalArgumentException ignored) {}
        }
        return SWORD;
    }
}
