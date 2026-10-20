package com.flansmodultimate.common.teams.editor;

import com.google.gson.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Set;

/** The server-wide, manually editable definition files. A journal recovers interrupted paired writes. */
public final class TeamEditorStore
{
    public static final int SCHEMA = 1;
    public static final int MAX_BYTES = 8 * 1024 * 1024;
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final Path directory;

    public record Document(JsonObject teams, JsonObject classes)
    {
        public Document copy()
        {
            return new Document(teams.deepCopy(), classes.deepCopy());
        }
    }

    public TeamEditorStore(Path directory)
    {
        this.directory = directory;
    }

    public Document load() throws IOException
    {
        Files.createDirectories(directory);
        Path journal = directory.resolve("pending.json");
        if (Files.exists(journal))
        {
            JsonObject recovery = read(journal);
            Document recovered = new Document(entries(recovery.getAsJsonObject("teams")), entries(recovery.getAsJsonObject("classes")));
            write(recovered);
        }
        return new Document(loadEntries("teams.json"), loadEntries("classes.json"));
    }

    private JsonObject loadEntries(String name) throws IOException
    {
        Path path = directory.resolve(name);
        if (Files.exists(path))
            return entries(read(path));
        JsonObject empty = new JsonObject();
        atomicWrite(path, JSON.toJson(envelope(empty)));
        return empty;
    }

    private static JsonObject entries(JsonObject root) throws IOException
    {
        try
        {
            return checkedEntries(root);
        }
        catch (RuntimeException exception)
        {
            throw new IOException("Invalid definition document: " + exception.getMessage(), exception);
        }
    }

    private static JsonObject checkedEntries(JsonObject root) throws IOException
    {
        if (root == null || !root.has("schemaVersion") || root.get("schemaVersion").getAsInt() != SCHEMA || !root.has("entries") || !root.get("entries").isJsonObject())
            throw new IOException("Expected schemaVersion " + SCHEMA + " and an entries object");
        if (!Set.of("schemaVersion", "entries").containsAll(root.keySet()))
            throw new IOException("Unknown document field; expected schemaVersion and entries");
        return root.getAsJsonObject("entries");
    }

    public static JsonObject envelope(JsonObject entries)
    {
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", SCHEMA);
        root.add("entries", entries.deepCopy());
        return root;
    }

    public void write(Document document) throws IOException
    {
        Files.createDirectories(directory);
        JsonObject transaction = new JsonObject();
        transaction.add("teams", envelope(document.teams()));
        transaction.add("classes", envelope(document.classes()));
        Path journal = directory.resolve("pending.json");
        if (!Files.exists(journal))
        {
            for (String name : new String[]{"teams.json", "classes.json"})
            {
                Path path = directory.resolve(name);
                if (Files.exists(path))
                    Files.copy(path, directory.resolve(name + ".bak"), StandardCopyOption.REPLACE_EXISTING);
            }
        }
        // Once the journal is durable, a restart completes this exact transaction without replacing its pre-commit backups.
        atomicWrite(journal, JSON.toJson(transaction));
        atomicWrite(directory.resolve("teams.json"), JSON.toJson(envelope(document.teams())));
        atomicWrite(directory.resolve("classes.json"), JSON.toJson(envelope(document.classes())));
        Files.delete(journal);
    }

    public void exportPreset(String name, Document document) throws IOException
    {
        Path path = preset(name);
        Files.createDirectories(path.getParent());
        JsonObject root = new JsonObject();
        root.add("teams", envelope(document.teams()));
        root.add("classes", envelope(document.classes()));
        atomicWrite(path, JSON.toJson(root));
    }

    public Document importPreset(String name) throws IOException
    {
        JsonObject root = read(preset(name));
        return new Document(entries(root.getAsJsonObject("teams")), entries(root.getAsJsonObject("classes")));
    }

    private Path preset(String name)
    {
        if (!name.matches("[a-zA-Z0-9_-]{1,64}"))
            throw new IllegalArgumentException("Preset names may contain letters, numbers, underscores and hyphens");
        return directory.resolve("presets").resolve(name + ".json");
    }

    private static JsonObject read(Path path) throws IOException
    {
        if (Files.size(path) > MAX_BYTES)
            throw new IOException(path.getFileName() + " exceeds " + MAX_BYTES + " bytes");
        try
        {
            return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
        }
        catch (RuntimeException exception)
        {
            throw new IOException(path.getFileName() + ": " + exception.getMessage(), exception);
        }
    }

    private static void atomicWrite(Path target, String text) throws IOException
    {
        if (text.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES)
            throw new IOException("Definition file exceeds " + MAX_BYTES + " bytes");
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        Files.writeString(temporary, text, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.SYNC);
        try
        {
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (AtomicMoveNotSupportedException ignored)
        {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
