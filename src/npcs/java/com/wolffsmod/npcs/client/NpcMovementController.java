package com.wolffsmod.npcs.client;

import com.wolffsmod.npcs.combat.NpcWeaponOptions;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;

import java.util.Locale;

/** One editor choice for the two saved switches that select a vehicle NPC's movement controller. */
enum NpcMovementController
{
    FREE, HULL, VEHICLE;

    static NpcMovementController of(NpcWeaponOptions options)
    {
        if (!options.enabled(Feature.HULL_FACES_TRAVEL))
            return FREE;
        return options.enabled(Feature.DRIVING) ? VEHICLE : HULL;
    }

    void apply(NpcWeaponOptions options)
    {
        options.set(Feature.HULL_FACES_TRAVEL, this != FREE);
        // Free movement ignores the driving switch. Keep its saved preference until a controller is chosen again.
        if (this != FREE)
            options.set(Feature.DRIVING, this == VEHICLE);
    }

    String key()
    {
        return "wolffsmodnpcs.locomotion.controller." + name().toLowerCase(Locale.ROOT);
    }
}
