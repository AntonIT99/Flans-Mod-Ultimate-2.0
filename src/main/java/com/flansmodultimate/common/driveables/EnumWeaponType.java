package com.flansmodultimate.common.driveables;

import java.util.EnumSet;
import java.util.Set;

public enum EnumWeaponType
{
    NONE,
    MISSILE,
    BOMB,
    SHELL,
    MINE,
    GUN;

    public static final Set<EnumWeaponType> TAB_GUNS_TYPES = EnumSet.of(MINE, GUN);
    public static final Set<EnumWeaponType> TAB_DRIVEABLES_TYPES = EnumSet.of(MISSILE, BOMB, SHELL);

    public static EnumWeaponType parse(String value, EnumWeaponType fallback)
    {
        if (value == null)
            return fallback;
        try
        {
            return valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        }
        catch (IllegalArgumentException ignored)
        {
            return fallback;
        }
    }
}
