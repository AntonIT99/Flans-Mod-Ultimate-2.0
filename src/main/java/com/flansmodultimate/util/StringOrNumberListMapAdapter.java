package com.flansmodultimate.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class StringOrNumberListMapAdapter implements JsonDeserializer<Map<String, List<String>>>, JsonSerializer<Map<String, List<String>>>
{

    private static final Type TARGET_TYPE = new TypeToken<Map<String, List<String>>>() {}.getType();

    public static Type targetType()
    {
        return TARGET_TYPE;
    }

    @Override
    public Map<String, List<String>> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx)
        throws JsonParseException {

        Map<String, List<String>> out = new LinkedHashMap<>();
        if (json == null || json.isJsonNull())
            return out;

        if (!json.isJsonObject())
        {
            throw new JsonParseException("Expected properties to be a JSON object.");
        }

        JsonObject obj = json.getAsJsonObject();
        for (Map.Entry<String, JsonElement> e : obj.entrySet())
        {
            out.put(e.getKey(), toStringListStrict(e.getValue(), e.getKey()));
        }
        return out;
    }

    private static List<String> toStringListStrict(JsonElement value, String key)
    {
        if (value == null || value.isJsonNull())
        {
            return Collections.emptyList();
        }

        if (value.isJsonPrimitive())
        {
            return Collections.singletonList(primitiveToString(value.getAsJsonPrimitive(), key));
        }

        if (value.isJsonArray())
        {
            JsonArray arr = value.getAsJsonArray();
            List<String> list = new ArrayList<>(arr.size());
            for (JsonElement el : arr)
            {
                if (el == null || el.isJsonNull())
                {
                    continue;
                }
                if (!el.isJsonPrimitive())
                {
                    throw new JsonParseException("properties['" + key + "'] array must contain only strings/numbers.");
                }
                list.add(primitiveToString(el.getAsJsonPrimitive(), key));
            }
            return list;
        }

        // objects or other structures not supported
        throw new JsonParseException("properties['" + key + "'] must be string/number or array of string/number.");
    }

    private static String primitiveToString(JsonPrimitive p, String key)
    {
        if (p.isString())
            return p.getAsString();
        if (p.isNumber())
            return p.getAsNumber().toString();
        if (p.isBoolean())
        {
            throw new JsonParseException("properties['" + key + "'] boolean not supported.");
        }
        throw new JsonParseException("properties['" + key + "'] unsupported primitive.");
    }

    @Override
    public JsonElement serialize(Map<String, List<String>> src, Type typeOfSrc, JsonSerializationContext ctx)
    {
        JsonObject obj = new JsonObject();
        if (src == null)
            return obj;

        for (Map.Entry<String, List<String>> e : src.entrySet())
        {
            JsonArray arr = new JsonArray();
            List<String> values = e.getValue();
            if (values != null)
            {
                for (String s : values)
                {
                    if (s != null) arr.add(s);
                }
            }
            obj.add(e.getKey(), arr);
        }
        return obj;
    }
}

