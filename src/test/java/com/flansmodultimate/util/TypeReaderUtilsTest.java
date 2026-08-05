package com.flansmodultimate.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class TypeReaderUtilsTest
{
    @Test
    void keepsApostrophesInsideLegacyProjectileNames()
    {
        assertArrayEquals(new String[]{"Pzgr.L'Spur", "1", "72", "0", "850", "30"},
            TypeReaderUtils.splitValues("Pzgr.L'Spur 1 72 0 850 30"));
    }

    @Test
    void stillSupportsQuotedValues()
    {
        assertArrayEquals(new String[]{"High Explosive", "1", "72"},
            TypeReaderUtils.splitValues("\"High Explosive\" 1 72"));
    }
}
