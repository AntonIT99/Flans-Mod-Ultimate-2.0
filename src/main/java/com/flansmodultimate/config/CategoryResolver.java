package com.flansmodultimate.config;

import com.flansmodultimate.common.types.EnumType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/** Resolves definitions before any item is indexed; input categories are never mutated. */
final class CategoryResolver
{
    private record Key(boolean defaults, String name) {}

    private final EnumType type;
    private final Map<String, Category> defaults = new LinkedHashMap<>();
    private final Map<String, Category> active = new LinkedHashMap<>();
    private final Map<Key, Category> resolved = new HashMap<>();
    private final Set<Key> visiting = new HashSet<>();
    private final boolean useDefaults;

    CategoryResolver(EnumType type, List<Category> defaults, List<Category> users, boolean useDefaults)
    {
        this.type = type;
        this.useDefaults = useDefaults;
        for (Category category : defaults)
            this.defaults.put(category.getName(), category);
        if (useDefaults)
            active.putAll(this.defaults);
        for (Category category : users)
        {
            // User declarations retain user-file application order, after untouched defaults.
            active.remove(category.getName());
            active.put(category.getName(), category);
        }
    }

    List<Category> resolve(Consumer<String> errors)
    {
        List<Category> result = new ArrayList<>();
        for (String name : active.keySet())
        {
            try
            {
                result.add(resolve(new Key(false, name)));
            }
            catch (IllegalArgumentException e)
            {
                errors.accept("Skipping " + type.getIdentifier() + " category '" + name + "': " + e.getMessage());
            }
        }
        return result;
    }

    private Category resolve(Key key)
    {
        if (resolved.containsKey(key))
            return resolved.get(key);
        Category declaration = (key.defaults ? defaults : active).get(key.name);
        if (declaration == null)
            throw new IllegalArgumentException("Unknown same-type parent: " + key.name);
        if (declaration.getType() != type)
            throw new IllegalArgumentException("Parent/category has a different InfoType subtype: " + key.name);
        if (!visiting.add(key))
            throw new IllegalArgumentException("Inheritance cycle at " + key.name);
        try
        {
            Category result = new Category(type, key.name);
            if (!key.defaults && useDefaults && defaults.containsKey(key.name))
                merge(result, resolve(new Key(true, key.name)));
            String parent = declaration.getInherits();
            if (parent != null && !parent.isBlank())
            {
                boolean explicitDefault = parent.startsWith("default:");
                merge(result, resolve(new Key(key.defaults || explicitDefault,
                    explicitDefault ? parent.substring("default:".length()) : parent)));
            }
            merge(result, declaration);
            resolved.put(key, result);
            return result;
        }
        finally
        {
            visiting.remove(key);
        }
    }

    private static void merge(Category target, Category source)
    {
        mergeLists(target.getProperties(), source.getProperties());
        mergeMap(target.getPropertyModes(), source.getPropertyModes());
        mergeLists(target.getExceptions(), source.getExceptions());
        Set<String> seen = new HashSet<>();
        target.getItems().forEach(item -> seen.add(item.toLowerCase(Locale.ROOT)));
        if (source.getItems() != null)
            for (String item : source.getItems())
                if (item != null && seen.add(item.toLowerCase(Locale.ROOT)))
                    target.getItems().add(item);
    }

    private static void mergeLists(Map<String, List<String>> target, Map<String, List<String>> source)
    {
        if (source != null)
            source.forEach((key, value) -> {
                target.keySet().removeIf(existing -> existing.equalsIgnoreCase(key));
                target.put(key, value == null ? new ArrayList<>() : new ArrayList<>(value));
            });
    }

    private static <T> void mergeMap(Map<String, T> target, Map<String, T> source)
    {
        if (source != null)
            source.forEach((key, value) -> {
                target.keySet().removeIf(existing -> existing.equalsIgnoreCase(key));
                target.put(key, value);
            });
    }
}
