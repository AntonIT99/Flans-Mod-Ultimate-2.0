package com.flansmodultimate.common.driveables;

import java.util.EnumSet;
import java.util.Set;

public enum EnumWeaponType
{
    MISSILE,
    BOMB,
    SHELL,
    MINE,
    GUN;

    public static final Set<EnumWeaponType> TAB_GUNS_TYPES = EnumSet.of(MINE, GUN);
    public static final Set<EnumWeaponType> TAB_DRIVEABLES_TYPES = EnumSet.of(MISSILE, BOMB, SHELL);
}
