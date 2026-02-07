package com.flansmodultimate.util;

import java.util.Map;
import java.util.stream.Collectors;

public class DynamicReference
{
    private String value;

    public DynamicReference(String value)
    {
        this.value = value;
    }

    public String get()
    {
        return value;
    }

    public void update(String newValue)
    {
        value = newValue;
    }

    public static void storeOrUpdate(String key, String value, Map<String, DynamicReference> references)
    {
        if (references.containsKey(key) && !references.get(key).get().equals(value))
        {
            references.get(key).update(value);
        }
        else
        {
            references.put(key, new DynamicReference(value));
        }
    }

    public static Map<String, String> getAliasMapping(Map<String, DynamicReference> references)
    {
        return references.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(entry.getValue().get()))
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().get()));
    }
}
