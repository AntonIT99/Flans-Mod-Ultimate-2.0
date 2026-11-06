package com.flansmodultimate.config;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Bakes a loaded or reloaded config file and resynchronizes the common values with connected clients. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ConfigBaking
{
    /** Called by the platform config events with the spec of the file that was loaded or reloaded. */
    public static void onConfigLoaded(Object spec)
    {
        if (spec == ModCommonConfig.configSpec)
        {
            ModCommonConfig.bake();
            ModCommonConfigSync.resyncAllClientsIfServer();
        }

        if (spec == ModApocalypseConfig.configSpec)
        {
            ModApocalypseConfig.bake();
            ModCommonConfigSync.resyncAllClientsIfServer();
        }

        if (spec == ModClientConfig.configSpec)
            ModClientConfig.bake();
    }
}
