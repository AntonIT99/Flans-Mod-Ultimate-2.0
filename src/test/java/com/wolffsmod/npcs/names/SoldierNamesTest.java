package com.wolffsmod.npcs.names;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SoldierNamesTest
{
    @Test
    void romanAssociationDelegatesToCustomNpcsAndKeepsLegacyPersistence()
    {
        assertTrue(SoldierNames.acceptsAssociation("customnpcs:roman"));
        assertFalse(SoldierNames.acceptsAssociation("customnpcs:missing"));
        assertEquals(0, SoldierNames.generatorId("customnpcs:roman"));
        assertEquals("customnpcs:roman", SoldierNames.associationId(0));
        try (var generator = org.mockito.Mockito.mockStatic(nikedemos.markovnames.generators.MarkovGenerator.class))
        {
            generator.when(() -> nikedemos.markovnames.generators.MarkovGenerator.fetch(0, 2)).thenReturn("Livia");
            assertEquals("Livia", SoldierNames.generate(0, 2));
        }
        CompoundTag tag = new CompoundTag();
        SoldierNames.saveSelection(10, tag);
        SoldierNames.saveSelection(0, tag);
        assertFalse(tag.contains(SoldierNames.SAVE_KEY));
        assertEquals(0, SoldierNames.loadSelection(0, tag));
    }

    @Test
    void bundledListsOnlyProduceWholeComponentsOfTheSelectedGender() throws Exception
    {
        for (String id : SoldierNames.ids().stream()
            .filter(id -> !id.startsWith("clone_") && !id.startsWith("imperial_") && !id.startsWith("droid_") && !id.startsWith("jedi_") && !id.startsWith("sith_")).toList())
        {
            try (var stream = SoldierNames.class.getResourceAsStream("/data/wolffsmodnpcs/names/" + id + ".json"))
            {
                assertNotNull(stream);
                SoldierNameList list = new Gson().fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), SoldierNameList.class);
                Random random = new Random(1234);
                for (int gender = 0; gender <= 2; gender++)
                {
                    final int selectedGender = gender;
                    for (int sample = 0; sample < 500; sample++)
                    {
                        String generated = list.generate(gender, random);
                        assertTrue(list.groups().stream().anyMatch(group -> (selectedGender != 2 && matches(group.male(), group.surnames(), group.surnameFirst(), generated))
                            || (selectedGender != 1 && matches(group.female(), group.femaleSurnames(), group.surnameFirst(), generated))), id + ": " + generated);
                    }
                }
            }
        }
    }

    private static boolean matches(List<String> given, List<String> surnames, boolean surnameFirst, String generated)
    {
        if (surnameFirst)
            return given.stream().anyMatch(name -> generated.endsWith(" " + name) && surnames.contains(generated.substring(0, generated.length() - name.length() - 1)));
        return given.stream().anyMatch(name -> generated.startsWith(name + " ") && surnames.contains(generated.substring(name.length() + 1)));
    }

    @Test
    void groupSelectionNeverMixesTraditionsAndEitherGenderReachesBoth()
    {
        SoldierNameList list = new SoldierNameList("test", List.of(new SoldierNameList.Group(1, List.of("Adam"), List.of("Alice"), List.of("Smith"), null),
            new SoldierNameList.Group(1, List.of("Ivan"), List.of("Anna"), List.of("Ivanov"), List.of("Ivanova"))));
        Random random = new Random(42);
        var results = new java.util.HashSet<String>();
        for (int index = 0; index < 100; index++)
            results.add(list.generate(0, random));
        assertEquals(java.util.Set.of("Adam Smith", "Alice Smith", "Ivan Ivanov", "Anna Ivanova"), results);
    }

    @Test
    void invalidPoolsFailInsteadOfGeneratingPartialNames()
    {
        List<SoldierNameList.Group> noGroups = List.of();
        List<String> empty = List.of();
        List<String> male = List.of("A");
        List<String> female = List.of("B");
        List<String> surnames = List.of("C");
        List<String> padded = List.of(" A");
        List<String> duplicates = List.of("A", "A");
        List<String> controls = List.of("A\nB");
        assertThrows(IllegalArgumentException.class, () -> new SoldierNameList("empty", noGroups));
        assertThrows(IllegalArgumentException.class, () -> new SoldierNameList.Group(0, male, female, surnames, null));
        assertThrows(IllegalArgumentException.class, () -> new SoldierNameList.Group(1, empty, female, surnames, null));
        assertThrows(IllegalArgumentException.class, () -> new SoldierNameList.Group(1, padded, female, surnames, null));
        assertThrows(IllegalArgumentException.class, () -> new SoldierNameList.Group(1, duplicates, female, surnames, null));
        assertThrows(IllegalArgumentException.class, () -> new SoldierNameList.Group(1, controls, female, surnames, null));
    }

    @Test
    void legacySlotsStayUnchangedAndStableIdsSurviveNbtRoundTrips()
    {
        String[] legacy = new String[10];
        Arrays.fill(legacy, "legacy");
        String[] labels = SoldierNames.appendLabels(legacy);
        assertArrayEquals(legacy, Arrays.copyOf(labels, 10));
        assertEquals(80, labels.length);
        assertEquals("wolffsmodnpcs.names.german", labels[10]);
        assertEquals("wolffsmodnpcs.names.russian", labels[13]);
        for (int slot = 0; slot < 10; slot++)
        {
            assertFalse(SoldierNames.contains(slot));
            assertEquals("", SoldierNames.stableId(slot));
        }
        for (int slot = 10; slot < labels.length; slot++)
        {
            CompoundTag tag = new CompoundTag();
            SoldierNames.saveSelection(slot, tag);
            assertEquals(slot, SoldierNames.loadSelection(0, tag.copy()));
            assertFalse(SoldierNames.generate(slot, 1).isBlank());
        }
        assertFalse(SoldierNames.contains(-1));
        assertFalse(SoldierNames.contains(labels.length));
        assertEquals(0, SoldierNames.generatorId("wolffsmodnpcs:missing"));
        assertThrows(IllegalStateException.class, () -> SoldierNames.appendLabels(new String[11]));
    }

    @Test
    void historicalListsAppendAfterTheFourModernSlots()
    {
        assertEquals(List.of("german", "british", "american", "russian"), SoldierNames.ids().subList(0, 4));
        assertEquals(
            List.of("german_20th", "british_20th", "american_20th", "russian_20th", "canadian_20th", "italian_20th", "french_20th", "dutch_20th", "chinese_20th", "polish_20th", "hungarian_20th",
                "finnish_20th", "romanian_20th", "jewish_20th", "estonian_20th", "latvian_20th", "croatian_20th", "spanish_20th", "serbian_20th", "bulgarian_20th", "swedish_20th", "greek_20th",
                "ukrainian_20th", "czech_20th", "slovak_20th", "lithuanian_20th", "norwegian_20th", "danish_20th", "belgian_20th", "portuguese_20th", "soviet_20th", "japanese_20th"),
            SoldierNames.ids().subList(4, 36));
        assertEquals(14, SoldierNames.generatorId("wolffsmodnpcs:german_20th"));
        assertEquals(44, SoldierNames.generatorId("wolffsmodnpcs:soviet_20th"));
        assertEquals(45, SoldierNames.generatorId("wolffsmodnpcs:japanese_20th"));
        assertEquals(70, SoldierNames.ids().stream().distinct().count());
    }

    @Test
    void surnameFirstOrderPreservesWholeMultiwordGivenNames()
    {
        SoldierNameList list = new SoldierNameList("test_20th", List.of(new SoldierNameList.Group(1, List.of("János Pál"), List.of("Anna Mária"), List.of("Kovács"), null, true)));
        assertEquals("Kovács János Pál", list.generate(1, new Random(1)));
        assertEquals("Kovács Anna Mária", list.generate(2, new Random(1)));
    }

    @Test
    void stableIdentityOverridesNumericSlotsAndReturningToLegacyClearsIt()
    {
        CompoundTag tag = new CompoundTag();
        tag.putString("Name", "Hans Müller");
        assertEquals(7, SoldierNames.loadSelection(7, tag));
        SoldierNames.saveSelection(10, tag);
        assertEquals(10, SoldierNames.loadSelection(13, tag.copy()));
        SoldierNames.saveSelection(1, tag);
        assertFalse(tag.contains(SoldierNames.SAVE_KEY));
        assertEquals(1, SoldierNames.loadSelection(1, tag));
        tag.putString(SoldierNames.SAVE_KEY, "wolffsmodnpcs:missing");
        assertEquals(0, SoldierNames.loadSelection(13, tag));
        assertEquals("Hans Müller", tag.getString("Name"));
    }
}
