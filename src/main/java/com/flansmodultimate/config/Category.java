package com.flansmodultimate.config;

import com.flansmodultimate.common.types.EnumType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    public Category(EnumType type, String name)
    {
        this.type = type;
        this.name = name;
    }
}
