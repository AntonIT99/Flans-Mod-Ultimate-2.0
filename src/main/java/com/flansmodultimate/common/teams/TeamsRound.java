package com.flansmodultimate.common.teams;

import com.flansmodultimate.common.types.Team;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Getter
public final class TeamsRound implements Comparable<TeamsRound>
{
    private static final String NBT_ID = "id";
    private static final String NBT_MAP = "map";
    private static final String NBT_GAME_TYPE = "game_type";
    private static final String NBT_TEAMS = "teams";
    private static final String NBT_TIME_LIMIT = "time_limit";
    private static final String NBT_SCORE_LIMIT = "score_limit";
    private static final String NBT_POPULARITY = "popularity";
    private static final String NBT_ROUNDS_SINCE_PLAYED = "rounds_since_played";

    private final UUID id;
    private final String mapId;
    private final String gameTypeId;
    private final List<String> teamIds;
    private final int timeLimitMinutes;
    private final int scoreLimit;
    private final float popularity;
    private int roundsSincePlayed;

    public TeamsRound(String mapId, String gameTypeId, List<String> teamIds, int timeLimitMinutes, int scoreLimit)
    {
        this(UUID.randomUUID(), mapId, gameTypeId, teamIds, timeLimitMinutes, scoreLimit, 0.5F, 0);
    }

    private TeamsRound(UUID id, String mapId, String gameTypeId, List<String> teamIds, int timeLimitMinutes, int scoreLimit, float popularity, int roundsSincePlayed)
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

    public int getTimeLimitTicks()
    {
        return timeLimitMinutes * 60 * 20;
    }

    public void markPlayed()
    {
        roundsSincePlayed = 0;
    }

    public void markSkipped()
    {
        roundsSincePlayed++;
    }

    public float getWeight()
    {
        return popularity * 4F + roundsSincePlayed;
    }

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
        tag.putUUID(NBT_ID, id);
        tag.putString(NBT_MAP, mapId);
        tag.putString(NBT_GAME_TYPE, gameTypeId);
        ListTag teams = new ListTag();
        teamIds.forEach(team -> teams.add(StringTag.valueOf(team)));
        tag.put(NBT_TEAMS, teams);
        tag.putInt(NBT_TIME_LIMIT, timeLimitMinutes);
        tag.putInt(NBT_SCORE_LIMIT, scoreLimit);
        tag.putFloat(NBT_POPULARITY, popularity);
        tag.putInt(NBT_ROUNDS_SINCE_PLAYED, roundsSincePlayed);
        return tag;
    }

    public static TeamsRound load(CompoundTag tag)
    {
        List<String> teams = tag.getList(NBT_TEAMS, Tag.TAG_STRING).stream().map(Tag::getAsString).toList();
        UUID id = tag.hasUUID(NBT_ID) ? tag.getUUID(NBT_ID) : UUID.randomUUID();
        return new TeamsRound(id, tag.getString(NBT_MAP), tag.getString(NBT_GAME_TYPE), teams,
            tag.getInt(NBT_TIME_LIMIT), tag.getInt(NBT_SCORE_LIMIT), tag.getFloat(NBT_POPULARITY), tag.getInt(NBT_ROUNDS_SINCE_PLAYED));
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
