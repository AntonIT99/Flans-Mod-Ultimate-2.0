package com.flansmodultimate.config;

/** The server's rule for how humanoid mobs hold a gun. Mobs have no aim control, so only firing raises one. */
public enum EnumEntityAimPose
{
    /** A held gun always keeps the arms raised in the aiming pose. */
    ENFORCED,
    /** A gun is raised only while the mob is firing it. */
    DYNAMIC
}
