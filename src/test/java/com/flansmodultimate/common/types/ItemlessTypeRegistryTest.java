package com.flansmodultimate.common.types;

import com.flansmodultimate.ContentPack;
import com.flansmodultimate.IContentProvider;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ItemlessTypeRegistryTest
{
    private static final IContentProvider PACK_A = new ContentPack("Pack A", Path.of("build", "test-packs", "a"));
    private static final IContentProvider PACK_B = new ContentPack("Pack B", Path.of("build", "test-packs", "b"));

    private static PlayerClass playerClass(String shortName, IContentProvider pack)
    {
        PlayerClass type = new PlayerClass();
        type.read(new TypeFile(shortName, EnumType.PLAYER_CLASS, pack, List.of("ShortName " + shortName)));
        type.contentPack = pack;
        type.fileName = shortName + ".txt";
        return type;
    }

    @Test
    void twoPacksSharingAShortnameBothStayReachable()
    {
        ItemlessTypeRegistry<PlayerClass> registry = new ItemlessTypeRegistry<>("player class");
        PlayerClass first = playerClass("assault", PACK_A);
        PlayerClass second = playerClass("assault", PACK_B);

        assertEquals("assault", registry.register(first));
        assertEquals("assault_2", registry.register(second));

        assertNotSame(first, second);
        assertEquals(List.of(first, second), List.copyOf(registry.values()));
        assertSame(first, registry.get("assault"));
        assertSame(second, registry.get("assault_2"));
    }

    @Test
    void aThirdCollisionKeepsCounting()
    {
        ItemlessTypeRegistry<PlayerClass> registry = new ItemlessTypeRegistry<>("player class");
        registry.register(playerClass("medic", PACK_A));
        registry.register(playerClass("medic", PACK_B));
        PlayerClass third = playerClass("medic", new ContentPack("Pack C", Path.of("build", "test-packs", "c")));

        assertEquals("medic_3", registry.register(third));
        assertSame(third, registry.get("medic_3"));
    }

    @Test
    void aReferenceInsideAPackResolvesToThatPacksOwnDefinition()
    {
        ItemlessTypeRegistry<PlayerClass> registry = new ItemlessTypeRegistry<>("player class");
        PlayerClass first = playerClass("sniper", PACK_A);
        PlayerClass second = playerClass("sniper", PACK_B);
        registry.register(first);
        registry.register(second);

        assertSame(first, registry.get("sniper", PACK_A));
        assertSame(second, registry.get("sniper", PACK_B));
        // The pack that had to take the alias still answers to the alias from anywhere.
        assertSame(second, registry.get("sniper_2", PACK_A));
    }

    @Test
    void aPackThatDefinesNeitherNameFallsBackToTheGlobalName()
    {
        ItemlessTypeRegistry<PlayerClass> registry = new ItemlessTypeRegistry<>("player class");
        PlayerClass first = playerClass("engineer", PACK_A);
        registry.register(first);

        assertSame(first, registry.get("engineer", PACK_B));
        // No pack context at all is the same question as a plain global lookup.
        assertSame(first, registry.get("engineer", null));
        assertSame(first, registry.get("engineer"));
    }

    @Test
    void lookupsIgnoreCaseAndSurroundingSpace()
    {
        ItemlessTypeRegistry<PlayerClass> registry = new ItemlessTypeRegistry<>("player class");
        PlayerClass first = playerClass("Recon", PACK_A);
        registry.register(first);

        assertSame(first, registry.get("  RECON "));
        assertSame(first, registry.get("recon", PACK_A));
    }

    @Test
    void aDuplicateInsideOneContentPackIsDropped()
    {
        ItemlessTypeRegistry<PlayerClass> registry = new ItemlessTypeRegistry<>("player class");
        PlayerClass first = playerClass("pilot", PACK_A);
        PlayerClass duplicate = playerClass("pilot", PACK_A);

        assertEquals("pilot", registry.register(first));
        assertTrue(registry.register(duplicate).isEmpty());

        assertEquals(List.of(first), List.copyOf(registry.values()));
        assertSame(first, registry.get("pilot"));
        assertNull(registry.get("pilot_2"));
    }

    @Test
    void blankAndUnknownNamesResolveToNothing()
    {
        ItemlessTypeRegistry<PlayerClass> registry = new ItemlessTypeRegistry<>("player class");
        registry.register(playerClass("scout", PACK_A));

        assertNull(registry.get(null));
        assertNull(registry.get("   "));
        assertNull(registry.get("nosuchclass"));
        assertNull(registry.get("nosuchclass", PACK_A));
    }
}
