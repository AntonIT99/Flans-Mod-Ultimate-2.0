package com.flansmodultimate.common.types;

import com.flansmodultimate.IContentProvider;
import com.flansmodultimate.config.Category;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TypeFile
{
    @Getter
    private final String name;
    @Getter
    private final EnumType type;
    @Getter
    private final IContentProvider contentPack;
    private final Map<String, List<String>> configMap = new HashMap<>();

    public TypeFile(String name, EnumType type, IContentProvider contentPack, List<String> lines)
    {
        this.name = name;
        this.type = type;
        this.contentPack = contentPack;
        for (String line : lines)
        {
            if (line.isBlank() || line.startsWith("//"))
                continue;

            String[] split = line.trim().split("\\s+", 2);
            configMap.putIfAbsent(split[0].toLowerCase(Locale.ROOT), new ArrayList<>());
            configMap.get(split[0].toLowerCase(Locale.ROOT)).add((split.length > 1) ? split[1] : null);
        }
    }

    public boolean hasConfigLine(String key)
    {
        return configMap.containsKey(key.toLowerCase(Locale.ROOT));
    }

    public List<String> getConfigLines(String key)
    {
        return configMap.get(key.toLowerCase(Locale.ROOT));
    }

    public void addCategoryConfigMap(Category category)
    {
        category.getProperties().forEach((field, value) -> configMap.computeIfAbsent(field.toLowerCase(Locale.ROOT), key -> new ArrayList<>()).addAll(value));
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
