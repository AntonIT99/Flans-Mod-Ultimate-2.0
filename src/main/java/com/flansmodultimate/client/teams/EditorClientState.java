package com.flansmodultimate.client.teams;

import com.flansmodultimate.client.gui.TeamsEditorScreen;
import com.flansmodultimate.client.render.layer.PlayerSkinOverrides;
import com.flansmodultimate.common.teams.editor.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.Minecraft;

import java.util.UUID;

/** Connection-scoped mirror. It never reads or writes the client's config directory. */
public final class EditorClientState
{
    private static TeamEditorStore.Document document = new TeamEditorStore.Document(new JsonObject(), new JsonObject());
    private static long revision;
    private static UUID transfer;
    private static int nextChunk;
    private static int chunkCount;
    private static final StringBuilder incoming = new StringBuilder();

    private EditorClientState()
    {}

    @SuppressWarnings("java:S107") // A chunk carries independent transport metadata and editor response fields.
    public static void accept(UUID id, long incomingRevision, int index, int count, String text, String open, String key, String message)
    {
        if (count > 1024 || index < 0 || index >= count)
            return;
        if (index == 0)
        {
            transfer = id;
            nextChunk = 0;
            chunkCount = count;
            incoming.setLength(0);
        }
        if (!id.equals(transfer) || index != nextChunk || count != chunkCount || incoming.length() + text.length() > 2 * TeamEditorStore.MAX_BYTES)
            return;
        incoming.append(text);
        nextChunk++;
        if (nextChunk != count)
            return;
        JsonObject root = JsonParser.parseString(incoming.toString()).getAsJsonObject();
        TeamEditorStore.Document received = new TeamEditorStore.Document(root.getAsJsonObject("teams"), root.getAsJsonObject("classes"));
        EditorSchema.validate(received);
        document = received;
        revision = incomingRevision;
        EditorCatalog.installClient(received);
        PlayerSkinOverrides.clearValidationCache();
        incoming.setLength(0);
        transfer = null;
        Minecraft minecraft = Minecraft.getInstance();
        if (!open.isEmpty())
            minecraft.setScreen(new TeamsEditorScreen(open, key, message));
        else if (minecraft.screen instanceof TeamsEditorScreen editor)
            editor.serverUpdate(message);
    }

    public static TeamEditorStore.Document document()
    {
        return document.copy();
    }

    public static long revision()
    {
        return revision;
    }

    public static void clear()
    {
        document = new TeamEditorStore.Document(new JsonObject(), new JsonObject());
        revision = 0;
        transfer = null;
        incoming.setLength(0);
        EditorCatalog.clearClient();
    }
}
