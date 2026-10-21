package com.wolffsmod.npcs.teams;

import com.google.gson.*;
import com.wolffsmod.npcs.NpcsMod;
import com.wolffsmod.npcs.platform.NpcPlatform;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * The server's squad templates, kept in {@code config/wolffsmodnpcs/squad-templates.json} so operators can also edit
 * them by hand, as the teams and classes editor keeps its definitions. Read on first use and written after every
 * change. Server thread.
 */
public final class SquadTemplates
{
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String FILE_NAME = "squad-templates.json";
    private static final String TEMPLATES_KEY = "templates";

    private static SquadTemplates instance;

    private final Path file;
    private final Map<String, SquadTemplate> templates = new TreeMap<>();

    SquadTemplates(Path file)
    {
        this.file = file;
    }

    public static SquadTemplates get()
    {
        if (instance == null)
        {
            instance = new SquadTemplates(NpcPlatform.configDir().resolve(NpcsMod.MOD_ID).resolve(FILE_NAME));
            instance.load();
        }
        return instance;
    }

    public Optional<SquadTemplate> find(String id)
    {
        return Optional.ofNullable(templates.get(id.toLowerCase(Locale.ROOT)));
    }

    public Collection<SquadTemplate> all()
    {
        return Collections.unmodifiableCollection(templates.values());
    }

    /** Adds or replaces a template and saves the file. */
    public void put(SquadTemplate template)
    {
        templates.put(template.id().toLowerCase(Locale.ROOT), template);
        save();
    }

    public boolean remove(String id)
    {
        boolean removed = templates.remove(id.toLowerCase(Locale.ROOT)) != null;
        if (removed)
            save();
        return removed;
    }

    /** Reads the file; a missing or unreadable one leaves no templates and is reported, never thrown. */
    void load()
    {
        templates.clear();
        if (!Files.exists(file))
            return;
        try
        {
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            for (JsonElement element : root.getAsJsonArray(TEMPLATES_KEY))
            {
                SquadTemplate template = JSON.fromJson(element, SquadTemplate.class);
                if (template != null && template.id() != null && !template.id().isBlank())
                    templates.put(template.id().toLowerCase(Locale.ROOT),
                        new SquadTemplate(template.id(), template.name(), template.posture(), template.nameList(), template.members() == null ? List.of() : template.members()));
            }
        }
        catch (IOException | RuntimeException e)
        {
            NpcsMod.log.error("Cannot read squad templates from {}", file, e);
        }
    }

    /** Writes the file through a temporary one, so an interrupted write never leaves it half written. */
    void save()
    {
        JsonObject root = new JsonObject();
        JsonArray list = new JsonArray();
        templates.values().forEach(template -> list.add(JSON.toJsonTree(template)));
        root.add(TEMPLATES_KEY, list);
        try
        {
            Files.createDirectories(file.getParent());
            Path temporary = file.resolveSibling(FILE_NAME + ".tmp");
            Files.writeString(temporary, JSON.toJson(root), StandardCharsets.UTF_8);
            replace(temporary);
        }
        catch (IOException e)
        {
            NpcsMod.log.error("Cannot save squad templates to {}", file, e);
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
