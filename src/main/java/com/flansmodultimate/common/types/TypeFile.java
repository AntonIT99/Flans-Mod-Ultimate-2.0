package com.flansmodultimate.common.types;

import com.flansmodultimate.IContentProvider;
import com.flansmodultimate.config.Category;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class TypeFile
{
    private final String name;
    private final EnumType type;
    private final IContentProvider contentPack;
    private final List<String> lines = new ArrayList<>();

    public TypeFile(String name, EnumType type, IContentProvider contentPack, List<String> lines)
    {
        this.name = name;
        this.type = type;
        this.contentPack = contentPack;
        this.lines.addAll(lines);
    }

    public void addCategoryLines(Category category)
    {
        lines.addAll(category.toLines());
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
