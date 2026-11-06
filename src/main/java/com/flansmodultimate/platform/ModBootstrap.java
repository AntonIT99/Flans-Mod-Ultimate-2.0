package com.flansmodultimate.platform;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.config.ModApocalypseConfig;
import com.flansmodultimate.config.ModClientConfig;
import com.flansmodultimate.config.ModCommonConfig;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.spongepowered.asm.mixin.Mixins;

/**
 * Loader setup run from the mod constructor before registries and content: the mixin config, config files and the mod-bus
 * listeners that are not static subscribers. Forge adds the mixin config in code; NeoForge reads it from the mod metadata.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ModBootstrap
{
    /** Returns the mod event bus the registries attach to. */
    public static IEventBus initialize(FMLJavaModLoadingContext context)
    {
        Mixins.addConfiguration(FlansMod.MOD_ID + ".mixins.json");

        context.registerConfig(ModConfig.Type.COMMON, ModCommonConfig.configSpec);
        context.registerConfig(ModConfig.Type.COMMON, ModApocalypseConfig.configSpec, ModApocalypseConfig.CONFIG_FILE_NAME);
        context.registerConfig(ModConfig.Type.CLIENT, ModClientConfig.configSpec);
        return context.getModEventBus();
    }
}
