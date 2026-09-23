package com.flansmodultimate.common.types;

import com.flansmodultimate.IContentProvider;
import com.flansmodultimate.config.Category;
import com.flansmodultimate.config.CategoryPropertyMode;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class TypeFile
{
    private static final Pattern CONFIG_LINE_SEPARATOR = Pattern.compile("\\s+");

    @Getter
    private final String name;
    @Getter
    private final EnumType type;
    @Getter
    private final IContentProvider contentPack;
    @Getter
    private final List<String> lines = new ArrayList<>();
    private final Map<String, List<String>> configMap = new HashMap<>();

    public TypeFile(String name, EnumType type, IContentProvider contentPack, List<String> lines)
    {
        this.name = name;
        this.type = type;
        this.contentPack = contentPack;
        this.lines.addAll(lines);

        for (String line : this.lines)
        {
            if (line.isBlank() || line.startsWith("//"))
                continue;

            String[] split = CONFIG_LINE_SEPARATOR.split(line.trim(), 2);
            String configKey = split[0].toLowerCase(Locale.ROOT);
            configMap.putIfAbsent(configKey, new ArrayList<>());
            configMap.get(configKey).add((split.length > 1) ? split[1] : null);
        }
    }

    public boolean hasConfigLine(String key)
    {
        return configMap.containsKey(key.toLowerCase(Locale.ROOT));
    }

    public boolean hasAnyConfigLine(String... keys)
    {
        for (String key : keys)
        {
            if (hasConfigLine(key))
                return true;
        }
        return false;
    }

    public List<String> getConfigLines(String key)
    {
        return configMap.get(key.toLowerCase(Locale.ROOT));
    }

    public void addCategoryConfigMap(Category category, String shortname)
    {
        category.getPropertiesFor(shortname).forEach((field, value) -> {
            String key = field.toLowerCase(Locale.ROOT);
            List<String> categoryValues = value == null ? List.of() : value;
            CategoryPropertyMode mode = category.getPropertyMode(field);

            switch (mode)
            {
                case REPLACE -> configMap.put(key, new ArrayList<>(categoryValues));
                case IF_ABSENT -> {
                    if (!configMap.containsKey(key) || configMap.get(key).isEmpty())
                        configMap.put(key, new ArrayList<>(categoryValues));
                }
                case APPEND -> configMap.computeIfAbsent(key, ignored -> new ArrayList<>()).addAll(categoryValues);
            }
        });
    }

    public String toString()
    {
        return type.getConfigFolderName() + "/" + getName() + " [" + contentPack.getName() + "]";
    }

    public static String getContentPackName(String toStringValue)
    {
        return toStringValue.split("\\[")[1].split("\\]")[0];
    }
}
