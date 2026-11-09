package com.flansmodultimate.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResourceUtilsSanitizeTest
{
    @Test
    void lowercasesAndKeepsLegalResourceCharacters()
    {
        assertEquals("navalgun_15.5cm_3rdyeartype", ResourceUtils.sanitize("Navalgun_15.5cm_3rdYearType"));
        assertEquals("ak-47_scope", ResourceUtils.sanitize("Ak-47_Scope"));
    }

    @Test
    void dropsCharactersNoFileNameCanHold()
    {
        // The definition pasted the display name's ':' into the texture name of an existing file.
        assertEquals("navalgun_15.5cm_3rdyeartype", ResourceUtils.sanitize("Navalgun_15.5cm_3rdYear:Type"));
        assertEquals(ResourceUtils.sanitize("Gun"), ResourceUtils.sanitize("Gun?"));
        assertEquals(ResourceUtils.sanitize("abc"), ResourceUtils.sanitize("a/b\\c"));
    }

    @Test
    void stillReplacesCharactersFileNamesCanHold()
    {
        assertEquals("gun__1_", ResourceUtils.sanitize("Gun (1)"));
        assertEquals("gun_sight", ResourceUtils.sanitize("Gun Sight"));
    }

    @Test
    void stripsFileNameStems()
    {
        assertEquals("navalgun_3rdyeartype", ResourceUtils.sanitizeFileNameStem("skins/Navalgun_3rdYear:Type.png"));
    }
}
