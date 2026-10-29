package com.flansmodultimate.client.gui.centre;

import com.flansmodultimate.common.teams.centre.TeamsSection;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.*;
import java.util.function.Function;

/**
 * The Teams section of the command centre as the screen reads it.
 *
 * @param current
 *            the rotation index of the round being played, or -1
 * @param next
 *            the rotation index of the round that starts next, or -1
 * @param queued
 *            the rotation index of the round an operator queued, or -1
 */
public record TeamsData(boolean enabled, int players, int current, int next, int queued, @Nullable LiveRound running, List<Round> rounds, List<MapInfo> maps, List<GameTypeInfo> gameTypes,
    List<TeamInfo> teams, List<Pool> pools, CompoundTag rules, CompoundTag settings, Map<String, String> variables)
{
    /**
     * A round of the rotation.
     *
     * @param sides
     *            the side of each team, parallel to {@code teams}, numbered from 0
     * @param attackers
     *            the attacking side, or -1
     */
    public record Round(int index, String id, String map, String gameType, List<String> teams, List<Integer> sides, int minutes, int scoreLimit, int attackers, boolean canAttack)
    {
        public int sideCount()
        {
            return (int) sides.stream().distinct().count();
        }

        /** The teams of each side, in round order. */
        public List<List<String>> teamsBySide()
        {
            List<List<String>> bySide = new ArrayList<>();
            for (int i = 0; i < teams.size(); i++)
            {
                int side = i < sides.size() ? sides.get(i) : i;
                while (bySide.size() <= side)
                    bySide.add(new ArrayList<>());
                bySide.get(side).add(teams.get(i));
            }
            bySide.removeIf(List::isEmpty);
            return bySide;
        }
    }

    /** The round being played, with how its teams stand. */
    public record LiveRound(Round round, int timeLeft, String phase, int intermissionLeft, List<LiveTeam> teams)
    {}

    public record LiveTeam(String id, String name, int colour, int side, int score, int players)
    {}

    /**
     * @param bounds
     *            the corners of its playing area, lowest first, or empty for none
     * @param playable
     *            the game types that can be played on it
     */
    public record MapInfo(String id, String name, String dimension, List<Integer> bounds, int bases, boolean capturePoints, String image, List<String> playable)
    {}

    public record GameTypeInfo(String id, String name, int minTeams, int maxTeams, int minSides, int maxSides, boolean canAttack, int defaultScore)
    {}

    public record TeamInfo(String id, String name, int colour)
    {}

    public record Pool(String id, String name)
    {}

    public static TeamsData read(CompoundTag tag)
    {
        LiveRound live = tag.contains(TeamsSection.ROUND, Tag.TAG_COMPOUND) ? liveRound(tag.getCompound(TeamsSection.ROUND), tag.getInt(TeamsSection.CURRENT)) : null;
        List<Round> rounds = new ArrayList<>();
        ListTag roundList = tag.getList(TeamsSection.ROUNDS, Tag.TAG_COMPOUND);
        for (int i = 0; i < roundList.size(); i++)
            rounds.add(round(roundList.getCompound(i), i));
        Map<String, String> variables = new LinkedHashMap<>();
        CompoundTag variableTag = tag.getCompound(TeamsSection.VARIABLES);
        variableTag.getAllKeys().stream().sorted().forEach(key -> variables.put(key, variableTag.getString(key)));
        return new TeamsData(tag.getBoolean(TeamsSection.ENABLED), tag.getInt(TeamsSection.PLAYERS), index(tag, TeamsSection.CURRENT), index(tag, TeamsSection.NEXT), index(tag, TeamsSection.QUEUED),
            live, List.copyOf(rounds), list(tag, TeamsSection.MAPS, TeamsData::map), list(tag, TeamsSection.GAME_TYPES, TeamsData::gameType),
            list(tag, TeamsSection.TEAMS, entry -> new TeamInfo(entry.getString(TeamsSection.ID), displayName(entry), entry.getInt(TeamsSection.COLOUR))),
            list(tag, TeamsSection.POOLS, entry -> new Pool(entry.getString(TeamsSection.ID), displayName(entry))), tag.getCompound(TeamsSection.RULES), tag.getCompound(TeamsSection.SETTINGS),
            Collections.unmodifiableMap(variables));
    }

    private static int index(CompoundTag tag, String key)
    {
        return tag.contains(key) ? tag.getInt(key) : -1;
    }

    private static <T> List<T> list(CompoundTag tag, String key, Function<CompoundTag, T> reader)
    {
        ListTag list = tag.getList(key, Tag.TAG_COMPOUND);
        List<T> read = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++)
            read.add(reader.apply(list.getCompound(i)));
        return List.copyOf(read);
    }

    private static Round round(CompoundTag tag, int index)
    {
        List<String> teams = new ArrayList<>();
        ListTag teamList = tag.getList(TeamsSection.TEAMS, Tag.TAG_STRING);
        for (int i = 0; i < teamList.size(); i++)
            teams.add(teamList.getString(i));
        List<Integer> sides = Arrays.stream(tag.getIntArray(TeamsSection.SIDES)).boxed().toList();
        return new Round(index, tag.getString(TeamsSection.ID), tag.getString(TeamsSection.MAP), tag.getString(TeamsSection.GAME_TYPE), List.copyOf(teams), sides, tag.getInt(TeamsSection.MINUTES),
            tag.getInt(TeamsSection.SCORE_LIMIT), tag.getInt(TeamsSection.ATTACKERS), tag.getBoolean(TeamsSection.CAN_ATTACK));
    }

    private static LiveRound liveRound(CompoundTag tag, int index)
    {
        List<LiveTeam> teams = list(tag, TeamsSection.TEAMS, entry -> new LiveTeam(entry.getString(TeamsSection.ID), displayName(entry), entry.getInt(TeamsSection.COLOUR),
            entry.getInt(TeamsSection.SIDE), entry.getInt(TeamsSection.SCORE), entry.getInt(TeamsSection.PLAYERS)));
        return new LiveRound(round(tag, index), tag.getInt(TeamsSection.TIME_LEFT), tag.getString(TeamsSection.PHASE), tag.getInt(TeamsSection.INTERMISSION_LEFT), teams);
    }

    private static MapInfo map(CompoundTag tag)
    {
        List<String> playable = new ArrayList<>();
        ListTag playableList = tag.getList(TeamsSection.PLAYABLE, Tag.TAG_STRING);
        for (int i = 0; i < playableList.size(); i++)
            playable.add(playableList.getString(i));
        return new MapInfo(tag.getString(TeamsSection.ID), tag.getString(TeamsSection.NAME), tag.getString(TeamsSection.DIMENSION),
            Arrays.stream(tag.getIntArray(TeamsSection.BOUNDS)).boxed().toList(), tag.getInt(TeamsSection.BASES), tag.getBoolean(TeamsSection.CAPTURE_POINTS), tag.getString(TeamsSection.IMAGE),
            List.copyOf(playable));
    }

    private static String displayName(CompoundTag tag)
    {
        String key = tag.getString("nameTranslationKey");
        String name = tag.getString(TeamsSection.NAME);
        return key.isBlank() ? name : Component.translatableWithFallback(key, name).getString();
    }

    private static GameTypeInfo gameType(CompoundTag tag)
    {
        return new GameTypeInfo(tag.getString(TeamsSection.ID), tag.getString(TeamsSection.NAME), tag.getInt(TeamsSection.MIN_TEAMS), tag.getInt(TeamsSection.MAX_TEAMS),
            tag.getInt(TeamsSection.MIN_SIDES), tag.getInt(TeamsSection.MAX_SIDES), tag.getBoolean(TeamsSection.CAN_ATTACK), tag.getInt(TeamsSection.DEFAULT_SCORE));
    }

    /** The round being played, if any. */
    public Optional<LiveRound> live()
    {
        return Optional.ofNullable(running);
    }

    public Optional<Round> round(int index)
    {
        return index >= 0 && index < rounds.size() ? Optional.of(rounds.get(index)) : Optional.empty();
    }

    public Optional<Round> roundById(@Nullable String id)
    {
        return rounds.stream().filter(round -> round.id().equals(id)).findFirst();
    }

    public Optional<MapInfo> map(@Nullable String id)
    {
        return maps.stream().filter(map -> map.id().equals(id)).findFirst();
    }

    public Optional<GameTypeInfo> gameType(@Nullable String id)
    {
        return gameTypes.stream().filter(type -> type.id().equals(id)).findFirst();
    }

    public Optional<TeamInfo> team(@Nullable String id)
    {
        return teams.stream().filter(team -> team.id().equals(id)).findFirst();
    }

    /** A game type's name, or its id when it is unknown. */
    public String gameTypeName(String id)
    {
        return gameType(id).map(GameTypeInfo::name).orElse(id);
    }

    /** A map's name, or its id when it is unknown. */
    public String mapName(String id)
    {
        return map(id).map(MapInfo::name).orElse(id);
    }

    /** A team's name in its colour, or its id when it is unknown. */
    public MutableComponent teamName(String id)
    {
        return team(id).map(team -> Component.literal(team.name()).withStyle(Style.EMPTY.withColor(team.colour()))).orElseGet(() -> Component.literal(id));
    }

    /** A round's line-up: its sides' teams, allies joined by a plus, sides by "vs". */
    public MutableComponent lineup(Round round)
    {
        MutableComponent lineup = Component.empty();
        List<List<String>> sides = round.teamsBySide();
        for (int side = 0; side < sides.size(); side++)
        {
            if (side > 0)
                lineup.append(Component.literal(" vs ").withStyle(Style.EMPTY.withColor(CentreStyle.MUTED & 0xFFFFFF)));
            List<String> allies = sides.get(side);
            for (int i = 0; i < allies.size(); i++)
            {
                if (i > 0)
                    lineup.append(" + ");
                lineup.append(teamName(allies.get(i)));
            }
        }
        return lineup;
    }

    /** A round as it is listed: its game type on its map. */
    public Component title(Round round)
    {
        return Component.translatable("gui.flansmodultimate.centre.round_title", gameTypeName(round.gameType()), mapName(round.map()));
    }
}
