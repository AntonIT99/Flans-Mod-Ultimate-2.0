package com.flansmodultimate.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModClientConfigTest
{
    @TempDir
    Path tempDir;

    @Test
    void explosionVisualMultipliersPreserveDefaultsAndRejectUnsafeValues() throws IllegalAccessException
    {
        int checked = 0;
        for (Field field : ModClientConfig.class.getFields())
        {
            if (field.getType() != ModConfigSpec.DoubleValue.class)
                continue;
            ModConfigSpec.DoubleValue value = (ModConfigSpec.DoubleValue) field.get(null);
            if (!value.getPath().get(0).equals("Explosion Visual Settings"))
                continue;
            ModConfigSpec.ValueSpec spec = ModClientConfig.configSpec.getSpec().get(value.getPath());
            assertEquals(1D, value.getDefault(), field.getName());
            assertTrue(spec.test(0.5D), field.getName());
            assertTrue(spec.test(4D), field.getName());
            assertFalse(spec.test(-1D), field.getName());
            assertFalse(spec.test(5D), field.getName());
            assertFalse(spec.test(Double.NaN), field.getName());
            assertEquals(!field.getName().equals("EXPLOSION_PARTICLE_LIFETIME"), spec.test(0D), field.getName());
            checked++;
        }
        assertEquals(22, checked);
    }

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
