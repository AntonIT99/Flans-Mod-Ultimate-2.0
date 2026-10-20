package com.wolffsmod.npcs.combat;

import com.wolffsmod.npcs.properties.NpcPresentation;
import com.wolffsmod.npcs.properties.NpcTypeProperties;
import com.wolffsmod.npcs.sim.NpcTuning;

/** Implemented on Custom NPCs' stats object so its editor, save and preset paths share the settings. */
public interface NpcWeaponSettings
{
    NpcWeaponOptions wolffsmodnpcsWeaponOptions();

    NpcTypeProperties wolffsmodnpcsTypeProperties();

    NpcPresentation wolffsmodnpcsPresentation();

    NpcCombatSettings wolffsmodnpcsCombat();

    NpcPartSettings wolffsmodnpcsParts();

    NpcTuning wolffsmodnpcsTuning();

    /** @return how many times the server had applied its ranges to every NPC when this NPC last took them */
    int wolffsmodnpcsRangeGeneration();

    void wolffsmodnpcsRangeGeneration(int generation);
}
