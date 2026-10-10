package com.wolffsmod.npcs.combat;

import com.wolffsmod.npcs.properties.NpcPresentation;
import com.wolffsmod.npcs.properties.NpcTypeProperties;

/** Implemented on Custom NPCs' stats object so its editor, save and preset paths share the settings. */
public interface NpcWeaponSettings
{
    NpcWeaponOptions wolffsmodnpcsWeaponOptions();

    NpcTypeProperties wolffsmodnpcsTypeProperties();

    NpcPresentation wolffsmodnpcsPresentation();
}
