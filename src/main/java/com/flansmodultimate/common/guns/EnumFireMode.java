package com.flansmodultimate.common.guns;

public enum EnumFireMode
{
    SEMIAUTO,
    FULLAUTO,
    MINIGUN,
    BURST;

    public static EnumFireMode getFireMode(String s)
    {
        for (EnumFireMode mode : EnumFireMode.values())
        {
            if (s.equalsIgnoreCase(mode.toString()))
                return mode;
        }
        return SEMIAUTO;
    }
}
