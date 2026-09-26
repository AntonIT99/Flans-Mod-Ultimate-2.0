package com.flansmodultimate.config;

/**
 * How a player's arms hold a gun, as everyone sees them. This is the player's own choice, which the server
 * may override for everyone with {@link EnumPlayerAimPose}.
 */
public enum EnumAimPose
{
    /** A held gun always keeps the arms raised in the aiming pose. */
    ENFORCED,
    /** A gun is raised only while it is fired or aimed; a shield stays raised. */
    DYNAMIC
}
