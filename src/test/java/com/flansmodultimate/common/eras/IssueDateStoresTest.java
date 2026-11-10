package com.flansmodultimate.common.eras;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class IssueDateStoresTest
{
    @TempDir
    Path directory;

    @Test
    void overridesSurviveASaveAndKeepABackup() throws IOException
    {
        Path file = directory.resolve("issue-dates.json");
        IssueDateOverrides overrides = new IssueDateOverrides(file);
        overrides.set("M4A1", new ServicePeriod(ServiceDate.parse("1942-02"), ServiceDate.parse("1957")));
        overrides.set("stg44", new ServicePeriod(ServiceDate.parse("~1944"), null));
        overrides.save();
        assertFalse(Files.exists(directory.resolve("issue-dates.json.bak")));
        overrides.save();
        assertTrue(Files.exists(directory.resolve("issue-dates.json.bak")));

        IssueDateOverrides loaded = new IssueDateOverrides(file);
        assertTrue(loaded.load().isEmpty());
        assertEquals(overrides.getAll(), loaded.getAll());
        assertEquals("1942-02 1957", loaded.get("m4a1").orElseThrow().toString());
        assertTrue(loaded.clear("STG44"));
        assertFalse(loaded.clear("stg44"));
        assertTrue(loaded.get(null).isEmpty());
    }

    @Test
    void unreadableOverridesAreReportedAndSkipped() throws IOException
    {
        Path file = directory.resolve("issue-dates.json");
        Files.writeString(file, """
            {"version": 1, "items": {
              "good": {"issue": "1944"},
              "bad": {"issue": "1944-13"},
              "empty": {},
              "text": "1944"
            }}""", StandardCharsets.UTF_8);
        IssueDateOverrides overrides = new IssueDateOverrides(file);
        assertEquals(3, overrides.load().size());
        assertEquals(1, overrides.getAll().size());

        Files.writeString(file, "{ not json", StandardCharsets.UTF_8);
        assertEquals(1, overrides.load().size());
        assertTrue(overrides.getAll().isEmpty());
        assertTrue(new IssueDateOverrides(directory.resolve("missing.json")).load().isEmpty());
    }

    @Test
    void erasFileIsCreatedWithTheDefaultsThenReadBack() throws IOException
    {
        Path file = directory.resolve("config").resolve("eras.json");
        assertSame(EraCatalog.defaults(), IssueDates.loadEras(file));
        assertTrue(Files.isRegularFile(file));
        assertEquals(EraCatalog.defaults().getEras(), IssueDates.loadEras(file).getEras());

        Files.writeString(file, "[]", StandardCharsets.UTF_8);
        assertSame(EraCatalog.defaults(), IssueDates.loadEras(file));
    }
}
