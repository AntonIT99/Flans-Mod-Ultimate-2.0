package com.flansmodultimate.platform;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

/** Loader boundary for runtime side and production checks. */
public final class PlatformEnvironment
{
    private PlatformEnvironment() {}

    public static boolean isClient()
    {
        return FMLEnvironment.dist == Dist.CLIENT;
    }

    public static boolean isProduction()
    {
        return FMLEnvironment.production;
    }
}
