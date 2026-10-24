package com.wolffsmod.npcs.names;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class DroidDesignationsTest
{
    private static final List<Pattern> PATTERNS = List.of(Pattern.compile("B1-[0-9]{3,4}|[0-9]{4}|3B3-(?:[0-9]{2}|[0-9]{4})"), Pattern.compile("B1-[0-9]{3,4}|OOM-[0-9]{1,3}"),
        Pattern.compile("B2-[0-9]{4}"), Pattern.compile("BX-[0-9]{4}"), Pattern.compile("TX-[0-9]{2}|TA-[0-9]{3}"), Pattern.compile("IG-100-[0-9]{4}"), Pattern.compile("Droideka-[0-9]{4}"));

    @Test
    void differentModelFamiliesKeepTheirDisplayConventions()
    {
        for (int index = 0; index < PATTERNS.size(); index++)
            for (int gender = 0; gender <= 2; gender++)
                for (int sample = 0; sample < 100; sample++)
                {
                    String name = SoldierNames.generate(63 + index, gender);
                    assertTrue(PATTERNS.get(index).matcher(name).matches(), name);
                }
    }
}
