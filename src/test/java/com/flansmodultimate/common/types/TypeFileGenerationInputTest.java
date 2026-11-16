package com.flansmodultimate.common.types;

import com.flansmodultimate.config.Category;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TypeFileGenerationInputTest
{
    @Test
    void categoryStatChangesLeaveGenerationInputUnchanged()
    {
        TypeFile plain = file();
        TypeFile balanced = file();
        balanced.addCategoryConfigMap(category("Recoil", "4"), "m4a1");

        assertEquals(plain.getGenerationInput(), balanced.getGenerationInput());
        assertNotEquals(plain.getDefinitionInput(), balanced.getDefinitionInput());
    }

    @Test
    void categoryValuesReadByGeneratorsChangeGenerationInput()
    {
        for (String key : List.of("Name", "Icon", "Texture", "Model", "Paintjob", "RecipeOutput", "SmeltableFrom"))
        {
            TypeFile plain = file();
            TypeFile categorized = file();
            categorized.addCategoryConfigMap(category(key, "changed"), "m4a1");
            assertNotEquals(plain.getGenerationInput(), categorized.getGenerationInput(), key);
        }
    }

    @Test
    void packTextChangesGenerationInput()
    {
        TypeFile edited = new TypeFile("m4a1", EnumType.GUN, null, List.of("ShortName m4a1", "Name M4A1", "Recoil 5"));
        assertNotEquals(file().getGenerationInput(), edited.getGenerationInput());
    }

    private static TypeFile file()
    {
        return new TypeFile("m4a1", EnumType.GUN, null, List.of("ShortName m4a1", "Name M4A1", "Recoil 3"));
    }

    private static Category category(String key, String value)
    {
        Category category = new Category(EnumType.GUN, "M4 carbine");
        category.getProperties().put(key, List.of(value));
        category.getItems().add("m4a1");
        return category;
    }
}
