package com.wolffsmod.npcs.teams;

import com.flansmodultimate.api.commandcentre.ISettingsPresets;
import com.flansmodultimate.api.teams.FlansTeams;
import com.flansmodultimate.api.teams.ITeam;
import com.flansmodultimate.api.teams.ITeamsRound;
import noppes.npcs.controllers.ServerCloneController;

import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.util.*;

/** The NPC module's adapter to the shared preset manager. Reuses the existing bot library and round application. */
public final class BotSettingsPresets implements ISettingsPresets
{
    private static final String TEAMS = "teams";
    private static final String SETTINGS = "settings";
    public static final ResourceLocation KEY = ResourceLocation.fromNamespaceAndPath("wolffsmodnpcs", "bots");

    @Override
    public Collection<String> names()
    {
        return List.copyOf(BotPresets.get().names());
    }

    private static CompoundTag document(List<String> teams, CompoundTag settings)
    {
        CompoundTag data = new CompoundTag();
        ListTag names = new ListTag();
        teams.forEach(team -> names.add(StringTag.valueOf(team)));
        data.put(TEAMS, names);
        data.put(SETTINGS, BotSettings.load(settings).save());
        return data;
    }

    private static List<String> teams(ITeamsRound round)
    {
        return round.getTeams().stream().map(ITeam::getShortName).toList();
    }

    private static List<String> teams(CompoundTag data)
    {
        return data.getList(TEAMS, Tag.TAG_STRING).stream().map(Tag::getAsString).toList();
    }

    @Override
    public CompoundTag read(String name)
    {
        BotPresets.Preset preset = BotPresets.get().find(name).orElseThrow(() -> new IllegalArgumentException("Unknown bot preset"));
        return document(preset.teams(), preset.settings());
    }

    @Override
    public CompoundTag capture(String target)
    {
        ITeamsRound round = round(target);
        return document(teams(round), round.getExtensionData(BotSettings.KEY));
    }

    @Override
    public void validate(CompoundTag data)
    {
        List<String> teams = teams(data);
        Set<String> knownTeams = new HashSet<>(teams);
        if (!data.getAllKeys().equals(Set.of(TEAMS, SETTINGS)) || !data.contains(TEAMS, Tag.TAG_LIST) || !data.contains(SETTINGS, Tag.TAG_COMPOUND) || teams.isEmpty() || teams.size() > 16
            || knownTeams.size() != teams.size() || teams.stream().anyMatch(String::isBlank))
            throw new IllegalArgumentException("Bot preset must contain settings and a unique, ordered team list");
        CompoundTag settings = data.getCompound(SETTINGS).copy();
        // Old exports may carry the removed player-respawn switch; it no longer affects any battle.
        if (settings.contains("player_tickets", Tag.TAG_BYTE))
            settings.remove("player_tickets");
        // Presets exported before emplacement support inherit its default-on switch.
        if (!settings.contains("emplacements"))
            settings.putBoolean("emplacements", true);
        if (!settings.contains("player_seat_deployment"))
            settings.putBoolean("player_seat_deployment", true);
        CompoundTag canonical = BotSettings.load(settings).save();
        checkSettings(settings, canonical, SETTINGS);
        if (!knownTeams.containsAll(settings.getCompound(TEAMS).getAllKeys()))
            throw new IllegalArgumentException("Bot forces reference a team outside the source team list");
    }

    /** Reject wrong types, unknown keys, invalid enum values and values the normal loader would silently repair. */
    private static void checkSettings(Tag value, Tag canonical, String path)
    {
        if (value instanceof CompoundTag object && canonical instanceof CompoundTag expected)
        {
            checkObject(object, expected, path);
            return;
        }
        if (value instanceof ListTag list && canonical instanceof ListTag expected && list.size() == expected.size())
        {
            for (int i = 0; i < list.size(); i++)
                checkSettings(list.get(i), expected.get(i), path + "[" + i + "]");
            return;
        }
        if (value instanceof NumericTag number && canonical instanceof NumericTag expected)
        {
            boolean valid = sameNumber(number, expected);
            if (valid)
                return;
        }
        else if (Objects.equals(value, canonical))
            return;
        throw new IllegalArgumentException("Invalid bot setting: " + path);
    }

    private static void checkObject(CompoundTag object, CompoundTag expected, String path)
    {
        if (!object.getAllKeys().equals(expected.getAllKeys()))
            throw new IllegalArgumentException("Missing or unknown bot setting in " + path);
        for (String key : object.getAllKeys())
            checkSettings(object.get(key), expected.get(key), path + "." + key);
    }

    private static boolean sameNumber(NumericTag value, NumericTag expected)
    {
        if (expected instanceof ByteTag)
            return value instanceof ByteTag && value.getAsByte() == expected.getAsByte();
        if (value instanceof ByteTag)
            return false;
        if (expected instanceof FloatTag)
            return Double.isFinite(value.getAsDouble()) && value.getAsFloat() == expected.getAsFloat();
        return value.getAsDouble() == expected.getAsDouble();
    }

    @Override
    public void save(String name, CompoundTag data, boolean replace) throws IOException
    {
        validate(data);
        if (BotPresets.invalidName(name))
            throw new IllegalArgumentException("Invalid bot preset name");
        if (!replace && BotPresets.get().find(name).isPresent())
            throw new IllegalArgumentException("A bot preset with this name already exists");
        BotPresets.get().putChecked(new BotPresets.Preset(name, teams(data), data.getCompound(SETTINGS)));
    }

    @Override
    public void remove(String name) throws IOException
    {
        BotPresets.get().removeChecked(name);
    }

    private static ITeamsRound round(String target)
    {
        return BotCommands.roundNamed(target).orElseThrow(() -> new IllegalArgumentException("Unknown round: " + target));
    }

    private static List<ITeamsRound> rounds(String target)
    {
        List<ITeamsRound> rounds = target.equals("all") ? FlansTeams.getRounds() : List.of(round(target));
        if (rounds.isEmpty())
            throw new IllegalArgumentException("The rotation has no rounds");
        return rounds;
    }

    @Override
    public List<Component> preview(CompoundTag data, String target)
    {
        validate(data);
        List<Component> lines = new ArrayList<>();
        List<String> source = teams(data);
        for (ITeamsRound round : rounds(target))
        {
            List<String> destination = teams(round);
            BotSettings settings = BotPresets.carryOver(data.getCompound(SETTINGS), source, destination);
            lines.add(Component.literal(round.getGameTypeId() + " @ " + round.getMapId()));
            for (int i = 0; i < destination.size(); i++)
            {
                String team = destination.get(i);
                String from = source.contains(team) || i >= source.size() ? team : source.get(i);
                lines.add(Component.literal(from + " → " + team));
            }
            lines.add(Component.translatable("wolffsmodnpcs.centre.bots.preset_summary", settings.getMode(), settings.getDifficulty(), BotEditor.fielded(round, settings)));
            lines.addAll(BotEditor.problems(round, settings));
            changes(BotSettings.load(round.getExtensionData(BotSettings.KEY)).save(), settings.save(), "", lines);
            for (var force : settings.getTeams().values())
            {
                if (!force.getCloneName().isEmpty() && (ServerCloneController.Instance == null || !ServerCloneController.Instance.getClones(force.getCloneTab()).contains(force.getCloneName())))
                    lines.add(Component.translatable("wolffsmodnpcs.centre.bots.preset_missing_clone", force.getCloneTab(), force.getCloneName()));
            }
        }
        return lines;
    }

    private static void changes(CompoundTag current, CompoundTag desired, String prefix, List<Component> lines)
    {
        Set<String> keys = new TreeSet<>(current.getAllKeys());
        keys.addAll(desired.getAllKeys());
        for (String key : keys)
        {
            Tag before = current.get(key);
            Tag after = desired.get(key);
            if (before instanceof CompoundTag beforeObject && after instanceof CompoundTag afterObject)
                changes(beforeObject, afterObject, prefix + key + ".", lines);
            else if (!Objects.equals(before, after))
                lines.add(Component.literal(prefix + key + ": " + Objects.toString(before, "—") + " → " + Objects.toString(after, "—")));
        }
    }

    @Override
    public void apply(CompoundTag data, String target)
    {
        validate(data);
        List<ITeamsRound> rounds = rounds(target);
        Map<ITeamsRound, CompoundTag> settings = new LinkedHashMap<>();
        for (ITeamsRound round : rounds)
            settings.put(round, BotPresets.carryOver(data.getCompound(SETTINGS), teams(data), teams(round)).save());
        settings.forEach((round, value) -> FlansTeams.setRoundExtensionData(round, BotSettings.KEY, value));
    }
}
