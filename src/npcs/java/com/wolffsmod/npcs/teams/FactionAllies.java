package com.wolffsmod.npcs.teams;

import noppes.npcs.controllers.data.Faction;

import java.util.Set;

/** The ids of the factions a Custom NPCs faction is allied with, saved with the faction. */
public interface FactionAllies
{
    Set<Integer> wolffsmodnpcsAllies();

    static Set<Integer> of(Faction faction)
    {
        return ((FactionAllies) faction).wolffsmodnpcsAllies();
    }
}
