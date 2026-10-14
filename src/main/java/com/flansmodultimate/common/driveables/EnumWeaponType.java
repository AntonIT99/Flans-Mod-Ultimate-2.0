package com.flansmodultimate.common.driveables;

import java.util.*;

public enum EnumWeaponType
{
    NONE, MISSILE, BOMB, SHELL, MINE, GUN;

    public static final Set<EnumWeaponType> TAB_GUNS_TYPES = Collections.unmodifiableSet(EnumSet.of(MINE, GUN));
    public static final Set<EnumWeaponType> TAB_DRIVEABLES_TYPES = Collections.unmodifiableSet(EnumSet.of(MISSILE, BOMB, SHELL));

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
