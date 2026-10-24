package com.wolffsmod.npcs.properties;

/** The selected entity model's maximum health before Custom NPCs replaces it with the NPC's own maximum. */
public interface NpcModelHealth
{
    /** Returns the model's original maximum, or NaN until a model has been created. Not saved with NPC defaults. */
    double wolffsmodnpcsModelMaxHealth();
}
