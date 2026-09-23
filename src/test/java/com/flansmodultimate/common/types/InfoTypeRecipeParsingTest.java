package com.flansmodultimate.common.types;

import com.flansmodultimate.ContentPack;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InfoTypeRecipeParsingTest
{
    @Test
    void parsesRecipePrefixedRowsAsOneRecipe()
    {
        InfoType type = load(List.of(
            "ShortName test_recipe",
            "Recipe M minecraft:iron_ingot",
            "Recipe   M",
            "Recipe MMM",
            "Recipe   M",
            "RecipeOutput 1"
        ));

        assertEquals(List.of("M", "minecraft:iron_ingot"), type.getRecipeTokens());
        assertEquals(' ', type.getRecipeGrid()[0][0]);
        assertEquals('M', type.getRecipeGrid()[0][2]);
        assertEquals('M', type.getRecipeGrid()[1][0]);
        assertEquals('M', type.getRecipeGrid()[2][2]);
    }

    @Test
    void keepsBareRecipeRowsCompatible()
    {
        InfoType type = load(List.of(
            "ShortName test_recipe",
            "Recipe M minecraft:iron_ingot",
            "  M",
            "MMM",
            "  M"
        ));

        assertEquals(List.of("M", "minecraft:iron_ingot"), type.getRecipeTokens());
        assertEquals(' ', type.getRecipeGrid()[0][0]);
        assertEquals('M', type.getRecipeGrid()[0][2]);
    }

    private static InfoType load(List<String> lines)
    {
        InfoType type = new InfoType() {};
        TypeFile file = new TypeFile("test_recipe", EnumType.GUN,
            new ContentPack("test", Path.of("build", "test-packs", "recipe-parsing")), lines);
        type.load(file);
        return type;
    }
}
