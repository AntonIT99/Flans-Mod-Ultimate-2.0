package com.flansmodultimate.common.eras;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static com.flansmodultimate.common.eras.ServiceDate.monthIndex;
import static org.junit.jupiter.api.Assertions.*;

class EraCatalogTest
{
    private static final EraCatalog ERAS = EraCatalog.defaults();

    @Test
    void defaultsAreFoundByAlias()
    {
        Era ww1 = ERAS.find("great war");
        assertNotNull(ww1);
        assertEquals("WW1", ww1.id());
        assertNull(ERAS.find("Atlantis"));
        assertNull(ERAS.find("Future"));
    }

    @Test
    void eraEndIsTheLatestEndOfTheErasContainingTheMonth()
    {
        assertEquals(monthIndex(1939, 8), ERAS.eraEnd(monthIndex(1937, 1)));
        assertEquals(monthIndex(1945, 9), ERAS.eraEnd(monthIndex(1944, 6)));
        // Early Modern and Napoleonic both contain 1800; the Napoleonic era ends later.
        assertEquals(monthIndex(1815, 11), ERAS.eraEnd(monthIndex(1800, 1)));
        assertEquals(ServiceDate.OPEN_HIGH, ERAS.eraEnd(monthIndex(1991, 6)));
    }

    @Test
    void jsonKeepsEveryEra()
    {
        EraCatalog.Loaded loaded = EraCatalog.fromJson(ERAS.toJson());
        assertTrue(loaded.problems().isEmpty(), loaded.problems()::toString);
        assertEquals(ERAS.getEras(), loaded.catalog().getEras());
    }

    @Test
    void unusableErasAreSkippedAndReported()
    {
        String json = """
            {"version": 1, "eras": [
              {"id": "Ancient", "from": "800BC", "to": "500"},
              {"id": "ancient", "from": "1900"},
              {"id": "Future", "from": "2100"},
              {"id": "Backwards", "from": "1950", "to": "1940"},
              {"id": "Nested", "from": "WW2"},
              {"id": "Aliased", "from": "1960", "aliases": ["Old Times", "Ancient"]},
              "not an object"
            ]}""";
        EraCatalog.Loaded loaded = EraCatalog.fromJson(JsonParser.parseString(json).getAsJsonObject());
        assertEquals(1, loaded.catalog().getEras().size());
        assertEquals(monthIndex(-799, 1), loaded.catalog().find("Ancient").first());
        assertEquals(6, loaded.problems().size(), loaded.problems()::toString);
    }

    @Test
    void missingEraListFallsBackToDefaults()
    {
        EraCatalog.Loaded loaded = EraCatalog.fromJson(JsonParser.parseString("{\"version\": \"one\"}").getAsJsonObject());
        assertSame(ERAS, loaded.catalog());
        assertEquals(2, loaded.problems().size());
    }
}
