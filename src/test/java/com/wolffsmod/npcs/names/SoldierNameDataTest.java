package com.wolffsmod.npcs.names;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Content-level checks for the historical expansion, independently of Custom NPCs and its menus. */
class SoldierNameDataTest
{
    @Test
    void everyHistoricalListHasSubstantialPoolsPeriodMetadataAndReferences() throws IOException
    {
        for (String id : SoldierNames.ids().subList(4, SoldierNames.ids().size()))
        {
            JsonObject json = read(id);
            SoldierNameList list = new Gson().fromJson(json, SoldierNameList.class);
            assertTrue(json.get("setting").getAsString().contains("1914–1945"), id);
            assertFalse(json.getAsJsonArray("sources").isEmpty(), id);
            assertTrue(list.groups().stream().mapToInt(group -> group.male().size()).sum() >= 50, id);
            assertTrue(list.groups().stream().mapToInt(group -> group.female().size()).sum() >= 40, id);
            assertTrue(list.groups().stream().mapToInt(group -> group.surnames().size()).sum() >= 45, id);
            for (SoldierNameList.Group group : list.groups())
            {
                // All current historical pools use Latin letters or transliteration; catch accidental mixed scripts.
                for (List<String> pool : List.of(group.male(), group.female(), group.surnames(), group.femaleSurnames()))
                    for (String name : pool)
                        assertTrue(name.codePoints().filter(Character::isLetter).allMatch(character -> Character.UnicodeScript.of(character) == Character.UnicodeScript.LATIN), id + ": " + name);
            }
        }
    }

    @Test
    void combinedArmiesKeepTheirNamingGroupsAndSovietContainsTheRussianPool() throws IOException
    {
        assertEquals(3, list("british_20th").groups().size());
        assertEquals(2, list("canadian_20th").groups().size());
        assertEquals(2, list("belgian_20th").groups().size());
        assertEquals(2, list("jewish_20th").groups().size());
        SoldierNameList soviet = list("soviet_20th");
        assertEquals(16, soviet.groups().size());
        for (String component : List.of("russian_20th", "ukrainian_20th", "estonian_20th", "latvian_20th", "lithuanian_20th", "romanian_20th"))
            for (SoldierNameList.Group expected : list(component).groups())
                assertTrue(soviet.groups().stream().anyMatch(actual -> actual.male().equals(expected.male()) && actual.female().equals(expected.female())
                    && actual.surnames().equals(expected.surnames()) && actual.femaleSurnames().equals(expected.femaleSurnames()) && actual.surnameFirst() == expected.surnameFirst()), component);
    }

    @Test
    void historicalNamesHaveTheirOwnPoolsAndRespectLanguageSpecificPresentation() throws IOException
    {
        for (String nation : List.of("german", "british", "american", "russian"))
            assertNotEquals(list(nation).groups().get(0).male(), list(nation + "_20th").groups().get(0).male(), nation);
        for (String nation : List.of("hungarian_20th", "chinese_20th", "japanese_20th"))
            assertTrue(list(nation).groups().stream().allMatch(SoldierNameList.Group::surnameFirst), nation);
        for (String nation : List.of("russian_20th", "polish_20th", "czech_20th", "slovak_20th", "latvian_20th", "lithuanian_20th", "bulgarian_20th", "greek_20th"))
            assertNotEquals(list(nation).groups().get(0).surnames(), list(nation).groups().get(0).femaleSurnames(), nation);
        assertTrue(list("japanese_20th").groups().get(0).male().containsAll(List.of("Kiyoshi", "Shigeru", "Saburō", "Isamu", "Minoru")));
    }

    private static SoldierNameList list(String id) throws IOException
    {
        return new Gson().fromJson(read(id), SoldierNameList.class);
    }

    private static JsonObject read(String id) throws IOException
    {
        try (var stream = SoldierNames.class.getResourceAsStream("/data/wolffsmodnpcs/names/" + id + ".json"))
        {
            assertNotNull(stream, id);
            try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8))
            {
                return new Gson().fromJson(reader, JsonObject.class);
            }
        }
    }
}
