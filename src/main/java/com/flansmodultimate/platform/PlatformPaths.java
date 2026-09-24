package com.flansmodultimate.platform;

import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;

/** Loader boundary for the game, config, and mods directories. */
public final class PlatformPaths
{
    private PlatformPaths() {}

    public static Path gameDir()
    {
        return FMLPaths.GAMEDIR.get();
    }

    public static Path configDir()
    {
        return FMLPaths.CONFIGDIR.get();
    }

    public static Path modsDir()
    {
        return FMLPaths.MODSDIR.get();
    }
}
