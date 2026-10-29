package com.wolffsmod.npcs.crew;

import com.flansmodultimate.api.driveables.WeaponMount;

import java.util.Locale;

/** The kind of place a crew member takes in a vehicle NPC, by what its seat offers. */
public enum CrewPost
{
    /** The driver's seat, whose aim turns the main turret. */
    DRIVER,
    /** A seat with a gun of its own. */
    GUNNER,
    /** A seat without a gun. */
    PASSENGER;

    public static CrewPost of(WeaponMount mount)
    {
        if (mount.driver())
            return DRIVER;
        return mount.gun().isPresent() ? GUNNER : PASSENGER;
    }

    /** @return the post's name as commands and saved settings write it */
    public String id()
    {
        return name().toLowerCase(Locale.ROOT);
    }
}
