package com.flansmodultimate.event.handler;

import com.flansmodultimate.ContentManager;
import com.flansmodultimate.FlansMod;
import com.flansmodultimate.ModRepositorySource;
import com.flansmodultimate.config.ModClientConfig;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.config.ModCommonConfigSync;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

import java.nio.file.Files;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@net.neoforged.fml.common.EventBusSubscriber(modid = FlansMod.MOD_ID, bus = net.neoforged.fml.common.EventBusSubscriber.Bus.MOD)
public final class ModCommonEventHandler
{
    @SubscribeEvent
    public static void commonSetup(FMLCommonSetupEvent event)
    {
    }

    @SubscribeEvent
    public static void registerPack(AddPackFindersEvent event)
    {
        if (ContentManager.getFlanFolder() != null && Files.exists(ContentManager.getFlanFolder()))
        {
            event.addRepositorySource(new ModRepositorySource(ContentManager.getFlanFolder(), event.getPackType()));
        }
    }

    @SubscribeEvent
    public static void onConfigLoading(ModConfigEvent.Loading event)
    {
        if (event.getConfig().getSpec() == ModCommonConfig.configSpec)
        {
            ModCommonConfig.bake();
            ModCommonConfigSync.resyncAllClientsIfServer();
        }

        if (event.getConfig().getSpec() == ModClientConfig.configSpec)
            ModClientConfig.bake();
    }

    @SubscribeEvent
    public static void onConfigReloading(ModConfigEvent.Reloading event)
    {
        if (event.getConfig().getSpec() == ModCommonConfig.configSpec)
        {
            ModCommonConfig.bake();
            ModCommonConfigSync.resyncAllClientsIfServer();
        }

        if (event.getConfig().getSpec() == ModClientConfig.configSpec)
            ModClientConfig.bake();
    }
}