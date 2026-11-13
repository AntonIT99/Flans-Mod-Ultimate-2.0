package com.flansmodultimate.common.eras;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Service periods a server owner set by command, kept in {@code issue-dates.json} and keyed by lower-case shortname. An
 * override replaces whatever the content pack and categories say about that item. Server thread.
 */
public final class IssueDateOverrides
{
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final int FORMAT_VERSION = 1;
    private static final long MAX_BYTES = 4L * 1024 * 1024;
    private static final String KEY_VERSION = "version";
    private static final String KEY_ITEMS = "items";
    private static final String KEY_ISSUE = "issue";
    private static final String KEY_RETIREMENT = "retirement";

    private final Path file;
    private final Map<String, ServicePeriod> periods = new TreeMap<>();

    public IssueDateOverrides(Path file)
    {
        this.file = file;
    }

    /**
     * Replaces the overrides held with those in the file. A missing file means no overrides; an unreadable one leaves none
     * loaded and an entry that cannot be read is skipped. Either way the problems are returned for the log.
     */
    public List<String> load()
    {
        periods.clear();
        List<String> problems = new ArrayList<>();
        if (!Files.isRegularFile(file))
            return problems;
        JsonObject root;
        try
        {
            if (Files.size(file) > MAX_BYTES)
                throw new IOException("the file exceeds " + MAX_BYTES + " bytes");
            root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
        }
        catch (IOException | RuntimeException exception)
        {
            problems.add("Could not read " + file.getFileName() + ": " + exception.getMessage());
            return problems;
        }
        JsonElement items = root.get(KEY_ITEMS);
        if (items == null || !items.isJsonObject())
        {
            problems.add(file.getFileName() + " has no \"" + KEY_ITEMS + "\" object");
            return problems;
        }
        for (Map.Entry<String, JsonElement> entry : items.getAsJsonObject().entrySet())
        {
            try
            {
                JsonObject item = entry.getValue().getAsJsonObject();
                ServiceDate issue = ServiceDate.parse(item.get(KEY_ISSUE).getAsString());
                JsonElement retirement = item.get(KEY_RETIREMENT);
                periods.put(key(entry.getKey()), new ServicePeriod(issue, retirement == null || retirement.isJsonNull() ? null : ServiceDate.parse(retirement.getAsString())));
            }
            catch (RuntimeException exception)
            {
                problems.add("Override for '" + entry.getKey() + "' skipped: " + (exception.getMessage() == null ? "expected {\"issue\": \"<date>\"}" : exception.getMessage()));
            }
        }
        return problems;
    }

    /** Writes every override, keeping the previous file as {@code .bak}. */
    public void save() throws IOException
    {
        JsonObject items = new JsonObject();
        periods.forEach((shortname, period) ->
        {
            JsonObject item = new JsonObject();
            item.addProperty(KEY_ISSUE, period.introduction().toString());
            if (period.retirement() != null)
                item.addProperty(KEY_RETIREMENT, period.retirement().toString());
            items.add(shortname, item);
        });
        JsonObject root = new JsonObject();
        root.addProperty(KEY_VERSION, FORMAT_VERSION);
        root.add(KEY_ITEMS, items);

        Path parent = file.toAbsolutePath().getParent();
        if (parent != null)
            Files.createDirectories(parent);
        if (Files.isRegularFile(file))
            Files.copy(file, file.resolveSibling(file.getFileName() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(temporary, JSON.toJson(root), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.SYNC);
        try
        {
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (AtomicMoveNotSupportedException ignored)
        {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public Optional<ServicePeriod> get(@Nullable String shortname)
    {
        return shortname == null ? Optional.empty() : Optional.ofNullable(periods.get(key(shortname)));
    }

    public Map<String, ServicePeriod> getAll()
    {
        return Collections.unmodifiableMap(periods);
    }

    public void set(String shortname, ServicePeriod period)
    {
        periods.put(key(shortname), period);
    }

    /** Whether there was an override to remove. */
    public boolean clear(String shortname)
    {
        return periods.remove(key(shortname)) != null;
    }

    private static String key(String shortname)
    {
        return shortname.strip().toLowerCase(Locale.ROOT);
    }
}
