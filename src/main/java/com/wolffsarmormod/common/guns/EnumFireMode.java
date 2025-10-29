package com.wolffsarmormod.common.guns;

public enum EnumFireMode
{
    SEMIAUTO, FULLAUTO, MINIGUN, BURST;

    public static EnumFireMode getFireMode(String s)
    {
        return switch (s.toLowerCase())
        {
            case "fullauto" -> FULLAUTO;
            case "minigun" -> MINIGUN;
            case "burst" -> BURST;
            default -> SEMIAUTO;
        };
    }
}
