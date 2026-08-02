package com.flansmodultimate.common.teams;

import com.flansmodultimate.common.types.Team;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** One entry in the server's map rotation. */
public final class TeamsRound implements Comparable<TeamsRound>
{
    private final UUID id;
    private final String mapId;
    private final String gameTypeId;
    private final List<String> teamIds;
    private final int timeLimitMinutes;
    private final int scoreLimit;
    private float popularity;
    private int roundsSincePlayed;

    public TeamsRound(String mapId, String gameTypeId, List<String> teamIds, int timeLimitMinutes, int scoreLimit)
    {
        this(UUID.randomUUID(), mapId, gameTypeId, teamIds, timeLimitMinutes, scoreLimit, 0.5F, 0);
    }

    private TeamsRound(UUID id, String mapId, String gameTypeId, List<String> teamIds, int timeLimitMinutes,
                       int scoreLimit, float popularity, int roundsSincePlayed)
    {
        this.id = id;
        this.mapId = Objects.requireNonNull(mapId).toLowerCase(java.util.Locale.ROOT);
        this.gameTypeId = Objects.requireNonNull(gameTypeId).toLowerCase(java.util.Locale.ROOT);
        this.teamIds = teamIds.stream().map(idValue -> idValue.toLowerCase(java.util.Locale.ROOT)).toList();
        this.timeLimitMinutes = Math.max(1, timeLimitMinutes);
        this.scoreLimit = Math.max(1, scoreLimit);
        this.popularity = Math.max(0F, Math.min(1F, popularity));
        this.roundsSincePlayed = Math.max(0, roundsSincePlayed);
    }

    public UUID getId() { return id; }
    public String getMapId() { return mapId; }
    public String getGameTypeId() { return gameTypeId; }
    public List<String> getTeamIds() { return teamIds; }
    public int getTimeLimitMinutes() { return timeLimitMinutes; }
    public int getTimeLimitTicks() { return timeLimitMinutes * 60 * 20; }
    public int getScoreLimit() { return scoreLimit; }
    public float getPopularity() { return popularity; }
    public int getRoundsSincePlayed() { return roundsSincePlayed; }
    public void markPlayed() { roundsSincePlayed = 0; }
    public void markSkipped() { roundsSincePlayed++; }
    public float getWeight() { return popularity * 4F + roundsSincePlayed; }

    @Nullable
    public GameType getGametype()
    {
        return GameType.get(gameTypeId);
    }

    @Nullable
    public Team getTeam(int index)
    {
        return index >= 0 && index < teamIds.size() ? Team.getTeam(teamIds.get(index)) : null;
    }

    public int getTeamId(@Nullable Team team)
    {
        if (team == null)
            return 0;
        if (Team.SPECTATORS.equals(team))
            return 1;
        int index = teamIds.indexOf(team.getOriginalShortName());
        return index < 0 ? 0 : index + 2;
    }

    public CompoundTag save()
    {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", id);
        tag.putString("Map", mapId);
        tag.putString("GameType", gameTypeId);
        ListTag teams = new ListTag();
        teamIds.forEach(team -> teams.add(StringTag.valueOf(team)));
        tag.put("Teams", teams);
        tag.putInt("TimeLimit", timeLimitMinutes);
        tag.putInt("ScoreLimit", scoreLimit);
        tag.putFloat("Popularity", popularity);
        tag.putInt("RoundsSincePlayed", roundsSincePlayed);
        return tag;
    }

    public static TeamsRound load(CompoundTag tag)
    {
        List<String> teams = tag.getList("Teams", Tag.TAG_STRING).stream().map(Tag::getAsString).toList();
        UUID id = tag.hasUUID("Id") ? tag.getUUID("Id") : UUID.randomUUID();
        return new TeamsRound(id, tag.getString("Map"), tag.getString("GameType"), teams,
            tag.getInt("TimeLimit"), tag.getInt("ScoreLimit"), tag.getFloat("Popularity"), tag.getInt("RoundsSincePlayed"));
    }

    @Override
    public int compareTo(@NotNull TeamsRound other)
    {
        return Float.compare(other.getWeight(), getWeight());
    }

    @Override
    public boolean equals(Object value)
    {
        return value instanceof TeamsRound other && id.equals(other.id);
    }

    @Override
    public int hashCode()
    {
        return id.hashCode();
    }
}
