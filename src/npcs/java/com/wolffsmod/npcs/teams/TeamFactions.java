package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.teams.ITeam;
import com.flansmodultimate.platform.world.FlanSavedData;
import com.flansmodultimate.platform.world.SavedDataPlatform;
import noppes.npcs.controllers.FactionController;
import noppes.npcs.controllers.data.Faction;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.NotNull;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;

import java.util.HashMap;
import java.util.Map;

/** Ordinary editable Custom NPCs factions associated with Flan teams, independently of hidden Teams-round factions. Server thread only. */
public final class TeamFactions extends FlanSavedData
{
    private static final String ID = "wolffsmodnpcs_team_factions";
    private static final String TEAMS = "teams";
    private final Map<String, Integer> factions = new HashMap<>();

    public static TeamFactions get(MinecraftServer server)
    {
        return SavedDataPlatform.computeIfAbsent(server.overworld(), ID, TeamFactions::new, TeamFactions::load);
    }

    /** Reuses the saved id even after a builder renames or edits the faction. Deleted factions are recreated on request. */
    public Faction getOrCreate(ITeam team)
    {
        FactionController controller = FactionController.instance;
        Integer id = factions.get(team.getShortName());
        Faction faction = id == null ? null : controller.getFaction(id);
        if (faction != null)
            return faction;
        String name = team.getName();
        faction = controller.getFactionFromName(name);
        if (faction == null)
        {
            faction = (Faction) controller.create(name, team.getTeamColour());
            faction.hideFaction = false;
            controller.saveFaction(faction);
        }
        factions.put(team.getShortName(), faction.id);
        setDirty();
        return faction;
    }

    public void apply(EntityNPCInterface npc, ITeam team)
    {
        Faction faction = getOrCreate(team);
        npc.setFaction(faction.id);
        npc.faction = faction;
        npc.updateClient = true;
        npc.updateAI = true;
    }

    @Override
    @NotNull
    public CompoundTag save(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries)
    {
        CompoundTag teams = new CompoundTag();
        factions.forEach(teams::putInt);
        tag.put(TEAMS, teams);
        return tag;
    }

    @SuppressWarnings("unused") // The saved-data platform supplies registries; the mapping contains only ids.
    static TeamFactions load(CompoundTag tag, HolderLookup.Provider registries)
    {
        TeamFactions data = new TeamFactions();
        CompoundTag teams = tag.getCompound(TEAMS);
        for (String team : teams.getAllKeys())
            if (!team.isBlank() && teams.contains(team, Tag.TAG_INT) && teams.getInt(team) >= 0)
                data.factions.put(team, teams.getInt(team));
        return data;
    }
}
