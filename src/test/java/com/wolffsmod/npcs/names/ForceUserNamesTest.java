package com.wolffsmod.npcs.names;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class ForceUserNamesTest
{
    private static final List<String> IDS = List.of("jedi_knight", "jedi_master", "jedi_temple_guard", "jedi_wayfarer", "sith_apprentice", "sith_warrior", "sith_assassin", "sith_marauder",
        "sith_lord");
    private static final List<String> TITLES = List.of("Jedi Knight", "Jedi Master", "Jedi Temple Guard", "Jedi Wayfarer", "Sith Apprentice", "Sith Warrior", "Sith Assassin", "Sith Marauder",
        "Darth");

    @Test
    void rankedPoolsPreserveTitlesGenderAndAnonymousGuards() throws Exception
    {
        for (int index = 0; index < IDS.size(); index++)
        {
            String id = IDS.get(index);
            assertEquals(71 + index, SoldierNames.generatorId("wolffsmodnpcs:" + id));
            try (var stream = SoldierNames.class.getResourceAsStream("/data/wolffsmodnpcs/names/" + id + ".json"))
            {
                assertNotNull(stream);
                SoldierNameList list = new Gson().fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), SoldierNameList.class);
                for (int gender = 0; gender <= 2; gender++)
                    for (int sample = 0; sample < 100; sample++)
                        assertName(list, index, gender, sample);
            }
        }
    }

    private static void assertName(SoldierNameList list, int index, int gender, int sample)
    {
        String name = list.generate(gender, new Random(sample));
        assertTrue(name.startsWith(TITLES.get(index)), name);
        if (list.id().equals("jedi_temple_guard"))
        {
            assertEquals("Jedi Temple Guard", name);
            return;
        }
        SoldierNameList.Group group = list.groups().get(0);
        List<String> given = gender == 2 ? group.female() : group.male();
        List<String> surnames = gender == 2 ? group.femaleSurnames() : group.surnames();
        if (gender != 0)
            assertTrue(given.stream().anyMatch(first -> surnames.stream().anyMatch(last -> name.equals(first + " " + last))), name);
    }
}
