package com.flansmodultimate.util;

import com.flansmodultimate.FlansMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SoundJsonProcessor {
    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();

    private static final String DEFAULT_CATEGORY = "player";

    /**
     * Full processing:
     * - Strip BOM, lowercase content
     * - Parse & dedupe top-level keys (keep first)
     * - Sanitize top-level keys and any sound identifiers (namespace:path)
     * - Add entries for .ogg under soundsDir not referenced anywhere in sounds.json
     * - Pretty print; delete file if empty
     *
     * @param soundsJsonFile path to sounds.json
     * @param namespace      namespace to use (e.g., "flansmod")
     * @param soundsDir      folder that contains .ogg files (e.g., assets/flansmod/sounds)
     */
    public static void process(Path soundsJsonFile, String namespace, Path soundsDir) {
        try
        {
            String content = Files.readString(soundsJsonFile, StandardCharsets.UTF_8);

            // Strip UTF-8 BOM if present
            if (!content.isEmpty() && content.charAt(0) == '\uFEFF')
            {
                content = content.substring(1);
            }

            if (content.isBlank())
            {
                Files.deleteIfExists(soundsJsonFile);
                return;
            }

            // Normalize case
            content = content.toLowerCase(Locale.ROOT);

            // Parse & dedupe to a JsonObject (keeps first key occurrence)
            JsonObject root = parseTopLevelObjectKeepingFirst(content);
            if (root == null)
            {
                // Not an object or malformed; write normalized content and stop
                Files.writeString(soundsJsonFile, content, StandardCharsets.UTF_8);
                return;
            }

            // Sanitize keys and sound names inside definitions
            root = sanitizeKeysAndSoundNames(root, namespace);

            // Optionally augment with unreferenced .ogg files
            if (namespace != null && soundsDir != null)
            {
                augmentWithUnreferencedSounds(root, namespace, soundsDir);
            }

            // If empty after operations -> delete file
            if (root.entrySet().isEmpty())
            {
                Files.deleteIfExists(soundsJsonFile);
                return;
            }

            // Pretty print with trailing newline
            String out = GSON.toJson(root);
            if (!out.endsWith("\n")) out += "\n";
            Files.writeString(soundsJsonFile, out, StandardCharsets.UTF_8);

        }
        catch (IOException e)
        {
            FlansMod.log.error("Could not process {}", soundsJsonFile, e);
        }
    }

    /** Returns a JsonObject with duplicate top-level keys removed (keeps first). Null if not an object. */
    private static JsonObject parseTopLevelObjectKeepingFirst(String normalizedContent)
    {
        try (StringReader sr = new StringReader(normalizedContent); JsonReader reader = new JsonReader(sr))
        {
            reader.setLenient(true);

            if (reader.peek() != JsonToken.BEGIN_OBJECT)
                return null;
            reader.beginObject();

            LinkedHashMap<String, JsonElement> unique = new LinkedHashMap<>();

            while (reader.peek() == JsonToken.NAME)
            {
                String name = reader.nextName();
                if (unique.containsKey(name))
                    skipAny(reader);
                else
                    unique.put(name, readAny(reader));
            }
            if (reader.peek() == JsonToken.END_OBJECT)
                reader.endObject();

            JsonObject obj = new JsonObject();
            for (Map.Entry<String, JsonElement> e : unique.entrySet())
            {
                obj.add(e.getKey(), e.getValue());
            }
            return obj;
        }
        catch (Exception ignore)
        {
            return null;
        }
    }

    /**
     * Sanitize top-level keys and all sound identifiers inside "sounds" arrays.
     * Keeps insertion order and preserves "first wins" semantics, resolving collisions post-sanitization.
     */
    private static JsonObject sanitizeKeysAndSoundNames(JsonObject root, String namespace)
    {
        // 1) Sanitize all sound identifiers inside definitions
        JsonObject sanitizedDefs = new JsonObject();
        for (Map.Entry<String, JsonElement> e : root.entrySet())
        {
            String key = e.getKey();
            JsonElement val = e.getValue();

            JsonElement newVal = val;
            if (val.isJsonObject()) {
                newVal = sanitizeDefinitionSounds(val.getAsJsonObject(), namespace);
            }
            sanitizedDefs.add(key, newVal);
        }

        // 2) Sanitize top-level keys, ensuring uniqueness (append -1, -2, ...)
        LinkedHashMap<String, JsonElement> finalMap = new LinkedHashMap<>();
        Set<String> seen = new HashSet<>();
        for (Map.Entry<String, JsonElement> e : sanitizedDefs.entrySet())
        {
            String rawKey = e.getKey();
            String sanitizedKey = ResourceUtils.sanitize(rawKey);
            sanitizedKey = ensureUniqueKey(sanitizedKey, seen);
            seen.add(sanitizedKey);
            finalMap.put(sanitizedKey, e.getValue());
        }

        JsonObject out = new JsonObject();
        finalMap.forEach(out::add);
        return out;
    }

    /** Sanitize any string or object-with-name entries inside a single sound definition. */
    private static JsonObject sanitizeDefinitionSounds(JsonObject def, String namespace)
    {
        JsonObject out = new JsonObject();
        // Copy everything; rewrite "sounds" when present
        for (Map.Entry<String, JsonElement> e : def.entrySet())
        {
            String k = e.getKey();
            if (!"sounds".equals(k))
            {
                out.add(k, e.getValue());
                continue;
            }

            JsonElement soundsEl = e.getValue();
            if (!soundsEl.isJsonArray())
            {
                out.add("sounds", soundsEl);
                continue;
            }

            JsonArray arr = new JsonArray();
            for (JsonElement se : soundsEl.getAsJsonArray())
            {
                if (se.isJsonPrimitive() && se.getAsJsonPrimitive().isString())
                {
                    String id = se.getAsString();
                    arr.add(new JsonPrimitive(sanitizeIdentifier(id, namespace)));
                }
                else if (se.isJsonObject())
                {
                    JsonObject sobj = se.getAsJsonObject();
                    JsonObject copy = new JsonObject();
                    for (Map.Entry<String, JsonElement> pe : sobj.entrySet())
                    {
                        if ("name".equals(pe.getKey()) && pe.getValue().isJsonPrimitive() && pe.getValue().getAsJsonPrimitive().isString())
                        {
                            String id = pe.getValue().getAsString();
                            copy.addProperty("name", sanitizeIdentifier(id, namespace));
                        }
                        else
                        {
                            copy.add(pe.getKey(), pe.getValue());
                        }
                    }
                    arr.add(copy);
                } else {
                    // passthrough unknown forms
                    arr.add(se);
                }
            }
            out.add("sounds", arr);
        }
        return out;
    }

    /** Add new entries for .ogg files not referenced anywhere in sounds.json. */
    private static void augmentWithUnreferencedSounds(JsonObject root, String namespace, Path soundsDir)
    {
        // 1) Gather all referenced identifiers from every "sounds" array (strings or objects with "name")
        Set<String> referenced = collectReferencedSoundIds(root, namespace);

        // 2) Gather all .ogg identifiers available on disk (sanitized)
        Set<OggDescriptor> oggs = collectOggs(namespace, soundsDir);

        // 3) Build a fast lookup of existing top-level keys
        Set<String> existingKeys = new HashSet<>(root.keySet());

        // 4) For each ogg NOT referenced, add a new event entry at the end
        List<OggDescriptor> missing = oggs.stream()
                .filter(o -> !referenced.contains(o.identifier))
                .sorted(Comparator.comparing(o -> o.eventKey))
                .toList();

        for (OggDescriptor o : missing)
        {
            String key = ensureUniqueKey(o.eventKey, existingKeys);
            existingKeys.add(key);

            JsonObject def = new JsonObject();
            def.addProperty("category", DEFAULT_CATEGORY);

            JsonArray arr = new JsonArray();
            arr.add(new JsonPrimitive(o.identifier));
            def.add("sounds", arr);

            root.add(key, def);
        }
    }

    /** Ensure the event key doesn’t collide with existing top-level keys by appending -1, -2, ... */
    private static String ensureUniqueKey(String base, Set<String> existing)
    {
        if (!existing.contains(base))
            return base;
        int i = 1;
        while (true)
        {
            String candidate = base + "-" + i;
            if (!existing.contains(candidate)) return candidate;
            i++;
        }
    }

    /**
     * Represents a discovered ogg file and the proposed event key.
     *
     * @param identifier e.g., "flansmod:weapons/rifle_shot"   (sanitized)
     * @param eventKey   e.g., "rifle_shot"                    (sanitized)
     */
    private record OggDescriptor(String identifier, String eventKey) {}

    /** Collect all .ogg files under soundsDir as fully qualified identifiers + default event keys (both sanitized). */
    private static Set<OggDescriptor> collectOggs(String namespace, Path soundsDir)
    {
        if (!Files.isDirectory(soundsDir))
            return Collections.emptySet();

        try (Stream<Path> stream = Files.walk(soundsDir))
        {
            return stream
                    .filter(p -> Files.isRegularFile(p) && p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".ogg"))
                    .map(p -> {
                        Path rel = soundsDir.relativize(p);
                        // Build id path with forward slashes and no ".ogg"
                        String relUnix = rel.toString().replace('\\', '/');
                        String noExt = relUnix.endsWith(".ogg") ? relUnix.substring(0, relUnix.length() - 4) : relUnix;

                        // SANITIZE the path part and the derived event key
                        String sanitizedPath = ResourceUtils.sanitize(noExt);
                        String identifier = sanitizeIdentifier(namespace + ":" + sanitizedPath, namespace);

                        String baseName = stripOgg(p.getFileName().toString());
                        String eventKey = ResourceUtils.sanitize(baseName);
                        return new OggDescriptor(identifier, eventKey);
                    })
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        catch (IOException e)
        {
            FlansMod.log.warn("Could not scan sounds dir {}", soundsDir, e);
            return Collections.emptySet();
        }
    }

    private static String stripOgg(String name)
    {
        return name.toLowerCase(Locale.ROOT).endsWith(".ogg") ? name.substring(0, name.length() - 4) : name;
    }

    /** Build a set of all referenced sound identifiers found in all "sounds" arrays (SANITIZED). */
    private static Set<String> collectReferencedSoundIds(JsonObject root, String namespace)
    {
        Set<String> out = new HashSet<>();
        for (Map.Entry<String, JsonElement> e : root.entrySet())
        {
            JsonElement defEl = e.getValue();
            if (!defEl.isJsonObject())
                continue;
            JsonObject def = defEl.getAsJsonObject();
            JsonElement soundsEl = def.get("sounds");
            if (soundsEl == null || !soundsEl.isJsonArray())
                continue;

            JsonArray arr = soundsEl.getAsJsonArray();
            for (JsonElement se : arr)
            {
                if (se.isJsonPrimitive() && se.getAsJsonPrimitive().isString())
                {
                    out.add(sanitizeIdentifier(se.getAsString(), namespace));
                }
                else if (se.isJsonObject())
                {
                    JsonObject sobj = se.getAsJsonObject();
                    JsonElement nameEl = sobj.get("name");
                    if (nameEl != null && nameEl.isJsonPrimitive() && nameEl.getAsJsonPrimitive().isString())
                    {
                        out.add(sanitizeIdentifier(nameEl.getAsString(), namespace));
                    }
                }
            }
        }
        return out;
    }

    /** Reads the next JSON value (any type) as JsonElement. */
    private static JsonElement readAny(JsonReader reader) throws IOException
    {
        JsonToken t = reader.peek();
        return switch (t)
        {
            case BEGIN_OBJECT -> {
                JsonObject obj = new JsonObject();
                reader.beginObject();
                while (reader.peek() == JsonToken.NAME)
                {
                    String name = reader.nextName();
                    obj.add(name, readAny(reader));
                }
                reader.endObject();
                yield obj;
            }
            case BEGIN_ARRAY -> {
                JsonArray arr = new JsonArray();
                reader.beginArray();
                while (reader.peek() != JsonToken.END_ARRAY)
                {
                    arr.add(readAny(reader));
                }
                reader.endArray();
                yield arr;
            }
            case STRING -> new JsonPrimitive(reader.nextString());
            case NUMBER -> numberPrimitive(reader.nextString());
            case BOOLEAN -> new JsonPrimitive(reader.nextBoolean());
            case NULL -> {
                reader.nextNull();
                yield JsonNull.INSTANCE;
            }
            default -> {
                reader.skipValue();
                yield JsonNull.INSTANCE;
            }
        };
    }

    /** Skips the next JSON value (object/array/primitive/null). */
    private static void skipAny(JsonReader reader) throws IOException {
        JsonToken t = reader.peek();
        switch (t)
        {
            case BEGIN_OBJECT:
                reader.beginObject();
                while (reader.peek() == JsonToken.NAME)
                {
                    reader.nextName();
                    skipAny(reader);
                }
                reader.endObject();
                break;
            case BEGIN_ARRAY:
                reader.beginArray();
                while (reader.peek() != JsonToken.END_ARRAY)
                {
                    skipAny(reader);
                }
                reader.endArray();
                break;
            default:
                reader.skipValue();
        }
    }

    /** Preserve numeric exactness where possible. */
    private static JsonPrimitive numberPrimitive(String raw) {
        try
        {
            long l = Long.parseLong(raw);
            return new JsonPrimitive(l);
        }
        catch (NumberFormatException ignored) {}

        try
        {
            return new JsonPrimitive(new BigDecimal(raw));
        }
        catch (NumberFormatException nfe)
        {
            return new JsonPrimitive(raw);
        }
    }

    /* ----------------- sanitize helpers ----------------- */

    /**
     * Sanitize a sound identifier to minecraft rules:
     * - Lowercase, normalize slashes
     * - Accept both "path" and "namespace:path" (fill missing namespace with provided one)
     * - Sanitize namespace and path with ResourceUtils.sanitize(...)
     */
    private static String sanitizeIdentifier(String id, String defaultNamespace)
    {
        if (id == null || id.isBlank())
            return id;

        String s = id.replace('\\', '/').toLowerCase(Locale.ROOT).trim();
        String ns;
        String path;

        int colon = s.indexOf(':');
        if (colon >= 0)
        {
            ns = s.substring(0, colon);
            path = s.substring(colon + 1);
            if (ns.isBlank() && defaultNamespace != null) ns = defaultNamespace;
        }
        else
        {
            ns = (defaultNamespace != null) ? defaultNamespace : "";
            path = s;
        }

        ns = ResourceUtils.sanitize(ns);
        path = ResourceUtils.sanitize(path);

        if (ns.isEmpty())
            return path; // fallback; usually we always have ns
        return ns + ":" + path;
    }
}
