package com.flansmodultimate.common.guns;

import java.util.Locale;

public enum EnumFunction
{
    SHOOT,
    ZOOM,
    ADS_ZOOM,
    MELEE,
    CUSTOM_MELEE,
    /** Hold to charge and release to throw the weapon itself, like a trident. Secondary function only. */
    THROW;

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
            case "throw" -> THROW;
            default -> ADS_ZOOM;
        };
    }
}
