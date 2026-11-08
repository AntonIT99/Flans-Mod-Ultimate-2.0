package com.flansmodultimate.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ClassNameIndexTest
{
    private final ClassNameIndex index = new ClassNameIndex(List.of("a.NewBombs.ModelAIM120", "a.ModelX", "a.Modelx"));

    @Test
    void findsTheSpellingThePackUses()
    {
        assertEquals("a.NewBombs.ModelAIM120", index.resolve("a.newBombs.Modelaim120"));
    }

    @Test
    void prefersAnExactMatch()
    {
        assertEquals("a.Modelx", index.resolve("a.Modelx"));
        assertEquals("a.ModelX", index.resolve("a.ModelX"));
    }

    @Test
    void reportsUnknownClasses()
    {
        assertNull(index.resolve("a.ModelMissing"));
    }
}
