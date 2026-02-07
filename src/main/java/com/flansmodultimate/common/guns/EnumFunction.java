package com.flansmodultimate.common.guns;

import java.util.Locale;

public enum EnumFunction
{
    SHOOT,
    ZOOM,
    ADS_ZOOM,
    MELEE,
    CUSTOM_MELEE;

    public boolean isZoom()
    {
        return this == ZOOM || this == ADS_ZOOM;
    }

    public boolean isMelee()
    {
        return this == MELEE || this == CUSTOM_MELEE;
    }

    public static EnumFunction get(String s)
    {
        return switch (s.toLowerCase(Locale.ROOT))
        {
            case "shoot", "fire" -> SHOOT;
            case "zoom" -> ZOOM;
            case "melee" -> MELEE;
            case "custommelee", "custom_melee" -> CUSTOM_MELEE;
            default -> ADS_ZOOM;
        };
    }
}
