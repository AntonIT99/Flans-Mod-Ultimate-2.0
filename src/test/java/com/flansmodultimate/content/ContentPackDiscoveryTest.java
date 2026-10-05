package com.flansmodultimate.content;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ContentPackDiscoveryTest
{
    @TempDir Path root;

    @Test
    void duplicateSelectionHasExplicitPrecedenceAndPreservesDottedDirectoryNames() throws Exception
    {
        Files.writeString(root.resolve("Pack.jar"), "jar");
        Files.writeString(root.resolve("Pack.zip"), "zip");
        assertEquals(root.resolve("Pack.zip"), ContentPackDiscovery.select(root, Set.of()).get("Pack.zip"));
        Files.createDirectories(root.resolve("Pack"));
        Files.createDirectories(root.resolve("Pack.v2"));
        var selected = ContentPackDiscovery.select(root, Set.of());
        assertEquals(Set.of("Pack", "Pack.v2"), selected.keySet());
        assertFalse(selected.containsKey("Pack.jar"));
        assertFalse(selected.containsKey("Pack.zip"));
    }

    @Test
    void excludesPackModsAndDeduplicatesCaseVariants() throws Exception
    {
        Files.writeString(root.resolve("PACK.jar"), "jar");
        Files.writeString(root.resolve("pack.zip"), "zip");
        Files.writeString(root.resolve("module.jar"), "module");
        var selected = ContentPackDiscovery.select(root, Set.of(root.resolve("module.jar").toAbsolutePath().normalize()));
        assertEquals(Set.of("pack.zip"), selected.keySet());
    }
}
