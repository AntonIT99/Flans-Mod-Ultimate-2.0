package com.wolffsmod.npcs.names;

import noppes.npcs.controllers.data.Faction;

import net.minecraft.network.chat.Component;

/** Display reference for factions this module creates; faction names and ids remain canonical. */
public interface FactionDisplayName
{
    void wolffsmodnpcsBindName(ContentNameBinding binding);

    Component wolffsmodnpcsNameComponent();

    static Component of(Faction faction)
    {
        return ((FactionDisplayName) faction).wolffsmodnpcsNameComponent();
    }
}
