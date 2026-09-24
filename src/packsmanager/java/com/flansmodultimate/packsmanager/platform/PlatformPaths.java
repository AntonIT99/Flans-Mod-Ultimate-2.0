package com.flansmodultimate.packsmanager.platform;

import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

/** Loader boundary for the standalone Packs Manager mod's directories. */
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
}
