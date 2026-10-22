package com.flansmodultimate.common.teams.editor;

import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.teams.TeamsManager;
import com.flansmodultimate.common.types.*;
import com.flansmodultimate.network.client.teams.PacketEditorState;
import com.flansmodultimate.network.client.teams.PacketTeamsState;
import com.flansmodultimate.platform.PlatformPaths;
import com.flansmodultimate.util.FlansLog;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.util.Locale;

/** Server-thread-only authority used by both commands and the graphical editor. */
public final class TeamEditorService
{
    private static final TeamEditorService INSTANCE = new TeamEditorService();
    private final TeamEditorStore store = new TeamEditorStore(PlatformPaths.configDir().resolve("flansmodultimate/teams-editor"));
    private TeamEditorStore.Document document = new TeamEditorStore.Document(new JsonObject(), new JsonObject());
    private long revision;
    @Nullable
    private MinecraftServer server;
    private boolean writable;

    private TeamEditorService()
    {}

    public static TeamEditorService get()
    {
        return INSTANCE;
    }

    public void start(MinecraftServer server)
    {
        this.server = server;
        revision++;
        document = new TeamEditorStore.Document(new JsonObject(), new JsonObject());
        EditorCatalog.clearServer();
        writable = false;
        try
        {
            reload();
        }
        catch (IOException | RuntimeException exception)
        {
            FlansLog.log.error("Cannot load team editor definitions; editing is locked until /teams editor reload succeeds", exception);
        }
    }

    public void stop()
    {
        server = null;
        writable = false;
        EditorCatalog.clearServer();
    }

    public void reload() throws IOException
    {
        TeamEditorStore.Document loaded = store.load();
        EditorSchema.validate(loaded);
        EditorCatalog next = new EditorCatalog(loaded);
        checkActiveRound(next);
        document = loaded;
        EditorCatalog.installServer(next);
        writable = true;
        revision++;
        publish();
    }

    public void exportPreset(String name) throws IOException
    {
        store.exportPreset(name, document);
    }

    public void importPreset(String name) throws IOException
    {
        TeamEditorStore.Document imported = store.importPreset(name);
        TeamEditorStore.Document next = document.copy();
        imported.teams().entrySet().forEach(entry -> next.teams().add(entry.getKey(), entry.getValue()));
        imported.classes().entrySet().forEach(entry -> next.classes().add(entry.getKey(), entry.getValue()));
        commit(next, revision);
    }

    public void edit(boolean team, String key, String operation, JsonObject values, long expectedRevision) throws IOException
    {
        TeamEditorStore.Document next = document.copy();
        JsonObject entries = team ? next.teams() : next.classes();
        if ("restore".equals(operation))
            entries.remove(key);
        else if ("delete".equals(operation) && key.startsWith("custom:"))
            entries.remove(key);
        else
        {
            JsonObject patch = entries.has(key) ? entries.getAsJsonObject(key).deepCopy() : new JsonObject();
            if ("disable".equals(operation) || "delete".equals(operation))
                patch.addProperty("disabled", true);
            else if ("enable".equals(operation))
                patch.addProperty("disabled", false);
            else if ("create".equals(operation))
            {
                if (!key.startsWith("custom:") || entries.has(key))
                    throw new IllegalArgumentException("Choose an unused custom ID");
                patch = values.deepCopy();
            }
            else if ("save".equals(operation))
                EditorDescriptions.merge(patch, values);
            else
                throw new IllegalArgumentException("Unknown editor operation");
            entries.add(key, patch);
        }
        commit(next, expectedRevision);
    }

    private void commit(TeamEditorStore.Document next, long expectedRevision) throws IOException
    {
        if (server == null || !writable)
            throw new IllegalArgumentException("Editor is locked: fix the JSON files and run /teams editor reload");
        if (expectedRevision != revision)
            throw new IllegalArgumentException("Definitions changed while you were editing. Reopen the entry before saving");
        TeamEditorStore.Document disk = store.load();
        if (!disk.equals(document))
            throw new IllegalArgumentException("JSON files changed on disk. Run /teams editor reload before editing");
        EditorSchema.validate(next);
        EditorCatalog catalog = new EditorCatalog(next);
        checkActiveRound(catalog);
        store.write(next);
        document = next;
        EditorCatalog.installServer(catalog);
        revision++;
        publish();
    }

    private static void checkActiveRound(EditorCatalog catalog)
    {
        TeamsManager manager = TeamsManager.getInstance();
        if (!manager.isRoundRunning())
            return;
        manager.getCurrentRound().ifPresent(round -> round.getTeamIds().forEach(id ->
        {
            Team base = Team.packValues().stream().filter(team -> team.getShortName().equals(id)).findFirst().orElse(null);
            if (catalog.team(id, base) == null)
                throw new IllegalArgumentException("Team " + id + " is in the running round. Stop the round before removing it");
        }));
    }

    private void publish()
    {
        if (server == null)
            return;
        for (ServerPlayer player : server.getPlayerList().getPlayers())
        {
            PlayerData data = PlayerData.getInstance(player);
            data.setTeam(resolveTeam(data.getTeam()));
            data.setNewTeam(resolveTeam(data.getNewTeam()));
            PlayerClass pending = data.getNewPlayerClass();
            if (pending != null)
            {
                PlayerClass updated = PlayerClass.getPlayerClass(pending.getShortName());
                Team team = data.getNewTeam();
                if (team != null && team != Team.SPECTATORS && (updated == null || !team.getClasses().contains(updated)))
                {
                    updated = team.getClasses().stream().findFirst().orElse(null);
                    if (updated == null)
                        data.setNewTeam(Team.SPECTATORS);
                    player.sendSystemMessage(Component.literal("Your class was removed. Your next spawn uses the updated team selection."));
                }
                data.setNewPlayerClass(updated);
            }
            sync(player, "", "", "");
        }
        TeamsManager.getInstance().syncAll(PacketTeamsState.OpenScreen.NONE);
        TeamsManager.getInstance().syncPlayerClassSkins(true);
    }

    @Nullable
    private static Team resolveTeam(@Nullable Team previous)
    {
        if (previous == null || previous == Team.SPECTATORS)
            return previous;
        Team updated = Team.getTeam(previous.getShortName());
        return updated == null ? Team.SPECTATORS : updated;
    }

    public void sync(ServerPlayer player, String open, String key, String message)
    {
        PacketEditorState.send(player, document, revision, open, key, message);
    }

    public long revision()
    {
        return revision;
    }

    public static String resolveKey(boolean team, String id)
    {
        if (id.startsWith("pack/") || id.startsWith("custom:"))
            return id;
        InfoType type = team ? Team.getTeam(id) : PlayerClass.getPlayerClass(id);
        if (type == null)
            type = (team ? Team.packValues().stream() : PlayerClass.packValues().stream()).filter(candidate -> candidate.getShortName().equalsIgnoreCase(id)).findFirst().orElse(null);
        if (type == Team.SPECTATORS)
            throw new IllegalArgumentException("The built-in spectators cannot be edited");
        return type == null ? "custom:" + id.toLowerCase(Locale.ROOT) : EditorCatalog.key(type);
    }
}
