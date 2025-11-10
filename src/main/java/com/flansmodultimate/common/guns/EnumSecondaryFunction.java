package com.flansmodultimate.common.guns;

import java.util.Locale;

public enum EnumSecondaryFunction
{
    ZOOM,
    ADS_ZOOM,
    MELEE,
    CUSTOM_MELEE;

    public static EnumSecondaryFunction get(String s)
    {
        return switch (s.toLowerCase(Locale.ROOT))
        {
            case "zoom" -> ZOOM;
            case "melee" -> MELEE;
            case "custommelee", "custom_melee" -> CUSTOM_MELEE;
            default -> ADS_ZOOM;
        };
    }
}
