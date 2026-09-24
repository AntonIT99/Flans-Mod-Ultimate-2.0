package com.flansmodultimate.platform;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

import net.minecraft.server.MinecraftServer;

/** Loader boundary for runtime side, production, loaded-mod, and current-server queries. */
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

    public static boolean isModLoaded(String modId)
    {
        return ModList.get().isLoaded(modId);
    }

    /** The running integrated or dedicated server, or {@code null} when none is running. */
    @Nullable
    public static MinecraftServer currentServer()
    {
        return ServerLifecycleHooks.getCurrentServer();
    }
}
