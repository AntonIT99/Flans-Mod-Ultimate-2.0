package com.flansmodultimate.packsmanager.platform;

import net.minecraftforge.fml.loading.FMLEnvironment;

/** Loader boundary for the standalone Packs Manager mod's production check. */
public final class PlatformEnvironment
{
    private PlatformEnvironment() {}

    public static boolean isProduction()
    {
        return FMLEnvironment.production;
    }
}
