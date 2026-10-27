package com.wolffsmod.npcs.client.centre;

import com.wolffsmod.npcs.teams.BattlesSection;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.*;
import java.util.function.Function;

/**
 * The module's section of the Teams command centre as its pages read it: managed factions, skirmishes, wars and the bots
 * of the rotation.
 *
 * @param others
 *            the Custom NPCs factions that are not managed
 * @param here
 *            the dimension the operator is in
 * @param live
 *            how the bots of the round being played stand, by team; empty between rounds
 */
record BattlesData(List<FactionInfo> factions, List<FactionInfo> others, List<TeamInfo> teams, List<SkirmishInfo> skirmishes, List<String> presets, List<WarInfo> wars, List<String> dimensions,
    String here, List<RoundInfo> rounds, List<String> botPresets, List<LiveTeam> live, Choices choices)
{
    /**
     * A Custom NPCs faction; what only managed factions have is empty or zero for the others.
     *
     * @param member
     *            whether the operator belongs to it
     */
    @SuppressWarnings("unused") // Constructed from the NPC command-centre snapshot and consumed by its registered pages.
    record FactionInfo(int id, String name, int colour, String outsiders, int members, boolean member, List<Integer> enemies, List<Branch> branches, String displayName)
    {}

    /** A Flan team serving a managed faction. */
    record Branch(String team, int weight, int count, String roles, String squads, String vehicles)
    {}

    record TeamInfo(String id, String name, int colour)
    {}

    /** A skirmish, with its rules and its sides. */
    record SkirmishInfo(String name, String state, Rules rules, String dimension, boolean war, List<Side> sides)
    {
        boolean draft()
        {
            return "draft".equals(state);
        }

        boolean live()
        {
            return "running".equals(state) || "paused".equals(state);
        }
    }

    /** How a skirmish is fought and ends. */
    record Rules(String intent, String end, int minutes, boolean respawn, int respawnSeconds, int tickets, String difficulty, boolean protection)
    {}

    /**
     * A side of a skirmish.
     *
     * @param infantry
     *            the loose infantry asked for, or -1 for what the faction's branches list
     * @param fight
     *            how it stands in a skirmish being fought, or null for a draft
     */
    record Side(int faction, String name, int colour, String position, double radius, int infantry, int squads, int vehicles, @Nullable Fight fight)
    {}

    /**
     * @param tickets
     *            tickets left, or -1 without a limit
     */
    record Fight(int standing, int deaths, int kills, int tickets)
    {}

    /** A war, with the territory around the operator. */
    record WarInfo(String name, boolean running, List<Integer> factions, List<Integer> holdings, List<String> scope, WarRules rules, List<Engagement> engagements, Territory map)
    {}

    record WarRules(int infantry, int respawn, int tickets, int range, int engagements, int flip, String difficulty)
    {}

    record Engagement(String name, int first, int second, int x, int z, int progress)
    {}

    /**
     * Who holds the chunks around the operator's, row by row from the north-west.
     *
     * @param owners
     *            each chunk's holder by its place in the war's factions, or -1 for no one
     * @param contested
     *            the indexes of the chunks an engagement fights over
     * @param covered
     *            whether the war is fought in the operator's dimension
     */
    record Territory(int centreX, int centreZ, int radius, List<Integer> owners, Set<Integer> contested, boolean covered)
    {
        int size()
        {
            return 2 * radius + 1;
        }
    }

    /** The bots of a round of the rotation. */
    record RoundInfo(int index, String map, String gameType, List<String> teams, boolean configured, boolean enabled, int total, String mode, String difficulty, int markers, boolean current)
    {}

    /**
     * @param tickets
     *            tickets left to the team's side, or -1 without a limit
     */
    record LiveTeam(String team, int colour, int alive, int slots, int tickets)
    {}

    /** The values the settings the pages offer may take, as the commands spell them. */
    record Choices(List<String> intents, List<String> ends, List<String> difficulties, List<String> outsiders, List<String> markerTypes, List<String> roles)
    {}

    static BattlesData read(CompoundTag tag)
    {
        CompoundTag choices = tag.getCompound(BattlesSection.CHOICES);
        return new BattlesData(list(tag, BattlesSection.FACTIONS, BattlesData::faction), list(tag, BattlesSection.OTHER_FACTIONS, BattlesData::faction),
            list(tag, BattlesSection.TEAMS, entry -> new TeamInfo(entry.getString(BattlesSection.ID), displayName(entry), entry.getInt(BattlesSection.COLOUR))),
            list(tag, BattlesSection.SKIRMISHES, BattlesData::skirmish), strings(tag, BattlesSection.PRESETS), list(tag, BattlesSection.WARS, BattlesData::war),
            strings(tag, BattlesSection.DIMENSIONS), tag.getString(BattlesSection.HERE), list(tag, BattlesSection.ROUNDS, BattlesData::round), strings(tag, BattlesSection.BOT_PRESETS),
            list(tag.getCompound(BattlesSection.LIVE), BattlesSection.TEAMS, BattlesData::liveTeam),
            new Choices(strings(choices, BattlesSection.INTENTS), strings(choices, BattlesSection.ENDS), strings(choices, BattlesSection.DIFFICULTIES), strings(choices, BattlesSection.OUTSIDER_RULES),
                strings(choices, BattlesSection.MARKER_TYPES), strings(choices, BattlesSection.BRANCH_ROLES)));
    }

    private static <T> List<T> list(CompoundTag tag, String key, Function<CompoundTag, T> reader)
    {
        ListTag list = tag.getList(key, Tag.TAG_COMPOUND);
        List<T> read = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++)
            read.add(reader.apply(list.getCompound(i)));
        return List.copyOf(read);
    }

    private static List<String> strings(CompoundTag tag, String key)
    {
        ListTag list = tag.getList(key, Tag.TAG_STRING);
        List<String> read = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++)
            read.add(list.getString(i));
        return List.copyOf(read);
    }

    private static List<Integer> ints(CompoundTag tag, String key)
    {
        return Arrays.stream(tag.getIntArray(key)).boxed().toList();
    }

    private static String displayName(CompoundTag tag)
    {
        net.minecraft.network.chat.Component name = net.minecraft.network.chat.Component.Serializer.fromJson(tag.getString("displayName"));
        return name == null ? tag.getString(BattlesSection.NAME) : name.getString();
    }

    private static FactionInfo faction(CompoundTag tag)
    {
        return new FactionInfo(tag.getInt(BattlesSection.ID), tag.getString(BattlesSection.NAME), tag.getInt(BattlesSection.COLOUR), tag.getString(BattlesSection.OUTSIDERS),
            tag.getInt(BattlesSection.MEMBERS), tag.getBoolean(BattlesSection.MEMBER), ints(tag, BattlesSection.ENEMIES),
            list(tag, BattlesSection.BRANCHES, entry -> new Branch(entry.getString(BattlesSection.TEAM), entry.getInt(BattlesSection.WEIGHT), entry.getInt(BattlesSection.COUNT),
                entry.getString(BattlesSection.ROLES), entry.getString(BattlesSection.SQUADS), entry.getString(BattlesSection.VEHICLES))),
            displayName(tag));
    }

    private static SkirmishInfo skirmish(CompoundTag tag)
    {
        Rules rules = new Rules(tag.getString(BattlesSection.INTENT), tag.getString(BattlesSection.END), tag.getInt(BattlesSection.MINUTES), tag.getBoolean(BattlesSection.RESPAWN),
            tag.getInt(BattlesSection.RESPAWN_SECONDS), tag.getInt(BattlesSection.TICKETS), tag.getString(BattlesSection.DIFFICULTY), tag.getBoolean(BattlesSection.PROTECTION));
        List<Side> sides = list(tag, BattlesSection.SIDES, entry ->
        {
            Fight fight = entry.contains(BattlesSection.STANDING)
                ? new Fight(entry.getInt(BattlesSection.STANDING), entry.getInt(BattlesSection.DEATHS), entry.getInt(BattlesSection.KILLS), entry.getInt(BattlesSection.TICKETS))
                : null;
            String position = entry.getInt(BattlesSection.X) + " " + entry.getInt(BattlesSection.Y) + " " + entry.getInt(BattlesSection.Z);
            return new Side(entry.getInt(BattlesSection.FACTION), entry.getString(BattlesSection.NAME), entry.getInt(BattlesSection.COLOUR), position, entry.getDouble(BattlesSection.RADIUS),
                entry.getInt(BattlesSection.INFANTRY), entry.getInt(BattlesSection.SQUADS), entry.getInt(BattlesSection.VEHICLES), fight);
        });
        return new SkirmishInfo(tag.getString(BattlesSection.NAME), tag.getString(BattlesSection.STATE), rules, tag.getString(BattlesSection.DIMENSION), tag.getBoolean(BattlesSection.WAR), sides);
    }

    private static WarInfo war(CompoundTag tag)
    {
        WarRules rules = new WarRules(tag.getInt(BattlesSection.INFANTRY), tag.getInt(BattlesSection.RESPAWN_SECONDS), tag.getInt(BattlesSection.TICKETS), tag.getInt(BattlesSection.RANGE),
            tag.getInt(BattlesSection.MOST_ENGAGEMENTS), tag.getInt(BattlesSection.FLIP), tag.getString(BattlesSection.DIFFICULTY));
        List<Engagement> engagements = list(tag, BattlesSection.ENGAGEMENTS, entry -> new Engagement(entry.getString(BattlesSection.NAME), entry.getInt(BattlesSection.FIRST),
            entry.getInt(BattlesSection.SECOND), entry.getInt(BattlesSection.X), entry.getInt(BattlesSection.Z), entry.getInt(BattlesSection.PROGRESS)));
        CompoundTag map = tag.getCompound(BattlesSection.MAP);
        Set<Integer> contested = new HashSet<>();
        byte[] fought = map.getByteArray(BattlesSection.CONTESTED);
        for (int i = 0; i < fought.length; i++)
            if (fought[i] != 0)
                contested.add(i);
        Territory territory = new Territory(map.getInt(BattlesSection.CENTRE_X), map.getInt(BattlesSection.CENTRE_Z), map.getInt(BattlesSection.RADIUS), ints(map, BattlesSection.OWNERS),
            Set.copyOf(contested), map.getBoolean(BattlesSection.DIMENSION));
        return new WarInfo(tag.getString(BattlesSection.NAME), tag.getBoolean(BattlesSection.RUNNING), ints(tag, BattlesSection.FACTIONS), ints(tag, BattlesSection.HOLDINGS),
            strings(tag, BattlesSection.SCOPE), rules, engagements, territory);
    }

    private static RoundInfo round(CompoundTag tag)
    {
        return new RoundInfo(tag.getInt(BattlesSection.INDEX), tag.getString(BattlesSection.MAP), tag.getString(BattlesSection.GAME_TYPE), strings(tag, BattlesSection.TEAMS),
            tag.getBoolean(BattlesSection.CONFIGURED), tag.getBoolean(BattlesSection.ENABLED), tag.getInt(BattlesSection.TOTAL), tag.getString(BattlesSection.MODE),
            tag.getString(BattlesSection.DIFFICULTY), tag.getInt(BattlesSection.MARKERS), tag.getBoolean(BattlesSection.CURRENT));
    }

    private static LiveTeam liveTeam(CompoundTag tag)
    {
        return new LiveTeam(tag.getString(BattlesSection.TEAM), tag.getInt(BattlesSection.COLOUR), tag.getInt(BattlesSection.ALIVE), tag.getInt(BattlesSection.SLOTS),
            tag.getInt(BattlesSection.TICKETS));
    }

    /** Every faction the section names, managed or not. */
    List<FactionInfo> allFactions()
    {
        List<FactionInfo> all = new ArrayList<>(factions);
        all.addAll(others);
        return all;
    }

    Optional<FactionInfo> faction(int id)
    {
        return allFactions().stream().filter(faction -> faction.id() == id).findFirst();
    }

    Optional<FactionInfo> managed(int id)
    {
        return factions.stream().filter(faction -> faction.id() == id).findFirst();
    }

    String factionName(int id)
    {
        return faction(id).map(FactionInfo::name).orElse("#" + id);
    }

    String factionDisplayName(int id)
    {
        return faction(id).map(FactionInfo::displayName).orElse("#" + id);
    }

    int factionColour(int id)
    {
        return faction(id).map(FactionInfo::colour).orElse(0xFFFFFF);
    }

    Optional<SkirmishInfo> skirmish(String name)
    {
        return skirmishes.stream().filter(skirmish -> skirmish.name().equals(name)).findFirst();
    }

    Optional<WarInfo> war(String name)
    {
        return wars.stream().filter(war -> war.name().equals(name)).findFirst();
    }

    Optional<TeamInfo> team(String id)
    {
        return teams.stream().filter(team -> team.id().equals(id)).findFirst();
    }
}
