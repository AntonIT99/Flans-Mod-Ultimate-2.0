package com.wolffsmod.npcs.teams;

/** Faction-owned monster aggression, shared by the editor and server target selector. */
public interface FactionMobSettings
{
    boolean wolffsmodnpcsAttackMobs();

    void wolffsmodnpcsSetAttackMobs(boolean enabled);
}
