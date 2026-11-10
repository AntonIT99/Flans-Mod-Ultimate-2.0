package com.flansmodultimate.common.eras;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EraStateTest
{
    @Test
    void stateSurvivesTheWire()
    {
        EraState state = new EraState(EraCatalog.defaults(), Map.of("m4a1", ServicePeriod.parse("1942-02 1957"), "stg44", ServicePeriod.parse("~1944")),
            List.of(new ScopedRestriction(EraScope.server(), EraRestriction.parse("until 1945")),
                new ScopedRestriction(EraScope.dimension("minecraft:the_nether"), EraRestriction.parse("equal WW2 undated=block"))), true);
        EraState read = EraState.fromJson(JsonParser.parseString(state.toJson().toString()).getAsJsonObject());
        assertEquals(state, read);
    }

    @Test
    void unreadableEntriesAreSkipped()
    {
        String json = """
            {"overrides": {"good": "1944", "bad": "1944-13", "worse": {}},
             "restrictions": [{"scope": "server", "key": "", "value": "until 1945"}, {"scope": "galaxy", "key": "", "value": "until 1945"},
               {"scope": "server", "key": "", "value": "sometimes"}, "nonsense"],
             "bypass": "maybe"}""";
        EraState read = EraState.fromJson(JsonParser.parseString(json).getAsJsonObject());
        assertEquals(EraCatalog.defaults(), read.eras());
        assertEquals(1, read.overrides().size());
        assertEquals(1, read.restrictions().size());
        assertFalse(read.bypass());
    }

    @Test
    void savedRestrictionsSurviveAReload()
    {
        EraSavedData data = new EraSavedData();
        data.setServer(EraRestriction.parse("between WW2 1946 match=introduction"));
        data.setDimension("minecraft:the_nether", EraRestriction.parse("notbefore 2000 despawn=on"));
        data.setDimension("minecraft:the_end", EraRestriction.parse("until 1918"));
        data.setDimension("minecraft:the_end", null);
        CompoundTag saved = data.save(new CompoundTag(), null);
        saved.getCompound("dimensions").putString("broken", "sometimes 1944");

        EraSavedData loaded = EraSavedData.load(saved);
        assertEquals(data.getServer(), loaded.getServer());
        assertEquals(data.getDimensions(), loaded.getDimensions());
        assertTrue(loaded.getDimension("minecraft:the_end").isEmpty());
    }

    @Test
    void periodsAndMonthsPrintInTheContentSyntax()
    {
        assertEquals("1942-02 1957", ServicePeriod.parse(" 1942-02   1957 ").toString());
        assertThrows(IllegalArgumentException.class, () -> ServicePeriod.parse("1942 1950 1960"));
        assertEquals("1939-09", ServiceDate.formatMonth(ServiceDate.monthIndex(1939, 9)));
        assertEquals("480BC", ServiceDate.formatMonth(ServiceDate.monthIndex(-479, 3)));
        assertEquals("open", ServiceDate.formatMonth(ServiceDate.OPEN_HIGH));
        assertEquals("Future", ServiceDate.formatMonth(ServiceDate.FUTURE));
        assertEquals(EraScope.Kind.DIMENSION, EraScope.Kind.byId("Dimension"));
        assertEquals("dimension minecraft:the_nether", EraScope.dimension("minecraft:the_nether").toString());
    }
}
