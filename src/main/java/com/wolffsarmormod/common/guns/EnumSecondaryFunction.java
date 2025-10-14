package com.wolffsarmormod.common.guns;

public enum EnumSecondaryFunction
{
    ZOOM, ADS_ZOOM, MELEE, CUSTOM_MELEE;

    public static EnumSecondaryFunction get(String s)
    {
        s = s.toLowerCase();
        return switch (s)
        {
            case "zoom" -> ZOOM;
            case "melee" -> MELEE;
            case "custommelee", "custom_melee" -> CUSTOM_MELEE;
            default -> ADS_ZOOM;
        };
    }
}
