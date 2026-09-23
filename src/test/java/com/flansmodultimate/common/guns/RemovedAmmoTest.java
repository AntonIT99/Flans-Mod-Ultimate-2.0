package com.flansmodultimate.common.guns;

import com.flansmodultimate.ContentPack;
import com.flansmodultimate.IContentProvider;
import com.flansmodultimate.common.types.EnumType;
import com.flansmodultimate.common.types.TypeFile;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@code RemoveAmmo} exists to repair a pack that hands a weapon ammunition it could never
 * fire, so it has to be readable in the same shapes packs already write, and it must never
 * remove anything the author did not name.
 */
class RemovedAmmoTest
{
    @Test
    void aDefinitionWithoutTheKeyRemovesNothing()
    {
        RemovedAmmo removed = read("Ammo ships_shell_german20mmaa");
        assertTrue(removed.isEmpty());
        assertFalse(removed.removes("ships_shell_german20mmaa"));
    }

    @Test
    void aNamedRoundIsRemovedAndOnlyThatRound()
    {
        RemovedAmmo removed = read("RemoveAmmo ships_shell_german20mmaa");
        assertTrue(removed.removes("ships_shell_german20mmaa"));
        assertFalse(removed.removes("ships_shell_german203mm"));
    }

    @Test
    void namesMatchCaseInsensitively()
    {
        RemovedAmmo removed = read("RemoveAmmo Ships_Shell_German20mmAA");
        assertTrue(removed.removes("ships_shell_german20mmaa"));
        assertTrue(removed.removes("SHIPS_SHELL_GERMAN20MMAA"));
    }

    @Test
    void theKeyIsRepeatableAndAcceptsSeveralNamesOnOneLine()
    {
        RemovedAmmo removed = read(
            "RemoveAmmo firstRound secondRound",
            "RemoveAmmo thirdRound");
        assertEquals(3, removed.keys().size());
        for (String name : List.of("firstRound", "secondRound", "thirdRound"))
            assertTrue(removed.removes(name), name);
    }

    @Test
    void aTrailingCommentIsNotMistakenForAnAmmoName()
    {
        RemovedAmmo removed = read("RemoveAmmo firstRound // wrong calibre for this battery");
        assertTrue(removed.removes("firstRound"));
        assertEquals(1, removed.keys().size());
    }

    @Test
    void aNullFileIsToleratedRatherThanThrowing()
    {
        assertTrue(RemovedAmmo.read(null).isEmpty());
    }

    private static RemovedAmmo read(String... lines)
    {
        IContentProvider pack = new ContentPack("test", Path.of("build", "test-packs", "ammo"));
        return RemovedAmmo.read(new TypeFile("syntheticWeapon", EnumType.GUN, pack, List.of(lines)));
    }
}
