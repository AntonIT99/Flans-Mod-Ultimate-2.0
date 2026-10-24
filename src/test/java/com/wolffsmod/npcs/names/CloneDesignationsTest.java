package com.wolffsmod.npcs.names;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class CloneDesignationsTest
{
    private static final Pattern ALPHA = Pattern.compile("(?:A|Alpha)-(?:[1-9][0-9]?|100)");
    private static final Pattern NULL = Pattern.compile("(?:N|Null)-(?:[1-9]|1[0-2])");
    private static final Pattern ARC = Pattern.compile("CT-(?:[0-9]{4}|[0-9]{2}-[0-9]{4})|ARC-[0-9]{4}|(?:A|Alpha)-(?:[1-9][0-9]?|100)|(?:N|Null)-(?:[1-9]|1[0-2])");
    private static final List<Pattern> PATTERNS = List.of(Pattern.compile("CT-(?:[0-9]{4}|[0-9]{2}-[0-9]{4})"), Pattern.compile("CC-[0-9]{4}(?:-[0-9]{2})?"), ARC, Pattern.compile("RC-[0-9]{4}"),
        Pattern.compile("CL-[0-9]{4}"), Pattern.compile("CS-[0-9]{4}"));

    @Test
    void appendedGeneratorsKeepPrefixesAndNumericBlocksForEveryGender()
    {
        String[] prefixes = {"CT", "CC", "ARC", "RC", "CL", "CS"};
        for (int index = 0; index < prefixes.length; index++)
        {
            int generator = 46 + index;
            assertEquals(generator, SoldierNames.generatorId("wolffsmodnpcs:clone_" + prefixes[index].toLowerCase(java.util.Locale.ROOT)));
            for (int gender = 0; gender <= 2; gender++)
                for (int sample = 0; sample < 100; sample++)
                {
                    String name = SoldierNames.generate(generator, gender);
                    assertTrue(PATTERNS.get(index).matcher(name).matches(), name);
                }
        }
    }

    @Test
    void mixedArcPoolReachesAllVariantsAndFixedCharactersIgnoreGender()
    {
        var seen = new java.util.HashSet<String>();
        for (int sample = 0; sample < 1000; sample++)
        {
            String name = SoldierNames.generate(48, 0);
            seen.add(name.substring(0, name.indexOf('-')));
            String alpha = SoldierNames.generate(52, 0);
            assertTrue(ALPHA.matcher(alpha).matches(), alpha);
            String nullClone = SoldierNames.generate(53, 0);
            assertTrue(NULL.matcher(nullClone).matches(), nullClone);
        }
        assertEquals(java.util.Set.of("CT", "ARC", "A", "Alpha", "N", "Null"), seen);
        for (int gender = 0; gender <= 2; gender++)
        {
            assertEquals("Captain Rex", SoldierNames.generate(54, gender));
            assertEquals("Commander Cody", SoldierNames.generate(55, gender));
        }
        assertThrows(IllegalArgumentException.class, () -> new SoldierNameList.Serial("N-", 12, 1));
    }

    @Test
    void patternsPreserveLeadingZerosAndGenerationDoesNotDependOnGender()
    {
        SoldierNameList list = new SoldierNameList("test", null, List.of("CT-##-####"));
        assertEquals(list.generate(1, new Random(1)), list.generate(2, new Random(1)));
        assertEquals("CT-00-0000", list.generate(0, new Random(0)
        {
            @Override
            public int nextInt(int bound)
            {
                return 0;
            }
        }));
        List<String> invalid = List.of("CT-XXXX");
        assertThrows(IllegalArgumentException.class, () -> new SoldierNameList("test", null, invalid));
    }
}
