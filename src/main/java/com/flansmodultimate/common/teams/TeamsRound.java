package com.flansmodultimate.common.teams;

import com.flansmodultimate.api.ITeam;
import com.flansmodultimate.api.ITeamsRound;
import com.flansmodultimate.common.types.Team;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

/**
 * One entry of the rotation: a map, a game type and the teams that play it.
 *
 * <p>
 * The teams are grouped into sides. Teams on the same side are allied: they share one score, hold
 * capture points together and win or lose together. A round saved without sides puts every team
 * on a side of its own, which is how rounds were played before alliances existed.
 * </p>
 *
 * <p>
 * Players and bases carry a <em>round team id</em>: 0 for none, 1 for spectators and 2 onwards for
 * the teams in round order. A side goes by the round team id of its first team, its <em>side
 * id</em>, so in a round without alliances the two are the same number.
 * </p>
 */
@Getter
public final class TeamsRound implements Comparable<TeamsRound>, ITeamsRound
{
    /** Most teams a round can hold, whatever its game type allows. */
    public static final int MAX_TEAMS = 8;
    /** Most sides a round can hold, whatever its game type allows. */
    public static final int MAX_SIDES = 4;
    /** Round team id of the first team of a round; the ids below it mean nobody and the spectators. */
    public static final int FIRST_TEAM_ID = 2;

    /** The teams of a round and the side each one is on, as two parallel lists. */
    public record Lineup(List<String> teamIds, List<Integer> sides)
    {}

    private static final String NBT_ID = "id";
    private static final String NBT_MAP = "map";
    private static final String NBT_GAME_TYPE = "game_type";
    private static final String NBT_TEAMS = "teams";
    private static final String NBT_SIDES = "sides";
    private static final String NBT_TIME_LIMIT = "time_limit";
    private static final String NBT_SCORE_LIMIT = "score_limit";
    private static final String NBT_POPULARITY = "popularity";
    private static final String NBT_ROUNDS_SINCE_PLAYED = "rounds_since_played";
    private static final String NBT_EXTENSIONS = "extensions";
    private static final String NBT_ATTACKING_SIDE = "attacking_side";

    private final UUID id;
    private final String mapId;
    private final String gameTypeId;
    private final List<String> teamIds;
    /** Side of each team, parallel to {@link #teamIds} and numbered from 0 in order of first appearance. */
    private final List<Integer> sides;
    private final int timeLimitMinutes;
    private int scoreLimit;
    private final float popularity;
    private int roundsSincePlayed;
    /**
     * The side, by its position in round order, that attacks in a Domination or Conquest round played attacker against
     * defender: only it takes capture points, and the other sides defend the points they start with. -1 for a round
     * every side may capture in.
     */
    @Getter(AccessLevel.NONE)
    private int attackingSide = -1;
    /** Data addons keep with the round, by the string form of their namespaced key. */
    @Getter(AccessLevel.NONE)
    private final Map<String, CompoundTag> extensions = new TreeMap<>();

    /** A round in which every team fights for itself. */
    public TeamsRound(String mapId, String gameTypeId, List<String> teamIds, int timeLimitMinutes, int scoreLimit)
    {
        this(mapId, gameTypeId, teamIds, List.of(), timeLimitMinutes, scoreLimit);
    }

    /**
     * @param sides
     *            the side of each team, parallel to {@code teamIds}; teams given the same number are allied.
     *            Anything that does not line up with the teams puts each team on its own side.
     */
    public TeamsRound(String mapId, String gameTypeId, List<String> teamIds, List<Integer> sides, int timeLimitMinutes, int scoreLimit)
    {
        this(UUID.randomUUID(), mapId, gameTypeId, teamIds, sides, timeLimitMinutes, scoreLimit, 0.5F, 0);
    }

    // The saved form of a round has this many independent values.
    @SuppressWarnings("java:S107")
    private TeamsRound(UUID id, String mapId, String gameTypeId, List<String> teamIds, List<Integer> sides, int timeLimitMinutes, int scoreLimit, float popularity, int roundsSincePlayed)
    {
        this.id = id;
        this.mapId = Objects.requireNonNull(mapId).toLowerCase(Locale.ROOT);
        this.gameTypeId = Objects.requireNonNull(gameTypeId).toLowerCase(Locale.ROOT);
        this.teamIds = teamIds.stream().map(idValue -> idValue.toLowerCase(Locale.ROOT)).toList();
        this.sides = normaliseSides(sides, this.teamIds.size());
        this.timeLimitMinutes = Math.max(1, timeLimitMinutes);
        this.scoreLimit = Math.max(1, scoreLimit);
        this.popularity = Math.max(0F, Math.min(1F, popularity));
        this.roundsSincePlayed = Math.max(0, roundsSincePlayed);
    }

    /** Renumbers sides from 0 in order of first appearance, or gives every team its own when they do not match the teams. */
    static List<Integer> normaliseSides(List<Integer> sides, int teamCount)
    {
        List<Integer> normalised = new ArrayList<>(teamCount);
        if (sides.size() != teamCount)
        {
            for (int i = 0; i < teamCount; i++)
                normalised.add(i);
            return List.copyOf(normalised);
        }
        List<Integer> seen = new ArrayList<>();
        for (Integer side : sides)
        {
            int index = seen.indexOf(side);
            if (index < 0)
            {
                index = seen.size();
                seen.add(side);
            }
            normalised.add(index);
        }
        return List.copyOf(normalised);
    }

    public int getTimeLimitTicks()
    {
        return timeLimitMinutes * 60 * 20;
    }

    @Override
    public OptionalInt getAttackingSide()
    {
        return attackingSide >= 0 && attackingSide < getSideCount() ? OptionalInt.of(attackingSide) : OptionalInt.empty();
    }

    /**
     * Makes the round one of attackers against defenders, or one every side captures in again.
     *
     * @param side
     *            the attacking side by its position in round order, or -1 for none
     */
    public void setAttackingSide(int side)
    {
        attackingSide = side >= 0 && side < getSideCount() ? side : -1;
    }

    /** Whether the round's game type can be played attacker against defender: Domination and Conquest can. */
    public boolean canBeAttack()
    {
        GameType type = getGametype();
        ObjectiveMode mode = type == null ? ObjectiveMode.NONE : type.getObjectiveMode();
        return mode == ObjectiveMode.DOMINATION || mode == ObjectiveMode.CONQUEST;
    }

    /** The side id that attacks in a round played attacker against defender, or 0 in any other round. */
    public int getAttackingSideId()
    {
        OptionalInt side = getAttackingSide();
        return side.isPresent() ? getSideIds().get(side.getAsInt()) : 0;
    }

    /** Whether a side may take capture points it does not hold: every side may, except the defenders of an attack. */
    public boolean mayCapture(int sideId)
    {
        int attacker = getAttackingSideId();
        return attacker == 0 || attacker == sideId;
    }

    /** Operators can change the score limit of a round while it is played; it is saved with the round. */
    public void setScoreLimit(int scoreLimit)
    {
        this.scoreLimit = Math.max(1, scoreLimit);
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
        int index = teamIds.indexOf(team.getShortName());
        return index < 0 ? 0 : index + FIRST_TEAM_ID;
    }

    // --- Sides ---

    /** Number of sides fighting each other in this round. */
    public int getSideCount()
    {
        return (int) sides.stream().distinct().count();
    }

    /** Whether any side of this round is made of more than one team. */
    public boolean hasAlliances()
    {
        return getSideCount() < teamIds.size();
    }

    /**
     * The side id a round team id belongs to: the round team id of the first team on its side.
     * Ids that are not a team of this round, such as nobody and the spectators, are returned as they are.
     */
    public int getSideId(int teamId)
    {
        int index = teamId - FIRST_TEAM_ID;
        if (index < 0 || index >= sides.size())
            return teamId;
        return sides.indexOf(sides.get(index)) + FIRST_TEAM_ID;
    }

    /** The side id a team fights for, 0 for a team that is not in this round and 1 for the spectators. */
    public int getSideId(@Nullable Team team)
    {
        return getSideId(getTeamId(team));
    }

    /** The side ids of this round, in the order their first teams were listed. */
    public List<Integer> getSideIds()
    {
        List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < sides.size(); i++)
            if (sides.indexOf(sides.get(i)) == i)
                ids.add(i + FIRST_TEAM_ID);
        return ids;
    }

    /** The position of a side in round order, from 0, as the API numbers sides; -1 for an id that is not a side of this round. */
    public int getSideIndex(int sideId)
    {
        return getSideIds().indexOf(sideId);
    }

    @Override
    public int getSideOf(ITeam team)
    {
        int index = team instanceof Team internal ? teamIds.indexOf(internal.getShortName()) : -1;
        return index < 0 ? -1 : sides.get(index);
    }

    @Override
    public List<ITeam> getTeams()
    {
        List<ITeam> teams = new ArrayList<>();
        for (int i = 0; i < teamIds.size(); i++)
        {
            Team team = getTeam(i);
            if (team != null)
                teams.add(team);
        }
        return List.copyOf(teams);
    }

    @Override
    public List<ITeam> getTeamsOfSide(int side)
    {
        List<Integer> sideIds = getSideIds();
        return side < 0 || side >= sideIds.size() ? List.of() : List.copyOf(getTeamsOnSide(sideIds.get(side)));
    }

    @Override
    public boolean areTeamsAllied(ITeam first, ITeam second)
    {
        int side = getSideOf(first);
        return side >= 0 && side == getSideOf(second);
    }

    /** The first team of a side, which is the one the score of the whole side is kept under. */
    @Nullable
    public Team getSideLeader(int sideId)
    {
        return getTeam(getSideId(sideId) - FIRST_TEAM_ID);
    }

    /** Every team of a side, in round order; empty for an id that is not a side of this round. */
    public List<Team> getTeamsOnSide(int sideId)
    {
        List<Team> teams = new ArrayList<>();
        for (int i = 0; i < teamIds.size(); i++)
        {
            Team team = getTeam(i);
            if (team != null && getSideId(i + FIRST_TEAM_ID) == sideId)
                teams.add(team);
        }
        return teams;
    }

    /** Whether two teams fight on the same side of this round. A team is its own ally; outsiders are nobody's. */
    public boolean areAllied(@Nullable Team first, @Nullable Team second)
    {
        int firstSide = getSideId(first);
        return firstSide >= FIRST_TEAM_ID && firstSide == getSideId(second);
    }

    /** The names of the teams of a side in their colours, joined with ampersands. */
    public Component getSideDisplayComponent(int sideId)
    {
        MutableComponent name = Component.empty();
        List<Team> teams = getTeamsOnSide(sideId);
        for (int i = 0; i < teams.size(); i++)
        {
            if (i > 0)
                name.append(" & ");
            name.append(teams.get(i).getDisplayComponent());
        }
        return name;
    }

    /** The short names of the teams as an operator writes them: allies joined by {@code +}, sides by {@code vs}. */
    public String describeTeams()
    {
        List<String> parts = new ArrayList<>();
        for (int sideId : getSideIds())
        {
            List<String> members = new ArrayList<>();
            for (int i = 0; i < teamIds.size(); i++)
                if (getSideId(i + FIRST_TEAM_ID) == sideId)
                    members.add(teamIds.get(i));
            parts.add(String.join("+", members));
        }
        return String.join(" vs ", parts);
    }

    /**
     * Reads the teams of a round as an operator writes them. Each argument is one side with its
     * allied teams joined by {@code +}; an argument may also hold several sides separated by commas.
     * Blank names are skipped, so stray separators do no harm.
     */
    public static Lineup parseLineup(List<String> sideArguments)
    {
        List<String> teams = new ArrayList<>();
        List<Integer> sides = new ArrayList<>();
        int side = 0;
        for (String argument : sideArguments)
        {
            for (String members : argument.split(","))
            {
                int before = teams.size();
                for (String team : members.split("\\+"))
                {
                    if (team.isBlank())
                        continue;
                    teams.add(team.strip());
                    sides.add(side);
                }
                if (teams.size() > before)
                    side++;
            }
        }
        return new Lineup(List.copyOf(teams), List.copyOf(sides));
    }

    // --- Addon data ---

    @Override
    public CompoundTag getExtensionData(ResourceLocation key)
    {
        CompoundTag data = extensions.get(key.toString());
        return data == null ? new CompoundTag() : data.copy();
    }

    /** Keeps a copy of an addon's data under its key; an empty tag removes the key. The caller saves the rotation. */
    void setExtensionData(ResourceLocation key, CompoundTag data)
    {
        if (data.isEmpty())
            extensions.remove(key.toString());
        else
            extensions.put(key.toString(), data.copy());
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
        tag.putIntArray(NBT_SIDES, sides);
        tag.putInt(NBT_TIME_LIMIT, timeLimitMinutes);
        tag.putInt(NBT_SCORE_LIMIT, scoreLimit);
        tag.putFloat(NBT_POPULARITY, popularity);
        tag.putInt(NBT_ROUNDS_SINCE_PLAYED, roundsSincePlayed);
        if (attackingSide >= 0)
            tag.putInt(NBT_ATTACKING_SIDE, attackingSide);
        if (!extensions.isEmpty())
        {
            CompoundTag extensionTag = new CompoundTag();
            extensions.forEach((key, data) -> extensionTag.put(key, data.copy()));
            tag.put(NBT_EXTENSIONS, extensionTag);
        }
        return tag;
    }

    public static TeamsRound load(CompoundTag tag)
    {
        List<String> teams = tag.getList(NBT_TEAMS, Tag.TAG_STRING).stream().map(Tag::getAsString).toList();
        // Rounds saved before alliances existed have no sides, which reads as an empty array.
        List<Integer> sides = java.util.Arrays.stream(tag.getIntArray(NBT_SIDES)).boxed().toList();
        UUID id = tag.hasUUID(NBT_ID) ? tag.getUUID(NBT_ID) : UUID.randomUUID();
        TeamsRound round = new TeamsRound(id, tag.getString(NBT_MAP), tag.getString(NBT_GAME_TYPE), teams, sides, tag.getInt(NBT_TIME_LIMIT), tag.getInt(NBT_SCORE_LIMIT), tag.getFloat(NBT_POPULARITY),
            tag.getInt(NBT_ROUNDS_SINCE_PLAYED));
        round.setAttackingSide(tag.contains(NBT_ATTACKING_SIDE) ? tag.getInt(NBT_ATTACKING_SIDE) : -1);
        CompoundTag extensionTag = tag.getCompound(NBT_EXTENSIONS);
        for (String key : extensionTag.getAllKeys())
        {
            if (extensionTag.contains(key, Tag.TAG_COMPOUND))
                round.extensions.put(key, extensionTag.getCompound(key).copy());
        }
        return round;
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
