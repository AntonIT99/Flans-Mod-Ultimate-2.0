package com.flansmodultimate.config;

import com.flansmodultimate.ContentPack;
import com.flansmodultimate.IContentProvider;
import com.flansmodultimate.common.types.EnumType;
import com.flansmodultimate.common.types.TypeFile;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CategoryPropertyModeTest
{
    private static final IContentProvider PACK = new ContentPack("test",
        Path.of("build", "test-packs", "category-property-modes"));

    @Test
    void omittedModePreservesLegacyAppendBehavior()
    {
        TypeFile file = file("AddRound Ball 1 147 0 850 30");

        file.addCategoryConfigMap(category("AddRound", "Tracer 1 145 0 850 25", null), "belt");

        assertEquals(List.of("Ball 1 147 0 850 30", "Tracer 1 145 0 850 25"),
            file.getConfigLines("AddRound"));
    }

    @Test
    void replaceDiscardsDefinitionValues()
    {
        TypeFile file = file("AddRound Ball 1 147 0 850 30");

        file.addCategoryConfigMap(category("AddRound", "AP 1 162 0 800 45", "replace"), "belt");

        assertEquals(List.of("AP 1 162 0 800 45"), file.getConfigLines("AddRound"));
    }

    @Test
    void ifAbsentKeepsDefinitionValues()
    {
        TypeFile file = file("AddRound Ball 1 147 0 850 30");

        file.addCategoryConfigMap(category("AddRound", "AP 1 162 0 800 45", "ifAbsent"), "belt");

        assertEquals(List.of("Ball 1 147 0 850 30"), file.getConfigLines("AddRound"));
    }

    @Test
    void ifAbsentSuppliesMissingValues()
    {
        TypeFile file = file("RoundsPerItem 2");

        file.addCategoryConfigMap(category("AddRound", "AP 1 162 0 800 45", "IFABSENT"), "belt");

        assertEquals(List.of("AP 1 162 0 800 45"), file.getConfigLines("AddRound"));
    }

    @Test
    void propertyModeKeysAreCaseInsensitive()
    {
        TypeFile file = file("AddRound Ball 1 147 0 850 30");
        Category category = category("AddRound", "AP 1 162 0 800 45", null);
        category.setPropertyModes(Map.of("addround", "replace"));

        file.addCategoryConfigMap(category, "belt");

        assertEquals(List.of("AP 1 162 0 800 45"), file.getConfigLines("AddRound"));
    }

    private static TypeFile file(String... lines)
    {
        return new TypeFile("syntheticBullet", EnumType.BULLET, PACK, List.of(lines));
    }

    private static Category category(String property, String value, String mode)
    {
        Category category = new Category(EnumType.BULLET, "Synthetic belt");
        category.setProperties(Map.of(property, List.of(value)));
        if (mode != null)
            category.setPropertyModes(Map.of(property, mode));
        return category;
    }
}
