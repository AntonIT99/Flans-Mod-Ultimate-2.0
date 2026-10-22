package com.flansmodultimate.common.teams.editor;

import com.flansmodultimate.common.types.*;
import com.flansmodultimate.content.ContentPack;
import com.flansmodultimate.content.PackagedContentProvider;
import com.flansmodultimate.platform.PlatformEnvironment;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

/** Immutable side-specific views; pack registries are never changed by a server connection. */
public final class EditorCatalog
{
    public static final ContentPack CUSTOM_PACK = new ContentPack("Server editor", Path.of("config/flansmodultimate/teams-editor"));
    private static final EditorCatalog EMPTY = new EditorCatalog();
    private static EditorCatalog server = EMPTY;
    private static EditorCatalog client = EMPTY;
    private final Map<String, Team> teamOverrides = new LinkedHashMap<>();
    private final Map<String, PlayerClass> classOverrides = new LinkedHashMap<>();
    private final Set<String> disabledTeams = new HashSet<>();
    private final Set<String> disabledClasses = new HashSet<>();

    private EditorCatalog()
    {}

    public EditorCatalog(TeamEditorStore.Document document)
    {
        Map<String, Team> originals = new LinkedHashMap<>();
        Team.packValues().forEach(type -> originals.put(key(type), type));
        Map<String, PlayerClass> originalClasses = new LinkedHashMap<>();
        PlayerClass.packValues().forEach(type -> originalClasses.put(key(type), type));
        document.classes().entrySet().forEach(entry ->
        {
            JsonObject patch = entry.getValue().getAsJsonObject();
            PlayerClass base = originalClasses.get(entry.getKey());
            if (base == null && !entry.getKey().startsWith("custom:"))
                return;
            String id = base == null ? entry.getKey() : base.getShortName();
            if (disabled(patch))
                disabledClasses.add(id);
            else
                classOverrides.put(id, new EditedPlayerClass(id, base, patch));
        });
        document.teams().entrySet().forEach(entry ->
        {
            JsonObject patch = entry.getValue().getAsJsonObject();
            Team base = originals.get(entry.getKey());
            if (base == null && !entry.getKey().startsWith("custom:"))
                return;
            String id = base == null ? entry.getKey() : base.getShortName();
            if (disabled(patch))
                disabledTeams.add(id);
            else
                teamOverrides.put(id, new EditedTeam(id, base, patch));
        });
    }

    public static EditorCatalog current()
    {
        return PlatformEnvironment.isServerThread() ? server : client;
    }

    public static void installServer(EditorCatalog catalog)
    {
        server = catalog;
    }

    public static void installClient(TeamEditorStore.Document document)
    {
        client = new EditorCatalog(document);
    }

    public static void clearClient()
    {
        client = EMPTY;
    }

    public static void clearServer()
    {
        server = EMPTY;
    }

    public static String key(InfoType type)
    {
        if (type.getContentPack() == CUSTOM_PACK)
            return type.getShortName();
        String pack = type.getContentPack() instanceof PackagedContentProvider packaged ? packaged.getPackId() : type.getContentPack().getName();
        return "pack/" + URLEncoder.encode(pack.toLowerCase(Locale.ROOT), StandardCharsets.UTF_8) + "/" + type.getOriginalShortName().toLowerCase(Locale.ROOT);
    }

    public static boolean disabled(JsonObject patch)
    {
        return patch.has("disabled") && patch.get("disabled").getAsBoolean();
    }

    @Nullable
    public Team team(@Nullable String id, @Nullable Team base)
    {
        String name = base == null ? normalize(id) : base.getShortName();
        if (disabledTeams.contains(name))
            return null;
        return teamOverrides.getOrDefault(name, base);
    }

    @Nullable
    public PlayerClass playerClass(@Nullable String id, @Nullable PlayerClass base)
    {
        String name = base == null ? normalize(id) : base.getShortName();
        if (disabledClasses.contains(name))
            return null;
        return classOverrides.getOrDefault(name, base);
    }

    public Collection<Team> teams(Collection<Team> originals)
    {
        Map<String, Team> all = new LinkedHashMap<>();
        originals.forEach(type -> all.put(type.getShortName(), type));
        all.putAll(teamOverrides);
        disabledTeams.forEach(all::remove);
        return List.copyOf(all.values());
    }

    public Collection<PlayerClass> classes(Collection<PlayerClass> originals)
    {
        Map<String, PlayerClass> all = new LinkedHashMap<>();
        originals.forEach(type -> all.put(type.getShortName(), type));
        all.putAll(classOverrides);
        disabledClasses.forEach(all::remove);
        return List.copyOf(all.values());
    }

    @Nullable
    public static PlayerClass resolveClass(String reference)
    {
        PlayerClass base = PlayerClass.packValues().stream().filter(type -> key(type).equals(reference)).findFirst().orElse(null);
        return base == null ? PlayerClass.getPlayerClass(reference) : PlayerClass.getPlayerClass(base.getShortName());
    }

    private static String normalize(@Nullable String id)
    {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
    }
}
