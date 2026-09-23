package com.flansmodultimate.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.lang.reflect.Constructor;
import java.nio.file.Path;

/** Loads an in-memory NeoForge config without starting a mod container. */
final class TestConfigLoader
{
    private TestConfigLoader() {}

    static void load(ModConfigSpec spec, CommentedConfig config)
    {
        try
        {
            // ILoadedConfig is sealed in NeoForge; its only implementation is package-private.
            Class<?> implementation = Class.forName("net.neoforged.fml.config.LoadedConfig");
            Constructor<?> constructor = implementation.getDeclaredConstructor(CommentedConfig.class, Path.class,
                Class.forName("net.neoforged.fml.config.ModConfig"));
            constructor.setAccessible(true);
            spec.acceptConfig((IConfigSpec.ILoadedConfig) constructor.newInstance(config, null, null));
        }
        catch (ReflectiveOperationException ex)
        {
            throw new IllegalStateException("Could not attach in-memory NeoForge config", ex);
        }
    }
}
