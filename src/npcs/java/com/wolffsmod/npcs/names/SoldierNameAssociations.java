package com.wolffsmod.npcs.names;

import com.google.gson.Gson;
import com.wolffsmod.npcs.NpcsMod;
import com.wolffsmod.npcs.platform.NpcPlatform;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;

/** Server-owned shortname associations, read at server startup from config/wolffsmodnpcs/soldier-names.json. */
public record SoldierNameAssociations(Map<String, String> teams, Map<String, String> classes, Map<String, String> teamGenders, Map<String, String> classGenders)
{
    private static final String DEFAULTS = "/data/wolffsmodnpcs/soldier-names.json";
    private static SoldierNameAssociations current = new SoldierNameAssociations(Map.of(), Map.of());

    public SoldierNameAssociations(Map<String, String> teams, Map<String, String> classes)
    {
        this(teams, classes, Map.of(), Map.of());
    }

    public SoldierNameAssociations
    {
        teams = validate(teams);
        classes = validate(classes);
        teamGenders = validateGenders(teamGenders);
        classGenders = validateGenders(classGenders);
    }

    private static Map<String, String> validateGenders(Map<String, String> entries)
    {
        if (entries == null)
            return Map.of();
        entries.forEach((id, gender) ->
        {
            if (id.isBlank() || !id.equals(id.strip()) || gender == null || !List.of("male", "female", "mixed").contains(gender))
                throw new IllegalArgumentException("Invalid soldier name gender: " + id + " -> " + gender);
        });
        return normalize(entries);
    }

    /** Flan normalizes loaded shortnames to lowercase, while legacy source definitions often use mixed case. */
    private static Map<String, String> normalize(Map<String, String> entries)
    {
        Map<String, String> normalized = new HashMap<>();
        entries.forEach((id, value) ->
        {
            if (normalized.putIfAbsent(id.toLowerCase(Locale.ROOT), value) != null)
                throw new IllegalArgumentException("Duplicate soldier name association ID ignoring case: " + id);
        });
        return Map.copyOf(normalized);
    }

    /** Class gender overrides team gender independently of list inheritance; omitted values default to male. */
    public int gender(String team, String playerClass)
    {
        return switch (classGenders.getOrDefault(playerClass.toLowerCase(Locale.ROOT), teamGenders.getOrDefault(team.toLowerCase(Locale.ROOT), "male")))
        {
            case "female" -> 2;
            case "mixed" -> 0;
            default -> 1;
        };
    }

    private static Map<String, String> validate(Map<String, String> entries)
    {
        if (entries == null)
            return Map.of();
        entries.forEach((id, list) ->
        {
            if (id.isBlank() || !id.equals(id.strip()) || list == null || !list.isEmpty() && !SoldierNames.acceptsAssociation(list))
                throw new IllegalArgumentException("Invalid soldier name association: " + id + " -> " + list);
        });
        return normalize(entries);
    }

    /** A class entry wins even when empty; an empty entry disables inherited naming. IDs match without case sensitivity. */
    public OptionalInt find(String team, String playerClass)
    {
        String list = classes.getOrDefault(playerClass.toLowerCase(Locale.ROOT), teams.getOrDefault(team.toLowerCase(Locale.ROOT), ""));
        return list.isEmpty() ? OptionalInt.empty() : OptionalInt.of(SoldierNames.generatorId(list));
    }

    public static SoldierNameAssociations current()
    {
        return current;
    }

    /** Reloaded for each integrated or dedicated server; malformed user data disables automatic naming and stays intact. */
    public static void load()
    {
        Path file = NpcPlatform.configDir().resolve(NpcsMod.MOD_ID).resolve("soldier-names.json");
        current = new SoldierNameAssociations(Map.of(), Map.of());
        try
        {
            current = read(file);
        }
        catch (IOException | RuntimeException exception)
        {
            NpcsMod.log.error("Cannot load soldier name associations from {}; automatic class naming is disabled", file, exception);
        }
    }

    static SoldierNameAssociations read(Path file) throws IOException
    {
        if (!Files.exists(file))
        {
            Files.createDirectories(file.getParent());
            try (var defaults = SoldierNameAssociations.class.getResourceAsStream(DEFAULTS))
            {
                if (defaults == null)
                    throw new IOException("Missing bundled soldier name associations: " + DEFAULTS);
                Files.copy(defaults, file);
            }
        }
        try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8))
        {
            SoldierNameAssociations result = new Gson().fromJson(reader, SoldierNameAssociations.class);
            if (result == null)
                throw new IllegalArgumentException("Soldier name associations must be a JSON object");
            return result;
        }
    }
}
