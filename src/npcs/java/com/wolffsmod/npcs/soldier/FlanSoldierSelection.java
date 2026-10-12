package com.wolffsmod.npcs.soldier;

import com.flansmodultimate.api.*;

import net.minecraft.nbt.CompoundTag;

import java.util.Optional;

/**
 * The Flan's team and class a builder picked for an NPC in the model picker. It only records the choice the look came
 * from: the NPC's equipment changes when the builder asks for the class loadout, never because a class is selected.
 */
public record FlanSoldierSelection(ITeam team, IPlayerClass playerClass)
{
    /** Keys in the model's extra data, which Custom NPCs saves and synchronizes with the selected model and presets. */
    public static final String TEAM_KEY = "WolffsModSoldierTeam";
    public static final String CLASS_KEY = "WolffsModSoldierClass";

    /** @return the selection saved in the model's extra data, or empty without one or when its team or class is not loaded */
    public static Optional<FlanSoldierSelection> read(CompoundTag extra)
    {
        return resolve(extra.getString(TEAM_KEY), extra.getString(CLASS_KEY));
    }

    /** @return the class of that name in that team; a class the team does not offer is not a selection */
    public static Optional<FlanSoldierSelection> resolve(String team, String playerClass)
    {
        if (team.isEmpty() || playerClass.isEmpty())
            return Optional.empty();
        return FlansModApi.getTeam(team)
            .flatMap(found -> found.getClasses().stream().filter(candidate -> candidate.getShortName().equals(playerClass)).findFirst().map(candidate -> new FlanSoldierSelection(found, candidate)));
    }

    public void write(CompoundTag extra)
    {
        extra.putString(TEAM_KEY, team.getShortName());
        extra.putString(CLASS_KEY, playerClass.getShortName());
    }
}
