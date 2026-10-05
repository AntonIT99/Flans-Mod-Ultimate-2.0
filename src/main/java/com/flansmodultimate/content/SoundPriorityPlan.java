package com.flansmodultimate.content;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** One selection shared by the resource overlay and common/server sound timers. */
record SoundPriorityPlan(JsonObject events, Map<String, Integer> fileOwners, Map<String, Integer> lengths)
{
    record Assets(JsonObject events, Map<String, Integer> files) {}

    static SoundPriorityPlan create(List<Assets> highestFirst)
    {
        JsonObject events = new JsonObject();
        Map<String, Integer> owners = new LinkedHashMap<>();
        Map<String, Integer> files = new HashMap<>();
        for (int i = 0; i < highestFirst.size(); i++)
        {
            Assets source = highestFirst.get(i);
            source.events().entrySet().forEach(event -> {
                if (!events.has(event.getKey()) && event.getValue().isJsonObject())
                {
                    JsonObject selected = event.getValue().getAsJsonObject().deepCopy();
                    // Override Minecraft's usual cross-pack merging with the selected definition.
                    selected.addProperty("replace", true);
                    events.add(event.getKey(), selected);
                }
            });
            for (Map.Entry<String, Integer> file : source.files().entrySet())
                if (owners.putIfAbsent(file.getKey(), i) == null)
                    files.put(file.getKey(), file.getValue()); // Unknown length still owns and masks this path.
        }
        Map<String, Integer> lengths = new HashMap<>();
        for (String event : events.keySet())
            resolve(event, events, files, lengths, new HashSet<>());
        return new SoundPriorityPlan(events, Map.copyOf(owners), Map.copyOf(lengths));
    }

    private static int resolve(String event, JsonObject events, Map<String, Integer> files, Map<String, Integer> lengths, Set<String> visiting)
    {
        if (lengths.containsKey(event))
            return lengths.get(event);
        if (!events.has(event) || !visiting.add(event))
            return 0;
        JsonElement sounds = events.getAsJsonObject(event).get("sounds");
        int longest = 0;
        boolean unknown = false;
        if (sounds instanceof JsonArray variants)
            for (JsonElement variant : variants)
            {
                try
                {
                    String name = null;
                    boolean nested = false;
                    double pitch = 1;
                    if (variant.isJsonPrimitive() && variant.getAsJsonPrimitive().isString())
                        name = variant.getAsString();
                    else if (variant.isJsonObject())
                    {
                        JsonObject entry = variant.getAsJsonObject();
                        if (entry.has("name") && entry.get("name").isJsonPrimitive())
                            name = entry.get("name").getAsString();
                        nested = entry.has("type") && "event".equals(entry.get("type").getAsString());
                        if (entry.has("pitch"))
                            pitch = entry.get("pitch").getAsDouble();
                    }
                    // Unqualified sound references belong to Minecraft, just as in its sound loader.
                    int ticks;
                    if (name != null && name.startsWith("flansmod:"))
                    {
                        if (nested)
                            ticks = resolve(name.substring(9), events, files, lengths, visiting);
                        else
                            ticks = files.getOrDefault(name.substring(9), 0);
                    }
                    else
                    {
                        ticks = 0;
                    }
                    unknown |= ticks <= 0 || !Double.isFinite(pitch) || pitch <= 0;
                    if (ticks > 0 && Double.isFinite(pitch) && pitch > 0)
                        longest = Math.max(longest, (int)Math.max(1, Math.min(Integer.MAX_VALUE, ticks / pitch)));
                }
                catch (RuntimeException exception)
                {
                    unknown = true;
                }
            }
        visiting.remove(event);
        int result = unknown ? 0 : longest;
        lengths.put(event, result);
        return result;
    }
}
