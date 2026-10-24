package com.wolffsmod.npcs.names;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class ImperialDesignationsTest
{
    private static final List<Pattern> PATTERNS = List.of("TK", "TD", "TS", "TB", "RS", "TX").stream().map(prefix -> Pattern.compile(prefix + "-[0-9]{3,4}")).toList();
    private static final Pattern LEGENDS = Pattern.compile("TK-[0-9]{1,10}");

    @Test
    void imperialListsPreservePrefixesAndSupportedNumericLengths()
    {
        String[] prefixes = {"TK", "TD", "TS", "TB", "RS", "TX", "TK"};
        for (int index = 0; index < prefixes.length; index++)
            for (int gender = 0; gender <= 2; gender++)
                for (int sample = 0; sample < 100; sample++)
                {
                    String name = SoldierNames.generate(56 + index, gender);
                    assertTrue((index == 6 ? LEGENDS : PATTERNS.get(index)).matcher(name).matches(), name);
                }
    }
}
