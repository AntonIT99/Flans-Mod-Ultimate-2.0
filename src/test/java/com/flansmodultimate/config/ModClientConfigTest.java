package com.flansmodultimate.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModClientConfigTest
{
    @TempDir
    Path tempDir;

    @Test
    void readsEnabledUncensoredContentBeforeForgeLoadsTheConfig() throws IOException
    {
        Path configPath = tempDir.resolve("flansmodultimate-client.toml");
        Files.writeString(configPath, """
            ["General Settings"]
            enableUncensoredContent = true
            """);

        assertTrue(ModClientConfig.readUncensoredContentSetting(configPath));
    }

    @Test
    void defaultsToDisabledWhenStartupConfigDoesNotExist()
    {
        assertFalse(ModClientConfig.readUncensoredContentSetting(tempDir.resolve("missing.toml")));
    }
}
