package com.flansmodultimate.platform;

import com.flansmodultimate.common.recipe.GunpowderRecipeCondition;
import com.flansmodultimate.config.ModApocalypseConfig;
import com.flansmodultimate.config.ModClientConfig;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.platform.network.NetworkPlatform;
import com.flansmodultimate.platform.world.ChunkTicketPlatform;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;

/**
 * Loader setup run from the mod constructor before registries and content: the mixin config, config files and the mod-bus
 * listeners that are not static subscribers. Forge adds the mixin config in code; NeoForge reads it from the mod metadata.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ModBootstrap
{
    /** Returns the mod event bus the registries attach to. */
    public static IEventBus initialize(IEventBus modEventBus, ModContainer modContainer)
    {
        GunpowderRecipeCondition.CODECS.register(modEventBus);
        modEventBus.addListener(NetworkPlatform::register);
        modEventBus.addListener(ChunkTicketPlatform::register);

        modContainer.registerConfig(ModConfig.Type.COMMON, ModCommonConfig.configSpec);
        modContainer.registerConfig(ModConfig.Type.COMMON, ModApocalypseConfig.configSpec, ModApocalypseConfig.CONFIG_FILE_NAME);
        modContainer.registerConfig(ModConfig.Type.CLIENT, ModClientConfig.configSpec);
        return modEventBus;
    }
}
