package com.wolffsmod.npcs.teams;

import com.google.gson.*;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.wolffsmod.npcs.NpcsMod;
import com.wolffsmod.npcs.platform.NpcPlatform;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Bot settings saved under a name, to set up many rounds of a rotation alike: kept in
 * {@code config/wolffsmodnpcs/bot-presets.json}, next to the squad templates. A preset remembers the teams of the round it
 * was saved from; applied to a round with other teams, the forces of the teams the two share go to the same teams, and the
 * others in round order. Read on first use and written after every change. Server thread.
 */
public final class BotPresets
{
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String FILE_NAME = "bot-presets.json";
    private static final String PRESETS_KEY = "presets";
    private static final String NAME_KEY = "name";
    private static final String TEAMS_KEY = "teams";
    private static final String SETTINGS_KEY = "settings";
    /** What a preset may be called: lower-case letters, digits, - and _. */
    private static final Pattern NAME = Pattern.compile("[a-z0-9_-]{1,32}");

    private static BotPresets instance;

    private final Path file;
    private final Map<String, Preset> presets = new TreeMap<>();

    /**
     * A saved set of bot settings.
     *
     * @param teams
     *            the teams of the round it was saved from, in round order
     */
    public record Preset(String name, List<String> teams, CompoundTag settings)
    {
        public Preset
        {
            teams = List.copyOf(teams);
            settings = settings.copy();
        }
    }

    BotPresets(Path file)
    {
        this.file = file;
    }

    public static BotPresets get()
    {
        if (instance == null)
        {
            instance = new BotPresets(NpcPlatform.configDir().resolve(NpcsMod.MOD_ID).resolve(FILE_NAME));
            instance.load();
        }
        return instance;
    }

    /** Whether a name cannot be a preset's: only lower-case letters, digits, - and _ may be used. */
    public static boolean invalidName(String name)
    {
        return !NAME.matcher(name).matches();
    }

    public Collection<String> names()
    {
        return Collections.unmodifiableCollection(presets.keySet());
    }

    public Optional<Preset> find(String name)
    {
        return Optional.ofNullable(presets.get(name.toLowerCase(Locale.ROOT)));
    }

    /** Adds or replaces a preset and saves the file. */
    public void put(Preset preset)
    {
        presets.put(preset.name().toLowerCase(Locale.ROOT), preset);
        save();
    }

    public boolean remove(String name)
    {
        boolean removed = presets.remove(name.toLowerCase(Locale.ROOT)) != null;
        if (removed)
            save();
        return removed;
    }

    /**
     * The settings of one round carried over to a round with its own teams: a team the two share keeps its force, and
     * every other team of the target takes the force of the source team in its place in round order.
     *
     * @param sourceTeams
     *            the teams the settings were made for, in round order
     * @param targetTeams
     *            the teams of the round they go to, in round order
     */
    public static BotSettings carryOver(CompoundTag settings, List<String> sourceTeams, List<String> targetTeams)
    {
        BotSettings carried = BotSettings.load(settings);
        Map<String, BotSettings.TeamForce> forces = new LinkedHashMap<>(carried.getTeams());
        carried.getTeams().clear();
        for (int i = 0; i < targetTeams.size(); i++)
        {
            String target = targetTeams.get(i);
            String source = sourceTeams.contains(target) || i >= sourceTeams.size() ? target : sourceTeams.get(i);
            BotSettings.TeamForce force = forces.get(source);
            if (force != null)
                carried.getTeams().put(target, BotSettings.TeamForce.load(force.save()));
        }
        return carried;
    }

    /** Reads the file; a missing or unreadable one leaves no presets and is reported, never thrown. */
    void load()
    {
        presets.clear();
        if (!Files.exists(file))
            return;
        try
        {
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            for (JsonElement element : root.getAsJsonArray(PRESETS_KEY))
                read(element.getAsJsonObject()).ifPresent(preset -> presets.put(preset.name(), preset));
        }
        catch (IOException | RuntimeException e)
        {
            NpcsMod.log.error("Cannot read bot presets from {}", file, e);
        }
    }

    private static Optional<Preset> read(JsonObject entry)
    {
        String name = entry.has(NAME_KEY) ? entry.get(NAME_KEY).getAsString().toLowerCase(Locale.ROOT) : "";
        if (invalidName(name))
            return Optional.empty();
        List<String> teams = new ArrayList<>();
        if (entry.has(TEAMS_KEY))
            entry.getAsJsonArray(TEAMS_KEY).forEach(team -> teams.add(team.getAsString()));
        try
        {
            CompoundTag settings = entry.has(SETTINGS_KEY) ? TagParser.parseTag(entry.get(SETTINGS_KEY).getAsString()) : new CompoundTag();
            return Optional.of(new Preset(name, teams, settings));
        }
        catch (CommandSyntaxException e)
        {
            NpcsMod.log.error("Bot preset {} has unreadable settings", name, e);
            return Optional.empty();
        }
    }

    /** Writes the file through a temporary one, so an interrupted write never leaves it half written. */
    void save()
    {
        JsonObject root = new JsonObject();
        JsonArray list = new JsonArray();
        for (Preset preset : presets.values())
        {
            JsonObject entry = new JsonObject();
            entry.addProperty(NAME_KEY, preset.name());
            JsonArray teams = new JsonArray();
            preset.teams().forEach(teams::add);
            entry.add(TEAMS_KEY, teams);
            entry.addProperty(SETTINGS_KEY, preset.settings().toString());
            list.add(entry);
        }
        root.add(PRESETS_KEY, list);
        try
        {
            Files.createDirectories(file.getParent());
            Path temporary = file.resolveSibling(FILE_NAME + ".tmp");
            Files.writeString(temporary, JSON.toJson(root), StandardCharsets.UTF_8);
            replace(temporary);
        }
        catch (IOException e)
        {
            NpcsMod.log.error("Cannot save bot presets to {}", file, e);
        }
    }

    /** Puts the written file in place, atomically where the file system allows. */
    private void replace(Path temporary) throws IOException
    {
        try
        {
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (AtomicMoveNotSupportedException e)
        {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
