package com.flansmodultimate.common.teams;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** The single SavedData payload for maps, rotations, runtime state and player stats. */
public final class TeamsSavedData extends SavedData
{
    public static final String ID = "flansmodultimate_teams";

    final Map<String, TeamsMap> maps = new LinkedHashMap<>();
    final List<TeamsRound> rounds = new ArrayList<>();
    final Map<UUID, PlayerStats> stats = new LinkedHashMap<>();
    final CompoundTag runtime = new CompoundTag();

    public Collection<PlayerStats> getStats() { return java.util.Collections.unmodifiableCollection(stats.values()); }

    @Override
    public CompoundTag save(CompoundTag tag)
    {
        ListTag mapList = new ListTag();
        maps.values().forEach(map -> mapList.add(map.save()));
        tag.put("Maps", mapList);

        ListTag roundList = new ListTag();
        rounds.forEach(round -> roundList.add(round.save()));
        tag.put("Rounds", roundList);

        ListTag statList = new ListTag();
        stats.values().forEach(stat -> statList.add(stat.save()));
        tag.put("Stats", statList);
        tag.put("Runtime", runtime.copy());
        return tag;
    }

    public static TeamsSavedData load(CompoundTag tag)
    {
        TeamsSavedData result = new TeamsSavedData();
        for (Tag entry : tag.getList("Maps", Tag.TAG_COMPOUND))
        {
            TeamsMap map = TeamsMap.load((CompoundTag) entry);
            result.maps.put(map.getShortName(), map);
        }
        for (Tag entry : tag.getList("Rounds", Tag.TAG_COMPOUND))
            result.rounds.add(TeamsRound.load((CompoundTag) entry));
        for (Tag entry : tag.getList("Stats", Tag.TAG_COMPOUND))
        {
            PlayerStats stats = PlayerStats.load((CompoundTag) entry);
            result.stats.put(stats.getPlayerId(), stats);
        }
        if (tag.contains("Runtime", Tag.TAG_COMPOUND))
            result.runtime.merge(tag.getCompound("Runtime"));
        return result;
    }
}
