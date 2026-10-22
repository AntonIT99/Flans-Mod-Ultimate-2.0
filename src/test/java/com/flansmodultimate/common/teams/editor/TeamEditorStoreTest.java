package com.flansmodultimate.common.teams.editor;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class TeamEditorStoreTest
{
    @TempDir
    Path directory;

    private static TeamEditorStore.Document document(String name)
    {
        JsonObject teams = new JsonObject();
        JsonObject team = new JsonObject();
        team.addProperty("name", name);
        teams.add("custom:blue", team);
        return new TeamEditorStore.Document(teams, new JsonObject());
    }

    @Test
    void savesAcrossInstancesAndKeepsBackupWithoutMutatingTheDocument() throws IOException
    {
        TeamEditorStore store = new TeamEditorStore(directory);
        TeamEditorStore.Document first = document("Blue");
        store.write(first);
        assertEquals(first, new TeamEditorStore(directory).load());
        store.write(document("Green"));
        assertEquals("Blue", first.teams().getAsJsonObject("custom:blue").get("name").getAsString());
        assertTrue(Files.readString(directory.resolve("teams.json.bak")).contains("Blue"));
        assertFalse(Files.exists(directory.resolve("pending.json")));
    }

    @Test
    void recoversAnInterruptedPairedWrite() throws IOException
    {
        TeamEditorStore store = new TeamEditorStore(directory);
        store.write(document("Before"));
        TeamEditorStore.Document committed = document("After");
        JsonObject journal = new JsonObject();
        journal.add("teams", TeamEditorStore.envelope(committed.teams()));
        journal.add("classes", TeamEditorStore.envelope(committed.classes()));
        Files.writeString(directory.resolve("pending.json"), journal.toString());
        assertEquals(committed, store.load());
        assertEquals(committed, new TeamEditorStore(directory).load());
        assertFalse(Files.exists(directory.resolve("pending.json")));
    }

    @Test
    void rejectsMalformedAndFutureDocumentsWithoutOverwritingThem() throws IOException
    {
        TeamEditorStore store = new TeamEditorStore(directory);
        store.write(document("Before"));
        for (String invalid : new String[]{"{", "{\"schemaVersion\":2,\"entries\":{}}", "{\"schemaVersion\":null,\"entries\":{}}"})
        {
            Files.writeString(directory.resolve("teams.json"), invalid);
            assertThrows(IOException.class, store::load);
            assertEquals(invalid, Files.readString(directory.resolve("teams.json")));
        }
    }

    @Test
    void presetsRoundTripAndCannotEscapeTheirDirectory() throws IOException
    {
        TeamEditorStore store = new TeamEditorStore(directory);
        store.exportPreset("arena_1", document("Blue"));
        assertEquals(document("Blue"), store.importPreset("arena_1"));
        TeamEditorStore.Document exported = document("Blue");
        assertThrows(IllegalArgumentException.class, () -> store.exportPreset("../escape", exported));
    }
}
