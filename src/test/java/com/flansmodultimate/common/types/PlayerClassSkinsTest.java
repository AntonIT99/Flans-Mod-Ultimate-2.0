package com.flansmodultimate.common.types;

import com.flansmodultimate.api.teams.SkinOverrideVariant;
import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.content.ContentPack;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class PlayerClassSkinsTest
{
    @Test
    void repeatedAndInlineOverridesKeepDefinitionOrderAndLegacySanitizing()
    {
        PlayerClass playerClass = parse("SkinOverride Soldier_A Soldier_B // comment", "skinoverride Soldier_C", "SkinOverride soldier_a");
        assertEquals(List.of("soldier_a", "soldier_b", "soldier_c"), playerClass.getSkinOverrideVariants().stream().map(SkinOverrideVariant::name).toList());
        assertEquals("soldier_a", playerClass.getSkinOverride());
        assertTrue(playerClass.getSkinOverrideVariants().stream().allMatch(skin -> skin.texture() == null));
        List<SkinOverrideVariant> variants = playerClass.getSkinOverrideVariants();
        assertThrows(UnsupportedOperationException.class, variants::clear);
    }

    @Test
    void oneOrNoSkinRetainsTheExistingBehavior()
    {
        assertEquals("zombie", parse("SkinOverride Zombie").getSkinOverride());
        assertEquals(1, parse("SkinOverride Zombie").getSkinOverrideVariants().size());
        assertTrue(parse().getSkinOverrideVariants().isEmpty());
        assertEquals("", parse().getSkinOverride());
    }

    @Test
    void skinIsChosenOnClassApplicationAndRemainsStableUntilTheNextApplication()
    {
        UUID id = UUID.randomUUID();
        PlayerData data = PlayerData.getInstance(id);
        try
        {
            PlayerClass playerClass = parse("SkinOverride first second third");
            data.setNewPlayerClass(playerClass);
            Set<Integer> chosen = new HashSet<>();
            for (int spawn = 0; spawn < 128; spawn++)
            {
                data.applyPendingTeamSelection();
                int skin = data.getPlayerClassSkin();
                assertTrue(skin >= 0 && skin < 3);
                assertEquals(skin, data.getPlayerClassSkin());
                assertSame(playerClass, data.getPlayerClass());
                chosen.add(skin);
            }
            assertEquals(Set.of(0, 1, 2), chosen);
            data.setPlayerClass(parse("SkinOverride only"));
            assertEquals(0, data.getPlayerClassSkin());
            data.setPlayerClass(parse());
            assertEquals(-1, data.getPlayerClassSkin());
            data.setPlayerClass(playerClass);
            data.resetScore();
            assertEquals(-1, data.getPlayerClassSkin());
            assertNull(data.getPlayerClass());
            data.setPlayerClass(null);
            assertEquals(-1, data.getPlayerClassSkin());
        }
        finally
        {
            PlayerData.removeServerData(id);
        }
    }

    private static PlayerClass parse(String... lines)
    {
        PlayerClass playerClass = new PlayerClass();
        playerClass.read(new TypeFile("skin-test", EnumType.PLAYER_CLASS, new ContentPack("Skin tests", Path.of("build", "test-packs", "skins")), List.of(lines)));
        return playerClass;
    }
}
