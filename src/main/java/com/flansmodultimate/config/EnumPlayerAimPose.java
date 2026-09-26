package com.flansmodultimate.config;

/** The server's rule for how players' arms hold a gun. */
public enum EnumPlayerAimPose
{
    /** Each player's own {@link EnumAimPose} choice applies. */
    FREE_CHOICE,
    /** Every player holds guns in the always raised aiming pose. */
    ENFORCED,
    /** Every player raises a gun only while firing or aiming it. */
    DYNAMIC
}
