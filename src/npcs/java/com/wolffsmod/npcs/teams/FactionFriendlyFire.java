package com.wolffsmod.npcs.teams;

import noppes.npcs.controllers.data.Faction;

/** A Custom NPCs faction's own friendly fire settings, saved with the faction. */
public interface FactionFriendlyFire
{
    FriendlyFire.Setting wolffsmodnpcsFriendlyFire(FriendlyFire.Kind kind);

    void wolffsmodnpcsSetFriendlyFire(FriendlyFire.Kind kind, FriendlyFire.Setting setting);

    static FactionFriendlyFire of(Faction faction)
    {
        return (FactionFriendlyFire) faction;
    }
}
