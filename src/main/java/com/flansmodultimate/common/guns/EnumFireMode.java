package com.flansmodultimate.common.guns;

public enum EnumFireMode
{
    SEMIAUTO,
    FULLAUTO,
    MINIGUN,
    BURST;

    public boolean isAutomaticFire()
    {
        return this == FULLAUTO || this == MINIGUN;
    }

    public static EnumFireMode getFireMode(String s)
    {
        String normalized = normalize(s);
        for (EnumFireMode mode : EnumFireMode.values())
        {
            if (normalized.equals(normalize(mode.name())) || normalized.equals(normalize(mode.toString())))
                return mode;
        }
        return SEMIAUTO;
    }

    private static String normalize(String s)
    {
        return s == null ? "" : s.replace("-", "").replace("_", "").replace(" ", "").toLowerCase();
    }

    public String getDisplayName()
    {
        return switch (this)
        {
            case SEMIAUTO -> "Semi-Auto";
            case FULLAUTO -> "Full-Auto";
            case MINIGUN -> "Minigun";
            case BURST -> "Burst";
        };
    }
}
