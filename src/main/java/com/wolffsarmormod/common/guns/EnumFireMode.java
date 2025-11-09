package com.wolffsarmormod.common.guns;

import java.util.Locale;

public enum EnumFireMode
{
    SEMIAUTO,
    FULLAUTO,
    MINIGUN,
    BURST;

    public static EnumFireMode getFireMode(String s)
    {
        return switch (s.toLowerCase(Locale.ROOT))
        {
            case "fullauto" -> FULLAUTO;
            case "minigun" -> MINIGUN;
            case "burst" -> BURST;
            default -> SEMIAUTO;
        };
    }
}
