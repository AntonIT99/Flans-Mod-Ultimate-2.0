package com.flansmodultimate.config;

import com.flansmodultimate.common.types.EnumType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class Category
{
    private EnumType type;
    private String name;
    private Map<String, List<String>> properties = new HashMap<>();
    private List<String> items = new ArrayList<>();
    /** Property name -> items of this category that must NOT receive that property */
    private Map<String, List<String>> exceptions = new HashMap<>();

    public Category(EnumType type, String name)
    {
        this.type = type;
        this.name = name;
    }

    /**
     * @return the properties of this category that apply to the given item, with excepted properties removed
     */
    public Map<String, List<String>> getPropertiesFor(String item)
    {
        if (exceptions == null || exceptions.isEmpty())
            return properties;

        String itemKey = (item == null) ? "" : item.toLowerCase(Locale.ROOT);
        Map<String, List<String>> result = new LinkedHashMap<>();

        for (Map.Entry<String, List<String>> entry : properties.entrySet())
        {
            if (!isExcepted(entry.getKey(), itemKey))
                result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    private boolean isExcepted(String property, String itemKey)
    {
        for (Map.Entry<String, List<String>> entry : exceptions.entrySet())
        {
            if (!entry.getKey().equalsIgnoreCase(property) || entry.getValue() == null)
                continue;

            for (String excepted : entry.getValue())
            {
                if (excepted != null && excepted.toLowerCase(Locale.ROOT).equals(itemKey))
                    return true;
            }
        }
        return false;
    }
}
