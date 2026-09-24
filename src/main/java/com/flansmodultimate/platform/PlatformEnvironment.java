package com.flansmodultimate.platform;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;

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
