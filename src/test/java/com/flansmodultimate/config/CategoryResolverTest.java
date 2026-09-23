package com.flansmodultimate.config;

import com.flansmodultimate.ContentPack;
import com.flansmodultimate.common.types.EnumType;
import com.flansmodultimate.common.types.TypeFile;
import com.flansmodultimate.util.StringOrNumberListMapAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CategoryResolverTest
{
    private static final Gson GSON = new GsonBuilder()
        .registerTypeAdapter(StringOrNumberListMapAdapter.targetType(), new StringOrNumberListMapAdapter()).create();

    private static Category category(String name, String json)
    {
        Category result = GSON.fromJson(json, Category.class);
        result.setName(name);
        result.setType(EnumType.BULLET);
        return result;
    }

    private static List<Category> resolve(List<Category> defaults, List<Category> users, boolean enabled)
    {
        return new CategoryResolver(EnumType.BULLET, defaults, users, enabled).resolve(message -> fail(message));
    }

    @Test
    void redeclarationRetainsDefaultsAndOverridesWholePropertyCaseInsensitively()
    {
        Category base = category("Base", """
            {"properties":{"Mass":12,"AddRound":["Ball","Tracer"]},
             "propertyModes":{"Mass":"replace"},"items":["old"],"exceptions":{"Mass":["old"]}}
            """);
        Category user = category("Base", """
            {"properties":{"addround":["AP"]},"items":["OLD","new"]}
            """);
        List<Category> result = resolve(List.of(base), List.of(user), true);
        assertEquals(1, result.size());
        Category merged = result.get(0);
        assertEquals(Map.of("Mass", List.of("12"), "addround", List.of("AP")), merged.getProperties());
        assertEquals(List.of("old", "new"), merged.getItems());
        assertEquals(CategoryPropertyMode.REPLACE, merged.getPropertyMode("mass"));
        assertFalse(merged.getPropertiesFor("old").containsKey("Mass"));
        assertEquals(List.of("Ball", "Tracer"), base.getProperties().get("AddRound"));
        TypeFile file = new TypeFile("new", EnumType.BULLET,
            new ContentPack("test", Path.of("build", "test-packs", "inheritance")), List.of());
        file.addCategoryConfigMap(merged, "new");
        assertEquals(List.of("AP"), file.getConfigLines("AddRound"));
    }

    @Test
    void disabledDefaultsRequireExplicitReferenceIncludingSameName()
    {
        Category base = category("Base", """
            {"properties":{"Mass":12},"items":["old"]}
            """);
        Category plain = category("Base", "{}");
        assertTrue(resolve(List.of(base), List.of(plain), false).get(0).getProperties().isEmpty());
        assertTrue(resolve(List.of(base), List.of(), false).isEmpty());
        plain.setInherits("default:Base");
        Category inherited = resolve(List.of(base), List.of(plain), false).get(0);
        assertEquals(List.of("12"), inherited.getProperties().get("Mass"));
        assertEquals(List.of("old"), inherited.getItems());
    }

    @Test
    void forwardReferencesAndChainsUseResolvedUserOverrides()
    {
        Category base = category("Base", """
            {"properties":{"Mass":12},"items":["old"]}
            """);
        Category user = category("Base", """
            {"properties":{"Mass":20}}
            """);
        Category child = category("Child", """
            {"inherits":"Base","items":["child"]}
            """);
        Category grandchild = category("Grandchild", """
            {"inherits":"Child","properties":{"FallSpeed":2}}
            """);
        List<Category> result = resolve(List.of(base), List.of(grandchild, child, user), true);
        assertEquals(List.of("Grandchild", "Child", "Base"), result.stream().map(Category::getName).toList());
        assertEquals(List.of("20"), result.get(0).getProperties().get("Mass"));
        assertEquals(List.of("old", "child"), result.get(0).getItems());
        child.setInherits("default:Base");
        assertEquals(List.of("12"), resolve(List.of(base), List.of(child, user), true).get(0).getProperties().get("Mass"));
    }

    @Test
    void modesAndExceptionsOverrideByKeyAndEmptyExceptionClearsInheritedExclusion()
    {
        Category base = category("Base", """
            {"properties":{"Mass":12},"propertyModes":{"Mass":"replace"},
             "exceptions":{"Mass":["old"]},"items":["old"]}
            """);
        Category child = category("Child", """
            {"inherits":"default:Base","propertyModes":{"mass":"ifAbsent"},"exceptions":{"mass":[]}}
            """);
        Category result = resolve(List.of(base), List.of(child), false).get(0);
        assertEquals(CategoryPropertyMode.IF_ABSENT, result.getPropertyMode("Mass"));
        assertTrue(result.getPropertiesFor("old").containsKey("Mass"));
    }

    @Test
    void invalidParentsAndCyclesSkipDependentsButKeepUnrelatedCategories()
    {
        Category a = category("A", "{\"inherits\":\"B\"}");
        Category b = category("B", "{\"inherits\":\"A\"}");
        Category dependent = category("Dependent", "{\"inherits\":\"A\"}");
        Category missing = category("Missing", "{\"inherits\":\"Unknown\"}");
        Category self = category("Self", "{\"inherits\":\"Self\"}");
        Category valid = category("Valid", "{}");
        List<String> errors = new ArrayList<>();
        List<Category> result = new CategoryResolver(EnumType.BULLET, List.of(),
            List.of(a, b, dependent, missing, self, valid), true).resolve(errors::add);
        assertEquals(List.of("Valid"), result.stream().map(Category::getName).toList());
        assertEquals(5, errors.size());
        assertTrue(errors.stream().anyMatch(error -> error.contains("Unknown")));
        assertTrue(errors.stream().anyMatch(error -> error.contains("cycle")));
    }

    @Test
    void rejectsDifferentSubtypeAndDoesNotImplicitlyEnableDefaults()
    {
        Category gun = category("Gun", "{}");
        gun.setType(EnumType.GUN);
        Category child = category("Child", "{\"inherits\":\"default:Gun\"}");
        List<String> errors = new ArrayList<>();
        assertTrue(new CategoryResolver(EnumType.BULLET, List.of(gun), List.of(child), false).resolve(errors::add).isEmpty());
        assertTrue(errors.get(0).contains("different InfoType subtype"));
        child.setInherits("Gun");
        errors.clear();
        assertTrue(new CategoryResolver(EnumType.BULLET, List.of(gun), List.of(child), false).resolve(errors::add).isEmpty());
        assertTrue(errors.get(0).contains("Unknown same-type parent"));
    }
}
