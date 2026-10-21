package com.wolffsmod.npcs.teams;

import com.wolffsmod.npcs.crew.CrewPost;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.*;

/**
 * Lists of bot settings written as one line of text, as the editor screen shows them: entries separated by commas or
 * spaces, the values of an entry by colons. A value left out takes its default, and one that is not a number counts 0.
 * Pure text handling, any thread.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BotSettingsText
{
    private static final String ENTRY_SEPARATOR = "[,\\s]+";
    private static final String VALUE_SEPARATOR = ":";
    private static final String JOIN = ", ";
    /** The crew class that stands for one picked by the class weights. */
    public static final String ANY_CLASS = "any";

    /** {@code class:weight}, a class without a weight weighing 1. */
    public static Map<String, Integer> parseClassWeights(String text)
    {
        Map<String, Integer> weights = new LinkedHashMap<>();
        for (String[] values : entries(text))
            weights.put(values[0], number(values, 1, 1));
        return weights;
    }

    public static String formatClassWeights(Map<String, Integer> weights)
    {
        StringJoiner joiner = new StringJoiner(JOIN);
        weights.forEach((playerClass, weight) -> joiner.add(playerClass + VALUE_SEPARATOR + weight));
        return joiner.toString();
    }

    /** {@code class:role}, as {@code medic_class:medic, at_class:anti_tank}; a role that is not known is left out. */
    public static Map<String, BotSettings.ClassRole> parseRoles(String text)
    {
        Map<String, BotSettings.ClassRole> roles = new LinkedHashMap<>();
        for (String[] values : entries(text))
            if (values.length > 1)
                role(values[1]).ifPresent(role -> roles.put(values[0], role));
        return roles;
    }

    public static String formatRoles(Map<String, BotSettings.ClassRole> roles)
    {
        StringJoiner joiner = new StringJoiner(JOIN);
        roles.forEach((playerClass, role) -> joiner.add(playerClass + VALUE_SEPARATOR + role.name().toLowerCase(Locale.ROOT)));
        return joiner.toString();
    }

    /** A role by its name, with or without the underscore: {@code anti_tank} and {@code antitank} both name the anti-tank role. */
    public static Optional<BotSettings.ClassRole> role(String name)
    {
        String wanted = name.replace("_", "").toLowerCase(Locale.ROOT);
        return Arrays.stream(BotSettings.ClassRole.values()).filter(role -> role.name().replace("_", "").toLowerCase(Locale.ROOT).equals(wanted)).findFirst();
    }

    /** {@code template:count[:budget]}, a count left out being 1. */
    public static List<BotSettings.SquadEntry> parseSquads(String text)
    {
        List<BotSettings.SquadEntry> squads = new ArrayList<>();
        for (String[] values : entries(text))
            squads.add(new BotSettings.SquadEntry(values[0].toLowerCase(Locale.ROOT), number(values, 1, 1), number(values, 2, 0)));
        return squads;
    }

    public static String formatSquads(List<BotSettings.SquadEntry> squads)
    {
        StringJoiner joiner = new StringJoiner(JOIN);
        for (BotSettings.SquadEntry squad : squads)
            joiner.add(squad.template() + VALUE_SEPARATOR + squad.count() + (squad.budget() > 0 ? VALUE_SEPARATOR + squad.budget() : ""));
        return joiner.toString();
    }

    /**
     * {@code type:count[:budget[:respawn[:ticket cost[:carried template]]]]}, a count left out being 1 and a ticket cost 1.
     *
     * @param previous
     *            the entries before the edit, whose vehicle spawners the entries of the same type keep
     */
    public static List<BotSettings.VehicleEntry> parseVehicles(String text, List<BotSettings.VehicleEntry> previous)
    {
        List<BotSettings.VehicleEntry> vehicles = new ArrayList<>();
        for (String[] values : entries(text))
        {
            UUID spawner = previous.stream().filter(entry -> entry.type().equals(values[0])).map(BotSettings.VehicleEntry::spawner).filter(Objects::nonNull).findFirst().orElse(null);
            String carry = values.length > 5 ? values[5].toLowerCase(Locale.ROOT) : "";
            vehicles.add(new BotSettings.VehicleEntry(values[0], number(values, 1, 1), number(values, 2, 0), number(values, 3, 0), number(values, 4, 1), spawner, carry));
        }
        return vehicles;
    }

    public static String formatVehicles(List<BotSettings.VehicleEntry> vehicles)
    {
        StringJoiner joiner = new StringJoiner(JOIN);
        for (BotSettings.VehicleEntry vehicle : vehicles)
        {
            String entry = String.join(VALUE_SEPARATOR, vehicle.type(), Integer.toString(vehicle.count()), Integer.toString(vehicle.budget()), Integer.toString(vehicle.respawnSeconds()),
                Integer.toString(vehicle.ticketCost()));
            joiner.add(vehicle.carry().isEmpty() ? entry : entry + VALUE_SEPARATOR + vehicle.carry());
        }
        return joiner.toString();
    }

    /** The class crewing each kind of seat, as {@code driver:crewman, gunner:any, passenger:none}. */
    public static String formatCrew(BotSettings.TeamForce force)
    {
        StringJoiner joiner = new StringJoiner(JOIN);
        for (CrewPost post : CrewPost.values())
            joiner.add(post.id() + VALUE_SEPARATOR + crewClass(force.crewClass(post)));
        return joiner.toString();
    }

    /** @return the crew class as the editor shows it: {@code any} for a class picked by weight */
    public static String crewClass(String playerClass)
    {
        return playerClass.isEmpty() ? ANY_CLASS : playerClass;
    }

    /** @return the crew class an editor entry names: empty for {@code any} or nothing */
    public static String parseCrewClass(String text)
    {
        String value = text.strip();
        return ANY_CLASS.equalsIgnoreCase(value) ? "" : value;
    }

    /** {@code class:count} members of a squad template, a count left out being 1. */
    public static List<SquadTemplate.Member> parseMembers(String text)
    {
        List<SquadTemplate.Member> members = new ArrayList<>();
        for (String[] values : entries(text))
            members.add(new SquadTemplate.Member(values[0], number(values, 1, 1)));
        return members;
    }

    public static String formatMembers(List<SquadTemplate.Member> members)
    {
        StringJoiner joiner = new StringJoiner(JOIN);
        members.forEach(member -> joiner.add(member.playerClass() + VALUE_SEPARATOR + member.count()));
        return joiner.toString();
    }

    /** A whole number, or a fallback for text that is not one. */
    public static int parseNumber(String text, int fallback)
    {
        try
        {
            return Integer.parseInt(text.strip());
        }
        catch (NumberFormatException e)
        {
            return fallback;
        }
    }

    /** A number with a fraction, or a fallback for text that is not one. */
    public static float parseDecimal(String text, float fallback)
    {
        try
        {
            float value = Float.parseFloat(text.strip());
            return Float.isFinite(value) ? value : fallback;
        }
        catch (NumberFormatException e)
        {
            return fallback;
        }
    }

    private static List<String[]> entries(String text)
    {
        List<String[]> entries = new ArrayList<>();
        for (String entry : text.strip().split(ENTRY_SEPARATOR))
        {
            String[] values = entry.split(VALUE_SEPARATOR, -1);
            if (!values[0].isBlank())
                entries.add(values);
        }
        return entries;
    }

    private static int number(String[] values, int index, int fallback)
    {
        return index < values.length && !values[index].isBlank() ? Math.max(0, parseNumber(values[index], 0)) : fallback;
    }
}
