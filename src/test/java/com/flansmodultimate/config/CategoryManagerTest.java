package com.flansmodultimate.config;

import com.flansmodultimate.ContentPack;
import com.flansmodultimate.common.types.EnumType;
import com.flansmodultimate.common.types.TypeFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CategoryManagerTest
{
    @TempDir
    Path config;

    @Test
    void indexesResolvedCategoriesBySubtypeAndReplacesStateOnReload() throws IOException
    {
        Path defaults = Files.createDirectory(config.resolve("default"));
        Files.writeString(config.resolve("bullet_categories.json"), """
            {"Child":{"inherits":"Base","items":["shared"],"properties":{"AddRound":"AP"}},
             "Base":{"properties":{"Mass":12}}}
            """);
        Files.writeString(config.resolve("gun_categories.json"), """
            {"Gun":{"items":["shared"],"properties":{"Recoil":2}}}
            """);
        for (int reload = 0; reload < 2; reload++)
        {
            CategoryManager.loadCategories(config, defaults, false);
            TypeFile bullet = file(EnumType.BULLET);
            CategoryManager.applyCategoriesToFile(bullet);
            assertEquals(List.of("AP"), bullet.getConfigLines("AddRound"));
            assertEquals(List.of("12"), bullet.getConfigLines("Mass"));
            assertNull(bullet.getConfigLines("Recoil"));
            TypeFile gun = file(EnumType.GUN);
            CategoryManager.applyCategoriesToFile(gun);
            assertEquals(List.of("2"), gun.getConfigLines("Recoil"));
            assertNull(gun.getConfigLines("Mass"));
        }
        Files.writeString(config.resolve("bullet_categories.json"), "{}");
        Files.writeString(config.resolve("gun_categories.json"), "{}");
        CategoryManager.loadCategories(config, defaults, false);
        TypeFile bullet = file(EnumType.BULLET);
        CategoryManager.applyCategoriesToFile(bullet);
        assertNull(bullet.getConfigLines("Mass"));
    }

    @Test
    void builtInPushCategoryOverridesStationaryPak40Controls() throws IOException
    {
        Path defaults = Files.createDirectory(config.resolve("default"));
        CategoryManager.loadCategories(config, defaults, true);
        for (String shortName : List.of("pak40", "44_pak40"))
        {
            TypeFile gun = new TypeFile("Pak40", EnumType.VEHICLE,
                new ContentPack("test", config), List.of(
                    "ShortName " + shortName,
                    "MaxThrottle 0",
                    "MaxNegativeThrottle 0",
                    "TurnLeftSpeed 0",
                    "TurnRightSpeed 0",
                    "Tank true"));
            CategoryManager.applyCategoriesToFile(gun);
            assertEquals(List.of("4"), gun.getConfigLines("PushSpeedKmh"));
            assertEquals(List.of("0", "1"), gun.getConfigLines("MaxNegativeThrottle"));
            assertEquals(List.of("0", "1"), gun.getConfigLines("TurnLeftSpeed"));
            assertEquals(List.of("0", "1"), gun.getConfigLines("TurnRightSpeed"));
            assertEquals(List.of("true", "false"), gun.getConfigLines("Tank"));
        }
    }

    private TypeFile file(EnumType type)
    {
        return new TypeFile("shared", type, new ContentPack("test", config), List.of("ShortName shared"));
    }
}
