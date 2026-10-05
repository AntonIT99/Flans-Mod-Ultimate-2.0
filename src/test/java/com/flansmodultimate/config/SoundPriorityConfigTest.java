package com.flansmodultimate.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SoundPriorityConfigTest
{
    @TempDir Path root;

    @Test
    void addsAllSourcesOnFirstLoadAndNeverRewritesAnUnchangedList() throws Exception
    {
        Path file = root.resolve("content-loading.toml");
        List<String> sources = List.of("mod:official", "mod:flansmodultimate", "pack:a");
        assertEquals(sources, SoundPriorityConfig.synchronize(file, sources));
        byte[] before = Files.readAllBytes(file);
        var time = Files.getLastModifiedTime(file);
        assertEquals(sources, SoundPriorityConfig.synchronize(file, sources));
        assertArrayEquals(before, Files.readAllBytes(file));
        assertEquals(time, Files.getLastModifiedTime(file));
    }

    @Test
    void preservesCustomOrderOtherSettingsAndMissingSourcesWhileAppendingNewPacks() throws Exception
    {
        Path file = root.resolve("content-loading.toml");
        Files.writeString(file, "# Keep my setting\noverrideConfiguredSoundLengths = false\n"
            + "soundPackPriority = [\"pack:b\", \"pack:removed\", \"pack:a\"]\n");
        var expected = List.of("pack:b", "pack:removed", "pack:a", "pack:c");
        assertEquals(expected, SoundPriorityConfig.synchronize(file, List.of("pack:a", "pack:b", "pack:c")));
        try (var config = CommentedFileConfig.of(file, TomlFormat.instance()))
        {
            config.load();
            assertEquals(false, config.get("overrideConfiguredSoundLengths"));
            assertEquals(" Keep my setting", config.getComment("overrideConfiguredSoundLengths"));
        }
        assertEquals(expected, SoundPriorityConfig.synchronize(file, List.of("pack:a", "pack:b", "pack:removed", "pack:c")));
    }

    @Test
    void removesInvalidAndDuplicateEntriesWithoutLosingValidPriorities()
    {
        assertEquals(List.of("pack:b", "pack:a", "mod:new"), SoundPriorityConfig.reconcile(
            List.of(" pack:b ", "", 42, "pack:b", "pack:a"), List.of("pack:a", "mod:new")));
        assertEquals(List.of("pack:a"), SoundPriorityConfig.reconcile("bad setting", List.of("pack:a")));
    }
}
